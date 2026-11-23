import type { ReactNode } from 'react'

interface PageIntroProps {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}

export function PageIntro({ eyebrow, title, description, actions }: PageIntroProps) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {eyebrow && (
          <p className="mb-1 font-mono text-xs font-semibold uppercase tracking-widest text-brand-600">{eyebrow}</p>
        )}
        <h1 className="text-2xl font-bold tracking-tight text-ink-900">{title}</h1>
        {description && <p className="mt-1.5 max-w-2xl text-sm text-ink-500">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}
