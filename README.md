# 🤖 AI MATE - 智能编程导师 & 情绪陪伴伙伴

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?logo=springboot&logoColor=white" alt="Spring Boot 3.2"/>
  <img src="https://img.shields.io/badge/Vue-3-blue?logo=vue.js&logoColor=white" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/LangChain4j-Latest-purple?logo=java" alt="LangChain4j"/>
  <img src="https://img.shields.io/badge/DashScope-Qwen-blueviolet?logo=alibabacloud&logoColor=white" alt="通义千问"/>
</p>

<p align="center">
  💻 代码难题一键解答 · 💝 情绪波动温柔守护 · 🧠 长期记忆懂你所需
</p>

<p align="center">
  <a href="#-web-demo">Web Demo</a> •
  <a href="#-mobile-demo">Mobile Demo</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-核心功能">核心功能</a>
</p>

---

## 🖥️ Web Demo

> 网页端运行演示视频/截图占位区

<!-- 请在此处放置网页端演示 GIF 或视频链接 -->
![](./images/demo1.gif)

**网页端特性：**
- 三栏响应式布局（历史对话 | 聊天主区 | 情绪记录）
- 仿苹果设计风格，支持浅色/深色主题切换
- 交互式侧边面板（点击工具栏按钮弹出）
- 流式 SSE 打字机效果

---

## 📱 Mobile Demo

> 移动端运行演示视频/截图占位区

<!-- 请在此处放置移动端演示 GIF 或视频链接 -->
![](./images/demo2.gif)

**移动端特性：**
- iOS 原生风格设计（毛玻璃导航栏、大圆角、弹性动画）
- iPhone Safe Area 刘海屏适配
- 底部 Tab Bar + 侧滑面板交互
- PWA 支持，可添加到主屏幕

---

## 🚀 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| Java | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.8+ |

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/ai-code-helper.git
cd ai-code-helper
```

### 2. 配置后端

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_helper_db?createDatabaseIfNotExist=true
    username: root
    password: your-password

langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: your-dashscope-api-key
```

### 3. 启动后端

```bash
# Windows
./mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

后端服务运行在 `http://localhost:8081/api`

### 4. 启动 Web 前端

```bash
cd ai-code-helper-frontend
npm install
npm run dev
```

访问 `http://localhost:5173`

### 5. 启动移动端前端

```bash
cd ai-code-helper-mobile
npm install
npm run dev
```

访问 `http://localhost:5174`，使用 Chrome DevTools 切换至 iPhone 设备模式预览

---

## 🛠️ 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| **Spring Boot 3.2** | 核心框架 |
| **Java 21** | 开发语言 |
| **LangChain4j** | AI Agent 编排与 LLM 接入 |
| **DashScope (通义千问)** | 大语言模型 |
| **Chroma** | 向量数据库（长期记忆） |
| **MySQL + JPA** | 会话与情绪数据持久化 |
| **SSE** | 流式响应 |

### Web 前端

| 技术 | 用途 |
|------|------|
| **Vue 3** | 框架 |
| **Vite** | 构建工具 |
| **marked** | Markdown 渲染 |
| **highlight.js** | 代码高亮 |
| **Axios** | HTTP 客户端 |

### 移动端前端

| 技术 | 用途 |
|------|------|
| **Vue 3** | 框架 |
| **Vite** | 构建工具 |
| **iOS 原生风格 CSS** | Safe Area、毛玻璃、弹性动画 |
| **PWA** | 可添加到主屏幕 |

---

## ✨ 核心功能

### 1. 双 Agent 架构

```
┌─────────────────┐     ┌─────────────────┐
│  编程助手 Agent  │     │  情绪陪伴 Agent  │
│  (Code Helper)  │     │  (Companion)    │
└────────┬────────┘     └────────┬────────┘
         │                       │
         ▼                       ▼
  流式 SSE 聊天            情绪分析与记录
  面试题生成               主动关怀提醒
  简历优化建议             天气关联建议
```

### 2. 情绪量化追踪

- **90 天情绪热力图**：可视化展示情绪变化趋势
- **高危情绪检测**：自动识别绝望、自残倾向等风险
- **主动关怀**：每晚 22:00 定时情绪检查

### 3. 长期记忆系统

- **Chroma 向量数据库**：存储对话记忆
- **语义检索**：基于向量相似度召回相关记忆
- **用户画像**：MBTI、性格特质、学习偏好

### 4. 跨设备会话同步

- MySQL 持久化存储
- 本地缓存加速恢复
- 自动云端同步

---

## 📁 项目结构

```
ai-code-helper/
├── src/main/java/com/kinlik/aicodehelper/
│   ├── Controller/AiController.java      # REST API
│   ├── service/
│   │   ├── CompanionAgentService.java    # 情绪陪伴 Agent
│   │   ├── EmotionAnalysisService.java   # 情绪分析
│   │   ├── LongTermMemoryService.java    # 向量记忆
│   │   └── ProactiveCareService.java     # 主动关怀
│   └── config/CorsConfig.java            # 跨域配置
├── ai-code-helper-frontend/              # Web 端
│   ├── src/App.vue
│   ├── src/api.js
│   └── src/styles.css
├── ai-code-helper-mobile/                # 移动端
│   ├── src/App.vue
│   ├── src/api.js
│   └── src/style.css
└── docker-compose.yml                    # 一键部署
```

---

## 🔧 部署方式

### Docker 部署（推荐）

```bash
docker-compose up -d
```

### 手动部署

1. 启动 MySQL 和 Chroma
2. 运行后端：`./mvnw spring-boot:run`
3. 构建前端：`npm run build`
4. 将 `dist/` 部署到 Nginx

---

## 📄 License

MIT License © 2024 AI MATE

---

<p align="center">
  Made with ❤️ by KINLIK
</p>
