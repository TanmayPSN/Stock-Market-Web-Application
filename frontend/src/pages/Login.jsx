import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import { Eye, EyeOff, TrendingUp, Sun, Moon } from 'lucide-react'

export default function Login() {
  const { login, toggleTheme, theme } = useAuth()
  const navigate = useNavigate()

  const [form,    setForm]    = useState({ username: '', password: '' })
  const [error,   setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const [showPwd, setShowPwd] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/login', form)
      login(res.data)
      navigate(res.data.role === 'ROLE_ADMIN' ? '/admin' : '/dashboard')
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Invalid username or password'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.root}>

      {/* ── Animated background grid ── */}
      <div style={styles.gridBg} />

      {/* ── Floating orbs ── */}
      <div style={{ ...styles.orb, ...styles.orbCyan }} />
      <div style={{ ...styles.orb, ...styles.orbAmber }} />

      {/* ── Theme toggle ── */}
      <button
        onClick={toggleTheme}
        style={styles.themeBtn}
        title="Toggle theme"
      >
        {theme === 'dark'
          ? <Sun size={18} color="var(--accent-amber)" />
          : <Moon size={18} color="var(--accent-cyan)" />
        }
      </button>

      {/* ── Card ── */}
      <div style={styles.card} className="animate-fadeInUp">

        {/* Logo */}
        <div style={styles.logoRow}>
          <div style={styles.logoIcon}>
            <TrendingUp size={22} color="var(--bg-primary)" />
          </div>
          <span style={styles.logoText}>StockX</span>
        </div>

        <h1 style={styles.title}>Welcome back</h1>
        <p style={styles.subtitle}>Sign in to your trading account</p>

        {/* Error banner */}
        {error && (
          <div style={styles.errorBanner} className="animate-fadeIn">
            <span style={{ fontSize: 14 }}>⚠</span>
            {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} style={styles.form}>

          {/* Username */}
          <div style={styles.fieldGroup}>
            <label style={styles.label}>USERNAME</label>
            <input
              name="username"
              value={form.username}
              onChange={handleChange}
              placeholder="Enter your username"
              autoComplete="username"
              required
              style={styles.input}
              className="input"
              onFocus={e => e.target.style.borderColor = 'var(--accent-cyan)'}
              onBlur={e  => e.target.style.borderColor = 'var(--border-primary)'}
            />
          </div>

          {/* Password */}
          <div style={styles.fieldGroup}>
            <label style={styles.label}>PASSWORD</label>
            <div style={styles.pwdWrapper}>
              <input
                name="password"
                type={showPwd ? 'text' : 'password'}
                value={form.password}
                onChange={handleChange}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
                style={{ ...styles.input, paddingRight: 44 }}
                className="input"
              />
              <button
                type="button"
                onClick={() => setShowPwd(p => !p)}
                style={styles.eyeBtn}
              >
                {showPwd
                  ? <EyeOff size={16} color="var(--text-muted)" />
                  : <Eye    size={16} color="var(--text-muted)" />
                }
              </button>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading}
            style={styles.submitBtn}
            className="btn btn-primary"
          >
            {loading
              ? <span style={styles.spinner} />
              : 'SIGN IN'
            }
          </button>

        </form>

        {/* Divider */}
        <div style={styles.dividerRow}>
          <div style={styles.dividerLine} />
          <span style={styles.dividerText}>or</span>
          <div style={styles.dividerLine} />
        </div>

        {/* Register link */}
        <p style={styles.registerText}>
          Don't have an account?{' '}
          <Link to="/register" style={styles.link}>
            Create one
          </Link>
        </p>

        {/* Market ticker strip */}
        <div style={styles.tickerStrip}>
          <TickerItem ticker="TCS"      price="3,512" dir="up"   />
          <TickerItem ticker="INFY"     price="1,448" dir="down" />
          <TickerItem ticker="RELIANCE" price="2,461" dir="up"   />
          <TickerItem ticker="HDFC"     price="1,672" dir="up"   />
        </div>

      </div>
    </div>
  )
}

function TickerItem({ ticker, price, dir }) {
  return (
    <div style={tickerStyles.item}>
      <span style={tickerStyles.ticker}>{ticker}</span>
      <span style={{
        ...tickerStyles.price,
        color: dir === 'up' ? 'var(--gain)' : 'var(--loss)'
      }}>
        ₹{price}
      </span>
      <span style={{ color: dir === 'up' ? 'var(--gain)' : 'var(--loss)', fontSize: 10 }}>
        {dir === 'up' ? '▲' : '▼'}
      </span>
    </div>
  )
}

const tickerStyles = {
  item: {
    display:    'flex',
    alignItems: 'center',
    gap:        6,
  },
  ticker: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    letterSpacing: '0.08em',
  },
  price: {
    fontSize:   12,
    fontFamily: 'var(--font-mono)',
    fontWeight: 600,
  }
}

const styles = {
  root: {
    minHeight:       '100vh',
    display:         'flex',
    alignItems:      'center',
    justifyContent:  'center',
    background:      'var(--bg-primary)',
    position:        'relative',
    overflow:        'hidden',
    padding:         '24px',
  },
  gridBg: {
    position:           'absolute',
    inset:              0,
    backgroundImage:    `
      linear-gradient(var(--border-primary) 1px, transparent 1px),
      linear-gradient(90deg, var(--border-primary) 1px, transparent 1px)
    `,
    backgroundSize:     '40px 40px',
    opacity:            0.4,
    pointerEvents:      'none',
  },
  orb: {
    position:     'absolute',
    borderRadius: '50%',
    filter:       'blur(80px)',
    pointerEvents:'none',
    opacity:      0.15,
  },
  orbCyan: {
    width:      400,
    height:     400,
    background: 'var(--accent-cyan)',
    top:        '-10%',
    right:      '-10%',
  },
  orbAmber: {
    width:      300,
    height:     300,
    background: 'var(--accent-amber)',
    bottom:     '-10%',
    left:       '-5%',
  },
  themeBtn: {
    position:     'absolute',
    top:          24,
    right:        24,
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: '50%',
    width:        40,
    height:       40,
    display:      'flex',
    alignItems:   'center',
    justifyContent: 'center',
    cursor:       'pointer',
    transition:   'var(--transition)',
    zIndex:       10,
  },
  card: {
    position:     'relative',
    zIndex:       1,
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-xl)',
    padding:      '40px',
    width:        '100%',
    maxWidth:     '420px',
    boxShadow:    'var(--shadow-card), var(--shadow-glow-cyan)',
  },
  logoRow: {
    display:        'flex',
    alignItems:     'center',
    gap:            10,
    marginBottom:   28,
  },
  logoIcon: {
    width:          40,
    height:         40,
    background:     'var(--accent-cyan)',
    borderRadius:   'var(--radius-md)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    boxShadow:      'var(--shadow-glow-cyan)',
  },
  logoText: {
    fontFamily:    'var(--font-display)',
    fontSize:      22,
    fontWeight:    800,
    color:         'var(--text-primary)',
    letterSpacing: '-0.03em',
  },
  title: {
    fontSize:     28,
    marginBottom: 6,
    letterSpacing:'-0.02em',
  },
  subtitle: {
    fontSize:     14,
    color:        'var(--text-secondary)',
    marginBottom: 28,
    fontFamily:   'var(--font-mono)',
  },
  errorBanner: {
    display:      'flex',
    alignItems:   'center',
    gap:          8,
    background:   'var(--loss-dim)',
    border:       '1px solid var(--loss)',
    borderRadius: 'var(--radius-md)',
    padding:      '10px 14px',
    color:        'var(--loss)',
    fontSize:     13,
    fontFamily:   'var(--font-mono)',
    marginBottom: 20,
  },
  form: {
    display:       'flex',
    flexDirection: 'column',
    gap:           18,
  },
  fieldGroup: {
    display:       'flex',
    flexDirection: 'column',
    gap:           6,
  },
  label: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    letterSpacing: '0.1em',
    fontWeight:    700,
  },
  input: {
    width:      '100%',
  },
  pwdWrapper: {
    position: 'relative',
  },
  eyeBtn: {
    position:   'absolute',
    right:      12,
    top:        '50%',
    transform:  'translateY(-50%)',
    background: 'none',
    border:     'none',
    cursor:     'pointer',
    padding:    4,
    display:    'flex',
    alignItems: 'center',
  },
  submitBtn: {
    width:          '100%',
    justifyContent: 'center',
    padding:        '14px',
    fontSize:       14,
    marginTop:      4,
  },
  dividerRow: {
    display:    'flex',
    alignItems: 'center',
    gap:        12,
    margin:     '24px 0 16px',
  },
  dividerLine: {
    flex:       1,
    height:     1,
    background: 'var(--border-primary)',
  },
  dividerText: {
    fontSize:   12,
    color:      'var(--text-muted)',
    fontFamily: 'var(--font-mono)',
  },
  registerText: {
    textAlign:  'center',
    fontSize:   13,
    color:      'var(--text-secondary)',
    fontFamily: 'var(--font-mono)',
  },
  link: {
    color:          'var(--accent-cyan)',
    textDecoration: 'none',
    fontWeight:     700,
  },
  tickerStrip: {
    display:        'flex',
    justifyContent: 'space-between',
    marginTop:      28,
    paddingTop:     20,
    borderTop:      '1px solid var(--border-primary)',
  },
  spinner: {
    width:       16,
    height:      16,
    border:      '2px solid rgba(0,0,0,0.2)',
    borderTop:   '2px solid var(--bg-primary)',
    borderRadius:'50%',
    display:     'inline-block',
    animation:   'spin 0.8s linear infinite',
  },
}