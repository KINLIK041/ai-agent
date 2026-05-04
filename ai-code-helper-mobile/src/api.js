import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';
const api = axios.create({ baseURL: BASE_URL, timeout: 30000 });

// ========== 会话相关 ==========
export const getSessionList = (username) => api.get('/ai/session/list', { params: { username } });
export const getSessionDetail = (sessionId) => api.get(`/ai/session/detail/${sessionId}`);
export const saveSession = (payload) => api.post('/ai/session/save', payload);
export const deleteSession = (sessionId) => api.get(`/ai/session/delete/${sessionId}`);

// ========== 聊天 & 陪伴 ==========
export const sendCompanionMessage = (payload) => api.post('/ai/companion/chat', payload);
export const createChatStreamUrl = (sessionId, message) =>
  `${BASE_URL}/ai/chat?memoryId=0&message=${encodeURIComponent(message)}`;

// ========== 情绪相关 ==========
export const saveMood = (payload) => api.post('/ai/companion/mood', payload);
export const fetchMoodOverview = (username, days = 90) =>
  api.get('/ai/companion/mood/overview', { params: { username, days } });

// ========== 打卡 & 成就系统 (新) ==========
export const fetchMoodStreak = (username) =>
  api.get('/ai/companion/mood/streak', { params: { username } });

export const recordMoodCheckIn = (payload) =>
  api.post('/ai/companion/mood/check-in', payload);

// ========== 本周目标 (新) ==========
export const fetchWeeklyGoal = (username) =>
  api.get('/ai/companion/goal/current', { params: { username } });

export const recordWeeklyGoal = (username) =>
  api.post('/ai/companion/goal/record', { params: { username } });

// ========== AI 记忆管理 (新) ==========
export const fetchMemories = (username) =>
  api.get('/ai/memory/list', { params: { username } });

export const deleteMemory = (id, username) =>
  api.delete(`/ai/memory/${id}`, { params: { username } });

// ========== 危机热线 (新) ==========
export const fetchCrisisResources = () =>
  api.get('/ai/companion/crisis-resources');
