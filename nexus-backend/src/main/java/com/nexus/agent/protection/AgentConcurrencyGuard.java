package com.nexus.agent.protection;

import com.nexus.agent.core.AgentRequest;
import com.nexus.agent.core.AgentResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentConcurrencyGuard {

    private static final String RATE_LIMITED = "RATE_LIMITED";
    private static final String SERVER_BUSY = "SERVER_BUSY";
    private static final String CIRCUIT_OPEN = "CIRCUIT_OPEN";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String EXECUTION_ERROR = "EXECUTION_ERROR";
    private static final String INTERRUPTED = "INTERRUPTED";

    private final AgentProtectionProperties properties;

    private final ConcurrentHashMap<String, AtomicLong> lastRequestAt = new ConcurrentHashMap<>();
    private final AtomicLong lastThrottleCleanupAt = new AtomicLong();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong circuitOpenUntil = new AtomicLong();

    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong succeeded = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong timedOut = new AtomicLong();
    private final AtomicLong rejectedByRateLimit = new AtomicLong();
    private final AtomicLong rejectedByBusy = new AtomicLong();
    private final AtomicLong rejectedByCircuit = new AtomicLong();

    private Semaphore semaphore;
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        int maxConcurrent = Math.max(1, properties.getMaxConcurrentRequests());
        this.semaphore = new Semaphore(maxConcurrent);
        this.executor = Executors.newFixedThreadPool(maxConcurrent, new AgentThreadFactory());
        log.info("Agent concurrency guard started: maxConcurrent={}, timeoutMs={}, maxWaitMs={}",
                maxConcurrent, properties.getRequestTimeoutMs(), properties.getMaxWaitMs());
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public AgentResponse execute(AgentRequest request, String callerKey, Supplier<AgentResponse> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }

        long startedAt = System.currentTimeMillis();
        String normalizedKey = normalizeCallerKey(callerKey);

        if (isCircuitOpen()) {
            rejectedByCircuit.incrementAndGet();
            return fallback(request, CIRCUIT_OPEN, "AI客服暂时过载，正在自动恢复中，请稍后再试。", startedAt);
        }

        if (isRateLimited(normalizedKey)) {
            rejectedByRateLimit.incrementAndGet();
            return fallback(request, RATE_LIMITED, "您的提问太频繁了，请稍等一下再发送。", startedAt);
        }

        try {
            if (!semaphore.tryAcquire(Math.max(0, properties.getMaxWaitMs()), TimeUnit.MILLISECONDS)) {
                rejectedByBusy.incrementAndGet();
                return fallback(request, SERVER_BUSY, "当前咨询人数较多，AI客服正在排队处理，请稍后再试。", startedAt);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback(request, INTERRUPTED, "请求被中断，请稍后重试。", startedAt);
        }

        accepted.incrementAndGet();
        AtomicBoolean outcomeRecorded = new AtomicBoolean(false);
        Future<AgentResponse> future;
        try {
            future = executor.submit(() -> {
                try {
                    AgentResponse response = action.get();
                    recordOutcome(response, outcomeRecorded);
                    return response;
                } catch (Throwable e) {
                    recordFailureOnce(outcomeRecorded, EXECUTION_ERROR, e);
                    throw e;
                } finally {
                    semaphore.release();
                }
            });
        } catch (RejectedExecutionException e) {
            semaphore.release();
            recordFailureOnce(outcomeRecorded, EXECUTION_ERROR, e);
            return fallback(request, SERVER_BUSY, "AI客服线程池繁忙，请稍后再试。", startedAt);
        }

        try {
            return future.get(Math.max(1, properties.getRequestTimeoutMs()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timedOut.incrementAndGet();
            recordFailureOnce(outcomeRecorded, TIMEOUT, e);
            future.cancel(true);
            return fallback(request, TIMEOUT, "AI客服响应超时，已为您转入降级处理，请稍后再试或联系人工客服。", startedAt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailureOnce(outcomeRecorded, INTERRUPTED, e);
            future.cancel(true);
            return fallback(request, INTERRUPTED, "请求被中断，请稍后重试。", startedAt);
        } catch (ExecutionException e) {
            return fallback(request, EXECUTION_ERROR, "AI客服处理失败，请稍后重试或联系人工客服。", startedAt);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int maxConcurrent = Math.max(1, properties.getMaxConcurrentRequests());
        int availablePermits = semaphore == null ? maxConcurrent : semaphore.availablePermits();
        long now = System.currentTimeMillis();

        stats.put("enabled", properties.isEnabled());
        stats.put("maxConcurrentRequests", maxConcurrent);
        stats.put("inFlightRequests", maxConcurrent - availablePermits);
        stats.put("availablePermits", availablePermits);
        stats.put("circuitOpen", circuitOpenUntil.get() > now);
        stats.put("circuitOpenUntil", circuitOpenUntil.get());
        stats.put("consecutiveFailures", consecutiveFailures.get());
        stats.put("trackedCallerKeys", lastRequestAt.size());
        stats.put("accepted", accepted.get());
        stats.put("succeeded", succeeded.get());
        stats.put("failed", failed.get());
        stats.put("timedOut", timedOut.get());
        stats.put("rejectedByRateLimit", rejectedByRateLimit.get());
        stats.put("rejectedByBusy", rejectedByBusy.get());
        stats.put("rejectedByCircuit", rejectedByCircuit.get());
        return stats;
    }

    private void recordOutcome(AgentResponse response, AtomicBoolean outcomeRecorded) {
        if (response != null && response.isDegraded()) {
            recordFailureOnce(outcomeRecorded, response.getDegradeReason(), null);
            return;
        }
        if (outcomeRecorded.compareAndSet(false, true)) {
            consecutiveFailures.set(0);
            circuitOpenUntil.set(0);
            succeeded.incrementAndGet();
        }
    }

    private void recordFailureOnce(AtomicBoolean outcomeRecorded, String reason, Throwable throwable) {
        if (!outcomeRecorded.compareAndSet(false, true)) {
            return;
        }

        int failures = consecutiveFailures.incrementAndGet();
        failed.incrementAndGet();
        if (failures >= Math.max(1, properties.getCircuitFailureThreshold())) {
            long openUntil = System.currentTimeMillis() + Math.max(1, properties.getCircuitOpenMs());
            circuitOpenUntil.updateAndGet(current -> Math.max(current, openUntil));
            consecutiveFailures.set(0);
            log.warn("Agent circuit opened for {} ms after failure reason={}",
                    properties.getCircuitOpenMs(), reason, throwable);
        } else if (throwable != null) {
            log.warn("Agent request failed: reason={}, consecutiveFailures={}", reason, failures, throwable);
        } else {
            log.warn("Agent request degraded: reason={}, consecutiveFailures={}", reason, failures);
        }
    }

    private boolean isCircuitOpen() {
        return circuitOpenUntil.get() > System.currentTimeMillis();
    }

    private boolean isRateLimited(String callerKey) {
        long minInterval = properties.getPerUserMinIntervalMs();
        if (minInterval <= 0) {
            return false;
        }

        cleanupThrottleKeysIfNeeded();
        long now = System.currentTimeMillis();
        AtomicLong lastSeen = lastRequestAt.computeIfAbsent(callerKey, ignored -> new AtomicLong());

        while (true) {
            long previous = lastSeen.get();
            if (previous > 0 && now - previous < minInterval) {
                return true;
            }
            if (lastSeen.compareAndSet(previous, now)) {
                return false;
            }
        }
    }

    private void cleanupThrottleKeysIfNeeded() {
        if (lastRequestAt.size() <= Math.max(1, properties.getMaxThrottleKeys())) {
            return;
        }

        long now = System.currentTimeMillis();
        long previousCleanup = lastThrottleCleanupAt.get();
        if (now - previousCleanup < 10_000 || !lastThrottleCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }

        long cutoff = now - Math.max(properties.getThrottleKeyTtlMs(), properties.getPerUserMinIntervalMs() * 2);
        lastRequestAt.entrySet().removeIf(entry -> entry.getValue().get() < cutoff);
        if (lastRequestAt.size() > properties.getMaxThrottleKeys() * 2L) {
            lastRequestAt.clear();
            log.warn("Agent rate-limit key cache cleared because it exceeded the safety bound");
        }
    }

    private AgentResponse fallback(AgentRequest request, String reason, String message, long startedAt) {
        AgentResponse response = new AgentResponse();
        response.setSessionId(resolveSessionId(request));
        response.setResponse(message);
        response.setNeedHumanSupport(true);
        response.setDegraded(true);
        response.setDegradeReason(reason);
        response.setResponseTime(System.currentTimeMillis() - startedAt);
        return response;
    }

    private String resolveSessionId(AgentRequest request) {
        if (request != null && request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId();
        }
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private String normalizeCallerKey(String callerKey) {
        if (callerKey == null || callerKey.isBlank()) {
            return "anonymous";
        }
        return callerKey.trim();
    }

    private static class AgentThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "agent-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
