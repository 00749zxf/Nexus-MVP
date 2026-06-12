package com.nexus.agent.protection;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nexus.agent.protection")
public class AgentProtectionProperties {

    private boolean enabled = true;

    private int maxConcurrentRequests = 20;

    private long maxWaitMs = 200;

    private long requestTimeoutMs = 30_000;

    private long perUserMinIntervalMs = 800;

    private int circuitFailureThreshold = 5;

    private long circuitOpenMs = 30_000;

    private int maxThrottleKeys = 10_000;

    private long throttleKeyTtlMs = 60_000;
}
