import { createContext, useContext, useState, useEffect } from 'react'
import { createUser, getUser } from '../services/api'

const AppContext = createContext()

export function AppProvider({ children }) {
  const [user, setUser] = useState(null)
  const [userId, setUserId] = useState(() => {
    return localStorage.getItem('moneylens_userId') || null
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (userId) {
      loadUser(userId)
    } else {
      setLoading(false)
    }
  }, [userId])

  const loadUser = async (id) => {
    try {
      const res = await getUser(id)
      setUser(res.data)
    } catch {
      localStorage.removeItem('moneylens_userId')
      setUserId(null)
    } finally {
      setLoading(false)
    }
  }

  const login = async (name, email, budget) => {
    const res = await createUser({ name, email, monthlyBudget: budget })
    const newUser = res.data
    setUser(newUser)
    setUserId(newUser.id)
    localStorage.setItem('moneylens_userId', newUser.id)
    return newUser
  }

  const logout = () => {
    setUser(null)
    setUserId(null)
    localStorage.removeItem('moneylens_userId')
  }

  return (
    <AppContext.Provider value={{ user, userId, loading, login, logout, loadUser }}>
      {children}
    </AppContext.Provider>
  )
}

export const useApp = () => useContext(AppContext)
