import {
  Area,
  AreaChart,
  Bar,
  BarChart,
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
import type { DashboardMetric, TrendPoint } from '@/shared/api/types'

const axisStyle = { fill: '#64748b', fontSize: 12 }
const PIE_COLORS = ['#059669', '#0284c7', '#f59e0b', '#8b5cf6', '#ec4899']

const DEFAULT_DISTRIBUTION = [
  { name: 'Xe điện đô thị (EVs)', value: 62 },
  { name: 'Trạm sạc năng lượng xanh', value: 24 },
  { name: 'Tối ưu logistics xanh', value: 14 },
]

export function TrendChart({ data }: { data: TrendPoint[] }) {
  if (!data.length) {
    return <p className="px-5 py-10 text-center text-sm text-ink-500">Chưa có dữ liệu theo thời gian.</p>
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
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

export function MetricBarChart({ metrics }: { metrics: DashboardMetric[] }) {
  const data = metrics
    .filter((item) => item.value !== null && item.value !== undefined)
    .map((item) => ({ name: item.label, value: item.value as number }))

  if (!data.length) {
    return <p className="px-5 py-10 text-center text-sm text-ink-500">Chưa có chỉ số để hiển thị.</p>
  }

  return (
    <div className="h-64 w-full px-2 pb-4 pt-4">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 12, left: -16, bottom: 0 }}>
          <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="name" tick={axisStyle} axisLine={false} tickLine={false} interval={0} />
          <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
          <Tooltip cursor={{ fill: '#f1f5f9' }} />
          <Bar dataKey="value" name="Giá trị" fill="#059669" radius={[6, 6, 0, 0]} maxBarSize={48} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export function EmissionDistributionChart({
  data = DEFAULT_DISTRIBUTION,
}: {
  data?: { name: string; value: number }[]
}) {
  return (
    <div className="h-64 w-full px-2 pb-2 pt-2">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Tooltip formatter={(val) => [`${val ?? 0}%`, 'Tỷ trọng']} />
          <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: 12 }} />
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="45%"
            innerRadius={55}
            outerRadius={80}
            paddingAngle={4}
          >
            {data.map((_, index) => (
              <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
