import { useEffect, useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'
import { createTraceId } from '@/shared/lib/format'
import { useAuthStore } from '@/shared/stores/auth.store'
import { mapNotification } from './mappers'
import { API_BASE } from './client'
import type { NotificationEvent } from './types'

const SSE_URL = `${API_BASE}/notifications`
const MAX_NOTIFICATIONS = 50

interface SseState {
  notifications: NotificationEvent[]
  connected: boolean
  unreadCount: number
  markAllRead: () => void
  clear: () => void
}

/**
 * SSE dùng fetch stream thay vì EventSource để gửi được Authorization header.
 * Tự reconnect theo backoff exponential, có AbortController khi unmount/đổi token.
 */
export function useNotificationsSse(): SseState {
  const token = useAuthStore((state) => state.token)
  const [notifications, setNotifications] = useState<NotificationEvent[]>([])
  const [connected, setConnected] = useState(false)
  const streamRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!token) {
      setConnected(false)
      setNotifications([])
      return
    }

    let disposed = false
    let retry = 0
    let reconnectTimer: number | null = null

    const scheduleReconnect = () => {
      if (disposed) return
      const delay = Math.min(30_000, 1_000 * 2 ** retry) + Math.floor(Math.random() * 400)
      retry += 1
      reconnectTimer = window.setTimeout(connect, delay)
    }

    const handleBlock = (block: string) => {
      const eventName = /^event:\s*(.+)$/m.exec(block)?.[1]?.trim() ?? 'message'
      if (eventName !== 'notification' && eventName !== 'message') return

      const data = block
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trimStart())
        .join('\n')
      if (!data) return

      try {
        const parsed: unknown = JSON.parse(data)
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
          const event = mapNotification(parsed as Record<string, unknown>)
          setNotifications((prev) => [event, ...prev].slice(0, MAX_NOTIFICATIONS))
          toast(event.title, { description: event.message || undefined })
        }
      } catch {
        // Bỏ frame không phải JSON; không làm đứt stream.
      }
    }

    const connect = async () => {
      if (disposed) return
      const controller = new AbortController()
      streamRef.current = controller

      try {
        const response = await fetch(SSE_URL, {
          headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${token}`,
            'X-Trace-Id': createTraceId(),
          },
          credentials: 'include',
          signal: controller.signal,
        })
        if (!response.ok || !response.body) throw new Error(`SSE ${response.status}`)

        retry = 0
        setConnected(true)
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (!disposed) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const blocks = buffer.split('\n\n')
          buffer = blocks.pop() ?? ''
          blocks.forEach(handleBlock)
        }
      } catch {
        if (!disposed) setConnected(false)
      } finally {
        if (!disposed) {
          setConnected(false)
          scheduleReconnect()
        }
      }
    }

    void connect()

    return () => {
      disposed = true
      if (reconnectTimer !== null) window.clearTimeout(reconnectTimer)
      streamRef.current?.abort()
      streamRef.current = null
    }
  }, [token])

  const unreadCount = useMemo(
    () => notifications.filter((item) => !item.read).length,
    [notifications],
  )

  const markAllRead = () =>
    setNotifications((prev) => prev.map((item) => ({ ...item, read: true })))

  const clear = () => setNotifications([])

  return { notifications, connected, unreadCount, markAllRead, clear }
}
