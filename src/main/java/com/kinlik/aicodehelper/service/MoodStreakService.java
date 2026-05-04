package com.kinlik.aicodehelper.service;

import com.kinlik.aicodehelper.entity.MoodRecord;
import com.kinlik.aicodehelper.entity.UserAchievement;
import com.kinlik.aicodehelper.entity.WeeklyGoal;
import com.kinlik.aicodehelper.repository.MoodRecordRepository;
import com.kinlik.aicodehelper.repository.UserAchievementRepository;
import com.kinlik.aicodehelper.repository.WeeklyGoalRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MoodStreakService {

    @Resource
    private MoodRecordRepository moodRecordRepository;
    @Resource
    private UserAchievementRepository userAchievementRepository;
    @Resource
    private WeeklyGoalRepository weeklyGoalRepository;

    // 徽章定义
    private static final List<Map<String, Object>> BADGES = List.of(
            Map.of("id", "starter", "name", "初识自我", "icon", "🌱", "requirement", "连续记录3天"),
            Map.of("id", "awareness", "name", "自我觉察者", "icon", "🔍", "requirement", "连续记录7天"),
            Map.of("id", "week_warrior", "name", "周度坚持者", "icon", "📅", "requirement", "连续记录7天"),
            Map.of("id", "sunshine", "name", "阳光心态", "icon", "☀️", "requirement", "累计30次积极情绪"),
            Map.of("id", "warrior", "name", "情绪勇士", "icon", "💪", "requirement", "从消极情绪中恢复5次"),
            Map.of("id", "master", "name", "情绪大师", "icon", "👑", "requirement", "连续记录30天")
    );

    // 连续打卡里程碑
    private static final int[] MILESTONES = {3, 7, 14, 30, 60, 90};

    // 正能量语录
    private static final List<String> ENCOURAGEMENT_QUOTES = List.of(
            "每一次记录，都是对自己的温柔关照 🌸",
            "你的感受很重要，值得被认真对待 💝",
            "今天的你，比昨天更了解自己 ✨",
            "情绪没有对错，接纳就是成长 🌈",
            "你已经做得很好了，继续加油！💪",
            "记录本身，就是一种勇气 🦋",
            "你今天迈出了重要的一步 🌟",
            "对自己温柔，也是一种力量 💫"
    );

    public MoodStreakResponse getMoodStreak(String username) {
        List<MoodRecord> records = moodRecordRepository
                .findByUsernameOrderByRecordDateDesc(username);

        int currentStreak = calculateCurrentStreak(records);
        int bestStreak = calculateBestStreak(records);
        List<MoodDaySummary> last90Days = buildLast90Days(records);
        Map<String, Object> nextMilestone = getNextMilestone(currentStreak);
        List<Map<String, Object>> unlockedBadges = getUnlockedBadges(username);
        List<Map<String, Object>> allBadges = getAllBadgesWithStatus(username);
        String quote = getRandomQuote();

        return new MoodStreakResponse(
                currentStreak, bestStreak, last90Days,
                (int) nextMilestone.get("days"), (String) nextMilestone.get("badge"),
                unlockedBadges, allBadges, quote
        );
    }

    public int calculateCurrentStreak(List<MoodRecord> records) {
        if (records.isEmpty()) return 0;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        MoodRecord latest = records.get(0);
        LocalDate lastDate = latest.getRecordDate();

        if (lastDate.isBefore(yesterday)) return 0;

        int streak = 1;
        for (int i = 1; i < records.size(); i++) {
            if (records.get(i - 1).getRecordDate().minusDays(1)
                    .equals(records.get(i).getRecordDate())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public int calculateBestStreak(List<MoodRecord> records) {
        if (records.isEmpty()) return 0;
        List<MoodRecord> sorted = records.stream()
                .sorted(Comparator.comparing(MoodRecord::getRecordDate))
                .toList();

        int best = 1, current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).getRecordDate().plusDays(1)
                    .equals(sorted.get(i).getRecordDate())) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 1;
            }
        }
        return best;
    }

    private List<MoodDaySummary> buildLast90Days(List<MoodRecord> records) {
        Map<LocalDate, MoodRecord> map = records.stream()
                .collect(Collectors.toMap(MoodRecord::getRecordDate, r -> r, (a, b) -> a));
        List<MoodDaySummary> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 89; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            MoodRecord r = map.get(d);
            String level = r == null ? "none"
                    : (r.getHighRisk() != null && r.getHighRisk() ? "negative"
                    : (r.getPositiveEmotion() != null && r.getPositiveEmotion() ? "positive" : "neutral"));
            days.add(new MoodDaySummary(d.toString(), r != null, level,
                    r != null ? r.getEmotionType() : null,
                    r != null ? r.getEmotionIntensity() : null));
        }
        return days;
    }

    private Map<String, Object> getNextMilestone(int currentStreak) {
        for (int m : MILESTONES) {
            if (currentStreak < m) {
                return Map.of("days", m - currentStreak, "badge", getMilestoneBadgeName(m));
            }
        }
        return Map.of("days", 0, "badge", "已达成最高里程碑！");
    }

    private String getMilestoneBadgeName(int days) {
        return switch (days) {
            case 3 -> "初识自我 🌱";
            case 7 -> "自我觉察者 🔍";
            case 14 -> "周度坚持者 📅";
            case 30 -> "情绪大师 👑";
            case 60 -> "双月坚持者 🏆";
            case 90 -> "季度英雄 🌟";
            default -> "新成就";
        };
    }

    public List<Map<String, Object>> getUnlockedBadges(String username) {
        return userAchievementRepository.findByUsernameOrderByUnlockedAtDesc(username)
                .stream().map(a -> {
                    Map<String, Object> badge = getBadgeDef(a.getBadgeId());
                    badge.put("unlockedAt", a.getUnlockedAt());
                    badge.put("viewed", a.isViewed());
                    return badge;
                }).toList();
    }

    public List<Map<String, Object>> getAllBadgesWithStatus(String username) {
        Set<String> unlocked = userAchievementRepository
                .findByUsernameOrderByUnlockedAtDesc(username)
                .stream().map(UserAchievement::getBadgeId).collect(Collectors.toSet());
        return BADGES.stream().map(b -> {
            Map<String, Object> badge = new HashMap<>(b);
            badge.put("unlocked", unlocked.contains(b.get("id")));
            return badge;
        }).toList();
    }

    private Map<String, Object> getBadgeDef(String badgeId) {
        return new HashMap<>(BADGES.stream()
                .filter(b -> b.get("id").equals(badgeId))
                .findFirst().orElse(Map.of("id", badgeId, "name", badgeId, "icon", "🎖")));
    }

    public void checkAndUnlockBadges(String username) {
        List<MoodRecord> records = moodRecordRepository
                .findByUsernameOrderByRecordDateDesc(username);
        int streak = calculateCurrentStreak(records);
        int positiveCount = (int) records.stream()
                .filter(r -> r.getPositiveEmotion() != null && r.getPositiveEmotion()).count();
        int recoveryCount = countRecoveries(records);

        checkAndUnlock(username, "starter", streak >= 3);
        checkAndUnlock(username, "awareness", streak >= 7);
        checkAndUnlock(username, "master", streak >= 30);
        checkAndUnlock(username, "sunshine", positiveCount >= 30);
        checkAndUnlock(username, "warrior", recoveryCount >= 5);
    }

    private int countRecoveries(List<MoodRecord> records) {
        if (records.size() < 2) return 0;
        int count = 0;
        for (int i = 1; i < records.size(); i++) {
            MoodRecord prev = records.get(i - 1);
            MoodRecord curr = records.get(i);
            boolean prevNegative = prev.getHighRisk() != null && prev.getHighRisk()
                    || (prev.getPositiveEmotion() != null && !prev.getPositiveEmotion());
            boolean currPositive = curr.getPositiveEmotion() != null && curr.getPositiveEmotion();
            if (prevNegative && currPositive) count++;
        }
        return count;
    }

    private void checkAndUnlock(String username, String badgeId, boolean condition) {
        if (condition && !userAchievementRepository.existsByUsernameAndBadgeId(username, badgeId)) {
            UserAchievement achievement = new UserAchievement();
            achievement.setUsername(username);
            achievement.setBadgeId(badgeId);
            achievement.setViewed(false);
            userAchievementRepository.save(achievement);
            log.info("解锁新徽章: username={}, badgeId={}", username, badgeId);
        }
    }

    // ===== 本周目标 =====
    public WeeklyGoalResponse getWeeklyGoal(String username) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        Optional<WeeklyGoal> existing = weeklyGoalRepository.findByUsernameAndWeekStart(username, weekStart);

        if (existing.isPresent()) {
            WeeklyGoal goal = existing.get();
            return new WeeklyGoalResponse(goal.getType().name(),
                    goal.getTargetCount(), goal.getCurrentCount(), goal.isCompleted());
        }

        WeeklyGoal goal = new WeeklyGoal();
        goal.setUsername(username);
        goal.setWeekStart(weekStart);
        goal.setType(suggestGoalType(username));
        goal.setTargetCount(3);
        goal.setCurrentCount(0);
        goal.setCompleted(false);
        weeklyGoalRepository.save(goal);

        return new WeeklyGoalResponse(goal.getType().name(),
                goal.getTargetCount(), goal.getCurrentCount(), goal.isCompleted());
    }

    private WeeklyGoal.GoalType suggestGoalType(String username) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        List<WeeklyGoal> history = weeklyGoalRepository.findByUsernameOrderByWeekStartDesc(username);
        return history.isEmpty() ? WeeklyGoal.GoalType.GRATITUDE : history.get(0).getType();
    }

    public WeeklyGoalResponse recordWeeklyProgress(String username) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        WeeklyGoal goal = weeklyGoalRepository.findByUsernameAndWeekStart(username, weekStart)
                .orElseGet(() -> {
                    WeeklyGoal g = new WeeklyGoal();
                    g.setUsername(username);
                    g.setWeekStart(weekStart);
                    g.setType(WeeklyGoal.GoalType.GRATITUDE);
                    g.setTargetCount(3);
                    return g;
                });

        goal.setCurrentCount(goal.getCurrentCount() + 1);
        if (goal.getCurrentCount() >= goal.getTargetCount()) {
            goal.setCompleted(true);
            goal.setCompletedAt(LocalDateTime.now());
        }
        weeklyGoalRepository.save(goal);
        checkAndUnlockBadges(username);

        return new WeeklyGoalResponse(goal.getType().name(),
                goal.getTargetCount(), goal.getCurrentCount(), goal.isCompleted());
    }

    public String getRandomQuote() {
        return ENCOURAGEMENT_QUOTES.get(new Random().nextInt(ENCOURAGEMENT_QUOTES.size()));
    }

    // ===== 记忆管理 =====
    public List<MemoryFragmentView> getUserMemories(String username) {
        return userAchievementRepository.findByUsernameOrderByUnlockedAtDesc(username);
    }

    // Records
    public record MoodStreakResponse(
            int currentStreak, int bestStreak, List<MoodDaySummary> last90Days,
            int nextMilestone, String nextBadge,
            List<Map<String, Object>> unlockedBadges, List<Map<String, Object>> allBadges,
            String quote
    ) {}

    public record MoodDaySummary(String date, boolean hasRecord, String level,
                                  String emotionType, Integer intensity) {}

    public record WeeklyGoalResponse(String type, int targetCount, int currentCount, boolean completed) {}
}
