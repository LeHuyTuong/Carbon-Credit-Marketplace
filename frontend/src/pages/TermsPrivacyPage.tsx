import { Link } from 'react-router-dom'
import { ArrowLeft, FileCheck2, ShieldCheck } from 'lucide-react'
import { Card } from '@/shared/components/Card'

export function TermsPage() {
  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-12 sm:px-6">
      <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-700 hover:text-brand-800">
        <ArrowLeft className="size-4" /> Về trang chủ
      </Link>
      <Card className="mt-4 p-6 sm:p-8">
        <div className="flex items-center gap-3">
          <FileCheck2 className="size-6 text-brand-600" />
          <h1 className="text-2xl font-bold text-ink-900">Điều khoản sử dụng CarbonX</h1>
        </div>
        <div className="mt-6 space-y-4 text-sm leading-relaxed text-ink-700">
          <section>
            <h2 className="font-semibold text-ink-900">1. Phạm vi & vai trò trên sàn</h2>
            <p className="mt-1">
              CarbonX kết nối Chủ xe điện, Doanh nghiệp gom phát thải, Đơn vị thẩm định độc lập (CVA) và Quản trị viên (Admin)
              nhằm đo lường, phát hành, giao dịch và triệt tiêu tín chỉ carbon từ phương tiện điện.
            </p>
          </section>
          <section>
            <h2 className="font-semibold text-ink-900">2. Chuẩn phát hành & tính bất biến lô tín chỉ</h2>
            <p className="mt-1">
              Mỗi lô tín chỉ được cấp mã lô và dải số serial duy nhất theo chuẩn VM0038 sau khi CVA thẩm định và Admin phê duyệt.
              Thông số phát hành của lô đã cấp không bị thay đổi hồi tố khi quy tắc dự án được cập nhật về sau.
            </p>
          </section>
          <section>
            <h2 className="font-semibold text-ink-900">3. Phí sàn & thanh toán</h2>
            <p className="mt-1">
              Mọi giao dịch mua bán trên sàn áp dụng phí nền tảng cố định 5%. Tiền và tín chỉ được ghi nhận qua sổ cái bất biến
              kèm khoá giao dịch chống thanh toán trùng lặp.
            </p>
          </section>
        </div>
      </Card>
    </div>
  )
}

export function PrivacyPage() {
  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-12 sm:px-6">
      <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-700 hover:text-brand-800">
        <ArrowLeft className="size-4" /> Về trang chủ
      </Link>
      <Card className="mt-4 p-6 sm:p-8">
        <div className="flex items-center gap-3">
          <ShieldCheck className="size-6 text-brand-600" />
          <h1 className="text-2xl font-bold text-ink-900">Chính sách bảo mật CarbonX</h1>
        </div>
        <div className="mt-6 space-y-4 text-sm leading-relaxed text-ink-700">
          <section>
            <h2 className="font-semibold text-ink-900">1. Dữ liệu định danh KYC & phương tiện</h2>
            <p className="mt-1">
              Hồ sơ định danh (CCCD, giấy phép kinh doanh, cà vẹt xe và dữ liệu hành trình sạc kWh) chỉ được sử dụng phục vụ
              thẩm định MRV, phát hành tín chỉ và chia sẻ doanh thu minh bạch.
            </p>
          </section>
          <section>
            <h2 className="font-semibold text-ink-900">2. Bảo mật truy cập & giao dịch</h2>
            <p className="mt-1">
              Mọi API được xác thực bằng JWT và kiểm quyền theo vai trò tại máy chủ. Đơn vị thẩm định CVA không nắm giữ ví tài chính
              để đảm bảo tính độc lập khách quan.
            </p>
          </section>
          <section>
            <h2 className="font-semibold text-ink-900">3. Lưu trữ sổ cái</h2>
            <p className="mt-1">
              Lịch sử giao dịch ví ghi nhận đầy đủ số dư trước và sau giao dịch để chủ xe điện và doanh nghiệp đối soát theo từng lô tín chỉ.
            </p>
          </section>
        </div>
      </Card>
    </div>
  )
}
