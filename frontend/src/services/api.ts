import axios from 'axios';

// Khi chạy trên Vercel, luôn dùng URL tương đối '' để request đi qua Vercel Reverse Proxy (/api -> Render)
// Điều này giúp cookie JSESSIONID trở thành First-Party, không bao giờ bị Safari/iOS chặn.
const isVercel = typeof window !== 'undefined' && window.location.hostname.endsWith('vercel.app');
const API_BASE_URL = isVercel ? '' : (import.meta.env.VITE_API_URL || '');

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If unauthorized and not on login or public checkin page
    if (error.response?.status === 401) {
      const path = window.location.pathname;
      if (!path.startsWith('/login') && !path.startsWith('/checkin')) {
        // can redirect to login
      }
    }
    return Promise.reject(error);
  }
);

export default api;
