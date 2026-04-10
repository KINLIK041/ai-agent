package com.kinlik.aicodehelper.service;

import com.kinlik.aicodehelper.entity.MemoryFragment;
import com.kinlik.aicodehelper.entity.MoodRecord;
import com.kinlik.aicodehelper.entity.UserProfile;
import com.kinlik.aicodehelper.repository.MemoryFragmentRepository;
import com.kinlik.aicodehelper.repository.MoodRecordRepository;
import com.kinlik.aicodehelper.repository.UserProfileRepository;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CompanionAgentService {

    @Resource
    private UserProfileRepository userProfileRepository;

    @Resource
    private MoodRecordRepository moodRecordRepository;

    @Resource
    private MemoryFragmentRepository memoryFragmentRepository;

    @Resource
    private LongTermMemoryService longTermMemoryService;

    @Resource
    private EmotionAnalysisService emotionAnalysisService;

    @Resource
    private ProactiveCareService proactiveCareService;

    @Resource
    private ChatModel chatModel;

    public CompanionResponse chat(String username, String userMessage) {
        long start = System.currentTimeMillis();
        log.info("开始处理陪伴聊天: username={}", username);

        UserProfile profile = getOrCreateProfile(username);
        EmotionAnalysisService.EmotionResult emotion = emotionAnalysisService.analyzeEmotion(userMessage);
        log.info("情感分析完成，耗时: {}ms", System.currentTimeMillis() - start);

        extractAndStoreMemories(username, userMessage, emotion);

        List<String> relevantMemories = longTermMemoryService.retrieveRelevantMemories(username, userMessage, 5);
        log.info("记忆检索完成，耗时: {}ms", System.currentTimeMillis() - start);

        String systemPrompt = buildPersonalizedSystemPrompt(profile, emotion, relevantMemories);
        log.info("准备调用聊天模型...");
        String aiResponse = chatModel.chat(systemPrompt + "\n\n用户消息: " + userMessage);
        log.info("聊天模型返回完成，总耗时: {}ms", System.currentTimeMillis() - start);

        String careAdvice = null;
        if (emotion.needsSupport()) {
            careAdvice = proactiveCareService.handleNegativeEmotion(username, userMessage, emotion);
        }

        return new CompanionResponse(aiResponse, emotion, careAdvice);
    }

    public MoodRecord recordDailyMood(String username, MoodInput moodInput) {
        log.info("记录用户情绪: username={}, moodDescription={}", username, moodInput.moodDescription());

        EmotionAnalysisService.EmotionResult emotion = emotionAnalysisService.analyzeEmotion(moodInput.moodDescription());

        MoodRecord record = moodRecordRepository.findByUsernameAndRecordDate(username, LocalDate.now())
                .orElseGet(MoodRecord::new);
        record.setUsername(username);
        record.setRecordDate(LocalDate.now());
        record.setEmotionType(emotion.emotion());
        record.setEmotionIntensity(emotion.intensity());
        record.setEmotionDescription(moodInput.moodDescription());
        record.setTriggerEvent(moodInput.triggerEvent());

        String context = buildMoodContext(moodInput);
        String suggestion = proactiveCareService.generateProactiveSuggestion(username, context);
        record.setAiSuggestion(suggestion);

        MoodRecord savedRecord = moodRecordRepository.save(record);

        longTermMemoryService.storeMemory(username, "情绪记录：" + context, "mood", 0.95);
        syncLatestMoodSnapshot(username, emotion, moodInput);

        log.info("情绪记录成功: username={}, emotion={}", username, emotion.emotion());
        return savedRecord;
    }
    public String buildNightlyCheckInMessage(String username) {
        UserProfile profile = getOrCreateProfile(username);
        return proactiveCareService.generateMoodCheckMessage(profile);
    }

    private String buildMoodContext(MoodInput moodInput) {
        StringBuilder context = new StringBuilder("今天的情绪：").append(moodInput.moodDescription());
        if (StringUtils.hasText(moodInput.triggerEvent())) {
            context.append("；主要触发事件：").append(moodInput.triggerEvent());
        }
        return context.toString();
    }

    private void syncLatestMoodSnapshot(String username,
                                        EmotionAnalysisService.EmotionResult emotion,
                                        MoodInput moodInput) {
        MemoryFragment fragment = memoryFragmentRepository
                .findTopByUsernameAndCategoryOrderByCreatedAtDesc(username, "latest_mood")
                .orElseGet(MemoryFragment::new);
        fragment.setUsername(username);
        fragment.setCategory("latest_mood");
        fragment.setImportance(1.0);
        fragment.setEventTime(LocalDateTime.now());
        fragment.setContent(buildMoodSnapshotContent(emotion, moodInput));
        memoryFragmentRepository.save(fragment);
    }

    private String buildMoodSnapshotContent(EmotionAnalysisService.EmotionResult emotion, MoodInput moodInput) {
        StringBuilder content = new StringBuilder();
        content.append("最新情绪类型：").append(emotion.emotion())
                .append("；强度：").append(emotion.intensity())
                .append("；情绪描述：").append(moodInput.moodDescription());
        if (StringUtils.hasText(moodInput.triggerEvent())) {
            content.append("；触发事件：").append(moodInput.triggerEvent());
        }
        return content.toString();
    }

    private UserProfile getOrCreateProfile(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUsername(username);
                    profile.setMbti("INTP");
                    profile.setPersonalityTraits("理性、敏感、容易想很多，需要稳定而真诚的陪伴");
                    profile.setPreferences("喜欢安静的交流方式，希望被理解而不是被说教");
                    profile.setLearningGoals("提升专业能力，同时保持情绪稳定和内心能量");
                    return userProfileRepository.save(profile);
                });
    }

    private void extractAndStoreMemories(String username, String message, EmotionAnalysisService.EmotionResult emotion) {
        if (message.contains("我想学") || message.contains("我要学") || message.contains("计划学")) {
            longTermMemoryService.storeUserPreference(username, "学习目标: " + message);
        }

        if (message.contains("我喜欢") || message.contains("我不喜欢")) {
            longTermMemoryService.storeUserPreference(username, "偏好: " + message);
        }

        if (message.contains("我今天") || message.contains("昨天我")) {
            longTermMemoryService.storeUserExperience(username, "经历: " + message);
        }

        MemoryFragment fragment = new MemoryFragment();
        fragment.setUsername(username);
        fragment.setContent(message);
        fragment.setCategory("conversation");
        fragment.setImportance(emotion.intensity() / 10.0);
        fragment.setEventTime(LocalDateTime.now());
        memoryFragmentRepository.save(fragment);
    }

    private String buildPersonalizedSystemPrompt(UserProfile profile,
                                                 EmotionAnalysisService.EmotionResult emotion,
                                                 List<String> relevantMemories) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一位长期陪伴用户的私人情感支持助理，名字叫暮光陪伴。\n");
        prompt.append("你的目标是记录、理解并温柔回应用户每天的情绪变化，像值得信任的老朋友一样陪伴他。\n\n");

        prompt.append("用户信息:\n");
        prompt.append("- 姓名: ").append(profile.getUsername()).append("\n");
        prompt.append("- MBTI: ").append(profile.getMbti()).append("\n");
        prompt.append("- 性格: ").append(profile.getPersonalityTraits()).append("\n");
        prompt.append("- 陪伴偏好: ").append(profile.getPreferences()).append("\n\n");

        prompt.append("当前情绪状态:\n");
        prompt.append("- 情绪类型: ").append(emotion.emotion()).append("\n");
        prompt.append("- 情绪强度: ").append(emotion.intensity()).append("/10\n");
        prompt.append("- 情感倾向: ").append(emotion.sentiment()).append("\n\n");

        if (!relevantMemories.isEmpty()) {
            prompt.append("相关历史记忆:\n");
            relevantMemories.forEach(memory -> prompt.append("- ").append(memory).append("\n"));
            prompt.append("\n");
        }

        prompt.append("回复原则:\n");
        prompt.append("1. 优先共情，先理解用户感受，再给建议。\n");
        prompt.append("2. 语气温柔、自然、真诚，不要像客服或老师。\n");
        prompt.append("3. 多使用短句，避免堆砌大道理。\n");
        prompt.append("4. 如果用户情绪低落，提供轻量、可执行的陪伴建议。\n");
        prompt.append("5. 可以结合历史记忆体现熟悉感，但不要编造经历。\n");
        prompt.append("6. 避免输出任何与求职教程或他人品牌相关的话题。\n");

        return prompt.toString();
    }

    public record CompanionResponse(String message,
                                    EmotionAnalysisService.EmotionResult emotion,
                                    String careAdvice) {
    }

    public record MoodInput(String moodDescription, String triggerEvent) {
    }
}
