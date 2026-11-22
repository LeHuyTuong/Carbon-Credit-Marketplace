import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Bot, MessageCircle, Send, X } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { cn } from '@/shared/lib/cn'

interface ChatMessage {
  id: number
  sender: 'ai' | 'user'
  text: string
}

const GREETING =
  'Xin chào 👋 Mình là Casia AI. Mình có thể giúp bạn về tín chỉ carbon, giá marketplace hoặc báo cáo phát thải.'

export function AiChatWidget() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: 0, sender: 'ai', text: GREETING },
  ])
  const nextId = useRef(1)
  const listRef = useRef<HTMLDivElement>(null)

  const chatMutation = useMutation({
    mutationFn: (message: string) => api.post<string>('/ai/chat', { message }),
  })

  useEffect(() => {
    if (!open) return
    const list = listRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [messages, chatMutation.isPending, open])

  const handleSend = (event: FormEvent) => {
    event.preventDefault()
    const message = input.trim()
    if (!message || chatMutation.isPending) return

    setMessages((prev) => [...prev, { id: nextId.current++, sender: 'user', text: message }])
    setInput('')

    chatMutation.mutate(message, {
      onSuccess: (answer) => {
        const reply = typeof answer === 'string' ? answer : JSON.stringify(answer)
        setMessages((prev) => [
          ...prev,
          {
            id: nextId.current++,
            sender: 'ai',
            text: reply.trim() || 'Xin lỗi, mình chưa có câu trả lời cho câu này.',
          },
        ])
      },
      onError: () => {
        setMessages((prev) => [
          ...prev,
          {
            id: nextId.current++,
            sender: 'ai',
            text: 'Hiện mình chưa kết nối được dịch vụ AI. Vui lòng thử lại sau ít phút.',
          },
        ])
      },
    })
  }

  return (
    <>
      {open && (
        <section
          aria-label="Trợ lý Casia AI"
          className="fixed bottom-20 right-4 z-40 flex w-[min(22rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-ink-200 bg-white shadow-float sm:right-6"
        >
          <header className="flex items-center justify-between border-b border-ink-100 bg-brand-50 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="grid size-8 place-items-center rounded-xl bg-brand-600 text-white">
                <Bot aria-hidden className="size-4" />
              </span>
              <div>
                <p className="text-sm font-bold text-ink-900">Casia AI</p>
                <p className="text-[11px] text-ink-500">Trợ lý tín chỉ carbon</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Đóng cửa sổ chat"
              className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-white hover:text-ink-900"
            >
              <X aria-hidden className="size-4" />
            </button>
          </header>

          <div
            ref={listRef}
            role="log"
            aria-live="polite"
            className="scrollbar-thin flex max-h-80 min-h-52 flex-col gap-3 overflow-y-auto px-4 py-4"
          >
            {messages.map((message) => (
              <div
                key={message.id}
                className={cn('flex', message.sender === 'user' ? 'justify-end' : 'justify-start')}
              >
                <p
                  className={cn(
                    'max-w-[85%] whitespace-pre-wrap rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed',
                    message.sender === 'user'
                      ? 'rounded-br-sm bg-brand-600 text-white'
                      : 'rounded-bl-sm bg-ink-100 text-ink-800',
                  )}
                >
                  {message.text}
                </p>
              </div>
            ))}
            {chatMutation.isPending && (
              <p className="w-fit rounded-2xl rounded-bl-sm bg-ink-100 px-3.5 py-2.5 text-xs text-ink-500">
                Casia đang soạn câu trả lời…
              </p>
            )}
          </div>

          <form onSubmit={handleSend} className="flex items-center gap-2 border-t border-ink-100 p-3">
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Đặt câu hỏi cho Casia…"
              aria-label="Tin nhắn gửi Casia AI"
              className="h-10 min-w-0 flex-1 rounded-xl border border-ink-200 px-3 text-sm text-ink-900 outline-none placeholder:text-ink-400 focus:border-brand-500"
            />
            <Button type="submit" size="sm" aria-label="Gửi tin nhắn" loading={chatMutation.isPending}>
              <Send aria-hidden className="size-4" />
            </Button>
          </form>
        </section>
      )}

      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-label={open ? 'Ẩn trợ lý Casia AI' : 'Mở trợ lý Casia AI'}
        aria-expanded={open}
        className="fixed bottom-5 right-4 z-40 grid size-14 place-items-center rounded-full bg-brand-600 text-white shadow-float transition hover:bg-brand-700 sm:right-6"
      >
        {open ? <X aria-hidden className="size-6" /> : <MessageCircle aria-hidden className="size-6" />}
      </button>
    </>
  )
}
