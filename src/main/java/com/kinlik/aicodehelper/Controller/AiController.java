package com.kinlik.aicodehelper.Controller;

import com.kinlik.aicodehelper.ai.AiCodeHelperService;
import com.kinlik.aicodehelper.entity.ChatSession;
import com.kinlik.aicodehelper.repository.ChatSessionRepository;
import com.kinlik.aicodehelper.service.*;
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
    @Resource
    private MoodStreakService moodStreakService;
    @Resource
    private MemoryService memoryService;
    @Resource
    private NotificationHandler notificationHandler;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @PostMapping("/companion/chat")
    public ResponseEntity<Map<String, Object>> companionChat(@RequestBody ChatRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";
        CompanionAgentService.CompanionResponse response = companionAgentService.chat(username, request.message());
        return ResponseEntity.ok(Map.of(
                "message", response.message(),
                "careAdvice", response.careAdvice() == null ? "" : response.careAdvice(),
                "rememberedContext", response.rememberedContext() == null ? "" : response.rememberedContext(),
                "weather", Map.of(
                        "city", response.weather().city(),
                        "description", response.weather().description(),
                        "temperature", response.weather().temperature(),
                        "humidity", response.weather().humidity(),
                        "suggestion", response.weather().suggestion()
                ),
                "emotion", Map.of(
                        "type", response.emotion().emotion(),
                        "intensity", response.emotion().intensity(),
                        "sentiment", response.emotion().sentiment(),
                        "needsSupport", response.emotion().needsSupport(),
                        "vector", response.emotion().vector(),
                        "positive", response.emotion().positive(),
                        "highRisk", response.emotion().highRisk()
                )
        ));
    }

    @PostMapping("/companion/mood")
    public ResponseEntity<Map<String, Object>> recordMood(@RequestBody MoodRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";
        var record = companionAgentService.recordDailyMood(username, new CompanionAgentService.MoodInput(request.moodDescription(), request.triggerEvent()));
        return ResponseEntity.ok(Map.of(
                "message", "情绪记录成功",
                "recordDate", record.getRecordDate(),
                "emotionType", record.getEmotionType(),
                "emotionIntensity", record.getEmotionIntensity(),
                "emotionVector", record.getEmotionVector(),
                "positiveEmotion", record.getPositiveEmotion(),
                "highRisk", record.getHighRisk(),
                "aiSuggestion", record.getAiSuggestion()
        ));
    }

    @GetMapping("/companion/check-in")
    public ResponseEntity<Map<String, Object>> getNightlyCheckIn(@RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        return ResponseEntity.ok(Map.of(
                "username", actualUsername,
                "checkInMessage", companionAgentService.buildNightlyCheckInMessage(actualUsername)
        ));
    }

    @GetMapping("/companion/care")
    public ResponseEntity<String> getProactiveCare(@RequestParam String username, @RequestParam String context) {
        return ResponseEntity.ok(proactiveCareService.generateProactiveSuggestion(username, context));
    }

    @GetMapping("/companion/mood/overview")
    public ResponseEntity<Map<String, Object>> moodOverview(@RequestParam(required = false) String username, @RequestParam(defaultValue = "7") int days) {
        String actualUsername = username != null ? username : "KINLIK";
        MoodOverviewService.MoodOverview overview = moodOverviewService.getOverview(actualUsername, days);
        return ResponseEntity.ok(Map.of("username", actualUsername, "days", days, "summary", overview.summary(), "records", overview.records()));
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
        session.setMessagesJson(request.messagesJson());
        session.setPinned(request.pinned() != null ? request.pinned() : Boolean.FALSE);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        return ResponseEntity.ok(Map.of("success", true, "message", "会话保存成功"));
    }

    @GetMapping("/session/list")
    public ResponseEntity<List<ChatSession>> getSessionList(@RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        return ResponseEntity.ok(chatSessionRepository.findByUsernameOrderByUpdatedAtDesc(actualUsername));
    }

    @GetMapping("/session/detail/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionDetail(@PathVariable String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) return ResponseEntity.ok(Map.of("success", false, "message", "会话不存在"));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", session.getSessionId(),
                "username", session.getUsername(),
                "title", session.getTitle(),
                "lastMessage", session.getLastMessage(),
                "messagesJson", session.getMessagesJson() == null ? "[]" : session.getMessagesJson(),
                "pinned", session.getPinned() == null ? false : session.getPinned(),
                "updatedAt", session.getUpdatedAt()
        ));
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

    public record ChatRequest(String username, String message) {}
    public record MoodRequest(String username, String moodDescription, String triggerEvent) {}
    public record SessionRequest(String username, String sessionId, String title, String lastMessage, String messagesJson, Boolean pinned) {}

    // ===== 情绪打卡连续天数 & 成就系统 =====
    @GetMapping("/companion/mood/streak")
    public ResponseEntity<MoodStreakService.MoodStreakResponse> getMoodStreak(
            @RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        MoodStreakService.MoodStreakResponse streak = moodStreakService.getMoodStreak(actualUsername);
        return ResponseEntity.ok(streak);
    }

    @PostMapping("/companion/mood/check-in")
    public ResponseEntity<Map<String, Object>> recordMoodCheckIn(@RequestBody MoodRequest request) {
        String username = request.username() != null ? request.username() : "KINLIK";
        var record = companionAgentService.recordDailyMood(username,
                new CompanionAgentService.MoodInput(request.moodDescription(), request.triggerEvent()));
        moodStreakService.checkAndUnlockBadges(username);
        moodStreakService.recordWeeklyProgress(username);
        return ResponseEntity.ok(Map.of(
                "message", "打卡成功",
                "recordDate", record.getRecordDate(),
                "emotionType", record.getEmotionType(),
                "emotionIntensity", record.getEmotionIntensity(),
                "positiveEmotion", record.getPositiveEmotion(),
                "highRisk", record.getHighRisk(),
                "quote", moodStreakService.getRandomQuote(),
                "streak", moodStreakService.getMoodStreak(username)
        ));
    }

    // ===== 本周目标 =====
    @GetMapping("/companion/goal/current")
    public ResponseEntity<MoodStreakService.WeeklyGoalResponse> getWeeklyGoal(
            @RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        return ResponseEntity.ok(moodStreakService.getWeeklyGoal(actualUsername));
    }

    @PostMapping("/companion/goal/record")
    public ResponseEntity<MoodStreakService.WeeklyGoalResponse> recordGoalProgress(
            @RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        return ResponseEntity.ok(moodStreakService.recordWeeklyProgress(actualUsername));
    }

    // ===== AI 记忆管理 =====
    @GetMapping("/memory/list")
    public ResponseEntity<List<MemoryService.MemoryFragmentView>> getMemories(
            @RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        return ResponseEntity.ok(memoryService.getMemories(actualUsername));
    }

    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Map<String, Object>> deleteMemory(
            @PathVariable Long id,
            @RequestParam(required = false) String username) {
        String actualUsername = username != null ? username : "KINLIK";
        memoryService.deleteMemory(id, actualUsername);
        return ResponseEntity.ok(Map.of("success", true, "message", "记忆已删除"));
    }

    // ===== WebSocket 通知注册 =====
    @PostMapping("/notification/register")
    public ResponseEntity<Map<String, Object>> registerNotification(
            @RequestParam String username,
            @RequestParam(required = false) String token) {
        return ResponseEntity.ok(Map.of("success", true, "message", "通知注册成功"));
    }

    // ===== 危机热线资源 =====
    @GetMapping("/companion/crisis-resources")
    public ResponseEntity<Map<String, Object>> getCrisisResources() {
        List<Map<String, String>> contacts = List.of(
                Map.of("name", "心理危机干预中心", "phone", "010-82951332", "hours", "24小时"),
                Map.of("name", "希望24热线", "phone", "400-161-9995", "hours", "24小时"),
                Map.of("name", "青少年服务热线", "phone", "12355", "hours", "全天候"),
                Map.of("name", "全国卫生热线", "phone", "12320", "hours", "工作日")
        );
        List<String> quotes = List.of(
                "你并不孤单，愿意帮助你的人就在这里。",
                "此刻的痛苦只是暂时的，一切都会好起来的。",
                "寻求帮助是勇敢的表现，不是软弱。"
        );
        return ResponseEntity.ok(Map.of("contacts", contacts, "calmingQuotes", quotes));
    }
}
