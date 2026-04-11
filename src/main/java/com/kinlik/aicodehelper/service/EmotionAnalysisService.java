package com.kinlik.aicodehelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmotionAnalysisService {

    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmotionResult analyzeEmotion(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new EmotionResult("平静", 5, "中性", false, "0.10,0.10,0.80", false, false);
        }

        try {
            String systemPrompt = """
                你是一个情感分析专家。请分析用户文本的情感状态，严格返回JSON格式：
                {
                  "emotion": "开心/焦虑/疲惫/兴奋/沮丧/平静/愤怒/担忧/绝望/自残倾向",
                  "intensity": 1-10整数,
                  "sentiment": "正面/负面/中性",
                  "needs_support": true或false
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> input = new HashMap<>();
            input.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", text)
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-max");
            requestBody.put("input", input);
            requestBody.put("parameters", Map.of("result_format", "message"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("output").path("choices").get(0).path("message").path("content").asText();
            JsonNode emotionJson = objectMapper.readTree(content);

            String emotion = emotionJson.path("emotion").asText("平静");
            int intensity = emotionJson.path("intensity").asInt(5);
            String sentiment = emotionJson.path("sentiment").asText("中性");
            boolean support = emotionJson.path("needs_support").asBoolean(false);
            boolean positive = "正面".equals(sentiment);
            boolean highRisk = emotion.contains("绝望") || emotion.contains("自残") || text.contains("自残") || text.contains("不想活") || text.contains("绝望");
            String vector = buildEmotionVector(emotion, intensity, sentiment);

            return new EmotionResult(emotion, intensity, sentiment, support, vector, positive, highRisk);
        } catch (HttpClientErrorException e) {
            return new EmotionResult("平静", 5, "中性", false, "0.10,0.10,0.80", false, false);
        } catch (Exception e) {
            return new EmotionResult("平静", 5, "中性", false, "0.10,0.10,0.80", false, false);
        }
    }

    private String buildEmotionVector(String emotion, int intensity, String sentiment) {
        double positive = "正面".equals(sentiment) ? intensity / 10.0 : 0.1;
        double negative = "负面".equals(sentiment) ? intensity / 10.0 : 0.1;
        double stable = "中性".equals(sentiment) ? intensity / 10.0 : Math.max(0.1, 1 - Math.max(positive, negative));
        return String.format("%.2f,%.2f,%.2f", positive, negative, stable);
    }

    public record EmotionResult(String emotion, int intensity, String sentiment, boolean needsSupport, String vector, boolean positive, boolean highRisk) {}
}
