import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, userApi } from '../api/api';

const AuthContext = createContext(null);

const STAFF_ROLES = ['ADMIN', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT'];

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('stp_user');
    return stored ? JSON.parse(stored) : null;
  });
  const [loading, setLoading] = useState(Boolean(localStorage.getItem('stp_token')));

  useEffect(() => {
    if (!localStorage.getItem('stp_token')) {
      setLoading(false);
      return;
    }
    userApi
      .me()
      .then((fresh) => {
        setUser(fresh);
        localStorage.setItem('stp_user', JSON.stringify(fresh));
      })
      .catch(() => {
        localStorage.removeItem('stp_token');
        localStorage.removeItem('stp_user');
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const persist = useCallback((session) => {
    localStorage.setItem('stp_token', session.token);
    localStorage.setItem('stp_user', JSON.stringify(session.user));
    setUser(session.user);
    return session.user;
  }, []);

  const login = useCallback(async (payload) => persist(await authApi.login(payload)), [persist]);
  const register = useCallback(async (payload) => persist(await authApi.register(payload)), [persist]);
  const oauthLogin = useCallback(async (payload) => persist(await authApi.oauth(payload)), [persist]);

  const logout = useCallback(() => {
    localStorage.removeItem('stp_token');
    localStorage.removeItem('stp_user');
    setUser(null);
  }, []);

  const updateUser = useCallback((next) => {
    setUser(next);
    localStorage.setItem('stp_user', JSON.stringify(next));
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      register,
      oauthLogin,
      logout,
      updateUser,
      isStaff: Boolean(user && STAFF_ROLES.includes(user.role)),
      isAdmin: user?.role === 'ADMIN',
      isOperator: user?.role === 'LOGISTICS_OPERATOR',
      isSupport: user?.role === 'SUPPORT_AGENT',
      isBusiness: user?.role === 'BUSINESS_CLIENT',
    }),
    [user, loading, login, register, oauthLogin, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
