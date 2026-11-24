import { useId, useRef, useState } from 'react'
import { FileCheck2, UploadCloud, X } from 'lucide-react'
import { cn } from '@/shared/lib/cn'

interface FileDropzoneProps {
  label: string
  accept?: string
  hint?: string
  error?: string
  file: File | null
  onFileChange: (file: File | null) => void
  id?: string
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

export function FileDropzone({
  label,
  accept,
  hint,
  error,
  file,
  onFileChange,
  id,
}: FileDropzoneProps) {
  const autoId = useId()
  const inputId = id ?? autoId
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [dragging, setDragging] = useState(false)

  return (
    <div className="min-w-0">
      <label htmlFor={inputId} className="mb-1.5 block text-sm font-medium text-ink-700">
        {label}
      </label>

      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept={accept}
        className="sr-only"
        onChange={(event) => {
          onFileChange(event.target.files?.[0] ?? null)
        }}
      />

      {file ? (
        <div
          className={cn(
            'flex items-center justify-between gap-3 rounded-xl border bg-brand-50/40 px-4 py-3',
            error ? 'border-rose-400' : 'border-brand-300',
          )}
        >
          <div className="flex min-w-0 items-center gap-3">
            <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-brand-100 text-brand-700">
              <FileCheck2 aria-hidden className="size-4" />
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink-900">{file.name}</p>
              <p className="text-xs text-ink-500">{formatFileSize(file.size)}</p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-1.5">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              className="rounded-lg px-2.5 py-1.5 text-xs font-semibold text-brand-700 hover:bg-brand-100"
            >
              Đổi tệp
            </button>
            <button
              type="button"
              aria-label="Xoá tệp đã chọn"
              onClick={() => {
                if (inputRef.current) inputRef.current.value = ''
                onFileChange(null)
              }}
              className="grid size-7 place-items-center rounded-lg text-ink-500 hover:bg-rose-50 hover:text-rose-600"
            >
              <X aria-hidden className="size-4" />
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            const dropped = e.dataTransfer.files?.[0] ?? null
            if (dropped) onFileChange(dropped)
          }}
          className={cn(
            'flex w-full items-center gap-3 rounded-xl border-2 border-dashed px-4 py-3.5 text-left transition',
            dragging
              ? 'border-brand-500 bg-brand-50'
              : error
                ? 'border-rose-400 bg-rose-50/30 hover:border-rose-500'
                : 'border-ink-200 bg-ink-50/50 hover:border-brand-400 hover:bg-brand-50/40',
          )}
        >
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white text-brand-600 shadow-2xs ring-1 ring-ink-200">
            <UploadCloud aria-hidden className="size-4" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-ink-800">
              Chọn tệp hoặc kéo thả vào đây
            </p>
            <p className="truncate text-xs text-ink-500">
              {hint || (accept ? `Định dạng hỗ trợ: ${accept}` : 'Tối đa 10 MB')}
            </p>
          </div>
        </button>
      )}

      {error && (
        <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      )}
    </div>
  )
}
