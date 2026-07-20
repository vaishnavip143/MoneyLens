import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { useApp } from '../context/AppContext'
import LoginScreen from './LoginScreen'

export default function Layout() {
  const { user, loading } = useApp()

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-logo">💰</div>
        <div className="loading-text">MoneyLens</div>
      </div>
    )
  }

  if (!user) {
    return <LoginScreen />
  }

  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
