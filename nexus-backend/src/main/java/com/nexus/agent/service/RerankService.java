package com.nexus.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Rerank 重排服务
 * 对向量检索的 Top-K 结果按与问题的实际相关度二次打分，过滤低质量结果
 * 使用阿里云百炼 gte-rerank 模型
 */
@Slf4j
@Component
public class RerankService {

    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/rerank";

    @Value("${spring.ai.openai.embedding.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 对候选文本列表重排，返回按相关度降序排列的文本
     *
     * @param query      用户问题
     * @param candidates 向量检索出的候选文本列表
     * @param topN       重排后保留几条
     */
    public List<String> rerank(String query, List<String> candidates, int topN) {
        if (candidates.isEmpty()) return candidates;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "gte-rerank",
                    "query", query,
                    "documents", candidates,
                    "top_n", Math.min(topN, candidates.size()),
                    "return_documents", false
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(RERANK_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> results =
                        (List<Map<String, Object>>) response.getBody().get("results");

                // results 已经按 relevance_score 降序排好了
                return results.stream()
                        .filter(r -> {
                            Double score = (Double) r.get("relevance_score");
                            return score != null && score > 0.3;  // 过滤掉相关度极低的
                        })
                        .map(r -> {
                            Integer index = (Integer) r.get("index");
                            return candidates.get(index);
                        })
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Rerank 调用失败，降级使用原始顺序: {}", e.getMessage());
        }

        // 降级：Rerank 失败时直接返回原始列表
        return candidates.subList(0, Math.min(topN, candidates.size()));
    }
}