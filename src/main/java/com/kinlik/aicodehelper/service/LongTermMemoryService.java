package com.kinlik.aicodehelper.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LongTermMemoryService {

    @Value("${chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${chroma.collection-name:companion_memory}")
    private String collectionName;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    private ChromaEmbeddingStore embeddingStore;

    @PostConstruct
    public void init() {
        try {
            embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .build();
            log.info("Chroma向量数据库初始化完成: {}", chromaUrl);
        } catch (Exception e) {
            log.warn("Chroma初始化失败，记忆检索功能将暂时不可用。请检查 Docker 服务及版本兼容性: {}", e.getMessage());
            embeddingStore = null;
        }
    }

    public void storeMemory(String username, String content, String category, Double importance) {
        if (embeddingStore == null) {
            log.warn("Chroma未初始化，跳过记忆存储");
            return;
        }

        try {
            Metadata metadata = new Metadata();
            metadata.put("username", username);
            metadata.put("category", category);
            metadata.put("importance", importance.toString());
            metadata.put("timestamp", LocalDateTime.now().toString());

            TextSegment segment = TextSegment.from(content, metadata);

            Embedding embedding = qwenEmbeddingModel.embed(content).content();
            embeddingStore.add(embedding, segment);

            log.info("记忆存储成功: username={}, category={}", username, category);
        } catch (Exception e) {
            log.error("记忆存储失败", e);
        }
    }

    public List<String> retrieveRelevantMemories(String username, String query, int maxResults) {
        if (embeddingStore == null) {
            log.warn("Chroma未初始化，返回空记忆列表");
            return List.of();
        }

        try {
            Embedding queryEmbedding = qwenEmbeddingModel.embed(query).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.7)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

            List<String> memories = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                String memUsername = match.embedded().metadata().getString("username");
                if (username.equals(memUsername)) {
                    memories.add(match.embedded().text());
                }
            }

            return memories;
        } catch (Exception e) {
            log.error("记忆检索失败", e);
            return List.of();
        }
    }

    public void storeUserPreference(String username, String preference) {
        storeMemory(username, preference, "preference", 0.8);
    }

    public void storeUserExperience(String username, String experience) {
        storeMemory(username, experience, "experience", 0.9);
    }

    public void storeUserHabit(String username, String habit) {
        storeMemory(username, habit, "habit", 0.7);
    }
}
