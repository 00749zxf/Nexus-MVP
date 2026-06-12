package com.nexus.concurrency;

import com.nexus.mapper.CartMapper;
import com.nexus.mapper.MemberMapper;
import com.nexus.mapper.ProductMapper;
import com.nexus.model.dto.CartDTO;
import com.nexus.model.dto.OrderDTO;
import com.nexus.model.entity.Cart;
import com.nexus.model.entity.Member;
import com.nexus.model.entity.Product;
import com.nexus.service.CartService;
import com.nexus.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "nexus.agent.use-llm=false",
        "nexus.agent.rag-enabled=false",
        "logging.level.com.nexus=INFO"
})
@EnabledIfSystemProperty(named = "nexus.integration.concurrency", matches = "true")
class ConcurrencyIntegrationTest {

    private static final int THREADS = 20;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String runId;
    private Member member;

    @BeforeEach
    void setUp() {
        runId = "concurrency_" + System.nanoTime();
        member = createMember(runId + "_user");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("""
                DELETE oi FROM oms_order_item oi
                JOIN oms_order o ON oi.order_id = o.id
                JOIN ums_member m ON o.member_id = m.id
                WHERE m.username LIKE ?
                """, runId + "%");
        jdbcTemplate.update("""
                DELETE o FROM oms_order o
                JOIN ums_member m ON o.member_id = m.id
                WHERE m.username LIKE ?
                """, runId + "%");
        jdbcTemplate.update("""
                DELETE c FROM oms_cart c
                JOIN ums_member m ON c.member_id = m.id
                WHERE m.username LIKE ?
                """, runId + "%");
        jdbcTemplate.update("DELETE FROM pms_product WHERE name LIKE ?", runId + "%");
        jdbcTemplate.update("DELETE FROM ums_member WHERE username LIKE ?", runId + "%");
    }

    @Test
    void concurrentDirectOrdersDoNotOversellSingleStockItem() throws Exception {
        Product product = createProduct(runId + "_stock_1", 1);

        AtomicInteger successCount = runConcurrently(THREADS, () -> {
            authenticate(member.getUsername());
            orderService.createOrderDirect(orderRequest(product.getId(), 1));
        });

        Product reloaded = productMapper.selectById(product.getId());
        Long orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oms_order WHERE member_id = ?",
                Long.class,
                member.getId()
        );

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(orderCount).isEqualTo(1L);
        assertThat(reloaded.getStock()).isZero();
    }

    @Test
    void concurrentAddToCartCreatesSingleRowAndDoesNotExceedStock() throws Exception {
        Product product = createProduct(runId + "_cart_stock_10", 10);

        AtomicInteger successCount = runConcurrently(THREADS, () -> {
            authenticate(member.getUsername());
            CartDTO dto = new CartDTO();
            dto.setProductId(product.getId());
            dto.setQuantity(1);
            dto.setSelected(true);
            cartService.addToCart(dto);
        });

        List<Cart> carts = cartMapper.selectByMemberId(member.getId());

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(carts).hasSize(1);
        assertThat(carts.get(0).getProductId()).isEqualTo(product.getId());
        assertThat(carts.get(0).getQuantity()).isEqualTo(10);
    }

    private AtomicInteger runConcurrently(int threads, CheckedRunnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // Failed attempts are expected in these race tests.
                } finally {
                    SecurityContextHolder.clearContext();
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        return successes;
    }

    private Member createMember(String username) {
        Member newMember = new Member();
        newMember.setUsername(username);
        newMember.setPassword("$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJI6Au2e");
        newMember.setPhone("139" + String.valueOf(System.nanoTime()).substring(4, 12));
        newMember.setEmail(username + "@example.com");
        newMember.setStatus(1);
        memberMapper.insert(newMember);
        return newMember;
    }

    private Product createProduct(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("concurrency test product");
        product.setPrice(BigDecimal.valueOf(99));
        product.setStock(stock);
        product.setCategoryId(1L);
        product.setStatus(1);
        productMapper.insert(product);
        return product;
    }

    private OrderDTO orderRequest(Long productId, int quantity) {
        OrderDTO dto = new OrderDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setReceiverName("Test User");
        dto.setReceiverPhone("13900000000");
        dto.setReceiverAddress("Concurrency Test Address");
        return dto;
    }

    private void authenticate(String username) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
