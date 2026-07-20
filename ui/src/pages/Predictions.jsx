import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext'
import { getPredictions } from '../services/api'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  CartesianGrid, Cell
} from 'recharts'
import { TrendingUp, TrendingDown, Minus, Sparkles, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'

const COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316', '#22c55e']

export default function Predictions() {
  const { userId } = useApp()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [months, setMonths] = useState(3)

  useEffect(() => {
    if (userId) loadPredictions()
  }, [userId, months])

  const loadPredictions = async () => {
    setLoading(true)
    try {
      const res = await getPredictions(userId, months)
      setData(res.data)
    } catch {
      toast.error('Failed to load predictions')
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <div className="page-loader"><div className="spinner" /><p>Analyzing spending patterns...</p></div>

  const chartData = data?.predictedByCategory
    ? Object.entries(data.predictedByCategory).map(([name, value], i) => ({
        name,
        value,
        fill: COLORS[i % COLORS.length]
      }))
    : []

  return (
    <div className="predictions-page">
      <div className="page-header">
        <div>
          <h1>🔮 AI Predictions</h1>
          <p className="page-subtitle">What Gemini AI thinks your next month looks like</p>
        </div>
        <div className="month-selector">
          {[2, 3, 6].map((m) => (
            <button
              key={m}
              className={`chip ${months === m ? 'active' : ''}`}
              onClick={() => setMonths(m)}
            >
              {m}mo history
            </button>
          ))}
        </div>
      </div>

      {data && (
        <>
          {/* Main Prediction */}
          <div className="prediction-hero">
            <div className="prediction-hero-amount">
              <span className="hero-label">Predicted Next Month Spending</span>
              <span className="hero-value">₹{(data.predictedTotal || 0).toLocaleString('en-IN')}</span>
            </div>
            <div className="ai-badge-large">
              <Sparkles size={20} />
              Powered by Gemini AI + Linear Regression
            </div>
          </div>

          <div className="predictions-grid">
            {/* Category Chart */}
            <div className="card">
              <h3>Predicted by Category</h3>
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height={350}>
                  <BarChart data={chartData} layout="vertical" margin={{ left: 120 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#312e81" />
                    <XAxis type="number" stroke="#636faa" tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="name" stroke="#636faa" tick={{ fontSize: 12 }} width={120} />
                    <Tooltip
                      contentStyle={{
                        background: '#1e1b4b',
                        border: '1px solid #4338ca',
                        borderRadius: '10px',
                        color: '#e0e7ff'
                      }}
                      formatter={(value) => `₹${value.toLocaleString('en-IN')}`}
                    />
                    <Bar dataKey="value" animationDuration={800}>
                      {chartData.map((entry, i) => (
                        <Cell key={i} fill={entry.fill} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="empty-chart"><p>Not enough data for predictions</p></div>
              )}
            </div>

            {/* Trends */}
            <div className="card">
              <h3>📊 Spending Trends</h3>
              {data.trends?.length > 0 ? (
                <div className="trends-list">
                  {data.trends.map((t, i) => (
                    <div key={i} className="trend-row">
                      <span className="trend-cat">{t.category}</span>
                      <div className="trend-info">
                        <span className={`trend-direction ${t.trend}`}>
                          {t.trend === 'increasing' ? <TrendingUp size={16} /> :
                           t.trend === 'decreasing' ? <TrendingDown size={16} /> :
                           <Minus size={16} />}
                          {t.trend}
                        </span>
                        <span className="trend-pct">
                          {t.percentChange > 0 ? '+' : ''}{t.percentChange}%
                        </span>
                      </div>
                      <p className="trend-narrative">{t.aiNarrative}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-chart"><p>Need more months of data</p></div>
              )}
            </div>

            {/* AI Insight */}
            <div className="card ai-insight-full">
              <h3><Sparkles size={20} /> AI Financial Insight</h3>
              <div className="ai-insight-content">
                <p>{data.aiInsight || 'Upload more transactions for AI insights.'}</p>
              </div>
            </div>

            {/* Anomalies */}
            <div className="card">
              <h3><AlertTriangle size={20} /> Detected Anomalies</h3>
              {data.alerts?.length > 0 ? (
                <div className="alerts-list">
                  {data.alerts.map((a, i) => (
                    <div key={i} className={`alert-card severity-${a.severity?.toLowerCase()}`}>
                      <div className="alert-top">
                        <span className={`severity-badge ${a.severity?.toLowerCase()}`}>
                          {a.severity}
                        </span>
                        <span className="alert-amount">₹{a.amount?.toLocaleString('en-IN')}</span>
                      </div>
                      <p className="alert-text">{a.description}</p>
                      <p className="alert-expected">Expected: {a.expectedRange}</p>
                      {a.suggestion && <p className="alert-tip">💡 {a.suggestion}</p>}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="no-alerts">
                  <span>✅</span>
                  <p>All spending looks normal!</p>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
