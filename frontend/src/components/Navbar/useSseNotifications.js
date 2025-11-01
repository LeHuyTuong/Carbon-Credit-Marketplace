import { useEffect, useMemo, useRef, useState } from "react";
import { EventSourcePolyfill } from "event-source-polyfill";

const DEFAULT_BASE_URL = "http://163.61.111.120:8082";
const SSE_ENDPOINT = "/api/v1/notifications";
const MAX_NOTIFICATIONS = 50;

function normalizeBaseUrl(baseUrl) {
  const invalid = !baseUrl || baseUrl === "null" || baseUrl === "undefined";
  if (invalid) {
    if (typeof window !== "undefined" && window.location?.origin) {
      return window.location.origin;
    }
    return DEFAULT_BASE_URL;
  }
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}

function resolveStoredToken() {
  if (typeof window === "undefined") {
    return null;
  }

  const storages = [window.sessionStorage, window.localStorage];
  const flatKeys = [
    "accessToken",
    "token",
    "admin_token",
    "cva_token",
  ];

  for (const storage of storages) {
    for (const key of flatKeys) {
      const value = storage.getItem(key);
      if (value && value !== "null" && value !== "undefined") {
        return value;
      }
    }
  }

  const readAuth = (storage) => {
    const raw = storage.getItem("auth");
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw);
      const candidate = parsed?.token;
      return candidate && candidate !== "null" && candidate !== "undefined"
        ? candidate
        : null;
    } catch {
      return null;
    }
  };

  for (const storage of storages) {
    const token = readAuth(storage);
    if (token) return token;
  }

  return null;
}

function parseEventPayload(raw) {
  if (!raw) {
    return { message: "New notification" };
  }

  if (typeof raw === "string") {
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object") {
        return {
          title: parsed.title || parsed.subject || null,
          message:
            parsed.message ||
            parsed.content ||
            parsed.body ||
            parsed.description ||
            JSON.stringify(parsed),
          data: parsed,
        };
      }
    } catch {
      // fall through to return raw string
    }

    return { message: raw };
  }

  if (typeof raw === "object") {
    return {
      title: raw.title || raw.subject || null,
      message:
        raw.message ||
        raw.content ||
        raw.body ||
        raw.description ||
        JSON.stringify(raw),
      data: raw,
    };
  }

  return { message: String(raw) };
}

function createNotification(raw, overrides = {}) {
  const parsed = parseEventPayload(raw);
  const receivedAt = new Date();
  const id =
    overrides.id ||
    parsed.id ||
    `${receivedAt.getTime()}-${Math.random().toString(36).slice(2, 8)}`;

  return {
    id,
    title: parsed.title || null,
    message: parsed.message || "New notification",
    receivedAt,
    isUnread: true,
    raw,
    ...parsed.data && { data: parsed.data },
    ...overrides,
  };
}

function sanitizeToken(value) {
  if (!value || value === "null" || value === "undefined") return null;
  return value;
}

export default function useSseNotifications({
  enabled = true,
  token,
  baseUrl = import.meta.env?.VITE_API_BASE,
  onNotification,
} = {}) {
  const [notifications, setNotifications] = useState([]);
  const reconnectTimer = useRef(null);
  const eventSourceRef = useRef(null);

  const resolvedBase = normalizeBaseUrl(baseUrl);
  const providedToken =
    typeof token === "function" ? token() : token;
  const sanitizedToken = sanitizeToken(providedToken);
  const resolvedToken = sanitizedToken ?? resolveStoredToken();

  const canConnect = enabled && !!resolvedToken;

  useEffect(() => {
    if (!canConnect) {
      return () => {};
    }

    let cancelled = false;

    const connect = () => {
      if (cancelled) return;

      const es = new EventSourcePolyfill(`${resolvedBase}${SSE_ENDPOINT}`, {
        headers: {
          Authorization: `Bearer ${resolvedToken}`,
        },
        withCredentials: true,
      });

      const handleNotification = (event) => {
        const next = createNotification(event.data);
        setNotifications((prev) => {
          const merged = [next, ...prev];
          return merged.slice(0, MAX_NOTIFICATIONS);
        });
        if (typeof onNotification === "function") {
          onNotification(next);
        }
      };

      const handleInit = (event) => {
        console.info("SSE connected:", event.data);
      };

      es.addEventListener("notification", handleNotification);
      es.addEventListener("init", handleInit);

      es.onerror = (err) => {
        console.error("SSE error:", err);
        es.close();
        if (!cancelled) {
          reconnectTimer.current = setTimeout(connect, 5000);
        }
      };

      eventSourceRef.current = es;
    };

    connect();

    return () => {
      cancelled = true;
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current);
        reconnectTimer.current = null;
      }
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [canConnect, resolvedBase, resolvedToken, onNotification]);

  const markAllAsRead = () => {
    setNotifications((prev) =>
      prev.map((item) => ({ ...item, isUnread: false }))
    );
  };

  const markAsRead = (id) => {
    setNotifications((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, isUnread: false } : item
      )
    );
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  const unreadCount = useMemo(
    () => notifications.reduce((acc, item) => acc + (item.isUnread ? 1 : 0), 0),
    [notifications]
  );

  return {
    notifications,
    unreadCount,
    markAllAsRead,
    markAsRead,
    clearNotifications,
    canConnect,
  };
}

