package com.kinlik.aicodehelper.service;

import com.kinlik.aicodehelper.entity.MoodRecord;
import com.kinlik.aicodehelper.repository.MoodRecordRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MoodOverviewService {

    @Resource
    private MoodRecordRepository moodRecordRepository;

    public MoodOverview getOverview(String username, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(days - 1, 0));
        List<MoodRecord> records = moodRecordRepository.findByUsernameAndRecordDateBetween(username, startDate, endDate)
                .stream()
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .toList();

        String summary = buildSummary(records, days);
        List<MoodRecordView> views = records.stream()
                .map(record -> new MoodRecordView(
                        record.getRecordDate(),
                        record.getEmotionType(),
                        record.getEmotionIntensity(),
                        record.getEmotionDescription(),
                        record.getTriggerEvent(),
                        record.getAiSuggestion()
                ))
                .toList();
        return new MoodOverview(summary, views);
    }

    private String buildSummary(List<MoodRecord> records, int days) {
        if (records.isEmpty()) {
            return "最近" + days + "天还没有情绪记录，可以从今晚开始慢慢积累你的情绪轨迹。";
        }

        double avgIntensity = records.stream()
                .mapToInt(MoodRecord::getEmotionIntensity)
                .average()
                .orElse(5.0);

        String topEmotion = records.stream()
                .collect(Collectors.groupingBy(MoodRecord::getEmotionType, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("平静");

        return String.format("最近%d天共记录%d次情绪，最常出现的是“%s”，平均情绪强度为%.1f/10。",
                days,
                records.size(),
                topEmotion,
                avgIntensity);
    }

    public record MoodOverview(String summary, List<MoodRecordView> records) {
    }

    public record MoodRecordView(LocalDate recordDate,
                                 String emotionType,
                                 Integer emotionIntensity,
                                 String emotionDescription,
                                 String triggerEvent,
                                 String aiSuggestion) {
    }
}
