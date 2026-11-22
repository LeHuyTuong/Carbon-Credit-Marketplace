import { Link } from 'react-router-dom'
import { Compass, Leaf } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'

export function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-ink-50 px-4">
      <div className="text-center">
        <div className="mx-auto mb-6 grid size-12 place-items-center rounded-2xl bg-brand-600 text-white">
          <Leaf aria-hidden className="size-6" />
        </div>
        <EmptyState
          icon={Compass}
          title="404 — Không tìm thấy trang"
          description="Trang bạn tìm không tồn tại hoặc đã được di chuyển."
          action={
            <Link
              to="/"
              className="inline-flex h-11 items-center rounded-xl bg-brand-600 px-5 text-sm font-semibold text-white hover:bg-brand-700"
            >
              Về trang chủ
            </Link>
          }
        />
      </div>
    </div>
  )
}
