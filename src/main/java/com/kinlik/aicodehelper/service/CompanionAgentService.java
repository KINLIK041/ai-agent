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
    @Resource
    private WeatherService weatherService;
    @Resource
    private MoodStreakService moodStreakService;
    @Resource
    private MemoryService memoryService;

    public CompanionResponse chat(String username, String userMessage) {
        UserProfile profile = getOrCreateProfile(username);
        EmotionAnalysisService.EmotionResult emotion = emotionAnalysisService.analyzeEmotion(userMessage);
        extractAndStoreMemories(username, userMessage, emotion);
        List<String> relevantMemories = longTermMemoryService.retrieveRelevantMemories(username, userMessage, 5);
        String systemPrompt = buildPersonalizedSystemPrompt(profile, emotion, relevantMemories);
        String aiResponse = chatModel.chat(systemPrompt + "\n\n用户消息: " + userMessage);
        String careAdvice = emotion.needsSupport() ? proactiveCareService.handleNegativeEmotion(username, userMessage, emotion) : null;
        WeatherService.WeatherSummary weather = weatherService.getTodayWeather("Beijing");
        List<String> memoryHints = !relevantMemories.isEmpty()
                ? relevantMemories.subList(0, Math.min(2, relevantMemories.size())) : List.of();
        return new CompanionResponse(aiResponse, emotion, careAdvice, weather, memoryHints.isEmpty() ? null : memoryHints.get(0));
    }

    public MoodRecord recordDailyMood(String username, MoodInput moodInput) {
        EmotionAnalysisService.EmotionResult emotion = emotionAnalysisService.analyzeEmotion(moodInput.moodDescription());
        MoodRecord record = moodRecordRepository.findByUsernameAndRecordDate(username, LocalDate.now()).orElseGet(MoodRecord::new);
        record.setUsername(username);
        record.setRecordDate(LocalDate.now());
        record.setEmotionType(emotion.emotion());
        record.setEmotionIntensity(emotion.intensity());
        record.setEmotionDescription(moodInput.moodDescription());
        record.setTriggerEvent(moodInput.triggerEvent());
        record.setAiSuggestion(proactiveCareService.generateProactiveSuggestion(username, buildMoodContext(moodInput)));
        record.setEmotionVector(emotion.vector());
        record.setPositiveEmotion(emotion.positive());
        record.setHighRisk(emotion.highRisk());
        MoodRecord savedRecord = moodRecordRepository.save(record);
        longTermMemoryService.storeMemory(username, "情绪记录：" + buildMoodContext(moodInput) + "；向量：" + emotion.vector(), "mood", 0.95);
        syncLatestMoodSnapshot(username, emotion, moodInput);
        moodStreakService.checkAndUnlockBadges(username);
        return savedRecord;
    }

    public String buildNightlyCheckInMessage(String username) {
        return proactiveCareService.generateMoodCheckMessage(getOrCreateProfile(username));
    }

    private String buildMoodContext(MoodInput moodInput) {
        StringBuilder context = new StringBuilder("今天的情绪：").append(moodInput.moodDescription());
        if (StringUtils.hasText(moodInput.triggerEvent())) context.append("；主要触发事件：").append(moodInput.triggerEvent());
        return context.toString();
    }

    private void syncLatestMoodSnapshot(String username, EmotionAnalysisService.EmotionResult emotion, MoodInput moodInput) {
        MemoryFragment fragment = memoryFragmentRepository.findTopByUsernameAndCategoryOrderByCreatedAtDesc(username, "latest_mood").orElseGet(MemoryFragment::new);
        fragment.setUsername(username); fragment.setCategory("latest_mood"); fragment.setImportance(1.0); fragment.setEventTime(LocalDateTime.now());
        fragment.setContent("最新情绪类型：" + emotion.emotion() + "；强度：" + emotion.intensity() + "；向量：" + emotion.vector() + "；情绪描述：" + moodInput.moodDescription());
        memoryFragmentRepository.save(fragment);
    }

    private UserProfile getOrCreateProfile(String username) {
        return userProfileRepository.findByUsername(username).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUsername(username); profile.setMbti("INTP"); profile.setPersonalityTraits("理性、敏感、容易想很多，需要稳定而真诚的陪伴"); profile.setPreferences("喜欢安静的交流方式，希望被理解而不是被说教"); profile.setLearningGoals("提升专业能力，同时保持情绪稳定和内心能量");
            return userProfileRepository.save(profile);
        });
    }

    private void extractAndStoreMemories(String username, String message, EmotionAnalysisService.EmotionResult emotion) {
        MemoryFragment fragment = new MemoryFragment();
        fragment.setUsername(username); fragment.setContent(message + "；情绪向量：" + emotion.vector()); fragment.setCategory("conversation"); fragment.setImportance(emotion.intensity() / 10.0); fragment.setEventTime(LocalDateTime.now());
        memoryFragmentRepository.save(fragment);
    }

    private String buildPersonalizedSystemPrompt(UserProfile profile, EmotionAnalysisService.EmotionResult emotion, List<String> relevantMemories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位长期陪伴用户的私人情感支持助理，采用ReAct风格：先理解，再行动，再反馈。\n");
        prompt.append("用户：").append(profile.getUsername()).append("，性格：").append(profile.getPersonalityTraits()).append("\n");
        prompt.append("当前情绪：").append(emotion.emotion()).append("，强度：").append(emotion.intensity()).append("/10，向量：").append(emotion.vector()).append("\n");
        if (!relevantMemories.isEmpty()) prompt.append("相关长期记忆：\n").append(String.join("\n", relevantMemories)).append("\n");
        prompt.append("回复原则：优先共情，建议轻量具体；若检测到高危情绪，先提供安全引导与热线信息。\n");
        return prompt.toString();
    }

    public record CompanionResponse(String message, EmotionAnalysisService.EmotionResult emotion, String careAdvice, WeatherService.WeatherSummary weather, String rememberedContext) {}
    public record MoodInput(String moodDescription, String triggerEvent) {}
}
