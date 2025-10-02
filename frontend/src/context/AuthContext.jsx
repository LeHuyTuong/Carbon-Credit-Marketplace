import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const AuthContext = createContext(null);

function readPersistedAuth() {
  //ưu tiên session cho phiên hiện tại, nếu không có thì lấy local
  const fromSession = sessionStorage.getItem('auth');
  if (fromSession) return JSON.parse(fromSession);
  const fromLocal = localStorage.getItem('auth');
  if (fromLocal) return JSON.parse(fromLocal);
  return { user: null, token: null };
}

export function AuthProvider({ children }) {
    //lấy data đã lưu cho remember me
  const [{ user, token }, setAuth] = useState(() => readPersistedAuth());

  useEffect(() => {
    //không làm gì ở đây, state đã hydrate từ storage ở trên
  }, []);

  const login = (nextUser, nextToken, remember = false) => {
    const payload = { user: nextUser, token: nextToken };
    setAuth(payload);
    if (remember) {
      localStorage.setItem('auth', JSON.stringify(payload));
      sessionStorage.removeItem('auth');
    } else {
      sessionStorage.setItem('auth', JSON.stringify(payload));
      localStorage.removeItem('auth');
    }
  };

  const logout = () => {
    setAuth({ user: null, token: null });
    sessionStorage.removeItem('auth');
    localStorage.removeItem('auth');
  };

  //giảm re-render
  const value = useMemo(
    () => ({ user, token, isAuthenticated: !!user, login, logout }),
    [user, token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
