package com.kinlik.aicodehelper.repository;

import com.kinlik.aicodehelper.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUsernameOrderByUpdatedAtDesc(String username);
    ChatSession findBySessionId(String sessionId);
    void deleteByUsername(String username);
}
