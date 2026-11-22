import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface Column<T> {
  key: string
  label: string
  render: (row: T) => ReactNode
  align?: 'left' | 'right'
  className?: string
}

interface DataTableProps<T> {
  label: string
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
}

export function DataTable<T>({ label, columns, rows, rowKey }: DataTableProps<T>) {
  return (
    <div className="w-full min-w-0 max-w-full overflow-x-auto">
      <table className="w-full min-w-[720px] text-sm" aria-label={label}>
        <thead className="bg-ink-50 text-left text-xs uppercase tracking-wider text-ink-500">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cn('px-4 py-3 font-semibold', column.align === 'right' && 'text-right', column.className)}
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">
          {rows.map((row) => (
            <tr key={rowKey(row)} className="hover:bg-ink-50/60">
              {columns.map((column) => (
                <td key={column.key} className={cn('min-w-0 break-words px-4 py-3 align-top text-ink-700', column.align === 'right' && 'text-right tabular-nums')}>
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
