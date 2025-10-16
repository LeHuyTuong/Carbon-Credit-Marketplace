import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

const AuthContext = createContext(null);

function readPersistedAuth() {
  //ưu tiên session cho phiên hiện tại, nếu không có thì lấy local
  const fromSession = sessionStorage.getItem("auth");
  if (fromSession) return JSON.parse(fromSession);

  const fromLocal = localStorage.getItem("auth");
  if (fromLocal) return JSON.parse(fromLocal);
  return { user: null, token: null, role: null };
}

export function AuthProvider({ children }) {
  //lấy data đã lưu cho remember me
  const [{ user, token, role, companyId }, setAuth] = useState(() => ({
    ...{ user: null, token: null, role: null, companyId: null },
    ...readPersistedAuth(),
  }));

  const login = (nextUser, nextToken, remember = false) => {
    const payload = {
      user: nextUser,
      token: nextToken,
      role: Array.isArray(nextUser?.roles)
        ? nextUser.roles.map((r) => r.name)
        : nextUser?.role || null,
      companyId: nextUser?.companyId || nextUser?.id || null,
    };

    setAuth(payload);

    //lưu vào storage
    const storage = remember ? localStorage : sessionStorage;
    const clear = remember ? sessionStorage : localStorage;
    storage.setItem("auth", JSON.stringify(payload));
    clear.removeItem("auth"); //xóa data ở nơi còn lại
  };

  // logout
  const logout = () => {
    setAuth({ user: null, token: null, role: null, companyId: null });
    sessionStorage.removeItem("auth");
    localStorage.removeItem("auth");
  };

  //giảm re-render không cần thiết
  const value = useMemo(
    () => ({
      user,
      token,
      role,
      companyId,
      primaryRole: Array.isArray(role) ? role[0] : role,
      isAuthenticated: !!token,
      login,
      logout,
    }),
    [user, token, role, companyId]
  );

  // tự động logout khi token hết hạn
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
