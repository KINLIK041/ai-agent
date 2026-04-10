package com.kinlik.aicodehelper.service;

import com.kinlik.aicodehelper.entity.MoodRecord;
import com.kinlik.aicodehelper.entity.UserProfile;
import com.kinlik.aicodehelper.repository.MoodRecordRepository;
import com.kinlik.aicodehelper.repository.UserProfileRepository;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ProactiveCareService {

    @Value("${companion.user.name:KINLIK}")
    private String defaultUsername;

    @Resource
    private UserProfileRepository userProfileRepository;

    @Resource
    private MoodRecordRepository moodRecordRepository;

    @Resource
    private LongTermMemoryService longTermMemoryService;

    @Resource
    private ChatModel chatModel;

    @Scheduled(cron = "${companion.emotion-check-cron:0 0 22 * * ?}")
    public void dailyMoodCheck() {
        log.info("执行每日晚间情绪关怀任务");
        UserProfile profile = userProfileRepository.findByUsername(defaultUsername)
                .orElseGet(() -> createDefaultProfile(defaultUsername));
        String checkMessage = generateMoodCheckMessage(profile);
        log.info("晚间情绪提醒: {}", checkMessage);
    }

    public String generateProactiveSuggestion(String username, String context) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseGet(() -> createDefaultProfile(username));

        List<String> recentMemories = longTermMemoryService.retrieveRelevantMemories(username, context, 3);
        String memoryContext = recentMemories.isEmpty() ? "" : "相关记忆:\n" + String.join("\n", recentMemories);

        String prompt = String.format("""
            你是一位私人情感支持助理，请基于以下信息生成一段简短、温柔、个性化的陪伴建议。

            用户画像：
            - 姓名：%s
            - MBTI：%s
            - 性格特点：%s
            - 偏好：%s

            %s

            当前情境：%s

            输出要求：
            1. 先接住情绪，再给建议。
            2. 语言像真实朋友，不要官话。
            3. 建议要轻量、具体、能立刻执行。
            4. 不要提及任何第三方博主、课程或品牌。
            """,
                profile.getUsername(),
                profile.getMbti(),
                profile.getPersonalityTraits(),
                profile.getPreferences(),
                memoryContext,
                context
        );

        return chatModel.chat(prompt);
    }

    public String generateMoodCheckMessage(UserProfile profile) {
        LocalDate today = LocalDate.now();
        boolean hasTodayRecord = moodRecordRepository
                .findByUsernameAndRecordDate(profile.getUsername(), today)
                .isPresent();

        if (hasTodayRecord) {
            return profile.getUsername() + "，今晚的情绪已经记下来了。如果你还想多说一点，我会继续陪你。";
        }

        List<MoodRecord> recentRecords = moodRecordRepository
                .findByUsernameOrderByRecordDateDesc(profile.getUsername())
                .stream()
                .limit(3)
                .toList();

        String moodTrend = analyzeMoodTrend(recentRecords);

        return String.format("""
            %s，晚上十点啦。
            %s
            如果愿意的话，告诉我今天你的心情、最触动你的一件事，或者此刻最想被怎样安慰。
            我会认真记住，也会好好陪你。
            """,
                profile.getUsername(),
                moodTrend
        ).trim();
    }

    private String analyzeMoodTrend(List<MoodRecord> records) {
        if (records.isEmpty()) {
            return "这是我们第一次做晚间情绪记录，今晚就从你的真实感受开始。";
        }

        double avgIntensity = records.stream()
                .mapToInt(MoodRecord::getEmotionIntensity)
                .average()
                .orElse(5.0);

        if (avgIntensity >= 7) {
            return "我留意到你最近情绪起伏有点明显，今晚我们慢一点，也对自己温柔一点。";
        }
        if (avgIntensity <= 3) {
            return "最近你的状态相对平稳，这很难得，也值得被认真看见。";
        }
        return "这几天你的情绪有一些细小波动，今晚可以把心里的感受慢慢说给我听。";
    }

    private UserProfile createDefaultProfile(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setMbti("INTP");
        profile.setPersonalityTraits("理性、细腻、偶尔内耗，需要被温柔理解和稳定陪伴");
        profile.setPreferences("偏好安静真诚的交流，不喜欢被生硬说教");
        profile.setLearningGoals("在成长中保持节奏感，也照顾好自己的情绪和能量");
        return userProfileRepository.save(profile);
    }

    public String handleNegativeEmotion(String username,
                                        String message,
                                        EmotionAnalysisService.EmotionResult emotion) {
        if (!emotion.needsSupport() || !"负面".equals(emotion.sentiment())) {
            return null;
        }

        String careResponse = generateProactiveSuggestion(
                username,
                "用户当前情绪：" + emotion.emotion() + "，强度：" + emotion.intensity() + "；用户表达：" + message
        );

        log.info("检测到负面情绪，生成陪伴建议: {}", careResponse);
        return careResponse;
    }
}
