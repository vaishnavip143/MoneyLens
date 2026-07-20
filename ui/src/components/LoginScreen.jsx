import { useState } from 'react'
import { useApp } from '../context/AppContext'
import { Wallet, User, Mail, DollarSign, ArrowRight } from 'lucide-react'
import toast from 'react-hot-toast'

export default function LoginScreen() {
  const { login } = useApp()
  const [form, setForm] = useState({ name: '', email: '', monthlyBudget: '' })
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.name || !form.email) {
      toast.error('Please fill in your name and email')
      return
    }
    setSubmitting(true)
    try {
      await login(form.name, form.email, parseFloat(form.monthlyBudget) || 0)
      toast.success(`Welcome, ${form.name}! 🎉`)
    } catch (err) {
      toast.error('Failed to create account')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-hero">
          <div className="login-logo">💰</div>
          <h1>MoneyLens</h1>
          <p>AI-Powered Finance Intelligence</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <User size={18} />
            <input
              type="text"
              placeholder="Your name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>

          <div className="input-group">
            <Mail size={18} />
            <input
              type="email"
              placeholder="Email address"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>

          <div className="input-group">
            <DollarSign size={18} />
            <input
              type="number"
              placeholder="Monthly budget (₹)"
              value={form.monthlyBudget}
              onChange={(e) => setForm({ ...form, monthlyBudget: e.target.value })}
            />
          </div>

          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Setting up...' : 'Get Started'}
            {!submitting && <ArrowRight size={18} />}
          </button>
        </form>

        <p className="login-hint">
          Free to use. Your data stays local. AI-powered insights.
        </p>
      </div>
    </div>
  )
}
