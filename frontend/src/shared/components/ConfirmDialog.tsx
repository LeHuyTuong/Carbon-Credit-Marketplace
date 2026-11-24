import { AlertTriangle, X } from 'lucide-react'
import { Button } from './Button'

interface ConfirmDialogProps {
  open: boolean
  title: string
  description: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: 'primary' | 'danger'
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Huỷ',
  variant = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4 backdrop-blur-xs"
      onClick={() => {
        if (!loading) onCancel()
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-md rounded-card bg-white p-6 shadow-float"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <span
              className={`grid size-10 shrink-0 place-items-center rounded-xl ${
                variant === 'danger' ? 'bg-rose-50 text-rose-600' : 'bg-brand-50 text-brand-600'
              }`}
            >
              <AlertTriangle aria-hidden className="size-5" />
            </span>
            <div>
              <h3 className="text-base font-bold text-ink-900">{title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-600">{description}</p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            disabled={loading}
            onClick={onCancel}
            className="grid size-8 shrink-0 place-items-center rounded-lg text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="mt-6 flex justify-end gap-2 border-t border-ink-100 pt-4">
          <Button variant="outline" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button variant={variant} loading={loading} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
