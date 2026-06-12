package com.nexus.agent.protection;

import com.nexus.agent.core.AgentRequest;
import com.nexus.agent.core.AgentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConcurrencyGuardTest {

    private AgentConcurrencyGuard guard;

    @AfterEach
    void tearDown() {
        if (guard != null) {
            guard.shutdown();
        }
    }

    @Test
    void sameCallerIsRateLimitedInsideMinInterval() {
        guard = newGuard(1, 50, 1_000, 500, 5);

        AgentResponse first = guard.execute(request("s1"), "member:1", () -> ok("s1"));
        AgentResponse second = guard.execute(request("s1"), "member:1", () -> ok("s1"));

        assertThat(first.isDegraded()).isFalse();
        assertThat(second.isDegraded()).isTrue();
        assertThat(second.getDegradeReason()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void globalConcurrencyLimitFailsFastWhenLlmWorkersAreFull() throws Exception {
        guard = newGuard(1, 20, 2_000, 0, 5);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService callerPool = Executors.newSingleThreadExecutor();

        try {
            Future<AgentResponse> first = callerPool.submit(() -> guard.execute(request("s1"), "member:1", () -> {
                entered.countDown();
                await(release);
                return ok("s1");
            }));

            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            AgentResponse second = guard.execute(request("s2"), "member:2", () -> ok("s2"));

            assertThat(second.isDegraded()).isTrue();
            assertThat(second.getDegradeReason()).isEqualTo("SERVER_BUSY");

            release.countDown();
            assertThat(first.get(1, TimeUnit.SECONDS).isDegraded()).isFalse();
        } finally {
            release.countDown();
            callerPool.shutdownNow();
        }
    }

    @Test
    void circuitOpensAfterConsecutiveDegradedResponses() {
        guard = newGuard(1, 50, 1_000, 0, 1);

        AgentResponse first = guard.execute(request("s1"), "member:1", () -> degraded("s1", "LLM_EXECUTION_ERROR"));
        AgentResponse second = guard.execute(request("s2"), "member:2", () -> ok("s2"));

        assertThat(first.isDegraded()).isTrue();
        assertThat(second.isDegraded()).isTrue();
        assertThat(second.getDegradeReason()).isEqualTo("CIRCUIT_OPEN");
    }

    private AgentConcurrencyGuard newGuard(int maxConcurrent, long maxWaitMs, long timeoutMs,
                                           long perUserMinIntervalMs, int circuitFailureThreshold) {
        AgentProtectionProperties properties = new AgentProtectionProperties();
        properties.setEnabled(true);
        properties.setMaxConcurrentRequests(maxConcurrent);
        properties.setMaxWaitMs(maxWaitMs);
        properties.setRequestTimeoutMs(timeoutMs);
        properties.setPerUserMinIntervalMs(perUserMinIntervalMs);
        properties.setCircuitFailureThreshold(circuitFailureThreshold);
        properties.setCircuitOpenMs(1_000);

        AgentConcurrencyGuard newGuard = new AgentConcurrencyGuard(properties);
        newGuard.start();
        return newGuard;
    }

    private AgentRequest request(String sessionId) {
        AgentRequest request = new AgentRequest();
        request.setSessionId(sessionId);
        request.setMessage("hello");
        return request;
    }

    private AgentResponse ok(String sessionId) {
        AgentResponse response = new AgentResponse();
        response.setSessionId(sessionId);
        response.setResponse("ok");
        return response;
    }

    private AgentResponse degraded(String sessionId, String reason) {
        AgentResponse response = ok(sessionId);
        response.setDegraded(true);
        response.setDegradeReason(reason);
        return response;
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
