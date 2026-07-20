import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext'
import { getTransactions } from '../services/api'
import { Search, Filter, ArrowDownRight, ArrowUpRight, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Transactions() {
  const { userId } = useApp()
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('ALL')

  useEffect(() => {
    if (userId) loadTransactions()
  }, [userId])

  const loadTransactions = async () => {
    try {
      const res = await getTransactions(userId)
      setTransactions(res.data)
    } catch {
      toast.error('Failed to load transactions')
    } finally {
      setLoading(false)
    }
  }

  const filtered = transactions.filter((tx) => {
    const matchSearch = tx.rawDescription?.toLowerCase().includes(search.toLowerCase()) ||
                        tx.merchant?.toLowerCase().includes(search.toLowerCase())
    const matchFilter = filter === 'ALL' ||
                        (filter === 'DEBIT' && tx.type === 'DEBIT') ||
                        (filter === 'CREDIT' && tx.type === 'CREDIT')
    return matchSearch && matchFilter
  })

  return (
    <div className="transactions-page">
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p className="page-subtitle">{transactions.length} total transactions</p>
        </div>
        <button className="btn-secondary" onClick={loadTransactions}>
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {/* Filters */}
      <div className="filters-bar">
        <div className="search-box">
          <Search size={18} />
          <input
            type="text"
            placeholder="Search transactions..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="filter-chips">
          {['ALL', 'DEBIT', 'CREDIT'].map((f) => (
            <button
              key={f}
              className={`chip ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Transaction Table */}
      {loading ? (
        <div className="page-loader"><div className="spinner" /><p>Loading...</p></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📋</span>
          <h2>No transactions found</h2>
          <p>Upload a CSV to see your transactions here</p>
        </div>
      ) : (
        <div className="tx-table-wrapper">
          <table className="tx-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Category</th>
                <th>Merchant</th>
                <th>Amount</th>
                <th>Type</th>
                <th>AI Score</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((tx) => (
                <tr key={tx.id}>
                  <td className="tx-date">
                    {new Date(tx.transactionDate).toLocaleDateString('en-IN', {
                      day: 'numeric', month: 'short'
                    })}
                  </td>
                  <td className="tx-desc">{tx.rawDescription}</td>
                  <td>
                    <span className="category-badge">
                      {tx.category}
                    </span>
                  </td>
                  <td className="tx-merchant">{tx.merchant || '—'}</td>
                  <td className={`tx-amount ${tx.type === 'CREDIT' ? 'credit' : 'debit'}`}>
                    {tx.type === 'CREDIT' ? '+' : '-'}₹{tx.amount?.toLocaleString('en-IN')}
                  </td>
                  <td>
                    <span className={`type-badge ${tx.type?.toLowerCase()}`}>
                      {tx.type === 'CREDIT' ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                      {tx.type}
                    </span>
                  </td>
                  <td>
                    <span className="ai-score">
                      {tx.aiConfidence ? `${(tx.aiConfidence * 100).toFixed(0)}%` : '—'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
