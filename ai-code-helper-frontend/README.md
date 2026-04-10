# AI MATE Frontend

基于 Vue 3 + Vite + Axios 的单页聊天室应用，用于连接后端 AI 接口，提供：

- 编程学习 / 求职答疑的流式聊天
- 情绪陪伴分析
- 情绪记录
- 主动关怀提醒

## 启动方式

```bash
npm install
npm run dev
```

默认访问地址：`http://localhost:5173`

## 后端接口

前端默认请求：`http://localhost:8081/api`

已接入接口：

- `GET /ai/chat`：SSE 流式聊天
- `POST /ai/companion/chat`：陪伴对话与情绪分析
- `POST /ai/companion/mood`：情绪记录
- `GET /ai/companion/care`：主动关怀

## 页面说明

- 进入页面自动生成一个聊天室 ID（用于 `memoryId`）
- 上方展示聊天记录，用户消息右对齐，AI 消息左对齐
- 下方输入区支持回车发送
- 每次发送后会：
  - 通过 SSE 获取主对话流式回复
  - 同步记录当前情绪描述
  - 获取陪伴情绪分析结果
  - 当检测到需要支持时，自动追加主动关怀消息
