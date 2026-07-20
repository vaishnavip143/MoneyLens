import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext'
import { getDashboard } from '../services/api'
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis,
  Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid, Legend
} from 'recharts'
import {
  TrendingUp, TrendingDown, Wallet, PiggyBank,
  AlertTriangle, ArrowUpRight, ArrowDownRight, Activity
} from 'lucide-react'
import toast from 'react-hot-toast'

const COLORS = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e',
  '#f97316', '#eab308', '#22c55e', '#14b8a6',
  '#06b6d4', '#3b82f6', '#a855f7', '#78716c'
]

export default function Dashboard() {
  const { userId } = useApp()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (userId) loadDashboard()
  }, [userId])

  const loadDashboard = async () => {
    try {
      const res = await getDashboard(userId)
      setData(res.data)
    } catch (err) {
      toast.error('Failed to load dashboard')
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <PageLoader />
  if (!data) return <EmptyState />

  const categoryData = Object.entries(data.categoryBreakdown || {}).map(
    ([name, value], i) => ({
      name: name.split(' ').slice(1).join(' ') || name,
      emoji: name.split(' ')[0],
      value,
      fill: COLORS[i % COLORS.length]
    })
  )

  const prediction = data.predictionReport || {}
  const digest = data.todayDigest || {}

  return (
    <div className="dashboard">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="page-subtitle">Your financial intelligence at a glance</p>
        </div>
        <div className="header-badge">
          <Activity size={14} />
          AI Active
        </div>
      </div>

      {/* KPI Cards */}
      <div className="kpi-grid">
        <KPICard
          label="Monthly Spending"
          value={`₹${(data.monthlySpend || 0).toLocaleString('en-IN')}`}
          icon={<ArrowDownRight size={20} />}
          color="#f43f5e"
          subtitle="This month"
        />
        <KPICard
          label="Monthly Income"
          value={`₹${(data.monthlyIncome || 0).toLocaleString('en-IN')}`}
          icon={<ArrowUpRight size={20} />}
          color="#22c55e"
          subtitle="This month"
        />
        <KPICard
          label="Savings Rate"
          value={`${data.savingsRate || 0}%`}
          icon={<PiggyBank size={20} />}
          color="#6366f1"
          subtitle="Of income saved"
          alert={data.savingsRate < 20}
        />
        <KPICard
          label="Budget Remaining"
          value={`₹${(data.budgetRemaining || 0).toLocaleString('en-IN')}`}
          icon={<Wallet size={20} />}
          color={data.budgetRemaining >= 0 ? '#14b8a6' : '#f43f5e'}
          subtitle={data.budgetRemaining >= 0 ? 'Available' : 'Over budget!'}
          alert={data.budgetRemaining < 0}
        />
      </div>

      {/* Main Charts Row */}
      <div className="charts-row">
        {/* Category Pie Chart */}
        <div className="card chart-card">
          <h3>Spending by Category</h3>
          {categoryData.length > 0 ? (
            <div className="pie-chart-container">
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={3}
                    dataKey="value"
                    animationDuration={800}
                  >
                    {categoryData.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value) => `₹${value.toLocaleString('en-IN')}`}
                    contentStyle={{
                      background: '#1e1b4b',
                      border: '1px solid #4338ca',
                      borderRadius: '10px',
                      color: '#e0e7ff'
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="pie-legend">
                {categoryData.map((item, i) => (
                  <div key={i} className="legend-item">
                    <span className="legend-dot" style={{ background: item.fill }} />
                    <span className="legend-label">{item.emoji} {item.name}</span>
                    <span className="legend-value">₹{item.value.toLocaleString('en-IN')}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="empty-chart">
              <p>No spending data yet</p>
              <p className="hint">Upload a CSV to see your breakdown</p>
            </div>
          )}
        </div>

        {/* Daily Trend */}
        <div className="card chart-card">
          <h3>Daily Spending Trend</h3>
          {data.dailyTrend?.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <AreaChart data={data.dailyTrend}>
                <defs>
                  <linearGradient id="spendGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#312e81" />
                <XAxis
                  dataKey="date"
                  stroke="#636faa"
                  tick={{ fontSize: 11 }}
                  tickFormatter={(v) => {
                    const d = new Date(v)
                    return `${d.getDate()}`
                  }}
                />
                <YAxis stroke="#636faa" tick={{ fontSize: 11 }} />
                <Tooltip
                  contentStyle={{
                    background: '#1e1b4b',
                    border: '1px solid #4338ca',
                    borderRadius: '10px',
                    color: '#e0e7ff'
                  }}
                  formatter={(value) => [`₹${value.toLocaleString('en-IN')}`, 'Spent']}
                />
                <Area
                  type="monotone"
                  dataKey="amount"
                  stroke="#6366f1"
                  strokeWidth={2}
                  fill="url(#spendGrad)"
                  animationDuration={1000}
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="empty-chart">
              <p>No daily data yet</p>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Row */}
      <div className="bottom-row">
        {/* AI Prediction */}
        <div className="card prediction-card">
          <div className="card-header-row">
            <h3>🔮 AI Prediction — Next Month</h3>
            <span className="ai-badge">Gemini AI</span>
          </div>
          <div className="prediction-main">
            <span className="prediction-amount">
              ₹{(prediction.predictedTotal || 0).toLocaleString('en-IN')}
            </span>
            <span className="prediction-label">Predicted Total Spend</span>
          </div>
          {prediction.predictedByCategory && (
            <div className="prediction-bars">
              {Object.entries(prediction.predictedByCategory).slice(0, 5).map(([cat, val]) => (
                <div key={cat} className="pred-bar-row">
                  <span className="pred-bar-label">{cat}</span>
                  <div className="pred-bar-track">
                    <div
                      className="pred-bar-fill"
                      style={{
                        width: `${Math.min((val / (prediction.predictedTotal || 1)) * 100, 100)}%`
                      }}
                    />
                  </div>
                  <span className="pred-bar-value">₹{val.toLocaleString('en-IN')}</span>
                </div>
              ))}
            </div>
          )}
          {prediction.aiInsight && (
            <div className="ai-insight-box">
              <span className="ai-icon">✨</span>
              <p>{prediction.aiInsight}</p>
            </div>
          )}
        </div>

        {/* Top Merchants */}
        <div className="card merchants-card">
          <h3>Top Merchants</h3>
          {data.topMerchants?.length > 0 ? (
            <div className="merchant-list">
              {data.topMerchants.map((m, i) => (
                <div key={i} className="merchant-item">
                  <div className="merchant-rank">#{i + 1}</div>
                  <div className="merchant-info">
                    <span className="merchant-name">{m.name}</span>
                    <span className="merchant-count">{m.transactionCount} transactions</span>
                  </div>
                  <span className="merchant-amount">
                    ₹{m.totalAmount.toLocaleString('en-IN')}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-chart">
              <p>No merchant data yet</p>
            </div>
          )}

          {/* Daily Digest */}
          {digest.aiSummary && (
            <div className="digest-preview">
              <h4>📱 Today's Digest</h4>
              <p className="digest-text">{digest.aiSummary}</p>
              <div className="digest-meta">
                <span>₹{digest.totalSpent?.toLocaleString('en-IN') || 0} spent today</span>
                <span>{digest.transactionCount || 0} transactions</span>
              </div>
            </div>
          )}
        </div>

        {/* Anomaly Alerts */}
        <div className="card alerts-card">
          <h3>🚨 Alerts & Anomalies</h3>
          {prediction.alerts?.length > 0 ? (
            <div className="alert-list">
              {prediction.alerts.map((alert, i) => (
                <div key={i} className={`alert-item severity-${alert.severity?.toLowerCase()}`}>
                  <div className="alert-header">
                    <AlertTriangle size={16} />
                    <span className="alert-severity">{alert.severity}</span>
                  </div>
                  <p className="alert-desc">{alert.description}</p>
                  <p className="alert-range">Expected: {alert.expectedRange}</p>
                  {alert.suggestion && (
                    <p className="alert-suggestion">💡 {alert.suggestion}</p>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="no-alerts">
              <span className="no-alerts-icon">✅</span>
              <p>No anomalies detected</p>
              <p className="hint">Your spending looks normal!</p>
            </div>
          )}

          {/* Spending Trends */}
          {prediction.trends?.length > 0 && (
            <div className="trends-section">
              <h4>📊 Trends</h4>
              {prediction.trends.slice(0, 4).map((trend, i) => (
                <div key={i} className="trend-item">
                  <span className="trend-category">{trend.category}</span>
                  <div className="trend-badge-row">
                    <span className={`trend-badge ${trend.trend}`}>
                      {trend.trend === 'increasing' ? '↑' : trend.trend === 'decreasing' ? '↓' : '→'}
                      {Math.abs(trend.percentChange)}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function KPICard({ label, value, icon, color, subtitle, alert }) {
  return (
    <div className={`kpi-card ${alert ? 'kpi-alert' : ''}`}>
      <div className="kpi-icon" style={{ background: `${color}20`, color }}>
        {icon}
      </div>
      <div className="kpi-content">
        <span className="kpi-value">{value}</span>
        <span className="kpi-label">{label}</span>
        <span className="kpi-subtitle">{subtitle}</span>
      </div>
    </div>
  )
}

function PageLoader() {
  return (
    <div className="page-loader">
      <div className="spinner" />
      <p>Loading dashboard...</p>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="empty-state">
      <span className="empty-icon">📊</span>
      <h2>Welcome to MoneyLens</h2>
      <p>Upload your first bank statement to see AI-powered insights!</p>
    </div>
  )
}
