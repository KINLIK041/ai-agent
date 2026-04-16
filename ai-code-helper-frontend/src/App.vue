<template>
<div class="app-shell" :class="themeClass">
  <aside class="sidebar" :class="{open:sidebarOpen}">
    <div class="sidebar-header"><h2>历史对话</h2><button class="new-chat-btn" @click="newSession">＋ 新对话</button></div>
    <label class="search-box"><span>⌕</span><input v-model.trim="keyword" placeholder="搜索会话" /></label>
    <div class="session-list"><template v-for="g in grouped" :key="g.key"><div class="group-title"><span>{{ g.label }}</span><small>{{ g.items.length }}</small></div><button v-for="s in g.items" :key="s.sessionId" class="session-item" :class="{active:String(s.sessionId)===sessionId}" @click="openSession(s)"><div class="session-title">{{ cut(s.title || '未命名对话', 18) }}</div><div class="session-preview">{{ cut(s.lastMessage || '暂无摘要', 26) }}</div><div class="session-time">{{ formatSessionTime(s.updatedAt) }}</div></button></template><div v-if="!grouped.length" class="empty-state">暂无匹配会话</div></div>
  </aside>

  <main class="chat-layout">
    <header class="hero-panel"><div class="header-left"><button class="menu-toggle" @click="sidebarOpen=!sidebarOpen">☰</button><div><p class="eyebrow">AI COMPANION · CAREER · CODING</p><h1>AI MATE</h1><p class="hero-copy">支持跨设备恢复完整会话历史、天气建议、以及最近 90 天情绪量化追踪。</p></div></div><div class="session-card"><div class="session-row"><span>Chat ID</span><strong>#{{ sessionId }}</strong></div><div class="session-row"><span>用户</span><input v-model="username" class="name-input" maxlength="20" /></div><div class="session-row"><span>主题</span><select v-model="themePreference" class="theme-select"><option value="auto">自动</option><option value="light">浅色</option><option value="dark">深色</option></select></div></div></header>
    <section ref="panelRef" class="message-panel"><div v-for="m in messages" :key="m.id" class="message-row" :class="m.role"><article class="bubble" :class="['bubble-'+m.role,m.variant?`bubble-${m.variant}`:'']"><div v-if="m.role!=='system'" class="bubble-meta"><span :class="m.role==='user'?'speaker-user':'speaker-ai'">{{ m.role==='user' ? username : (m.label || 'AI MATE') }}</span><time>{{ formatTime(m.timestamp) }}</time></div><div v-if="m.typing" class="typing-indicator"><span></span><span></span><span></span></div><div v-else :class="m.role==='system'?'system-text':'bubble-text markdown-body'" v-html="renderMarkdown(m.content)"></div></article></div></section>
    <section class="composer-panel"><div v-if="notice" class="notice" :class="notice.type">{{ notice.text }}</div><div v-if="weather.city" class="weather-banner"><strong>{{ weather.city }}</strong><span>{{ weather.description }} · {{ weather.temperature }} · 湿度 {{ weather.humidity }}</span><p>{{ weather.suggestion }}</p></div><div v-if="highRiskWarning" class="notice error">{{ highRiskWarning }}</div><form class="composer" @submit.prevent="send"><textarea ref="textareaRef" v-model="input" class="composer-input" rows="1" placeholder="输入内容，Enter 换行，Ctrl+Enter 发送..." @input="resizeTextarea" @keydown="handleKeydown"></textarea><div class="composer-footer"><p class="hint-text">完整历史已同步保存到 MySQL，会自动缓存到本地以加快恢复速度。</p><button class="send-button" :disabled="streaming || !input.trim()">{{ streaming ? 'AI 思考中...' : '发送' }}</button></div></form></section>
  </main>

  <aside class="mood-panel"><div class="mood-card"><div class="mood-head"><h3>90 天情绪记录</h3><span>{{ moodRecords.length }} 条</span></div><div class="mood-legend"><span class="legend positive">积极</span><span class="legend neutral">平稳</span><span class="legend negative">消极</span></div><div class="mood-grid"><div v-for="d in moodDays" :key="d.date" class="mood-day" :class="'mood-'+d.level" :title="`${d.date} ${d.label}`"></div></div><p class="mood-summary">{{ moodSummary }}</p><ul class="mood-list"><li v-for="item in moodRecords.slice(0,6)" :key="item.recordDate+item.emotionType"><strong>{{ item.recordDate }}</strong><span>{{ item.emotionType }}</span><em :class="item.positiveEmotion ? 'positive-text' : (item.highRisk ? 'negative-text' : '')">{{ item.emotionIntensity }}/10</em></li></ul></div></aside>
</div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import { createChatStreamUrl, fetchCareMessage, fetchMoodOverview, getSessionDetail, getSessionList, saveMood, saveSession, sendCompanionMessage } from './api';
const THEME='theme-preference',CACHE='ai-mate-session-cache',LAST='ai-mate-last-session-id';
const username=ref('KINLIK'),input=ref(''),streaming=ref(false),sessionId=ref(localStorage.getItem(LAST)||id()),messages=ref([]),sessions=ref([]),keyword=ref(''),themePreference=ref(localStorage.getItem(THEME)||'auto'),notice=ref(null),sidebarOpen=ref(false),panelRef=ref(null),textareaRef=ref(null),currentEventSource=ref(null),moodRecords=ref([]),currentTitle=ref(''),weather=ref({}),highRiskWarning=ref('');
marked.setOptions({breaks:true,gfm:true}); const renderer=new marked.Renderer(); renderer.code=({text,lang})=>{const l=hljs.getLanguage(lang||'')?lang:'plaintext';return `<pre><code class="hljs language-${l}">${hljs.highlight(text,{language:l}).value}</code></pre>`};
const themeClass=computed(()=>resolveTheme()==='light'?'theme-day':'theme-night');
const grouped=computed(()=>{const k=keyword.value.trim().toLowerCase();const list=sessions.value.filter(s=>!k||`${s.title||''} ${s.lastMessage||''}`.toLowerCase().includes(k)).sort((a,b)=>new Date(b.updatedAt||0)-new Date(a.updatedAt||0));const g={today:[],yesterday:[],week:[],earlier:[]};list.forEach(s=>g[bucket(s.updatedAt)].push(s));return[{key:'today',label:'今天',items:g.today},{key:'yesterday',label:'昨天',items:g.yesterday},{key:'week',label:'本周',items:g.week},{key:'earlier',label:'更早',items:g.earlier}].filter(x=>x.items.length)});
const moodDays=computed(()=>{const map=new Map(moodRecords.value.map(r=>[String(r.recordDate).slice(0,10),r]));const days=[];for(let i=89;i>=0;i--){const d=new Date();d.setDate(d.getDate()-i);const key=d.toISOString().slice(0,10);const row=map.get(key);days.push({date:key,label:row?.emotionType||'无记录',level:level(row)})}return days});
const moodSummary=computed(()=>moodRecords.value.length?`最近一条：${moodRecords.value[0]?.emotionType||'未知'}，共记录 ${moodRecords.value.length} 次。`:'最近 90 天暂无情绪记录。');
function id(){return String(Math.floor(100000+Math.random()*900000))} function resolveTheme(){return themePreference.value==='auto'?((new Date().getHours()>=6&&new Date().getHours()<18)?'light':'dark'):themePreference.value}
function store(k,v){localStorage.setItem(k,JSON.stringify(v))} function read(k){try{return JSON.parse(localStorage.getItem(k)||'{}')}catch{return {}}}
function persistMessages(){const c=read(CACHE);c[sessionId.value]=messages.value;store(CACHE,c)} function hydrateLocal(){const c=read(CACHE);return c[sessionId.value]||null}
function cut(t,n){return !t?'':(t.length>n?`${t.slice(0,n)}…`:t)} function renderMarkdown(t){return marked.parse((t||'').replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi,'').replace(/javascript:/gi,''),{renderer})}
function flash(text,type='info'){notice.value={text,type};clearTimeout(flash.t);flash.t=setTimeout(()=>notice.value=null,3000)} function bucket(d){if(!d)return'earlier';d=new Date(d);const n=new Date(),t=new Date(n.getFullYear(),n.getMonth(),n.getDate()),y=new Date(t),w=new Date(t);y.setDate(t.getDate()-1);w.setDate(t.getDate()-7);return d>=t?'today':d>=y?'yesterday':d>=w?'week':'earlier'}
function formatTime(t){
  return new Intl.DateTimeFormat('zh-CN',{hour:'2-digit',minute:'2-digit'}).format(t)}
function formatSessionTime(d){
  if(!d)return'刚刚';d=new Date(d);
  const x=Date.now()-d.getTime();
  if(x<60000)return'刚刚';
  if(x<3600000)return`${Math.floor(x/60000)} 分钟前`;
  if(x<86400000)return`${Math.floor(x/3600000)} 小时前`;
  return new Intl.DateTimeFormat('zh-CN',{month:'short',day:'numeric'}).format(d)}
function level(row){
  if(!row)return'none';
  if(row.highRisk)return'negative';
  if(row.positiveEmotion)return'positive';
  if((row.emotionIntensity||0)>=7&&!(row.positiveEmotion))return'negative';
  if((row.emotionIntensity||0)>=5)return'neutral';
  return'positive'}
function push(payload){
  messages.value.push({id:crypto.randomUUID(),timestamp:Date.now(),...payload})}
async function bottom(){await nextTick();if(panelRef.value)panelRef.value.scrollTop=panelRef.value.scrollHeight} function resizeTextarea(){const e=textareaRef.value;if(!e)return;e.style.height='auto';e.style.height=`${Math.min(e.scrollHeight,180)}px`}
async function loadSessions(){try{sessions.value=(await getSessionList(username.value)).data||[]}catch(e){flash(e.friendlyMessage||'加载会话失败','error')}} async function loadMood(){try{const {data}=await fetchMoodOverview(username.value,90);moodRecords.value=(data.records||[]).slice().sort((a,b)=>String(b.recordDate).localeCompare(String(a.recordDate)))}catch{moodRecords.value=[]}}
async function openSession(s){sessionId.value=String(s.sessionId);currentTitle.value=s.title||'';localStorage.setItem(LAST,sessionId.value);const local=hydrateLocal();if(local)messages.value=local;try{const {data}=await getSessionDetail(sessionId.value);messages.value=JSON.parse(data.messagesJson||'[]');if(!messages.value.length)messages.value=[welcomeHint(s.title)];persistMessages();flash('已从云端恢复该会话的完整历史。','success')}catch(e){if(!local)messages.value=[welcomeHint(s.title)];flash(e.friendlyMessage||'会话恢复失败','error')}sidebarOpen.value=false;bottom()}
function newSession(){sessionId.value=id();localStorage.setItem(LAST,sessionId.value);currentTitle.value='';messages.value=[welcome()];input.value='';sidebarOpen.value=false;persistMessages();nextTick(()=>{resizeTextarea();bottom()})}
function welcome(){return{id:crypto.randomUUID(),role:'ai',label:'AI MATE',variant:'welcome',content:'你好，我是 **AI MATE**。你可以问我编程学习路线、前端项目实战、面试准备、简历优化，也可以直接和我聊聊你的状态。',timestamp:Date.now()}} function welcomeHint(title){return{id:crypto.randomUUID(),role:'system',content:`已恢复到会话 **${title||'未命名对话'}**。完整历史会优先从云端加载，本地缓存仅用于加速恢复。`,timestamp:Date.now()}}
async function startStream(text){const ai={id:crypto.randomUUID(),role:'ai',label:'AI MATE',content:'',typing:true,timestamp:Date.now()};messages.value.push(ai);await bottom();const es=new EventSource(createChatStreamUrl(sessionId.value,text));currentEventSource.value=es;streaming.value=true;es.onmessage=async ev=>{if(ev.data==='[DONE]'){ai.typing=false;es.close();currentEventSource.value=null;streaming.value=false;await saveCurrentSession(text,ai.content);return}if(ai.typing){ai.typing=false;ai.content=''}ai.content+=ev.data;await bottom()};es.onerror=()=>{es.close();currentEventSource.value=null;ai.typing=false;if(!ai.content){ai.content='当前流式连接中断，请检查后端服务是否已启动。';ai.variant='error'}streaming.value=false;flash('流式聊天连接中断','error')}}
async function saveCurrentSession(userMessage,aiResponse){const title=currentTitle.value||userMessage.slice(0,20),lastMessage=aiResponse||userMessage,payload={username:username.value,sessionId:sessionId.value,title,lastMessage,messagesJson:JSON.stringify(messages.value),pinned:false};try{await saveSession(payload);flash('会话已同步到云端。','success')}catch(e){flash(e.friendlyMessage||'保存会话失败','error')}persistMessages();await loadSessions()}
async function companion(text){try{await saveMood({username:username.value,moodDescription:text,triggerEvent:''});const {data}=await sendCompanionMessage({username:username.value,message:text});weather.value=data.weather||{};highRiskWarning.value=data.emotion?.highRisk?'检测到高危情绪，请优先联系可信任的人或专业热线 12356。':'';push({role:'ai',label:'AI MATE',variant:'companion',content:data.message,emotion:data.emotion});if(data.careAdvice){push({role:'system',variant:data.emotion?.highRisk?'error':'care',content:data.careAdvice})}loadMood()}catch(e){push({role:'system',variant:'error',content:e.friendlyMessage||'陪伴分析失败。'})}}
async function send(){const text=input.value.trim();if(!text||streaming.value)return;input.value='';resizeTextarea();if(!currentTitle.value)currentTitle.value=text.slice(0,20);push({role:'user',content:text});persistMessages();await bottom();startStream(text);companion(text)} function handleKeydown(e){if(e.key==='Enter'&&(e.ctrlKey||e.metaKey)){e.preventDefault();send()}}
watch(messages,()=>{persistMessages();bottom()},{deep:true}); watch(username,()=>{loadSessions();loadMood()}); watch(themePreference,v=>localStorage.setItem(THEME,v)); watch(sessionId,v=>localStorage.setItem(LAST,v));
onMounted(async()=>{const local=hydrateLocal();messages.value=local||[welcome()];resizeTextarea();await loadSessions();await loadMood();const current=sessions.value.find(s=>String(s.sessionId)===sessionId.value);if(current)await openSession(current);bottom()}); onBeforeUnmount(()=>currentEventSource.value?.close())
</script>
