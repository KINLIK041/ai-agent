<template>
<div class="app-shell" :class="themeClass">

  <!-- ==================== 鼓励动画遮罩层 ==================== -->
  <div v-if="showEncouragement" class="encouragement-overlay" @click="showEncouragement = false">
    <div class="encouragement-card">
      <div class="encouragement-emoji">{{ encouragementEmoji }}</div>
      <p class="encouragement-quote">{{ displayedQuote }}</p>
      <p v-if="encouragementStreak > 0" class="encouragement-streak">
        <span class="streak-fire">🔥</span> 连续 {{ encouragementStreak }} 天
      </p>
    </div>
  </div>

  <!-- ==================== 呼吸练习遮罩 ==================== -->
  <div v-if="showBreathing" class="breathing-overlay" @click="closeBreathing">
    <div class="breathing-circle" :class="breathingPhase">
      <span class="breath-instruction">{{ breathingInstruction }}</span>
    </div>
    <div class="breathing-progress">
      <svg width="120" height="120" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="54" class="progress-bg" />
        <circle cx="60" cy="60" r="54" class="progress-ring"
          :stroke-dasharray="339.3"
          :stroke-dashoffset="breathingOffset" />
      </svg>
      <span class="breathing-timer">{{ breathingCountdown }}s</span>
    </div>
    <button class="breathing-skip" @click="closeBreathing">跳过</button>
  </div>

  <!-- 历史对话侧边面板 -->
  <aside class="sidebar" :class="{open:sidebarOpen}">
    <div class="drawer-header">
      <h2>历史对话</h2>
      <button class="icon-btn close-btn" @click="sidebarOpen=false" aria-label="关闭">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
      </button>
    </div>
    <button class="new-chat-btn" @click="newSession">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
      新对话
    </button>
    <label class="search-box">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
      <input v-model.trim="keyword" placeholder="搜索会话" />
    </label>
    <div class="session-list">
      <template v-for="g in grouped" :key="g.key">
        <div class="group-title"><span>{{ g.label }}</span><small>{{ g.items.length }}</small></div>
        <button v-for="s in g.items" :key="s.sessionId" class="session-item" :class="{active:String(s.sessionId)===sessionId}" @click="openSession(s)">
          <div class="session-title">{{ cut(s.title || '未命名对话', 18) }}</div>
          <div class="session-preview">{{ cut(s.lastMessage || '暂无摘要', 26) }}</div>
          <div class="session-time">{{ formatSessionTime(s.updatedAt) }}</div>
        </button>
      </template>
      <div v-if="!grouped.length" class="empty-state">暂无匹配会话</div>
    </div>
  </aside>

  <!-- 遮罩层 -->
  <div class="overlay" :class="{show: sidebarOpen || moodOpen}" @click="closePanels"></div>

  <!-- 主内容区 -->
  <main class="chat-layout">
    <!-- 顶部工具栏 -->
    <header class="topbar">
      <button class="toolbar-btn" @click="sidebarOpen=!sidebarOpen" :class="{active: sidebarOpen}">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 12h18M3 6h18M3 18h18"/></svg>
        <span class="toolbar-label">历史对话</span>
      </button>
      <h1 class="topbar-title">AI MATE</h1>
      <button class="toolbar-btn" @click="moodOpen=!moodOpen; currentTab='mood'" :class="{active: moodOpen}">
        <span class="toolbar-label">情绪中心</span>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
      </button>
    </header>

    <!-- 信息卡片行 -->
    <section class="info-bar">
      <div class="info-chip"><span>Chat ID</span><strong>#{{ sessionId }}</strong></div>
      <div class="info-chip"><span>用户</span><input v-model="username" class="name-input" maxlength="20" /></div>
      <div class="info-chip">
        <span>主题</span>
        <select v-model="themePreference" class="theme-select">
          <option value="auto">自动</option>
          <option value="light">浅色</option>
          <option value="dark">深色</option>
        </select>
      </div>
      <div class="info-chip clickable" @click="currentTab='me'; showSettings=true">
        <span>🏅 成就</span>
        <strong>{{ unlockedBadgeCount }}</strong>
      </div>
      <div class="info-chip clickable" @click="currentTab='memory'; showSettings=true">
        <span>🧠 记忆</span>
        <strong>管理</strong>
      </div>
    </section>

    <!-- 消息面板 -->
    <section ref="panelRef" class="message-panel">
      <div v-for="m in messages" :key="m.id" class="message-row" :class="m.role">
        <article class="bubble" :class="['bubble-'+m.role,m.variant?`bubble-${m.variant}`:'']">
          <!-- AI记忆标签 -->
          <div v-if="m.rememberedContext && m.role === 'ai'" class="memory-tag">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>
            AI 记得：{{ m.rememberedContext }}
          </div>
          <div v-if="m.role!=='system'" class="bubble-meta">
            <span :class="m.role==='user'?'speaker-user':'speaker-ai'">{{ m.role==='user' ? username : (m.label || 'AI MATE') }}</span>
            <time>{{ formatTime(m.timestamp) }}</time>
          </div>
          <div v-if="m.typing" class="typing-indicator"><span></span><span></span><span></span></div>
          <div v-else :class="m.role==='system'?'system-text':'bubble-text markdown-body'" v-html="renderMarkdown(m.content)"></div>
        </article>
      </div>
    </section>

    <!-- 输入面板 -->
    <section class="composer-panel">
      <div v-if="notice" class="notice" :class="notice.type">{{ notice.text }}</div>
      <div v-if="weather.city" class="weather-banner">
        <strong>{{ weather.city }}</strong>
        <span>{{ weather.description }} · {{ weather.temperature }} · 湿度 {{ weather.humidity }}</span>
        <p>{{ weather.suggestion }}</p>
      </div>
      <div v-if="highRiskWarning" class="notice error">
        {{ highRiskWarning }}
        <button class="crisis-link" @click="showCrisisResources = true">查看急救资源</button>
      </div>
      <form class="composer" @submit.prevent="send">
        <textarea ref="textareaRef" v-model="input" class="composer-input" rows="1" placeholder="输入内容，Enter 换行，Ctrl+Enter 发送..." @input="resizeTextarea" @keydown="handleKeydown"></textarea>
        <div class="composer-footer">
          <p class="hint-text">完整历史已同步保存到 MySQL，会自动缓存到本地以加快恢复速度。</p>
          <button class="send-button" :disabled="streaming || !input.trim()">{{ streaming ? 'AI 思考中...' : '发送' }}</button>
        </div>
      </form>
    </section>
  </main>

  <!-- 情绪记录侧边面板（增强版） -->
  <aside class="mood-panel" :class="{open:moodOpen}">
    <div class="drawer-header">
      <h2>情绪中心</h2>
      <button class="icon-btn close-btn" @click="moodOpen=false" aria-label="关闭">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
      </button>
    </div>

    <!-- 连续打卡 -->
    <div class="streak-card" v-if="streakData">
      <div class="streak-main">
        <span class="streak-fire">🔥</span>
        <span class="streak-count">{{ streakData.currentStreak }}</span>
        <span class="streak-unit">天连续记录</span>
      </div>
      <div class="streak-best">历史最高 {{ streakData.bestStreak }} 天</div>
      <div class="milestone-bar" v-if="streakData.nextMilestone > 0">
        <span>再坚持 {{ streakData.nextMilestone }} 天解锁「{{ streakData.nextBadge }}」</span>
        <div class="progress-track"><div class="progress-fill" :style="{width: Math.min((streakData.currentStreak % 7)/7*100,100)+'%'}"></div></div>
      </div>
    </div>

    <div class="mood-card">
      <div class="mood-legend">
        <span class="legend positive">积极</span>
        <span class="legend neutral">平稳</span>
        <span class="legend negative">消极</span>
      </div>
      <div class="mood-grid">
        <div v-for="d in moodDays" :key="d.date" class="mood-day" :class="'mood-'+d.level" :title="`${d.date} ${d.label}`"></div>
      </div>
      <p class="mood-summary">{{ moodSummary }}</p>
      <ul class="mood-list">
        <li v-for="item in moodRecords.slice(0,6)" :key="item.recordDate+item.emotionType">
          <strong>{{ item.recordDate }}</strong>
          <span>{{ item.emotionType }}</span>
          <em :class="item.positiveEmotion ? 'positive-text' : (item.highRisk ? 'negative-text' : '')">{{ item.emotionIntensity }}/10</em>
        </li>
      </ul>
    </div>

    <!-- 成就墙 -->
    <div class="achievement-section">
      <h3 class="section-title">🏅 成就墙</h3>
      <div class="badge-grid" v-if="streakData && streakData.allBadges">
        <div v-for="badge in streakData.allBadges" :key="badge.id" class="badge-item" :class="{locked: !badge.unlocked}" :title="badge.unlocked ? `${badge.name} - ${badge.requirement}` : `未解锁: ${badge.requirement}`">
          <span class="badge-icon">{{ badge.icon }}</span>
          <span class="badge-name">{{ badge.name }}</span>
        </div>
      </div>
    </div>

    <!-- 本周目标 -->
    <div class="goal-card" v-if="weeklyGoal">
      <h3 class="section-title">📅 本周目标</h3>
      <div class="goal-content">
        <span class="goal-type">{{ goalTypeName(weeklyGoal.type) }}</span>
        <div class="goal-bar"><div class="goal-fill" :style="{width: (weeklyGoal.currentCount/weeklyGoal.targetCount*100)+'%'}"></div></div>
        <span class="goal-count">{{ weeklyGoal.currentCount }} / {{ weeklyGoal.targetCount }}</span>
      </div>
      <button class="goal-btn" @click="handleRecordGoal" :disabled="weeklyGoal.completed">
        {{ weeklyGoal.completed ? '✓ 已完成' : '记录完成' }}
      </button>
    </div>
  </aside>

  <!-- 设置 / 记忆管理弹窗 -->
  <div class="modal-overlay" v-if="showSettings" @click="showSettings=false; currentTab='chat'">
    <div class="modal-box" @click.stop>
      <div class="modal-header">
        <h3>{{ currentTab === 'memory' ? '🧠 AI 记忆管理' : '设置' }}</h3>
        <button class="close-btn" @click="showSettings=false; currentTab='chat'">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>

      <div v-if="currentTab !== 'memory'" class="modal-body">
        <div class="setting-row">
          <span>用户名</span>
          <input v-model="username" class="setting-input" maxlength="20" />
        </div>
        <div class="setting-row">
          <span>主题</span>
          <select v-model="themePreference" class="setting-select">
            <option value="auto">自动</option>
            <option value="light">浅色</option>
            <option value="dark">深色</option>
          </select>
        </div>
        <div class="setting-row" @click="currentTab='memory'; loadMemories()">
          <span>🧠 AI 记忆管理</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </div>
      </div>

      <div v-if="currentTab === 'memory'" class="modal-body memory-body">
        <button class="back-btn" @click="currentTab='chat'">← 返回设置</button>
        <p class="memory-hint">以下是 AI 记住的关于你的信息，你可以删除不需要的记忆。</p>
        <div class="memory-list">
          <div v-for="m in memories" :key="m.id" class="memory-item">
            <div class="memory-content">{{ m.content }}</div>
            <div class="memory-meta">
              <span>{{ formatDate(m.createdAt) }}</span>
              <button class="delete-btn" @click="handleDeleteMemory(m.id)">删除</button>
            </div>
          </div>
          <div v-if="!memories.length" class="empty-state">AI 还没有记住任何内容</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 危机热线弹窗 -->
  <div class="modal-overlay" v-if="showCrisisResources" @click="showCrisisResources=false">
    <div class="modal-box crisis-box" @click.stop>
      <div class="modal-header">
        <h3>🆘 急救资源</h3>
        <button class="close-btn" @click="showCrisisResources=false">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>
      <div class="modal-body">
        <p class="crisis-intro">你并不孤单。以下资源可以立即帮助你：</p>
        <div v-for="c in crisisContacts" :key="c.name" class="contact-item">
          <span class="contact-name">{{ c.name }}</span>
          <a :href="'tel:'+c.phone" class="contact-phone">{{ c.phone }}</a>
          <small>{{ c.hours }}</small>
        </div>
        <div class="crisis-quotes" v-if="crisisQuotes.length">
          <p v-for="q in crisisQuotes" :key="q" class="crisis-quote">{{ q }}</p>
        </div>
        <button class="breathing-btn" @click="startBreathing">🫁 开始呼吸练习</button>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import { createChatStreamUrl, fetchCrisisResources, fetchMemories, fetchMoodOverview, fetchMoodStreak, fetchWeeklyGoal, getSessionDetail, getSessionList, recordMoodCheckIn, recordWeeklyGoal, saveMood, saveSession, sendCompanionMessage } from './api';

const THEME='theme-preference',CACHE='ai-mate-session-cache',LAST='ai-mate-last-session-id';

// 状态
const username=ref('KINLIK'),input=ref(''),streaming=ref(false),sessionId=ref(localStorage.getItem(LAST)||id()),messages=ref([]),sessions=ref([]),keyword=ref(''),themePreference=ref(localStorage.getItem(THEME)||'auto'),notice=ref(null),sidebarOpen=ref(false),moodOpen=ref(false),panelRef=ref(null),textareaRef=ref(null),currentEventSource=ref(null),currentTitle=ref(''),weather=ref({}),highRiskWarning=ref('');
const currentTab=ref('chat'),showSettings=ref(false),showCrisisResources=ref(false);
const showEncouragement=ref(false),showBreathing=ref(false);
const encouragementEmoji=ref('🌸'),encouragementQuote=ref(''),displayedQuote=ref(''),encouragementStreak=ref(0);
const breathingPhase=ref('inhale'),breathingInstruction=ref('吸气...'),breathingCountdown=ref(4),breathingOffset=ref(339.3);
const streakData=ref(null),weeklyGoal=ref(null),moodRecords=ref([]),memories=ref([]);
const crisisContacts=ref([]),crisisQuotes=ref([]);
let breathingTimer=null,quoteInterval=null;
const BREATHING_PHASES=[
  {phase:'inhale',duration:4000,instruction:'吸气...',offset:0},
  {phase:'hold',duration:4000,instruction:'屏住...',offset:85},
  {phase:'exhale',duration:6000,instruction:'呼气...',offset:339}
];

const unlockedBadgeCount=computed(()=>(streakData.value?.allBadges||[]).filter(b=>b.unlocked).length);

marked.setOptions({breaks:true,gfm:true});
const renderer=new marked.Renderer();
renderer.code=({text,lang})=>{const l=hljs.getLanguage(lang||'')?lang:'plaintext';return `<pre><code class="hljs language-${l}">${hljs.highlight(text,{language:l}).value}</code></pre>`};

const themeClass=computed(()=>resolveTheme()==='light'?'theme-day':'theme-night');
const grouped=computed(()=>{const k=keyword.value.trim().toLowerCase();const list=sessions.value.filter(s=>!k||`${s.title||''} ${s.lastMessage||''}`.toLowerCase().includes(k)).sort((a,b)=>new Date(b.updatedAt||0)-new Date(a.updatedAt||0));const g={today:[],yesterday:[],week:[],earlier:[]};list.forEach(s=>g[bucket(s.updatedAt)].push(s));return[{key:'today',label:'今天',items:g.today},{key:'yesterday',label:'昨天',items:g.yesterday},{key:'week',label:'本周',items:g.week},{key:'earlier',label:'更早',items:g.earlier}].filter(x=>x.items.length)});
const moodDays=computed(()=>{const map=new Map(moodRecords.value.map(r=>[String(r.recordDate).slice(0,10),r]));const days=[];for(let i=89;i>=0;i--){const d=new Date();d.setDate(d.getDate()-i);const key=d.toISOString().slice(0,10);const row=map.get(key);days.push({date:key,label:row?.emotionType||'无记录',level:level(row)})}return days});
const moodSummary=computed(()=>moodRecords.value.length?`最近：${moodRecords.value[0]?.emotionType||'未知'}，共 ${moodRecords.value.length} 次记录。`:'最近 90 天暂无情绪记录。');

function id(){return String(Math.floor(100000+Math.random()*900000))}
function resolveTheme(){return themePreference.value==='auto'?((new Date().getHours()>=6&&new Date().getHours()<18)?'light':'dark'):themePreference.value}
function store(k,v){localStorage.setItem(k,JSON.stringify(v))}
function read(k){try{return JSON.parse(localStorage.getItem(k)||'{}')}catch{return {}}}
function persistMessages(){const c=read(CACHE);c[sessionId.value]=messages.value;store(CACHE,c)}
function hydrateLocal(){const c=read(CACHE);return c[sessionId.value]||null}
function cut(t,n){return !t?'':(t.length>n?`${t.slice(0,n)}…`:t)}
function renderMarkdown(t){
  const raw=(t||'').replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi,'').replace(/javascript:/gi,'');
  let processed=raw;
  const boldMatches=raw.match(/\*\*/g);
  if(boldMatches&&boldMatches.length%2!==0){const lastIdx=raw.lastIndexOf('**');processed=raw.slice(0,lastIdx)+'\u200B**'+raw.slice(lastIdx+2)}
  const emMatches=processed.match(/(?<!\*)\*(?!\*)/g);
  if(emMatches&&emMatches.length%2!==0){const lastIdx=processed.lastIndexOf('*');processed=processed.slice(0,lastIdx)+'\u200B*'+processed.slice(lastIdx+1)}
  return marked.parse(processed,{renderer});
}
function flash(text,type='info'){notice.value={text,type};clearTimeout(flash.t);flash.t=setTimeout(()=>notice.value=null,3000)}
function bucket(d){if(!d)return'earlier';d=new Date(d);const n=new Date(),t=new Date(n.getFullYear(),n.getMonth(),n.getDate()),y=new Date(t),w=new Date(t);y.setDate(t.getDate()-1);w.setDate(t.getDate()-7);return d>=t?'today':d>=y?'yesterday':d>=w?'week':'earlier'}
function formatTime(t){return new Intl.DateTimeFormat('zh-CN',{hour:'2-digit',minute:'2-digit'}).format(t)}
function formatSessionTime(d){if(!d)return'刚刚';d=new Date(d);const x=Date.now()-d.getTime();if(x<60000)return'刚刚';if(x<3600000)return`${Math.floor(x/60000)} 分钟前`;if(x<86400000)return`${Math.floor(x/3600000)} 小时前`;return new Intl.DateTimeFormat('zh-CN',{month:'short',day:'numeric'}).format(d)}
function formatDate(d){if(!d)return'';const date=new Date(d);return new Intl.DateTimeFormat('zh-CN',{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}).format(date)}
function level(row){if(!row)return'none';if(row.highRisk)return'negative';if(row.positiveEmotion)return'positive';if((row.emotionIntensity||0)>=7&&!(row.positiveEmotion))return'negative';if((row.emotionIntensity||0)>=5)return'neutral';return'positive'}
function goalTypeName(type){const map={GRATITUDE:'🙏 感恩记录',MINDFULNESS:'🧘 正念练习',EXERCISE:'🏃 运动打卡',SOCIAL:'🤝 社交互动',CONSISTENCY:'📝 连续记录'};return map[type]||type}
function push(payload){messages.value.push({id:crypto.randomUUID(),timestamp:Date.now(),...payload})}
function closePanels(){sidebarOpen.value=false;moodOpen.value=false}

async function bottom(){await nextTick();if(panelRef.value)panelRef.value.scrollTop=panelRef.value.scrollHeight}
function resizeTextarea(){const e=textareaRef.value;if(!e)return;e.style.height='auto';e.style.height=`${Math.min(e.scrollHeight,180)}px`}

async function loadSessions(){try{sessions.value=(await getSessionList(username.value)).data||[]}catch{}}
async function loadMood(){try{const {data}=await fetchMoodOverview(username.value,90);moodRecords.value=(data.records||[]).slice().sort((a,b)=>String(b.recordDate).localeCompare(String(a.recordDate)))}catch{moodRecords.value=[]}}
async function loadStreak(){try{const {data}=await fetchMoodStreak(username.value);streakData.value=data}catch{streakData.value=null}}
async function loadWeeklyGoal(){try{const {data}=await fetchWeeklyGoal(username.value);weeklyGoal.value=data}catch{weeklyGoal.value=null}}
async function loadMemories(){try{const {data}=await fetchMemories(username.value);memories.value=data||[]}catch{memories.value=[]}}
async function loadCrisisResources(){try{const {data}=await fetchCrisisResources();crisisContacts.value=data.contacts||[];crisisQuotes.value=data.calmingQuotes||[]}catch{}}

async function openSession(s){sessionId.value=String(s.sessionId);currentTitle.value=s.title||'';localStorage.setItem(LAST,sessionId.value);const local=hydrateLocal();if(local)messages.value=local;try{const {data}=await getSessionDetail(sessionId.value);messages.value=JSON.parse(data.messagesJson||'[]');if(!messages.value.length)messages.value=[welcomeHint(s.title)];persistMessages();flash('已从云端恢复该会话的完整历史。','success')}catch(e){if(!local)messages.value=[welcomeHint(s.title)];flash(e.friendlyMessage||'会话恢复失败','error')}sidebarOpen.value=false;bottom()}
function newSession(){sessionId.value=id();localStorage.setItem(LAST,sessionId.value);currentTitle.value='';messages.value=[welcome()];input.value='';sidebarOpen.value=false;persistMessages();nextTick(()=>{resizeTextarea();bottom()})}
function welcome(){return{id:crypto.randomUUID(),role:'ai',label:'AI MATE',variant:'welcome',content:'你好，我是 **AI MATE**。你可以问我编程学习路线、前端项目实战、面试准备、简历优化，也可以直接和我聊聊你的状态。',timestamp:Date.now()}}
function welcomeHint(title){return{id:crypto.randomUUID(),role:'system',content:`已恢复到会话 **${title||'未命名对话'}**。`,timestamp:Date.now()}}

async function startStream(text){const ai={id:crypto.randomUUID(),role:'ai',label:'AI MATE',content:'',typing:true,timestamp:Date.now()};messages.value.push(ai);await bottom();const es=new EventSource(createChatStreamUrl(sessionId.value,text));currentEventSource.value=es;streaming.value=true;es.onmessage=async ev=>{if(ev.data==='[DONE]'){ai.typing=false;es.close();currentEventSource.value=null;streaming.value=false;await saveCurrentSession(text,ai.content);return}if(ai.typing){ai.typing=false;ai.content=''}ai.content+=ev.data;await bottom()};es.onerror=()=>{es.close();currentEventSource.value=null;ai.typing=false;if(!ai.content){ai.content='当前流式连接中断，请检查后端服务是否已启动。';ai.variant='error'}streaming.value=false;flash('流式聊天连接中断','error')}}
async function saveCurrentSession(userMessage,aiResponse){const title=currentTitle.value||userMessage.slice(0,20),lastMessage=aiResponse||userMessage,payload={username:username.value,sessionId:sessionId.value,title,lastMessage,messagesJson:JSON.stringify(messages.value),pinned:false};try{await saveSession(payload);flash('会话已同步到云端。','success')}catch(e){flash(e.friendlyMessage||'保存会话失败','error')}persistMessages();await loadSessions()}
async function companion(text){try{await saveMood({username:username.value,moodDescription:text,triggerEvent:''});const {data}=await sendCompanionMessage({username:username.value,message:text});weather.value=data.weather||{};highRiskWarning.value=data.emotion?.highRisk?'检测到高危情绪，请优先联系可信任的人或专业热线。':'';push({role:'ai',label:'AI MATE',variant:'companion',content:data.message,rememberedContext:data.rememberedContext,emotion:data.emotion});if(data.careAdvice){push({role:'system',variant:data.emotion?.highRisk?'error':'care',content:data.careAdvice})}loadMood();loadStreak();loadWeeklyGoal()}catch(e){push({role:'system',variant:'error',content:e.friendlyMessage||'陪伴分析失败。'})}}

async function send(){const text=input.value.trim();if(!text||streaming.value)return;input.value='';resizeTextarea();if(!currentTitle.value)currentTitle.value=text.slice(0,20);push({role:'user',content:text});persistMessages();await bottom();startStream(text);companion(text)}
function handleKeydown(e){if(e.key==='Enter'&&(e.ctrlKey||e.metaKey)){e.preventDefault();send()}}

// 鼓励动画
function triggerEncouragementAnimation(){showEncouragement.value=true;displayedQuote.value='';let i=0;quoteInterval=setInterval(()=>{displayedQuote.value=encouragementQuote.value.slice(0,i++);if(i>encouragementQuote.value.length)clearInterval(quoteInterval)},50);setTimeout(()=>{showEncouragement.value=false;clearInterval(quoteInterval)},3500)}

// 呼吸练习
function startBreathing(){showCrisisResources.value=false;showBreathing.value=true;breathingOffset.value=339.3;runBreathingCycle()}
function runBreathingCycle(){let phaseIdx=0;function runPhase(){if(!showBreathing.value)return;const p=BREATHING_PHASES[phaseIdx];breathingPhase.value=p.phase;breathingInstruction.value=p.instruction;breathingCountdown.value=Math.round(p.duration/1000);breathingOffset.value=p.offset;let countdown=Math.round(p.duration/1000);const ct=setInterval(()=>{breathingCountdown.value=--countdown;if(countdown<=0)clearInterval(ct)},1000);breathingTimer=setTimeout(()=>{clearInterval(ct);phaseIdx=(phaseIdx+1)%BREATHING_PHASES.length;runPhase()},p.duration)}runPhase()}
function closeBreathing(){showBreathing.value=false;clearTimeout(breathingTimer)}

// 本周目标
async function handleRecordGoal(){try{const {data}=await recordWeeklyGoal(username.value);weeklyGoal.value=data;flash('目标记录成功！','success')}catch{flash('记录失败','error')}}

// 记忆管理
async function handleDeleteMemory(id){try{await deleteMemory(id,username.value);memories.value=memories.value.filter(m=>m.id!==id);flash('记忆已删除','success')}catch{flash('删除失败','error')}}

watch(messages,()=>{persistMessages();bottom()},{deep:true});
watch(username,()=>{loadSessions();loadMood();loadStreak();loadWeeklyGoal()});
watch(themePreference,v=>localStorage.setItem(THEME,v));
watch(sessionId,v=>localStorage.setItem(LAST,v));

onMounted(async()=>{const local=hydrateLocal();messages.value=local||[welcome()];resizeTextarea();await Promise.all([loadSessions(),loadMood(),loadStreak(),loadWeeklyGoal(),loadCrisisResources()]);const current=sessions.value.find(s=>String(s.sessionId)===sessionId.value);if(current)await openSession(current);bottom()});
onBeforeUnmount(()=>{currentEventSource.value?.close();clearTimeout(flash.t);clearInterval(quoteInterval);clearTimeout(breathingTimer)});
</script>
