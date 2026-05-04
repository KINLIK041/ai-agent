package com.kinlik.aicodehelper.service;

import com.kinlik.aicodehelper.entity.MemoryFragment;
import com.kinlik.aicodehelper.repository.MemoryFragmentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MemoryService {

    @Resource
    private MemoryFragmentRepository memoryFragmentRepository;

    public List<MemoryFragmentView> getMemories(String username) {
        return memoryFragmentRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream().limit(50)
                .map(m -> new MemoryFragmentView(
                        m.getId(), m.getContent(), m.getCategory(),
                        m.getImportance(), m.getEventTime(), m.getCreatedAt()
                )).toList();
    }

    public void deleteMemory(Long id, String username) {
        memoryFragmentRepository.findById(id).ifPresent(m -> {
            if (m.getUsername().equals(username)) {
                memoryFragmentRepository.delete(m);
                log.info("删除记忆: id={}, username={}", id, username);
            }
        });
    }

    public List<String> getRecentMemoryHints(String username, int maxResults) {
        return memoryFragmentRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream().limit(maxResults)
                .map(MemoryFragment::getContent)
                .collect(Collectors.toList());
    }

    public record MemoryFragmentView(
            Long id, String content, String category,
            Double importance, LocalDateTime eventTime, LocalDateTime createdAt
    ) {}
}
