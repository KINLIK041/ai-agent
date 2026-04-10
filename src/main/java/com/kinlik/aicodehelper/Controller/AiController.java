package com.kinlik.aicodehelper.Controller;

import com.kinlik.aicodehelper.ai.AiCodeHelperService;
import com.kinlik.aicodehelper.entity.ChatSession;
import com.kinlik.aicodehelper.repository.ChatSessionRepository;
import com.kinlik.aicodehelper.service.CompanionAgentService;
import com.kinlik.aicodehelper.service.MoodOverviewService;
import com.kinlik.aicodehelper.service.ProactiveCareService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private CompanionAgentService companionAgentService;

    @Resource
    private ProactiveCareService proactiveCareService;

    @Resource
    private MoodOverviewService moodOverviewService;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @PostMapping("/companion/chat")
    public ResponseEntity<Map<String, Object>> companionChat(@RequestBody ChatRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";
        CompanionAgentService.CompanionResponse response =
                companionAgentService.chat(username, request.message());

        return ResponseEntity.ok(Map.of(
                "message", response.message(),
                "careAdvice", response.careAdvice() == null ? "" : response.careAdvice(),
                "emotion", Map.of(
                        "type", response.emotion().emotion(),
                        "intensity", response.emotion().intensity(),
                        "sentiment", response.emotion().sentiment(),
                        "needsSupport", response.emotion().needsSupport()
                )
        ));
    }

    @PostMapping("/companion/mood")
    public ResponseEntity<Map<String, Object>> recordMood(@RequestBody MoodRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";
        var record = companionAgentService.recordDailyMood(
                username,
                new CompanionAgentService.MoodInput(request.moodDescription(), request.triggerEvent())
        );
        return ResponseEntity.ok(Map.of(
                "message", "情绪记录成功",
                "recordDate", record.getRecordDate(),
                "emotionType", record.getEmotionType(),
                "aiSuggestion", record.getAiSuggestion()
        ));
    }

    @GetMapping("/companion/check-in")
    public ResponseEntity<Map<String, Object>> getNightlyCheckIn(@RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        String checkInMessage = companionAgentService.buildNightlyCheckInMessage(actualUsername);
        return ResponseEntity.ok(Map.of(
                "username", actualUsername,
                "checkInMessage", checkInMessage
        ));
    }

    @GetMapping("/companion/care")
    public ResponseEntity<String> getProactiveCare(@RequestParam String username,
                                                   @RequestParam String context) {
        String suggestion = proactiveCareService.generateProactiveSuggestion(username, context);
        return ResponseEntity.ok(suggestion);
    }

    @GetMapping("/companion/mood/overview")
    public ResponseEntity<Map<String, Object>> moodOverview(@RequestParam(required = false) String username,
                                                            @RequestParam(defaultValue = "7") int days) {
        String actualUsername = username != null ? username : "KINLIK";
        MoodOverviewService.MoodOverview overview = moodOverviewService.getOverview(actualUsername, days);
        return ResponseEntity.ok(Map.of(
                "username", actualUsername,
                "days", days,
                "summary", overview.summary(),
                "records", overview.records()
        ));
    }

    @PostMapping("/session/save")
    public ResponseEntity<Map<String, Object>> saveSession(@RequestBody SessionRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";

        ChatSession session = chatSessionRepository.findBySessionId(request.sessionId());
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(request.sessionId());
            session.setUsername(username);
            session.setCreatedAt(LocalDateTime.now());
        }

        session.setTitle(request.title());
        session.setLastMessage(request.lastMessage());
        session.setUpdatedAt(LocalDateTime.now());

        chatSessionRepository.save(session);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "会话保存成功"
        ));
    }

    @GetMapping("/session/list")
    public ResponseEntity<List<ChatSession>> getSessionList(@RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        List<ChatSession> sessions = chatSessionRepository.findByUsernameOrderByUpdatedAtDesc(actualUsername);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/session/delete/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        if (session != null) {
            chatSessionRepository.delete(session);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "会话不存在"));
    }

    public record ChatRequest(String username, String message) {
    }

    public record MoodRequest(String username, String moodDescription, String triggerEvent) {
    }

    public record SessionRequest(String username, String sessionId, String title, String lastMessage) {
    }
}
