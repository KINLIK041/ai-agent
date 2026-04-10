<template>
<div class="app-shell" :class="themeClass">
<div class="ambient ambient-a"></div><div class="ambient ambient-b"></div>
<div v-if="sidebarOpen" class="mobile-backdrop" @click="sidebarOpen=false"></div>
<aside class="sidebar" :class="{'sidebar-open':sidebarOpen}">
<div class="sidebar-header"><h2>历史对话</h2><button class="new-chat-btn" @click="createNewSession">+ 新对话</button></div>
<label class="search-box"><span>⌕</span><input v-model.trim="sessionSearch" placeholder="搜索会话" /></label>
<div class="session-list">
<template v-for="g in groupedSessions" :key="g.key"><div class="group-title"><span>{{g.label}}</span><small>{{g.items.length}}</small></div>
<div v-for="s in g.items" :key="s.sessionId" class="session-item" :class="{active:String(s.sessionId)===String(memoryId)}" @click="switchSession(s)">
<div class="session-title-row"><div class="session-title">{{cut(s.title||'未命名对话',20)}}</div><span v-if="s.pinned" class="pin-tag">置顶</span></div>
<div class="session-preview">{{cut(s.lastMessage||'暂无摘要',30)}}</div><div class="session-time">{{formatSessionTime(s.updatedAt)}}</div>
</div></template>
<div v-if="!groupedSessions.length" class="empty-state">暂无匹配会话</div></div></aside>
<main class="chat-layout">
<header class="hero-panel"><div class="header-left"><button class="menu-toggle" @click="sidebarOpen=!sidebarOpen">☰</button><div><p class="eyebrow">AI COMPANION · CAREER · CODING</p><h1>AI MATE</h1><p class="hero-copy">修复 API 路径，升级主题切换、历史会话和 Markdown 消息体验。</p></div></div>
<div class="session-card"><div class="session-row"><span>Chat ID</span><strong>#{{memoryId}}</strong></div><div class="session-row"><span>用户</span><input v-model="username" class="name-input" maxlength="20" /></div><div class="session-row compact"><span>状态</span><strong :class="isStreaming?'status-live':'status-idle'">{{isStreaming?'实时回复中':'等待输入'}}</strong></div><div class="session-row compact"><span>主题</span><select v-model="themePreference" class="theme-select"><option value="auto">自动</option><option value="light">浅色</option><option value="dark">深色</option></select></div><button class="check-in-btn" @click="requestCheckIn" :disabled="checkInLoading">{{checkInLoading?'签到中...':'夜间签到'}}</button></div></header>
<section ref="messagePanelRef" class="message-panel"><div v-for="m in messages" :key="m.id" class="message-row" :class="m.role==='system'?'is-system':m.role==='user'?'is-user':'is-ai'"><article class="bubble" :class="[`bubble-${m.role}`,m.variant?`bubble-${m.variant}`:'']"><div v-if="m.role!=='system'" class="bubble-meta"><span>{{m.role==='user'?username:(m.label||'AI MATE')}}</span><time>{{formatTime(m.timestamp)}}</time></div><div v-if="m.typing" class="typing-indicator"><span></span><span></span><span></span></div><div v-else :class="m.role==='system'?'system-text':'bubble-text markdown-body'" v-html="renderMarkdown(m.content)"></div><div v-if="m.emotion" class="emotion-card"><span>情绪：{{emotionMap[m.emotion.type]||m.emotion.type}}</span><span>强度：{{Math.round((m.emotion.intensity||0)*100)}}%</span><span>{{m.emotion.needsSupport?'建议重点关怀':'状态平稳'}}</span></div></article></div></section>
<section class="composer-panel"><div v-if="notice" class="notice" :class="notice.type">{{notice.text}}</div><div class="quick-actions"><button type="button" @click="fillPrompt('帮我制定一份前端面试冲刺计划')">面试冲刺</button><button type="button" @click="fillPrompt('我最近学 Vue 很焦虑，想要一个学习路线')">学习焦虑</button><button type="button" @click="fillPrompt('帮我优化前端简历项目描述')">简历优化</button></div><form class="composer" @submit.prevent="handleSend"><textarea ref="textareaRef" v-model="inputMessage" class="composer-input" rows="1" placeholder="输入你关于编程学习、求职准备或情绪状态的问题..." @input="autoResizeTextarea" @keydown="handleComposerKeydown"></textarea><div class="composer-footer"><p class="hint-text">Ctrl + Enter 发送，Enter 换行。</p><button type="submit" class="send-button" :disabled="isStreaming||!inputMessage.trim()">{{isStreaming?'AI 思考中...':'发送'}}</button></div></form></section>
</main></div>
</template>

<script setup>
import {nextTick,onBeforeUnmount,onMounted,ref,watch,computed} from 'vue'
import {marked} from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import {createChatStreamUrl,fetchCareMessage,fetchNightlyCheckIn,saveMood,sendCompanionMessage,saveSession,getSessionList,deleteSession} from './api'
const THEME='theme-preference',CACHE='ai-mate-session-cache',META='ai-mate-session-meta'
marked.setOptions({breaks:true,gfm:true});const renderer=new marked.Renderer();renderer.code=({text,lang})=>{const language=hljs.getLanguage(lang||'')?lang:'plaintext';return `<pre><code class="hljs language-${language}">${hljs.highlight(text,{language}).value}</code></pre>`}
const username=ref('KINLIK'),inputMessage=ref(''),isStreaming=ref(false),memoryId=ref(String(Math.floor(100000+Math.random()*900000))),messagePanelRef=ref(null),textareaRef=ref(null),currentEventSource=ref(null),sidebarOpen=ref(false),sessions=ref([]),currentSessionTitle=ref(''),sessionSearch=ref(''),themePreference=ref(localStorage.getItem(THEME)||'auto'),checkInLoading=ref(false),notice=ref(null)
const emotionMap={sadness:'失落',anxiety:'焦虑',stress:'压力',joy:'愉悦',anger:'烦躁',calm:'平静'}
const welcome=()=>({id:crypto.randomUUID(),role:'ai',label:'AI MATE',variant:'welcome',content:'你好，我是 **AI MATE**。你可以问我编程学习路线、前端项目实战、面试准备、简历优化，也可以直接和我聊聊你的状态。',timestamp:Date.now()})
const messages=ref([welcome()])
const resolvedTheme=computed(()=>themePreference.value==='auto'?((new Date().getHours()>=6&&new Date().getHours()<18)?'light':'dark'):themePreference.value)
const themeClass=computed(()=>resolvedTheme.value==='light'?'theme-day':'theme-night')
const groupedSessions=computed(()=>{const k=sessionSearch.value.trim().toLowerCase();const list=sessions.value.filter(s=>!k||`${s.title||''} ${s.lastMessage||''}`.toLowerCase().includes(k)).sort((a,b)=>(Number(!!b.pinned)-Number(!!a.pinned))||(new Date(b.updatedAt||0)-new Date(a.updatedAt||0)));const g={today:[],yesterday:[],week:[],earlier:[]};list.forEach(s=>g[groupOf(s.updatedAt)].push(s));return[{key:'today',label:'今天',items:g.today},{key:'yesterday',label:'昨天',items:g.yesterday},{key:'week',label:'本周',items:g.week},{key:'earlier',label:'更早',items:g.earlier}].filter(x=>x.items.length)})
const js=k=>{try{return JSON.parse(localStorage.getItem(k)||'{}')}catch{return {}}};const save=(k,v)=>localStorage.setItem(k,JSON.stringify(v))
const persist=(id,val=messages.value)=>{const c=js(CACHE);c[id]=val;save(CACHE,c)};const persistMeta=p=>save(META,{...js(META),...p})
const mergeMeta=list=>{const m=js(META);return(list||[]).map(s=>({...s,...(m[s.sessionId]||{})}))}
const cut=(t,n)=>!t?'':t.length>n?`${t.slice(0,n)}…`:t
const renderMarkdown=t=>marked.parse((t||'').replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi,'').replace(/javascript:/gi,''),{renderer})
const show=(text,type='info')=>{notice.value={text,type};clearTimeout(show.t);show.t=setTimeout(()=>notice.value=null,3000)}
const groupOf=d=>{if(!d)return'earlier';d=new Date(d);const n=new Date(),t=new Date(n.getFullYear(),n.getMonth(),n.getDate()),y=new Date(t),w=new Date(t);y.setDate(t.getDate()-1);w.setDate(t.getDate()-7);return d>=t?'today':d>=y?'yesterday':d>=w?'week':'earlier'}
const formatTime=t=>new Intl.DateTimeFormat('zh-CN',{hour:'2-digit',minute:'2-digit'}).format(t)
const formatSessionTime=d=>{if(!d)return'刚刚';d=new Date(d);const x=Date.now()-d.getTime();if(x<60000)return'刚刚';if(x<3600000)return`${Math.floor(x/60000)}分钟前`;if(x<86400000)return`${Math.floor(x/3600000)}小时前`;return new Intl.DateTimeFormat('zh-CN',{month:'short',day:'numeric'}).format(d)}
const pushMessage=p=>messages.value.push({id:crypto.randomUUID(),timestamp:Date.now(),...p})
async function scrollToBottom(){await nextTick();if(messagePanelRef.value)messagePanelRef.value.scrollTop=messagePanelRef.value.scrollHeight}
function autoResizeTextarea(){const el=textareaRef.value;if(!el)return;el.style.height='auto';el.style.height=`${Math.min(el.scrollHeight,180)}px`}
const fillPrompt=p=>{inputMessage.value=p;nextTick(autoResizeTextarea)}
async function loadSessions(){try{sessions.value=mergeMeta((await getSessionList(username.value)).data)}catch(e){show(e.friendlyMessage||'加载会话列表失败','error')}}
function switchSession(session){memoryId.value=String(session.sessionId);currentSessionTitle.value=session.title||'';messages.value=js(CACHE)[session.sessionId]||[{id:crypto.randomUUID(),role:'system',content:`已切换到之前的对话：${session.title||'未命名对话'}`,timestamp:Date.now()}];sidebarOpen.value=false;nextTick(scrollToBottom)}
async function handleDeleteSession(sessionId){if(!sessionId||!confirm('确定要删除这个会话吗？'))return;try{await deleteSession(sessionId);await loadSessions();if(String(memoryId.value)===String(sessionId))createNewSession()}catch(e){show(e.friendlyMessage||'删除失败，请重试','error')}}
function createNewSession(){memoryId.value=String(Math.floor(100000+Math.random()*900000));currentSessionTitle.value='';messages.value=[welcome()];sidebarOpen.value=false;inputMessage.value='';nextTick(()=>{autoResizeTextarea();scrollToBottom()})}
async function requestCheckIn(){checkInLoading.value=true;try{const {data}=await fetchNightlyCheckIn(username.value);pushMessage({role:'system',variant:'care',content:`### 夜间签到\n${data.checkInMessage}`})}catch(e){show(e.friendlyMessage||'签到失败','error')}finally{checkInLoading.value=false}}
async function startChatStream(message){const ai={id:crypto.randomUUID(),role:'ai',label:'AI MATE',content:'',typing:true,timestamp:Date.now()};messages.value.push(ai);await scrollToBottom();const es=new EventSource(createChatStreamUrl(memoryId.value,message));currentEventSource.value=es;isStreaming.value=true;es.onmessage=async ev=>{if(ev.data==='[DONE]'){ai.typing=false;es.close();currentEventSource.value=null;isStreaming.value=false;await saveCurrentSession(message,ai.content);return}if(ai.typing){ai.typing=false;ai.content=''}ai.content+=ev.data;await scrollToBottom()};es.onerror=()=>{es.close();currentEventSource.value=null;ai.typing=false;if(!ai.content){ai.content='当前流式连接中断，请检查后端服务是否已启动。';ai.variant='error'}isStreaming.value=false}}
async function saveCurrentSession(userMessage,aiResponse){try{const title=currentSessionTitle.value||userMessage.substring(0,20);await saveSession({username:username.value,sessionId:String(memoryId.value),title,lastMessage:aiResponse||userMessage});persist(String(memoryId.value));persistMeta({[String(memoryId.value)]:{title,lastMessage:aiResponse||userMessage,updatedAt:new Date().toISOString(),pinned:sessions.value.find(i=>String(i.sessionId)===String(memoryId.value))?.pinned||false}});await loadSessions()}catch(e){show(e.friendlyMessage||'保存会话失败','error')}}
async function runCompanionFlow(message){try{await saveMood({username:username.value,moodDescription:message,triggerEvent:''});const {data}=await sendCompanionMessage({username:username.value,message});pushMessage({role:'ai',label:'陪伴助手',variant:'companion',content:data.message,emotion:data.emotion});if(data.emotion?.needsSupport){try{const care=await fetchCareMessage(username.value,message);pushMessage({role:'ai',label:'主动关怀',variant:'care',content:care.data})}catch(e){show(e.friendlyMessage||'主动关怀接口调用失败','error')}}}catch(e){pushMessage({role:'system',variant:'error',content:e.friendlyMessage||'陪伴分析或情绪关怀接口调用失败。'})}}
async function handleSend(){const message=inputMessage.value.trim();if(!message||isStreaming.value)return;inputMessage.value='';autoResizeTextarea();if(!currentSessionTitle.value)currentSessionTitle.value=message.substring(0,20);pushMessage({role:'user',content:message});await scrollToBottom();startChatStream(message);runCompanionFlow(message)}
function handleComposerKeydown(e){if(e.key==='Enter'&&(e.ctrlKey||e.metaKey)){e.preventDefault();handleSend()}}
watch(messages,v=>{persist(String(memoryId.value),v);scrollToBottom()},{deep:true});watch(username,loadSessions);watch(themePreference,v=>localStorage.setItem(THEME,v))
onMounted(()=>{const c=js(CACHE)[memoryId.value];if(c)messages.value=c;else persist(String(memoryId.value),messages.value);autoResizeTextarea();scrollToBottom();loadSessions()})
onBeforeUnmount(()=>{currentEventSource.value?.close()})
</script>
