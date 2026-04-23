import axios from 'axios';

export const API_BASE_URL = 'http://localhost:8081/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 90000,
  headers: { 'Content-Type': 'application/json' },
});

function normalizeErrorMessage(error) {
  const status = error.response?.status;
  if (status === 404) return '请求的接口不存在，请确认后端服务与 API 路径配置。';
  if (status >= 500) return '服务暂时不可用，请稍后重试。';
  if (error.request) return '无法连接后端服务，请确认 8081 端口服务已启动。';
  return error.response?.data?.message || error.message || '请求失败，请稍后重试。';
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    error.friendlyMessage = normalizeErrorMessage(error);
    return Promise.reject(error);
  }
);

export function createChatStreamUrl(memoryId, message) {
  const url = new URL(`${API_BASE_URL}/ai/chat`);
  url.searchParams.set('memoryId', memoryId);
  url.searchParams.set('message', message);
  return url.toString();
}

export const sendCompanionMessage = (payload) => apiClient.post('/ai/companion/chat', payload);
export const saveMood = (payload) => apiClient.post('/ai/companion/mood', payload);
export const fetchCareMessage = (username, context) => apiClient.get('/ai/companion/care', { params: { username, context }, responseType: 'text' });
export const fetchNightlyCheckIn = (username) => apiClient.get('/ai/companion/check-in', { params: { username } });
export const fetchMoodOverview = (username, days = 90) => apiClient.get('/ai/companion/mood/overview', { params: { username, days } });
export const saveSession = (payload) => apiClient.post('/ai/session/save', payload);
export const getSessionList = (username) => apiClient.get('/ai/session/list', { params: { username } });
export const getSessionDetail = (sessionId) => apiClient.get(`/ai/session/detail/${sessionId}`);
export const deleteSession = (sessionId) => apiClient.get(`/ai/session/delete/${sessionId}`);
