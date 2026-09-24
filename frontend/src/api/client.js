import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

const client = axios.create({ baseURL });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('stp_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const path = window.location.pathname;
    if (status === 401 && !path.startsWith('/login') && !path.startsWith('/track')) {
      localStorage.removeItem('stp_token');
      localStorage.removeItem('stp_user');
      window.location.assign('/login?expired=1');
    }
    return Promise.reject(error);
  },
);

/** Pulls the server's message out of an error response, with a readable fallback. */
export function errorMessage(error, fallback = 'Something went wrong. Try again.') {
  const data = error?.response?.data;
  if (!data) return error?.message || fallback;
  if (data.fields) {
    const first = Object.values(data.fields)[0];
    if (first) return first;
  }
  return data.message || fallback;
}

export default client;
