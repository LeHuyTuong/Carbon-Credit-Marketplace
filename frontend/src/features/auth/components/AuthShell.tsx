import type { ReactNode } from 'react'
import { Leaf, ShieldCheck, TrendingUp } from 'lucide-react'

export function AuthShell({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-4rem)] w-full max-w-[1680px] items-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-2 lg:px-8">
      <section className="hidden lg:block">
        <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-700">
          <Leaf aria-hidden className="size-3.5" />
          Sàn tín chỉ carbon CarbonX
        </span>
        <p className="mt-6 font-extrabold text-4xl leading-[1.15] tracking-tight text-ink-900">
          Giao dịch tín chỉ carbon
          <span className="block text-brand-600">minh bạch và kiểm chứng.</span>
        </p>
        <p className="mt-4 max-w-md text-base leading-relaxed text-ink-500">
          Kết nối chủ xe điện, doanh nghiệp và đơn vị thẩm định trên một nền tảng duy nhất — đo lường,
          cấp tín chỉ và trao đổi theo thời gian thực.
        </p>
        <ul className="mt-8 space-y-4">
          {[
            { icon: TrendingUp, text: 'Giá tín chỉ cập nhật liên tục từ marketplace' },
            { icon: ShieldCheck, text: 'Báo cáo phát thải được thẩm định độc lập' },
            { icon: Leaf, text: 'Ví carbon và thanh toán tập trung' },
          ].map((item) => (
            <li key={item.text} className="flex items-center gap-3 text-sm text-ink-700">
              <span className="grid size-9 place-items-center rounded-xl bg-brand-50 text-brand-600">
                <item.icon aria-hidden className="size-4.5" />
              </span>
              {item.text}
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-auto w-full max-w-md">
        <div className="rounded-xl border border-ink-200 bg-white p-6 shadow-card sm:p-8">
          <h1 className="text-2xl font-bold tracking-tight text-ink-900">{title}</h1>
          <p className="mt-1.5 text-sm text-ink-500">{subtitle}</p>
          {children}
        </div>
        <div className="mt-5 text-center text-sm text-ink-600 [&_a]:inline-flex [&_a]:min-h-10 [&_a]:items-center [&_a]:px-1.5">{footer}</div>
      </section>
    </div>
  )
}
