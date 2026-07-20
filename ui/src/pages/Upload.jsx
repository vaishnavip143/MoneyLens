import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useApp } from '../context/AppContext'
import { uploadTransactions } from '../services/api'
import {
  Upload as UploadIcon, FileSpreadsheet, CheckCircle2,
  AlertTriangle, X, Loader2, ArrowUpRight, ArrowDownRight
} from 'lucide-react'
import toast from 'react-hot-toast'

export default function Upload() {
  const { userId } = useApp()
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState(null)

  const onDrop = useCallback((accepted) => {
    if (accepted.length > 0) {
      setFile(accepted[0])
      setResult(null)
    }
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'text/csv': ['.csv'], 'text/plain': ['.txt'] },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024,
  })

  const handleUpload = async () => {
    if (!file) return
    setUploading(true)
    try {
      const res = await uploadTransactions(userId, file)
      setResult(res.data)
      toast.success(`✅ ${res.data.successCount} transactions categorized!`)
    } catch (err) {
      toast.error('Upload failed. Check your CSV format.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="upload-page">
      <div className="page-header">
        <div>
          <h1>Upload Transactions</h1>
          <p className="page-subtitle">Upload a CSV file — AI will categorize everything automatically</p>
        </div>
      </div>

      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={`dropzone ${isDragActive ? 'dropzone-active' : ''} ${file ? 'dropzone-has-file' : ''}`}
      >
        <input {...getInputProps()} />
        {file ? (
          <div className="dropzone-file-info">
            <FileSpreadsheet size={40} className="file-icon" />
            <div>
              <p className="file-name">{file.name}</p>
              <p className="file-size">{(file.size / 1024).toFixed(1)} KB</p>
            </div>
            <button
              className="btn-icon"
              onClick={(e) => {
                e.stopPropagation()
                setFile(null)
                setResult(null)
              }}
            >
              <X size={18} />
            </button>
          </div>
        ) : (
          <div className="dropzone-content">
            <UploadIcon size={48} className="upload-icon" />
            <h3>Drop your CSV here</h3>
            <p>or click to browse</p>
            <p className="dropzone-hint">Supports: Swiggy, Zomato, SBI, HDFC, ICICI bank CSVs</p>
          </div>
        )}
      </div>

      {/* Format Help */}
      <div className="format-help">
        <h4>📋 Expected CSV Format</h4>
        <code>date, description, amount, type</code>
        <div className="format-example">
          <p>2025-07-02, SWIGGY BANGALORE, 450, DEBIT</p>
          <p>2025-07-01, SALARY ACME CORP, 75000, CREDIT</p>
        </div>
        <a href="/sample-transactions.csv" download className="download-sample">
          Download sample CSV
        </a>
      </div>

      {/* Upload Button */}
      {file && !result && (
        <button
          className="btn-primary btn-upload"
          onClick={handleUpload}
          disabled={uploading}
        >
          {uploading ? (
            <>
              <Loader2 size={18} className="spin" />
              Analyzing with AI...
            </>
          ) : (
            <>
              🧠 Categorize with AI
            </>
          )}
        </button>
      )}

      {/* Results */}
      {result && (
        <div className="upload-results">
          <div className="results-header">
            <CheckCircle2 size={24} className="success-icon" />
            <h3>Upload Complete!</h3>
          </div>

          <div className="results-stats">
            <div className="result-stat">
              <span className="stat-value">{result.totalTransactions}</span>
              <span className="stat-label">Total Transactions</span>
            </div>
            <div className="result-stat success">
              <span className="stat-value">{result.successCount}</span>
              <span className="stat-label">Categorized</span>
            </div>
            <div className="result-stat">
              <span className="stat-value" style={{ color: '#f43f5e' }}>
                ₹{(result.totalSpent || 0).toLocaleString('en-IN')}
              </span>
              <span className="stat-label">
                <ArrowDownRight size={14} /> Total Spent
              </span>
            </div>
            <div className="result-stat">
              <span className="stat-value" style={{ color: '#22c55e' }}>
                ₹{(result.totalEarned || 0).toLocaleString('en-IN')}
              </span>
              <span className="stat-label">
                <ArrowUpRight size={14} /> Total Earned
              </span>
            </div>
          </div>

          {/* Category Breakdown */}
          {result.categoryBreakdown && Object.keys(result.categoryBreakdown).length > 0 && (
            <div className="category-results">
              <h4>AI Category Breakdown</h4>
              <div className="category-bars">
                {Object.entries(result.categoryBreakdown).map(([cat, count]) => {
                  const pct = (count / result.totalTransactions) * 100
                  return (
                    <div key={cat} className="cat-bar-row">
                      <span className="cat-bar-label">{cat}</span>
                      <div className="cat-bar-track">
                        <div className="cat-bar-fill" style={{ width: `${pct}%` }} />
                      </div>
                      <span className="cat-bar-count">{count} txns</span>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* Anomalies */}
          {result.anomalies?.length > 0 && (
            <div className="anomaly-results">
              <h4><AlertTriangle size={18} /> Anomalies Detected</h4>
              {result.anomalies.map((a, i) => (
                <div key={i} className="anomaly-item">
                  <strong>{a.description}</strong> — ₹{a.amount?.toLocaleString('en-IN')}
                  <p>{a.reason}</p>
                </div>
              ))}
            </div>
          )}

          <button
            className="btn-secondary"
            onClick={() => { setFile(null); setResult(null) }}
          >
            Upload Another File
          </button>
        </div>
      )}
    </div>
  )
}
