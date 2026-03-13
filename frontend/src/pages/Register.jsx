import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import { Eye, EyeOff, TrendingUp, Sun, Moon, IndianRupee } from 'lucide-react'

export default function Register() {
  const { login, toggleTheme, theme } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    username:       '',
    email:          '',
    password:       '',
    initialBalance: ''
  })
  const [error,   setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const [showPwd, setShowPwd] = useState(false)
  const [step,    setStep]    = useState(1)
  // Step 1 = credentials, Step 2 = balance setup
  // Two-step form feels more premium than one long form.

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  const handleNextStep = (e) => {
    e.preventDefault()
    if (!form.username || !form.email || !form.password) {
      setError('Please fill in all fields')
      return
    }
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }
    setError('')
    setStep(2)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.initialBalance || parseFloat(form.initialBalance) < 1000) {
      setError('Minimum initial balance is ₹1,000')
      return
    }
    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/register', {
        ...form,
        initialBalance: parseFloat(form.initialBalance)
      })
      login(res.data)
      navigate('/dashboard')
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Registration failed. Please try again.'
      )
      setStep(1)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.root}>
      <div style={styles.gridBg} />
      <div style={{ ...styles.orb, ...styles.orbCyan }} />
      <div style={{ ...styles.orb, ...styles.orbAmber }} />

      {/* Theme toggle */}
      <button
        onClick={toggleTheme}
        style={styles.themeBtn}
        title="Toggle theme"
      >
        {theme === 'dark'
          ? <Sun  size={18} color="var(--accent-amber)" />
          : <Moon size={18} color="var(--accent-cyan)"  />
        }
      </button>

      <div style={styles.card} className="animate-fadeInUp">

        {/* Logo */}
        <div style={styles.logoRow}>
          <div style={styles.logoIcon}>
            <TrendingUp size={22} color="var(--bg-primary)" />
          </div>
          <span style={styles.logoText}>StockX</span>
        </div>

        {/* Step indicator */}
        <div style={styles.stepRow}>
          <StepDot num={1} active={step === 1} done={step > 1} label="Account" />
          <div style={styles.stepLine} />
          <StepDot num={2} active={step === 2} done={false}    label="Balance" />
        </div>

        <h1 style={styles.title}>
          {step === 1 ? 'Create account' : 'Fund your account'}
        </h1>
        <p style={styles.subtitle}>
          {step === 1
            ? 'Start trading in minutes'
            : 'Set your initial trading balance'
          }
        </p>

        {/* Error */}
        {error && (
          <div style={styles.errorBanner} className="animate-fadeIn">
            <span>⚠</span> {error}
          </div>
        )}

        {/* Step 1 */}
        {step === 1 && (
          <form onSubmit={handleNextStep} style={styles.form}
                className="animate-fadeInUp">

            <div style={styles.fieldGroup}>
              <label style={styles.label}>USERNAME</label>
              <input
                name="username"
                value={form.username}
                onChange={handleChange}
                placeholder="Choose a username"
                required
                className="input"
                style={styles.inputStyle}
              />
            </div>

            <div style={styles.fieldGroup}>
              <label style={styles.label}>EMAIL</label>
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                placeholder="your@email.com"
                required
                className="input"
                style={styles.inputStyle}
              />
            </div>

            <div style={styles.fieldGroup}>
              <label style={styles.label}>PASSWORD</label>
              <div style={styles.pwdWrapper}>
                <input
                  name="password"
                  type={showPwd ? 'text' : 'password'}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Min. 6 characters"
                  required
                  className="input"
                  style={{ ...styles.inputStyle, paddingRight: 44 }}
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

            {/* Password strength bar */}
            <PasswordStrength password={form.password} />

            <button
              type="submit"
              style={styles.submitBtn}
              className="btn btn-primary"
            >
              CONTINUE
            </button>

          </form>
        )}

        {/* Step 2 */}
        {step === 2 && (
          <form onSubmit={handleSubmit} style={styles.form}
                className="animate-fadeInUp">

            <div style={styles.fieldGroup}>
              <label style={styles.label}>INITIAL BALANCE (₹)</label>
              <div style={styles.pwdWrapper}>
                <span style={styles.currencyIcon}>
                  <IndianRupee size={14} color="var(--text-muted)" />
                </span>
                <input
                  name="initialBalance"
                  type="number"
                  min="1000"
                  step="100"
                  value={form.initialBalance}
                  onChange={handleChange}
                  placeholder="Min. 1,000"
                  required
                  className="input"
                  style={{ ...styles.inputStyle, paddingLeft: 36 }}
                />
              </div>
            </div>

            {/* Quick amount buttons */}
            <div style={styles.quickAmounts}>
              {['10000', '50000', '100000', '500000'].map(amt => (
                <button
                  key={amt}
                  type="button"
                  onClick={() =>
                    setForm({ ...form, initialBalance: amt })}
                  style={{
                    ...styles.quickBtn,
                    ...(form.initialBalance === amt
                      ? styles.quickBtnActive
                      : {})
                  }}
                >
                  ₹{parseInt(amt).toLocaleString('en-IN')}
                </button>
              ))}
            </div>

            {/* Margin preview */}
            {form.initialBalance && parseFloat(form.initialBalance) >= 1000 && (
              <div style={styles.marginPreview} className="animate-fadeIn">
                <div style={styles.marginRow}>
                  <span style={styles.marginLabel}>Your Balance</span>
                  <span style={styles.marginValue}>
                    ₹{parseFloat(form.initialBalance).toLocaleString('en-IN')}
                  </span>
                </div>
                <div style={styles.marginRow}>
                  <span style={styles.marginLabel}>5x Margin Power</span>
                  <span style={{ ...styles.marginValue, color: 'var(--accent-cyan)' }}>
                    ₹{(parseFloat(form.initialBalance) * 5)
                        .toLocaleString('en-IN')}
                  </span>
                </div>
              </div>
            )}

            <div style={{ display: 'flex', gap: 12 }}>
              <button
                type="button"
                onClick={() => setStep(1)}
                style={{ ...styles.submitBtn, flex: '0 0 auto', width: 'auto' }}
                className="btn btn-ghost"
              >
                BACK
              </button>
              <button
                type="submit"
                disabled={loading}
                style={{ ...styles.submitBtn, flex: 1 }}
                className="btn btn-primary"
              >
                {loading
                  ? <span style={styles.spinner} />
                  : 'CREATE ACCOUNT'
                }
              </button>
            </div>

          </form>
        )}

        <p style={{ ...styles.registerText, marginTop: 24 }}>
          Already have an account?{' '}
          <Link to="/login" style={styles.link}>Sign in</Link>
        </p>

      </div>
    </div>
  )
}

// ── Step dot indicator ──────────────────────────────────────────
function StepDot({ num, active, done, label }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column',
                  alignItems: 'center', gap: 4 }}>
      <div style={{
        width:          28,
        height:         28,
        borderRadius:   '50%',
        display:        'flex',
        alignItems:     'center',
        justifyContent: 'center',
        fontSize:       12,
        fontFamily:     'var(--font-mono)',
        fontWeight:     700,
        transition:     'var(--transition)',
        background:     done    ? 'var(--gain)'
                      : active  ? 'var(--accent-cyan)'
                      :           'var(--bg-input)',
        color:          (done || active) ? 'var(--bg-primary)' : 'var(--text-muted)',
        border:         active  ? '2px solid var(--accent-cyan)' : '2px solid transparent',
        boxShadow:      active  ? 'var(--shadow-glow-cyan)' : 'none',
      }}>
        {done ? '✓' : num}
      </div>
      <span style={{
        fontSize:   10,
        fontFamily: 'var(--font-mono)',
        color:      active ? 'var(--accent-cyan)' : 'var(--text-muted)',
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
      }}>
        {label}
      </span>
    </div>
  )
}

// ── Password strength indicator ─────────────────────────────────
function PasswordStrength({ password }) {
  if (!password) return null
  const strength =
    password.length >= 12 && /[A-Z]/.test(password) && /[0-9]/.test(password) ? 3
    : password.length >= 8  ? 2
    : password.length >= 6  ? 1
    : 0

  const labels = ['', 'Weak', 'Good', 'Strong']
  const colors = ['', 'var(--loss)', 'var(--accent-amber)', 'var(--gain)']

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <div style={{ display: 'flex', gap: 4 }}>
        {[1, 2, 3].map(i => (
          <div key={i} style={{
            flex:         1,
            height:       3,
            borderRadius: 2,
            background:   i <= strength ? colors[strength] : 'var(--border-primary)',
            transition:   'var(--transition)',
          }} />
        ))}
      </div>
      {strength > 0 && (
        <span style={{
          fontSize:   11,
          fontFamily: 'var(--font-mono)',
          color:      colors[strength],
          textAlign:  'right',
        }}>
          {labels[strength]}
        </span>
      )}
    </div>
  )
}

const styles = {
  root: {
    minHeight:      '100vh',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    background:     'var(--bg-primary)',
    position:       'relative',
    overflow:       'hidden',
    padding:        '24px',
  },
  gridBg: {
    position:        'absolute',
    inset:           0,
    backgroundImage: `
      linear-gradient(var(--border-primary) 1px, transparent 1px),
      linear-gradient(90deg, var(--border-primary) 1px, transparent 1px)
    `,
    backgroundSize:  '40px 40px',
    opacity:         0.4,
    pointerEvents:   'none',
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
    left:       '-10%',
  },
  orbAmber: {
    width:      300,
    height:     300,
    background: 'var(--accent-amber)',
    bottom:     '-10%',
    right:      '-5%',
  },
  themeBtn: {
    position:       'absolute',
    top:            24,
    right:          24,
    background:     'var(--bg-card)',
    border:         '1px solid var(--border-primary)',
    borderRadius:   '50%',
    width:          40,
    height:         40,
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    cursor:         'pointer',
    transition:     'var(--transition)',
    zIndex:         10,
  },
  card: {
    position:     'relative',
    zIndex:       1,
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-xl)',
    padding:      '40px',
    width:        '100%',
    maxWidth:     '440px',
    boxShadow:    'var(--shadow-card), var(--shadow-glow-cyan)',
  },
  logoRow: {
    display:      'flex',
    alignItems:   'center',
    gap:          10,
    marginBottom: 28,
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
  stepRow: {
    display:     'flex',
    alignItems:  'flex-start',
    gap:         0,
    marginBottom: 28,
  },
  stepLine: {
    flex:       1,
    height:     2,
    background: 'var(--border-primary)',
    marginTop:  13,
  },
  title: {
    fontSize:      26,
    marginBottom:  6,
    letterSpacing: '-0.02em',
  },
  subtitle: {
    fontSize:     14,
    color:        'var(--text-secondary)',
    marginBottom: 24,
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
  inputStyle: {
    width: '100%',
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
  currencyIcon: {
    position:   'absolute',
    left:       12,
    top:        '50%',
    transform:  'translateY(-50%)',
    display:    'flex',
    alignItems: 'center',
    pointerEvents: 'none',
  },
  submitBtn: {
    width:          '100%',
    justifyContent: 'center',
    padding:        '14px',
    fontSize:       14,
  },
  quickAmounts: {
    display:   'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap:       8,
  },
  quickBtn: {
    background:    'var(--bg-input)',
    border:        '1px solid var(--border-primary)',
    borderRadius:  'var(--radius-md)',
    padding:       '8px 4px',
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-secondary)',
    cursor:        'pointer',
    transition:    'var(--transition)',
    textAlign:     'center',
  },
  quickBtnActive: {
    borderColor: 'var(--accent-cyan)',
    color:       'var(--accent-cyan)',
    background:  'var(--accent-cyan-dim)',
  },
  marginPreview: {
    background:   'var(--accent-cyan-dim)',
    border:       '1px solid var(--border-accent)',
    borderRadius: 'var(--radius-md)',
    padding:      '14px 16px',
    display:      'flex',
    flexDirection:'column',
    gap:          8,
  },
  marginRow: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
  },
  marginLabel: {
    fontSize:   12,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
  },
  marginValue: {
    fontSize:   13,
    fontFamily: 'var(--font-mono)',
    fontWeight: 700,
    color:      'var(--text-primary)',
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
  spinner: {
    width:        16,
    height:       16,
    border:       '2px solid rgba(0,0,0,0.2)',
    borderTop:    '2px solid var(--bg-primary)',
    borderRadius: '50%',
    display:      'inline-block',
    animation:    'spin 0.8s linear infinite',
  },
}