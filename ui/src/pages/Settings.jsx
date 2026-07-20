import { useState } from 'react'
import { useApp } from '../context/AppContext'
import { updateBudget } from '../services/api'
import { Wallet, Save, User } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Settings() {
  const { user, loadUser } = useApp()
  const [budget, setBudget] = useState(user?.monthlyBudget || '')
  const [saving, setSaving] = useState(false)

  const handleSave = async () => {
    setSaving(true)
    try {
      await updateBudget(user.id, parseFloat(budget) || 0)
      await loadUser(user.id)
      toast.success('Budget updated! 💰')
    } catch {
      toast.error('Failed to update')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="settings-page">
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <p className="page-subtitle">Manage your account and preferences</p>
        </div>
      </div>

      <div className="settings-grid">
        {/* Profile */}
        <div className="card settings-card">
          <h3><User size={20} /> Profile</h3>
          <div className="settings-field">
            <label>Name</label>
            <div className="settings-value">{user?.name}</div>
          </div>
          <div className="settings-field">
            <label>Email</label>
            <div className="settings-value">{user?.email}</div>
          </div>
        </div>

        {/* Budget */}
        <div className="card settings-card">
          <h3><Wallet size={20} /> Monthly Budget</h3>
          <p className="settings-desc">
            Set your monthly budget. MoneyLens will alert you when spending gets close.
          </p>
          <div className="input-group">
            <Wallet size={18} />
            <input
              type="number"
              placeholder="Monthly budget (₹)"
              value={budget}
              onChange={(e) => setBudget(e.target.value)}
            />
          </div>
          <button className="btn-primary" onClick={handleSave} disabled={saving}>
            <Save size={16} />
            {saving ? 'Saving...' : 'Save Budget'}
          </button>
        </div>

        {/* API Info */}
        <div className="card settings-card">
          <h3>🤖 AI Configuration</h3>
          <div className="settings-field">
            <label>AI Provider</label>
            <div className="settings-value">Google Gemini 2.0 Flash (Free)</div>
          </div>
          <div className="settings-field">
            <label>Status</label>
            <div className="settings-value" style={{ color: '#22c55e' }}>● Active</div>
          </div>
          <p className="settings-hint">
            Your Gemini API key is configured in the backend's application.yml.
            Free tier: 60 requests per minute.
          </p>
        </div>
      </div>
    </div>
  )
}
