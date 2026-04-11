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
    @Resource
    private WeatherService weatherService;

    @Scheduled(cron = "${companion.emotion-check-cron:0 0 22 * * ?}")
    public void dailyMoodCheck() {
        UserProfile profile = userProfileRepository.findByUsername(defaultUsername)
                .orElseGet(() -> createDefaultProfile(defaultUsername));
        log.info("晚间情绪提醒: {}", generateMoodCheckMessage(profile));
    }

    public String generateProactiveSuggestion(String username, String context) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseGet(() -> createDefaultProfile(username));
        List<String> recentMemories = longTermMemoryService.retrieveRelevantMemories(username, context, 3);
        String memoryContext = recentMemories.isEmpty() ? "" : "相关记忆:\n" + String.join("\n", recentMemories);
        String prompt = String.format(
                "用户画像：%s；偏好：%s\n%s\n当前情境：%s\n请生成一段温柔、轻量、可执行的陪伴建议。",
                profile.getPersonalityTraits(),
                profile.getPreferences(),
                memoryContext,
                context
        );
        return chatModel.chat(prompt);
    }

    public String generateMoodCheckMessage(UserProfile profile) {
        LocalDate today = LocalDate.now();
        boolean hasTodayRecord = moodRecordRepository.findByUsernameAndRecordDate(profile.getUsername(), today).isPresent();
        if (hasTodayRecord) {
            return profile.getUsername() + "，今晚的情绪已经记下来了。如果你还想多说一点，我会继续陪你。";
        }
        return profile.getUsername() + "，晚上十点啦。如果愿意的话，告诉我今天你的心情、最触动你的一件事，或者此刻最想被怎样安慰。";
    }

    public String handleNegativeEmotion(String username, String message, EmotionAnalysisService.EmotionResult emotion) {
        if (emotion.highRisk()) {
            return "我注意到你现在可能处于高风险情绪状态。请优先联系身边可信任的人，或立即寻求专业帮助：心理援助热线 12356 / 24小时危机干预热线。如你愿意，我也可以先陪你做一次非常简短的呼吸稳定练习。";
        }
        if (!emotion.needsSupport() || !"负面".equals(emotion.sentiment())) {
            return null;
        }
        return generateProactiveSuggestion(
                username,
                "用户当前情绪：" + emotion.emotion() + "，强度：" + emotion.intensity() + "；向量：" + emotion.vector() + "；用户表达：" + message
        );
    }

    public String generateMorningBrief(String username, MoodRecord latestMood, String city) {
        WeatherService.WeatherSummary weather = weatherService.getTodayWeather(city);
        String moodText = latestMood == null
                ? "昨晚没有记录到明确情绪"
                : "昨晚你记录的是「" + latestMood.getEmotionType() + "」，强度 " + latestMood.getEmotionIntensity() + "/10";
        return username + "，早安。" + moodText + "。今天 " + weather.city() + " 天气 " + weather.description() + "，" + weather.suggestion() + "。";
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
}
