import { NavLink, useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import {
  LayoutDashboard, Upload, List, TrendingUp,
  Target, Settings, LogOut, Wallet
} from 'lucide-react'

const links = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/upload', icon: Upload, label: 'Upload' },
  { to: '/transactions', icon: List, label: 'Transactions' },
  { to: '/predictions', icon: TrendingUp, label: 'Predictions' },
  { to: '/goals', icon: Target, label: 'Goals' },
]

export default function Sidebar() {
  const { user, logout } = useApp()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="logo-icon">💰</div>
        <div className="logo-text">
          <span className="logo-name">MoneyLens</span>
          <span className="logo-tagline">AI Finance</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {links.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `nav-link ${isActive ? 'active' : ''}`
            }
          >
            <Icon size={20} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-card">
          <div className="user-avatar">
            {user.name?.charAt(0)?.toUpperCase()}
          </div>
          <div className="user-info">
            <span className="user-name">{user.name}</span>
            <span className="user-budget">
              <Wallet size={12} />
              ₹{(user.monthlyBudget || 0).toLocaleString('en-IN')}/mo
            </span>
          </div>
        </div>

        <NavLink to="/settings" className="nav-link">
          <Settings size={20} />
          <span>Settings</span>
        </NavLink>

        <button className="nav-link logout-btn" onClick={handleLogout}>
          <LogOut size={20} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  )
}
