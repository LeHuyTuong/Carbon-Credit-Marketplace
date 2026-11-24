import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { BreakdownPoint, TrendPoint } from '@/shared/api/types'

const axisStyle = { fill: '#64748b', fontSize: 12 }
const PIE_COLORS = ['#0f5132', '#166534', '#0284c7', '#d97706', '#64748b']

export function TrendChart({ data }: { data: TrendPoint[] }) {
  if (!data.length) {
    return (
      <div className="flex h-64 flex-col items-center justify-center px-5 text-center">
        <p className="text-sm font-medium text-ink-600">Chưa có chuỗi thời gian phát sinh</p>
        <p className="mt-1 text-xs text-ink-400">Biểu đồ tự cập nhật khi có giao dịch hoặc hồ sơ mới theo tháng.</p>
      </div>
    )
  }
  return (
    <div className="h-64 w-full px-2 pb-4 pt-4">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 12, left: -16, bottom: 0 }}>
          <defs>
            <linearGradient id="carbonTrend" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#10b981" stopOpacity={0.35} />
              <stop offset="100%" stopColor="#10b981" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="label" tick={axisStyle} axisLine={false} tickLine={false} />
          <YAxis tick={axisStyle} axisLine={false} tickLine={false} allowDecimals={false} />
          <Tooltip cursor={{ stroke: '#a7f3d0' }} />
          <Area
            type="monotone"
            dataKey="value"
            name="Số lượng"
            stroke="#059669"
            strokeWidth={2.5}
            fill="url(#carbonTrend)"
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

export function BreakdownDonutChart({ data }: { data: BreakdownPoint[] }) {
  if (!data.length) {
    return (
      <div className="flex h-64 flex-col items-center justify-center px-5 text-center">
        <p className="text-sm font-medium text-ink-600">Chưa có dữ liệu phân bổ</p>
        <p className="mt-1 text-xs text-ink-400">Dữ liệu thực tế theo vai trò sẽ hiển thị tại đây.</p>
      </div>
    )
  }

  return (
    <div className="h-64 w-full px-2 pb-2 pt-2">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Tooltip formatter={(val) => [`${val ?? 0}`, 'Số lượng']} />
          <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: 12 }} />
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="45%"
            innerRadius={52}
            outerRadius={78}
            paddingAngle={3}
            isAnimationActive={false}
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${entry.name}-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
