import client from './client';

export const authApi = {
  login: (payload) => client.post('/api/auth/login', payload).then((r) => r.data),
  register: (payload) => client.post('/api/auth/register', payload).then((r) => r.data),
  oauth: (payload) => client.post('/api/auth/oauth2', payload).then((r) => r.data),
  forgotPassword: (payload) => client.post('/api/auth/forgot-password', payload).then((r) => r.data),
  resetPassword: (payload) => client.post('/api/auth/reset-password', payload).then((r) => r.data),
  changePassword: (payload) => client.post('/api/auth/change-password', payload).then((r) => r.data),
};

export const userApi = {
  me: () => client.get('/api/users/me').then((r) => r.data),
  updateProfile: (payload) => client.put('/api/users/me', payload).then((r) => r.data),
  myActivity: (params) => client.get('/api/users/me/activity', { params }).then((r) => r.data),
  drivers: () => client.get('/api/users/drivers').then((r) => r.data),
  list: (params) => client.get('/api/users', { params }).then((r) => r.data),
  updateRole: (id, role) => client.patch(`/api/users/${id}/role`, { role }).then((r) => r.data),
  updateStatus: (id, active) => client.patch(`/api/users/${id}/status`, { active }).then((r) => r.data),
  remove: (id) => client.delete(`/api/users/${id}`).then((r) => r.data),
  activity: (params) => client.get('/api/users/activity', { params }).then((r) => r.data),
};

export const shipmentApi = {
  create: (payload) => client.post('/api/shipments', payload).then((r) => r.data),
  list: (params) => client.get('/api/shipments', { params }).then((r) => r.data),
  active: () => client.get('/api/shipments/active').then((r) => r.data),
  delayed: () => client.get('/api/shipments/delayed').then((r) => r.data),
  get: (id) => client.get(`/api/shipments/${id}`).then((r) => r.data),
  timeline: (id) => client.get(`/api/shipments/${id}/timeline`).then((r) => r.data),
  eta: (id) => client.get(`/api/shipments/${id}/eta`).then((r) => r.data),
  update: (id, payload) => client.put(`/api/shipments/${id}`, payload).then((r) => r.data),
  updateStatus: (id, payload) => client.patch(`/api/shipments/${id}/status`, payload).then((r) => r.data),
  updateLocation: (id, payload) => client.patch(`/api/shipments/${id}/location`, payload).then((r) => r.data),
  assignDriver: (id, driverId) => client.patch(`/api/shipments/${id}/driver`, { driverId }).then((r) => r.data),
  cancel: (id, reason) => client.post(`/api/shipments/${id}/cancel`, { reason }).then((r) => r.data),
  remove: (id) => client.delete(`/api/shipments/${id}`).then((r) => r.data),
};

export const trackApi = {
  track: (trackingNumber) => client.get(`/api/public/track/${trackingNumber}`).then((r) => r.data),
  eta: (trackingNumber) => client.get(`/api/public/track/${trackingNumber}/eta`).then((r) => r.data),
};

export const routeApi = {
  forShipment: (shipmentId) => client.get(`/api/routes/shipment/${shipmentId}`).then((r) => r.data),
  plan: (shipmentId, payload) => client.post(`/api/routes/shipment/${shipmentId}/plan`, payload).then((r) => r.data),
  optimize: (shipmentId) => client.post(`/api/routes/shipment/${shipmentId}/optimize`).then((r) => r.data),
  all: () => client.get('/api/routes').then((r) => r.data),
};

export const podApi = {
  capture: (shipmentId, payload) => client.post(`/api/pod/shipment/${shipmentId}`, payload).then((r) => r.data),
  byShipment: (shipmentId) => client.get(`/api/pod/shipment/${shipmentId}`).then((r) => r.data),
  pending: (params) => client.get('/api/pod/pending', { params }).then((r) => r.data),
  verify: (podId, payload) => client.post(`/api/pod/${podId}/verify`, payload).then((r) => r.data),
  addPhoto: (podId, file) => {
    const form = new FormData();
    form.append('file', file);
    return client.post(`/api/pod/${podId}/photo`, form).then((r) => r.data);
  },
};

export const notificationApi = {
  list: (params) => client.get('/api/notifications', { params }).then((r) => r.data),
  unreadCount: () => client.get('/api/notifications/unread-count').then((r) => r.data),
  markRead: (id) => client.patch(`/api/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => client.patch('/api/notifications/read-all').then((r) => r.data),
  remove: (id) => client.delete(`/api/notifications/${id}`).then((r) => r.data),
};

export const analyticsApi = {
  customer: () => client.get('/api/analytics/customer').then((r) => r.data),
  business: () => client.get('/api/analytics/business').then((r) => r.data),
  admin: () => client.get('/api/analytics/admin').then((r) => r.data),
  routes: () => client.get('/api/analytics/routes').then((r) => r.data),
};

export const reportApi = {
  download: async (format, params) => {
    const response = await client.get(`/api/reports/shipments.${format}`, {
      params,
      responseType: 'blob',
    });
    const url = URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.download = `shipment-report.${format}`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  },
};
