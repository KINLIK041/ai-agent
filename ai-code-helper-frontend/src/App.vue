<template>
  <div class="app-shell" :class="themeClass">
    <div class="ambient ambient-a"></div>
    <div class="ambient ambient-b"></div>

    <aside class="sidebar" :class="{ 'sidebar-open': sidebarOpen }">
      <div class="sidebar-header">
        <h2>历史对话</h2>
        <button @click="createNewSession" class="new-chat-btn">+ 新对话</button>
      </div>

      <div class="session-list">
        <div
            v-for="session in sessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === memoryId }"
            @click="switchSession(session)"
        >
          <div class="session-title">{{ session.title || '未命名对话' }}</div>
          <div class="session-preview">{{ session.lastMessage?.substring(0, 30) || '' }}...</div>
          <div class="session-time">{{ formatSessionTime(session.updatedAt) }}</div>
          <button @click.stop="handleDeleteSession(session.sessionId)" class="delete-btn">×</button>
        </div>

        <div v-if="sessions.length === 0" class="empty-state">
          暂无历史对话
        </div>
      </div>
    </aside>

    <main class="chat-layout">
      <header class="hero-panel">
        <div class="header-left">
          <button @click="toggleSidebar" class="menu-toggle">☰</button>
          <div>
            <p class="eyebrow">AI COMPANION · CAREER · CODING</p>
            <h1>AI MATE</h1>
            <p class="hero-copy">
              一个懂编程学习、求职成长，也会感知情绪并主动关怀你的智能聊天伙伴。
            </p>
          </div>
        </div>

        <div class="session-card">
          <div class="session-row">
            <span>Chat ID</span>
            <strong>#{{ memoryId }}</strong>
          </div>
          <div class="session-row">
            <span>用户</span>
            <input v-model="username" class="name-input" maxlength="20" />
          </div>
          <div class="session-row compact">
            <span>状态</span>
            <strong :class="isStreaming ? 'status-live' : 'status-idle'">
              {{ isStreaming ? '实时回复中' : '等待输入' }}
            </strong>
          </div>
          <div class="session-row compact">
            <span>主题</span>
            <strong>{{ themeLabel }}</strong>
          </div>
        </div>
      </header>

      <section ref="messagePanelRef" class="message-panel">
        <div
            v-for="item in messages"
            :key="item.id"
            class="message-row"
            :class="item.role === 'user' ? 'is-user' : 'is-ai'"
        >
          <article class="bubble" :class="[`bubble-${item.role}`, item.variant ? `bubble-${item.variant}` : '']">
            <div class="bubble-meta">
              <span>{{ item.role === 'user' ? username : item.label || 'AI MATE' }}</span>
              <time>{{ formatTime(item.timestamp) }}</time>
            </div>
            <p class="bubble-text">{{ item.content }}</p>

            <div v-if="item.emotion" class="emotion-card">
              <span>情绪：{{ emotionMap[item.emotion.type] || item.emotion.type }}</span>
              <span>强度：{{ Math.round((item.emotion.intensity || 0) * 100) }}%</span>
              <span>倾向：{{ item.emotion.sentiment }}</span>
              <span>{{ item.emotion.needsSupport ? '建议重点关怀' : '状态平稳' }}</span>
            </div>
          </article>
        </div>
      </section>

      <section class="composer-panel">
        <div class="quick-actions">
          <button type="button" @click="fillPrompt('帮我制定一份前端面试冲刺计划')">面试冲刺</button>
          <button type="button" @click="fillPrompt('我最近学 Vue 很焦虑，想要一个学习路线')">学习焦虑</button>
          <button type="button" @click="fillPrompt('帮我优化前端简历项目描述')">简历优化</button>
        </div>

        <form class="composer" @submit.prevent="handleSend">
          <textarea
              v-model.trim="inputMessage"
              class="composer-input"
              rows="3"
              placeholder="输入你关于编程学习、求职准备或情绪状态的问题..."
              @keydown.enter.exact.prevent="handleSend"
          ></textarea>

          <div class="composer-footer">
            <p class="hint-text">
              发送后将同步触发：流式聊天、陪伴分析、情绪记录；如检测到压力状态，会自动生成主动关怀消息。
            </p>
            <button type="submit" class="send-button" :disabled="isStreaming || !inputMessage">
              {{ isStreaming ? 'AI 思考中...' : '发送' }}
            </button>
          </div>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch, computed } from 'vue';
import {
  createChatStreamUrl,
  fetchCareMessage,
  saveMood,
  sendCompanionMessage,
  saveSession,
  getSessionList,
  deleteSession,
} from './api';

const username = ref('KINLIK');
const inputMessage = ref('');
const isStreaming = ref(false);
const memoryId = ref(Math.floor(100000 + Math.random() * 900000));
const messagePanelRef = ref(null);
const currentEventSource = ref(null);
const sidebarOpen = ref(false);
const sessions = ref([]);
const currentSessionTitle = ref('');

const emotionMap = {
  sadness: '失落',
  anxiety: '焦虑',
  stress: '压力',
  joy: '愉悦',
  anger: '烦躁',
  calm: '平静',
};

const messages = ref([
  {
    id: crypto.randomUUID(),
    role: 'ai',
    label: 'AI MATE',
    variant: 'welcome',
    content: '你好，我是 AI MATE。你可以问我编程学习路线、前端项目实战、面试准备、简历优化，也可以直接和我聊聊你的状态，我会尽量给出建议与关怀。',
    timestamp: Date.now(),
  },
]);

const themeClass = computed(() => {
  const hour = new Date().getHours();
  return hour >= 6 && hour < 18 ? 'theme-day' : 'theme-night';
});

const themeLabel = computed(() => {
  const hour = new Date().getHours();
  return hour >= 6 && hour < 18 ? '☀️ 日间模式' : '🌙 夜间模式';
});

function pushMessage(payload) {
  messages.value.push({
    id: crypto.randomUUID(),
    timestamp: Date.now(),
    ...payload,
  });
}

function formatTime(timestamp) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp);
}

function formatSessionTime(datetime) {
  if (!datetime) return '';
  const date = new Date(datetime);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;

  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
  }).format(date);
}

function fillPrompt(prompt) {
  inputMessage.value = prompt;
}

async function scrollToBottom() {
  await nextTick();
  const panel = messagePanelRef.value;
  if (!panel) return;
  panel.scrollTop = panel.scrollHeight;
}

async function loadSessions() {
  try {
    const response = await getSessionList(username.value);
    sessions.value = response.data;
  } catch (error) {
    console.error('加载会话列表失败:', error);
  }
}

async function switchSession(session) {
  memoryId.value = session.sessionId;
  currentSessionTitle.value = session.title;
  messages.value = [
    {
      id: crypto.randomUUID(),
      role: 'ai',
      label: 'AI MATE',
      content: `已切换到之前的对话：${session.title || '未命名对话'}`,
      timestamp: Date.now(),
    },
  ];
  sidebarOpen.value = false;
}

async function handleDeleteSession(sessionId) {
  if (!confirm('确定要删除这个会话吗？')) return;

  try {
    await deleteSession(sessionId);
    await loadSessions();

    if (memoryId.value === sessionId) {
      createNewSession();
    }
  } catch (error) {
    console.error('删除会话失败:', error);
    alert('删除失败，请重试');
  }
}

function createNewSession() {
  memoryId.value = Math.floor(100000 + Math.random() * 900000);
  currentSessionTitle.value = '';
  messages.value = [
    {
      id: crypto.randomUUID(),
      role: 'ai',
      label: 'AI MATE',
      variant: 'welcome',
      content: '你好，我是 AI MATE。你可以问我编程学习路线、前端项目实战、面试准备、简历优化，也可以直接和我聊聊你的状态，我会尽量给出建议与关怀。',
      timestamp: Date.now(),
    },
  ];
  sidebarOpen.value = false;
}

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value;
}

async function startChatStream(message) {
  const aiMessage = {
    id: crypto.randomUUID(),
    role: 'ai',
    label: 'AI MATE',
    content: '',
    timestamp: Date.now(),
  };
  messages.value.push(aiMessage);
  await scrollToBottom();

  const streamUrl = createChatStreamUrl(memoryId.value, message);
  const eventSource = new EventSource(streamUrl);
  currentEventSource.value = eventSource;
  isStreaming.value = true;

  eventSource.onmessage = async (event) => {
    if (event.data === '[DONE]') {
      eventSource.close();
      currentEventSource.value = null;
      isStreaming.value = false;
      await saveCurrentSession(message, aiMessage.content);
      return;
    }

    aiMessage.content += event.data;
    await scrollToBottom();
  };

  eventSource.onerror = () => {
    eventSource.close();
    currentEventSource.value = null;

    if (!aiMessage.content) {
      aiMessage.content = '当前流式连接中断，请检查后端服务是否已启动。';
      aiMessage.variant = 'error';
    }

    isStreaming.value = false;
  };
}

async function saveCurrentSession(userMessage, aiResponse) {
  try {
    const title = currentSessionTitle.value || userMessage.substring(0, 20);
    await saveSession({
      username: username.value,
      sessionId: memoryId.value.toString(),
      title: title,
      lastMessage: aiResponse || userMessage,
    });
    await loadSessions();
  } catch (error) {
    console.error('保存会话失败:', error);
  }
}

async function runCompanionFlow(message) {
  try {
    const moodPayload = {
      username: username.value,
      moodDescription: message,
      triggerEvent: '',
    };

    await saveMood(moodPayload);

    const companionPayload = {
      username: username.value,
      message,
    };

    const { data } = await sendCompanionMessage(companionPayload);

    pushMessage({
      role: 'ai',
      label: '陪伴助手',
      variant: 'companion',
      content: data.message,
      emotion: data.emotion,
    });

    if (data.emotion?.needsSupport) {
      try {
        const careResponse = await fetchCareMessage(username.value, message);
        pushMessage({
          role: 'ai',
          label: '主动关怀',
          variant: 'care',
          content: careResponse.data,
        });
      } catch (careError) {
        console.warn('主动关怀接口调用失败:', careError);
      }
    }
  } catch (error) {
    console.error('陪伴分析流程失败:', error);

    let errorMessage = '陪伴分析或情绪关怀接口调用失败';

    if (error.response) {
      errorMessage += ` (状态码: ${error.response.status})`;
      if (error.response.data?.message) {
        errorMessage += `: ${error.response.data.message}`;
      }
    } else if (error.request) {
      errorMessage += '，请确认后端服务已启动并可访问';
    }

    pushMessage({
      role: 'ai',
      label: '系统提示',
      variant: 'error',
      content: errorMessage + '。',
    });
  }
}

async function handleSend() {
  if (!inputMessage.value || isStreaming.value) {
    return;
  }

  const message = inputMessage.value;
  inputMessage.value = '';

  if (!currentSessionTitle.value) {
    currentSessionTitle.value = message.substring(0, 20);
  }

  pushMessage({
    role: 'user',
    content: message,
  });

  await scrollToBottom();

  startChatStream(message);
  runCompanionFlow(message);
}

watch(messages, () => {
  scrollToBottom();
}, { deep: true });

watch(username, () => {
  loadSessions();
});

onMounted(() => {
  scrollToBottom();
  loadSessions();
});

onBeforeUnmount(() => {
  currentEventSource.value?.close();
});
</script>
