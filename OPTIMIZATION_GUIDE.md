# AI Agent 项目优化建议与面试指南

## 一、移动端页面混乱的原因与修复

### 问题根因
1. **style.css 是 Vite 默认模板样式**：包含 `#app { width: 1126px }` 等桌面端布局代码，与移动端冲突
2. **缺少 viewport 配置**：`index.html` 的 meta viewport 缺少 `viewport-fit=cover` 和 `user-scalable=no`
3. **无 iOS Safe Area 适配**：刘海屏、灵动岛会遮挡顶部导航栏和底部输入区
4. **无 touch 事件优化**：`-webkit-tap-highlight-color` 未清除，按钮点击有灰色背景
5. **CSS 变量未定义**：App.vue 中使用了 `--bg-primary` 等变量但 style.css 中定义的是 `--bg`

### 已修复内容
- 重写 `style.css`：iOS 原生风格，包含 `env(safe-area-inset-*)` 适配
- 修复 `index.html`：添加 `viewport-fit=cover`、`apple-mobile-web-app-capable` 等 PWA 标签
- 统一 CSS 变量命名：暗色/浅色主题变量完全对应
- 添加 `-webkit-overflow-scrolling: touch` 实现惯性滚动

---

## 二、后端架构优化建议（面试重点）

### 1. 当前架构亮点（面试时先讲这些）

```
技术栈：Spring Boot 3.2 + Java 21 + LangChain4j + MySQL + Chroma
```

| 亮点 | 说明 |
|------|------|
| **多 Agent 架构** | 编程助手 + 情绪陪伴 Agent 分离，职责清晰 |
| **ReAct 风格提示** | `CompanionAgentService` 先理解→再行动→再反馈 |
| **长期记忆** | Chroma 向量数据库存储对话记忆，支持语义检索 |
| **情绪量化** | 90 天情绪热力图，支持高危情绪检测与干预 |
| **流式响应** | SSE 实现打字机效果，提升用户体验 |
| **主动关怀** | `@Scheduled` 定时晚间情绪检查 |

### 2. 可优化的方向（面试加分项）

#### A. 高可用与容错
```java
// 当前问题：Chroma 未启动时记忆功能完全不可用
// 优化：增加降级策略，Chroma 失败时回退到 MySQL 全文检索

// 当前问题：API Key 硬编码在 application.yml
// 优化：接入 Spring Cloud Config / AWS Secrets Manager
```

#### B. 性能优化
```java
// 1. 情绪分析每次请求都调用 LLM，成本高
//    → 增加本地规则引擎预筛选（关键词匹配）
//    → 简单情绪直接规则判断，复杂情绪再走 LLM

// 2. 天气服务每次请求都调用 wttr.in
//    → 增加 @Cacheable 缓存，1 小时刷新一次

// 3. 会话列表无分页
//    → 增加 Pageable 分页，避免大数据量查询
```

#### C. 安全加固
```java
// 1. 输入校验：当前只有 SafeInputGuardrail，缺少 XSS/SQL 注入过滤
// 2. 速率限制：缺少 @RateLimiter，容易被刷接口
// 3. 敏感词过滤：高危情绪检测后应触发告警通知（企业微信/钉钉）
```

#### D. 工程化改进
```java
// 1. 统一异常处理：缺少 @ControllerAdvice 全局异常拦截
// 2. API 版本控制：URL 中无版本号，未来迭代困难
// 3. 日志链路追踪：缺少 traceId，排查问题困难
// 4. 单元测试覆盖：test 目录为空，建议补充 Mockito 测试
```

#### E. 云原生改造
```yaml
# 1. 容器化：补充 Dockerfile + docker-compose.yml
# 2. 健康检查：增加 /actuator/health 端点
# 3. 指标监控：接入 Micrometer + Prometheus
# 4. 配置中心：Spring Cloud Config / Nacos
```

### 3. 面试话术模板

**Q: 这个项目最大的技术难点是什么？**
> "最大的难点是**情绪陪伴 Agent 的共情回复质量**。我们采用 ReAct 架构，结合用户画像 + 长期记忆 + 实时情绪分析三层上下文，让回复既有温度又个性化。另一个难点是**流式 SSE 与情绪分析的并发处理**，我们用了异步线程池解耦，避免阻塞主响应流。"

**Q: 如果用户量增长 10 倍，怎么优化？**
> "首先**水平扩展**，Spring Boot 无状态设计支持多实例部署；其次**缓存优化**，天气、用户画像等读多写少的数据接入 Redis；然后**异步化**，情绪分析、记忆存储等操作放入消息队列削峰；最后**向量数据库优化**，Chroma 可替换为 Milvus 支持分布式。"

**Q: 怎么保证 AI 回复的安全性？**
> "三层防护：① **输入层** `SafeInputGuardrail` 拦截恶意输入；② **模型层** 系统提示中明确约束回复边界；③ **输出层** 对高危情绪自动触发人工干预建议。后续计划接入内容审核 API 做第四层兜底。"

---

## 三、移动端 iOS 设计亮点（面试可讲）

| 设计点 | 实现 |
|--------|------|
| **iOS 原生风格** | SF Pro 字体、大圆角、0.5px 细边框 |
| **毛玻璃导航栏** | `backdrop-filter: blur(20px) saturate(180%)` |
| **Safe Area 适配** | `env(safe-area-inset-*)` 适配刘海屏/灵动岛 |
| **底部标签栏** | 仿 iOS Tab Bar，带图标 + 文字 |
| **弹性动画** | `cubic-bezier(0.32, 0.72, 0, 1)` 仿 iOS 弹簧效果 |
| **PWA 支持** | `apple-mobile-web-app-capable` 可添加到主屏幕 |
| **主题切换** | 自动/浅色/深色三档，CSS 变量切换 |

---

## 四、前后端连桥运行步骤

### 1. 启动后端
```bash
cd d:/web-ai-code/web-ai-project02/ai-code-helper
# 确保 MySQL 在 3306 端口运行，数据库 ai_helper_db 已创建
# 修改 application.yml 中的密码和 API Key
./mvnw spring-boot:run
# 后端运行在 http://localhost:8081/api
```

### 2. 启动移动端前端
```bash
cd d:/web-ai-code/web-ai-project02/ai-code-helper/ai-code-helper-mobile
npm run dev
# 前端运行在 http://localhost:5174
```

### 3. 浏览器模拟 iPhone
- 打开 Chrome DevTools → Toggle Device Toolbar
- 选择 **iPhone 14 Pro Max**
- 刷新页面

### 4. 真机调试（同一局域网）
```bash
# 前端启动时绑定局域网 IP
npm run dev -- --host 0.0.0.0
# 手机浏览器访问 http://<电脑IP>:5174
```

---

## 五、项目结构总览

```
ai-code-helper/
├── src/main/java/...          # Spring Boot 后端
│   ├── Controller/AiController.java    # REST API
│   ├── service/                        # 业务逻辑
│   │   ├── CompanionAgentService.java  # 情绪陪伴 Agent
│   │   ├── EmotionAnalysisService.java # 情绪分析
│   │   ├── LongTermMemoryService.java  # 向量记忆
│   │   └── ProactiveCareService.java   # 主动关怀
│   └── config/CorsConfig.java          # 跨域配置
├── ai-code-helper-frontend/   # 桌面端 Web（Vue3）
└── ai-code-helper-mobile/     # 移动端 Web（Vue3 + iOS 风格）
    ├── src/App.vue            # 主组件
    ├── src/api.js             # API 封装
    └── src/style.css          # iOS 原生样式
```
