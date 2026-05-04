# AI MATE 用户粘滞性功能增强需求

## 项目背景
AI MATE 是一个基于 Vue 3 + Spring Boot 的情感陪伴 AI 助手，已有基础的情绪记录、聊天对话、长期记忆功能。现在需要增强用户粘滞性，实现游戏化、个性化和主动关怀功能。

## 技术栈
- **前端**：Vue 3 (Composition API) + Vite
- **后端**：Spring Boot + JPA
- **数据库**：H2（开发）/ PostgreSQL（生产）
- **现有服务**：EmotionAnalysisService, LongTermMemoryService, MoodRecordRepository

---

## 功能需求清单

### 一、情绪打卡日历系统（P0 优先级）

#### 1.1 GitHub 风格贡献图
**前端实现要求：**
vue
<!-- 在 ai-code-helper-mobile/src/App.vue 的情绪面板中添加 -->
<div class="streak-calendar"> <div class="streak-header"> <span class="streak-count">🔥 {{ currentStreak }} 天连续记录</span> <span class="streak-best">历史最高: {{ bestStreak }} 天</span> </div>
<div class="calendar-grid"> <!-- 展示最近 90 天的情绪记录情况 --> <div v-for="day in last90Days" :key="day.date" class="calendar-day" :class="getDayClass(day)" :title="`${day.date}: ${day.mood || '无记录'}`" > <span v-if="day.hasRecord" class="check-icon">✓</span> </div> </div>
<!-- 进度条显示距离下一个成就还差几天 -->
<div v-if="nextMilestone > 0" class="milestone-progress"> <p>再坚持 {{ nextMilestone }} 天解锁「{{ nextBadge }}」徽章！</p> <progress :value="currentStreak % 7" max="7"></progress> </div> </div>
**CSS 样式要求：**
- 使用液态玻璃风格（与现有 design system 一致）
- 不同情绪等级对应不同颜色强度：
  - 积极情绪：绿色渐变（rgba(48, 209, 88, 0.3) → 0.9）
  - 平稳情绪：灰色渐变（rgba(120, 120, 128, 0.2) → 0.6）
  - 消极情绪：橙色/红色渐变
  - 无记录：透明背景 + 虚线边框
- 今天日期添加脉冲动画（pulse animation）
- 悬停/点击显示 tooltip（情绪类型 + 强度）

**后端数据接口：**
java // 新增 endpoint: GET /ai/companion/mood/streak?username={username} @GetMapping("/mood/streak") public ResponseEntity<MoodStreakResponse> getMoodStreak(@RequestParam String username) { // 返回： // - currentStreak: 当前连续天数 // - bestStreak: 历史最长连续天数 // - last90Days: List<MoodDaySummary> // - nextMilestone: 距离下一个成就的天数 // - nextBadge: 下一个徽章名称 }
**计算逻辑：**
java // 连续天数算法 public int calculateCurrentStreak(String username) { List<MoodRecord> records = moodRepo.findByUsernameOrderByRecordDateDesc(username); if (records.isEmpty()) return 0;
LocalDate today = LocalDate.now();
LocalDate lastDate = records.get(0).getRecordDate();

// 如果最后一次记录不是昨天或今天，连续中断
if (lastDate.isBefore(today.minusDays(1))) return 0;

int streak = 1;
for (int i = 1; i < records.size(); i++) {
if (records.get(i-1).getRecordDate().minusDays(1)
.equals(records.get(i).getRecordDate())) {
streak++;
} else {
break;
}
}
return streak;
}
#### 1.2 成就徽章系统
**徽章定义：**
avascript const BADGES = [ { id: 'starter', name: '初识自我', icon: '🌱', requirement: '连续记录3天' }, { id: 'awareness', name: '自我觉察者', icon: '🔍', requirement: '连续记录7天' }, { id: 'sunshine', name: '阳光心态', icon: '☀️', requirement: '累计30次积极情绪' }, { id: 'warrior', name: '情绪勇士', icon: '💪', requirement: '从消极情绪中恢复5次' }, { id: 'master', name: '情绪大师', icon: '👑', requirement: '连续记录30天' } ];
**UI 展示：**
- 在"我的"页面添加"成就墙"网格布局
- 已解锁徽章：彩色图标 + 解锁日期
- 未解锁徽章：灰度图标 + 进度提示
- 新解锁时触发庆祝动画（confetti effect）

---

### 二、鼓励动画与正能量语录（P0 优先级）

#### 2.1 记录后即时反馈
**交互流程：**
用户提交情绪记录 ↓ 显示全屏覆盖层（0.5s 淡入） ↓ 播放粒子动画（emoji 飘落：✨🌟💫） ↓ 显示随机正能量语录（打字机效果） ↓ 显示本次连续天数提示 ↓ 0.8s 后自动关闭，返回主界面
**语录库示例：**
javascript const ENCOURAGEMENT_QUOTES = [ "每一次记录，都是对自己的温柔关照 🌸", "你的感受很重要，值得被认真对待 💝", "今天的你，比昨天更了解自己 ✨", "情绪没有对错，接纳就是成长 🌈", "你已经做得很好了，继续加油！💪", "记录本身，就是一种勇气 🦋" ];
**动画实现：**
css @keyframes emojiRain { 0% { transform: translateY(-100vh) rotate(0deg); opacity: 1; } 100% { transform: translateY(100vh) rotate(360deg); opacity: 0; } }
.emoji-particle { position: fixed; font-size: 24px; animation: emojiRain 2s ease-out forwards; pointer-events: none; z-index: 9999; }
.encouragement-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; animation: fadeIn 0.5s ease; }
---

### 三、本周情绪目标（P1 优先级）

#### 3.1 目标设定与追踪
**数据结构：**
java @Entity public class WeeklyGoal { @Id @GeneratedValue private Long id; private String username; private LocalDate weekStart; // 本周一日期 private String goalType; // "gratitude", "mindfulness", "exercise" private int targetCount; // 目标次数 private int currentCount; // 当前完成次数 private boolean completed; }
**前端展示：**
vue
<div class="weekly-goal-card"> <h3>📅 本周目标</h3> <div class="goal-item"> <span class="goal-icon">🙏</span> <div class="goal-info"> <span class="goal-title">记录3次感恩时刻</span> <progress :value="gratitudeCount" :max="3"></progress> <span class="goal-progress">{{ gratitudeCount }}/3</span> </div> </div>
<button v-if="!goalCompleted" class="complete-goal-btn" @click="recordGratitude"> 记录感恩时刻 </button>
</div>
**智能推荐逻辑：**
java // 根据用户历史行为推荐下周目标 public WeeklyGoal suggestNextWeekGoal(String username) { var lastWeekStats = moodRepo.getWeeklyStats(username, lastWeek);
if (lastWeekStats.getNegativeRatio() > 0.5) {
    return new WeeklyGoal("gratitude", 3); // 感恩练习
} else if (lastWeekStats.getRecordCount() < 3) {
    return new WeeklyGoal("consistency", 5); // 提升记录频率
} else {
    return new WeeklyGoal("mindfulness", 7); // 正念练习
}
}
---

### 四、AI 记忆展示与管理（P0 优先级）

#### 4.1 聊天界面"AI 记得"标签
**UI 设计：**
vue
<!-- 在 AI 回复气泡上方添加小标签 -->
<div v-if="message.rememberedContext" class="memory-tag"> <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"> <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/> </svg> <span>AI 记得你说过：{{ message.rememberedContext }}</span> </div>
**CSS 样式：**
css .memory-tag { display: inline-flex; align-items: center; gap: 4px; padding: 4px 8px; margin-bottom: 6px; background: rgba(10, 132, 255, 0.1); border: 0.5px solid rgba(10, 132, 255, 0.3); border-radius: 12px; font-size: 11px; color: var(--accent); animation: slideDown 0.3s ease; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
**后端支持：**
java // 修改 CompanionAgentService，在回复中包含引用的记忆片段 public CompanionResponse chat(CompanionRequest request) { List<MemoryFragment> relevantMemories = memoryService.findRelevant(request.getMessage());
String contextHint = relevantMemories.isEmpty() ? null :
    "你之前提到过：" + relevantMemories.get(0).getContent();

return new CompanionResponse(
aiReply,
emotionAnalysis,
weatherInfo,
careAdvice,
contextHint // 新增字段
);
}
#### 4.2 记忆管理页面
**功能需求：**
vue
<!-- 在设置页面添加"记忆管理"入口 -->
<div class="memory-management"> <h3>🧠 AI 记住的内容</h3> <p class="memory-hint">你可以查看或删除 AI 记住的关于你的信息</p>
<div v-for="memory in memories" :key="memory.id" class="memory-item"> <div class="memory-content">{{ memory.content }}</div> <div class="memory-meta"> <span class="memory-date">{{ formatDate(memory.createdAt) }}</span> <button class="delete-btn" @click="deleteMemory(memory.id)">删除</button> </div> </div>
<div v-if="memories.length === 0" class="empty-memories"> AI 还没有记住任何内容 </div> </div>
**API 接口：**
java @GetMapping("/memory/list") public List<MemoryFragment> getUserMemories(@RequestParam String username);
@DeleteMapping("/memory/{id}") public void deleteMemory(@PathVariable Long id, @RequestParam String username);
---

### 五、场景化主动关怀推送（P0 优先级）

#### 5.1 定时任务调度
**后端实现：**
java @Component public class CareScheduler {
@Autowired private ProactiveCareService careService;
@Autowired private WebSocketNotificationService wsService;

// 每天早上 7:00-9:00 发送早安问候
@Scheduled(cron = "0 0 7-9 * * MON-FRI")
public void sendMorningGreeting() {
List<UserProfile> activeUsers = userRepo.findActiveUsers();

    for (UserProfile user : activeUsers) {
        WeatherInfo weather = weatherService.getCurrent(user.getLocation());
        String quote = quoteService.getRandomPositiveQuote();
        
        PushNotification notification = new PushNotification(
            "早安，" + user.getUsername() + " ☀️",
            String.format("今日%s，气温%d°C。\n\n💭 %s", 
                weather.getDescription(), 
                weather.getTemperature(), 
                quote),
            NotificationType.MORNING_GREETING
        );
        
        wsService.sendToUser(user.getUsername(), notification);
    }
}

// 每晚 22:00-24:00 引导晚安仪式
@Scheduled(cron = "0 0 22-23 * * *")
public void sendNightRitual() {
// 推送"记录今日三件好事"提醒
}

// 周一上午 9:00 推送本周目标
@Scheduled(cron = "0 0 9 * * MON")
public void sendWeeklyGoal() {
// 生成本周目标并推送
}

// 周五下午 17:00 推送本周回顾
@Scheduled(cron = "0 0 17 * * FRI")
public void sendWeeklyReview() {
// 汇总本周对话要点和情绪趋势
}
}
#### 5.2 前端通知接收
javascript // 在 App.vue onMounted 中建立 WebSocket 连接 onMounted(async () => { // ... existing code ...
// 建立实时通知连接 const wsUrl = ws://localhost:8081/ws/notifications?username=${username.value}; const ws = new WebSocket(wsUrl);
ws.onmessage = (event) => { const notification = JSON.parse(event.data); showPushNotification(notification); }; });
function showPushNotification(notification) { // 显示顶部横幅通知（类似 iOS 推送） const toast = document.createElement('div'); toast.className = push-notification ${notification.type}; toast.innerHTML = <strong>${notification.title}</strong> <p>${notification.body}</p> <button @click="handleNotificationAction('${notification.action}')"> ${notification.actionText || '查看'} </button>;
document.body.appendChild(toast); setTimeout(() => toast.remove(), 8000); // 8秒后自动消失 }
---

### 六、高危情绪急救包（P0 优先级）

#### 6.1 呼吸练习引导
**组件实现：**
vue <template>
<div v-if="showBreathingExercise" class="breathing-overlay"> <div class="breathing-circle" :class="breathingPhase"> <span class="breath-instruction">{{ instruction }}</span> </div>
<div class="breathing-timer">
  <svg width="120" height="120" viewBox="0 0 120 120">
    <circle cx="60" cy="60" r="54" stroke-width="4" fill="none" 
            :stroke-dasharray="circumference" 
            :stroke-dashoffset="progressOffset"/>
  </svg>
  <span class="timer-text">{{ remainingTime }}s</span>
</div>

<button class="skip-btn" @click="closeExercise">跳过</button>
</div> </template>
<script setup> const breathingPhases = [ { phase: 'inhale', duration: 4000, instruction: '吸气...' }, { phase: 'hold', duration: 4000, instruction: '屏住...' }, { phase: 'exhale', duration: 6000, instruction: '呼气...' } ]; let currentPhase = 0; let timer = null; function startBreathingCycle() { const phase = breathingPhases[currentPhase]; breathingPhase.value = phase.phase; instruction.value = phase.instruction; timer = setTimeout(() => { currentPhase = (currentPhase + 1) % breathingPhases.length; startBreathingCycle(); }, phase.duration); } </script>
<style scoped> .breathing-circle { width: 200px; height: 200px; border-radius: 50%; background: radial-gradient(circle, rgba(10, 132, 255, 0.3), transparent); display: flex; align-items: center; justify-content: center; transition: all 1s ease; } .breathing-circle.inhale { transform: scale(1.3); background: radial-gradient(circle, rgba(48, 209, 88, 0.4), transparent); } .breathing-circle.exhale { transform: scale(0.8); background: radial-gradient(circle, rgba(10, 132, 255, 0.2), transparent); } </style>
#### 6.2 危机干预资源
**数据配置：**
java @Component public class CrisisResources {
public static final List<CrisisContact> CONTACTS = List.of(
    new CrisisContact("心理危机干预中心", "010-82951332", "24小时"),
    new CrisisContact("希望24热线", "400-161-9995", "24小时"),
    new CrisisContact("青少年服务热线", "12355", "全天候"),
    new CrisisContact("全国卫生热线", "12320", "工作日")
);

public static final List<String> CALMING_MUSIC = List.of(
"https://music.163.com/#/playlist?id= calming_playlist_1",
"https://open.spotify.com/playlist/peaceful_piano"
);
}
**UI 展示：**
vue
<div v-if="highRiskDetected" class="crisis-intervention"> <div class="urgent-notice"> <svg width="32" height="32" viewBox="0 0 24 24" fill="#ff453a"> <path d="M12 2L1 21h22L12 2zm0 4l7.53 13H4.47L12 6z"/> </svg> <h3>我注意到你现在可能很难受</h3> <p>请记住，你并不孤单。以下资源可以立即帮助你：</p> </div>
<div class="immediate-actions"> <button class="action-btn primary" @click="startBreathing"> 🫁 开始呼吸练习 </button>
<button class="action-btn secondary" @click="playCalmingMusic">
  🎵 播放舒缓音乐
</button>
</div>
<div class="emergency-contacts"> <h4>紧急求助热线</h4> <div v-for="contact in crisisContacts" :key="contact.name" class="contact-item"> <span>{{ contact.name }}</span> <a :href="'tel:' + contact.phone">{{ contact.phone }}</a> <small>{{ contact.hours }}</small> </div> </div>
<div class="stability-plan"> <h4>📋 情绪稳定计划</h4> <ol> <li>深呼吸 5 次（点击上方按钮跟随练习）</li> <li>喝一杯温水，感受温度</li> <li>写下此刻的 3 个感受（不评判，只观察）</li> <li>联系一位信任的朋友或家人</li> <li>如果持续难受，请拨打上方热线</li> </ol> </div> </div>
---

## 后端实体类扩展

### 新增实体：WeeklyGoal
java package com.kinlik.aicodehelper.entity;
import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name = "weekly_goals") public class WeeklyGoal { @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
private String username;
private LocalDate weekStart;

@Enumerated(EnumType.STRING)
private GoalType type;

private int targetCount;
private int currentCount;
private boolean completed;
private LocalDate completedAt;

// Getters and Setters
}
enum GoalType { GRATITUDE, // 感恩记录 MINDFULNESS, // 正念练习 EXERCISE, // 运动打卡 SOCIAL, // 社交互动 CONSISTENCY // 连续记录 }
### 新增实体：UserAchievement
java @Entity @Table(name = "user_achievements") public class UserAchievement { @Id @GeneratedValue private Long id; private String username; private String badgeId; private LocalDateTime unlockedAt; private boolean viewed; // 是否已查看过 }
---

## API 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/companion/mood/streak` | 获取连续记录天数和日历数据 |
| POST | `/ai/companion/goal/record` | 记录一次目标完成 |
| GET | `/ai/companion/goal/current` | 获取本周目标进度 |
| GET | `/ai/memory/list` | 获取用户的所有记忆片段 |
| DELETE | `/ai/memory/{id}` | 删除指定记忆 |
| GET | `/ai/achievement/list` | 获取用户成就列表 |
| POST | `/ai/notification/register` | 注册推送通知 token |

---

## 输出要求

请提供：

1. **完整的前端代码**
    - App.vue 的新增组件和逻辑
    - style.css 的新增样式（保持液态玻璃风格）
    - api.js 的新增接口调用

2. **完整的后端代码**
    - 新增 Entity 类
    - 新增 Repository 接口
    - 新增 Service 方法
    - 新增 Controller endpoints
    - Scheduled 定时任务配置

3. **数据库迁移脚本**
    - H2 建表语句
    - PostgreSQL 兼容的 migration script

4. **测试用例**
    - 单元测试：连续天数计算逻辑
    - 集成测试：定时任务触发
    - 前端 E2E 测试：打卡流程

5. **部署检查清单**
    - 环境变量配置（WebSocket URL、推送服务密钥）
    - 定时任务启用确认
    - 性能监控指标

---

## 注意事项

⚠️ **隐私保护：**
- 所有情绪数据必须加密存储
- 记忆删除功能必须立即物理删除（不可恢复）
- 推送通知需用户明确授权（GDPR 合规）

✅ **性能优化：**
- 日历数据使用缓存（Redis），每 6 小时刷新
- WebSocket 连接池管理，避免内存泄漏
- 呼吸动画使用 CSS transform（GPU 加速）

✅ **可访问性：**
- 所有图标添加 aria-label
- 颜色对比度符合 WCAG AA 标准
- 键盘导航支持（Tab 键切换）

---

**请基于以上需求，生成完整的前后端实现代码。**
