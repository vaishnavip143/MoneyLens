import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext'
import { getGoals, createGoal, updateGoalProgress } from '../services/api'
import { Target, Plus, TrendingUp, Calendar, Sparkles, CheckCircle2 } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Goals() {
  const { userId } = useApp()
  const [goals, setGoals] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', targetAmount: '', deadline: '' })
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    if (userId) loadGoals()
  }, [userId])

  const loadGoals = async () => {
    try {
      const res = await getGoals(userId)
      setGoals(res.data)
    } catch {
      toast.error('Failed to load goals')
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!form.name || !form.targetAmount) {
      toast.error('Fill in all fields')
      return
    }
    setCreating(true)
    try {
      await createGoal(userId, {
        name: form.name,
        targetAmount: parseFloat(form.targetAmount),
        deadline: form.deadline || null,
      })
      toast.success('Goal created! 🎯')
      setShowForm(false)
      setForm({ name: '', targetAmount: '', deadline: '' })
      loadGoals()
    } catch {
      toast.error('Failed to create goal')
    } finally {
      setCreating(false)
    }
  }

  const handleProgress = async (goalId) => {
    const amount = prompt('Add amount saved (₹):')
    if (!amount || isNaN(amount)) return
    try {
      await updateGoalProgress(goalId, parseFloat(amount))
      toast.success('Progress updated! 💪')
      loadGoals()
    } catch {
      toast.error('Failed to update')
    }
  }

  if (loading) return <div className="page-loader"><div className="spinner" /><p>Loading goals...</p></div>

  return (
    <div className="goals-page">
      <div className="page-header">
        <div>
          <h1>🎯 Savings Goals</h1>
          <p className="page-subtitle">Set targets with AI feasibility analysis</p>
        </div>
        <button className="btn-primary" onClick={() => setShowForm(!showForm)}>
          <Plus size={18} /> New Goal
        </button>
      </div>

      {/* Create Form */}
      {showForm && (
        <div className="card goal-form-card">
          <h3>Create Savings Goal</h3>
          <form onSubmit={handleCreate} className="goal-form">
            <div className="form-row">
              <div className="input-group">
                <Target size={18} />
                <input
                  type="text"
                  placeholder="Goal name (e.g., Emergency Fund)"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </div>
              <div className="input-group">
                <TrendingUp size={18} />
                <input
                  type="number"
                  placeholder="Target amount (₹)"
                  value={form.targetAmount}
                  onChange={(e) => setForm({ ...form, targetAmount: e.target.value })}
                />
              </div>
              <div className="input-group">
                <Calendar size={18} />
                <input
                  type="date"
                  placeholder="Deadline"
                  value={form.deadline}
                  onChange={(e) => setForm({ ...form, deadline: e.target.value })}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary" disabled={creating}>
                {creating ? 'Creating...' : '🎯 Create Goal'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => setShowForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Goals Grid */}
      {goals.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">🎯</span>
          <h2>No goals yet</h2>
          <p>Create your first savings goal — AI will analyze if it's achievable!</p>
        </div>
      ) : (
        <div className="goals-grid">
          {goals.map((goal) => (
            <div key={goal.id} className={`goal-card ${goal.status === 'COMPLETED' ? 'completed' : ''}`}>
              <div className="goal-header">
                <h3>{goal.name}</h3>
                {goal.status === 'COMPLETED' ? (
                  <span className="goal-badge done"><CheckCircle2 size={14} /> Done!</span>
                ) : (
                  <span className="goal-badge active">Active</span>
                )}
              </div>

              <div className="goal-progress-bar">
                <div
                  className="goal-progress-fill"
                  style={{ width: `${Math.min(goal.progressPercentage || 0, 100)}%` }}
                />
              </div>

              <div className="goal-amounts">
                <span>₹{(goal.currentAmount || 0).toLocaleString('en-IN')} saved</span>
                <span>₹{(goal.targetAmount || 0).toLocaleString('en-IN')} target</span>
              </div>

              <div className="goal-meta">
                <span className="goal-pct">{goal.progressPercentage || 0}% complete</span>
                {goal.monthlyRequiredSaving && (
                  <span className="goal-monthly">
                    Need ₹{goal.monthlyRequiredSaving.toLocaleString('en-IN')}/mo
                  </span>
                )}
                {goal.deadline && (
                  <span className="goal-deadline">
                    📅 {new Date(goal.deadline).toLocaleDateString('en-IN')}
                  </span>
                )}
              </div>

              {/* AI Feasibility */}
              {goal.aiFeasibilityNote && (
                <div className="ai-feasibility">
                  <Sparkles size={14} />
                  <p>{goal.aiFeasibilityNote}</p>
                </div>
              )}

              {goal.status !== 'COMPLETED' && (
                <button className="btn-secondary btn-sm" onClick={() => handleProgress(goal.id)}>
                  + Add Savings
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
