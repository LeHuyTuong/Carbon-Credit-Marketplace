import { DashboardView } from './components/DashboardView'

export function AdminDashboardPage() {
  return (
    <DashboardView
      role="ADMIN"
      eyebrow="Quản trị"
      title="Tổng quan hệ thống"
      description="Giám sát người dùng, dự án, tin đăng và các yêu cầu rút tiền chờ xử lý."
    />
  )
}
