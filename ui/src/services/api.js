import axios from 'axios'

const API_BASE = '/api'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

// ─── Users ──────────────────────────────────────────────
export const createUser = (data) => api.post('/users', data)
export const getUser = (id) => api.get(`/users/${id}`)
export const updateBudget = (id, budget) =>
  api.put(`/users/${id}/budget?budget=${budget}`)

// ─── Transactions ───────────────────────────────────────
export const uploadTransactions = (userId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/transactions/upload?userId=${userId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}
export const getTransactions = (userId) =>
  api.get(`/transactions/${userId}`)
export const getCategoryBreakdown = (userId, start, end) =>
  api.get(`/transactions/${userId}/category-breakdown?startDate=${start}&endDate=${end}`)

// ─── Insights ───────────────────────────────────────────
export const getPredictions = (userId, months = 3) =>
  api.get(`/insights/predictions/${userId}?months=${months}`)
export const getDigest = (userId) =>
  api.get(`/insights/digest/${userId}`)
export const getDigestForDate = (userId, date) =>
  api.get(`/insights/digest/${userId}/${date}`)

// ─── Goals ──────────────────────────────────────────────
export const createGoal = (userId, data) =>
  api.post(`/goals/${userId}`, data)
export const getGoals = (userId) =>
  api.get(`/goals/${userId}`)
export const updateGoalProgress = (goalId, amount) =>
  api.put(`/goals/${goalId}/progress?amount=${amount}`)

// ─── Dashboard ──────────────────────────────────────────
export const getDashboard = (userId) =>
  api.get(`/dashboard/${userId}`)

export default api
