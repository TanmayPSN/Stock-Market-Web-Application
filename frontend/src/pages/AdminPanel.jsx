import React, { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import api from '../api/axios'
import {
  Users, TrendingUp, AlertTriangle,
  BarChart2, Power, Eye, CheckCircle,
  XCircle, RefreshCw, ChevronDown, PlusCircle
} from 'lucide-react'

export default function AdminPanel() {
  const [activeTab,   setActiveTab]   = useState('overview')
  const [summary,     setSummary]     = useState(null)
  const [users,       setUsers]       = useState([])
  const [trades,      setTrades]      = useState([])
  const [marginCalls, setMarginCalls] = useState([])
  const [stocks,      setStocks]      = useState([])
  const [loading,     setLoading]     = useState(true)
  const [marketOpen,  setMarketOpen]  = useState(false)
  const [refreshing,  setRefreshing]  = useState(false)
  const [actionMsg,   setActionMsg]   = useState('')
  const [userSearch,      setUserSearch]      = useState('')
  const [tradeSearch,     setTradeSearch]     = useState('')
  const [tradeSideFilter, setTradeSideFilter] = useState('ALL')

  // ── Add Stock Form State ──
  const [stockForm, setStockForm] = useState({
    ticker:               '',
    companyName:          '',
    currentPrice:         '',
    openPrice:            '',
    highPrice:            '',
    lowPrice:             '',
    previousClosePrice:   '',
    totalSharesAvailable: '',
  })
  const [stockFormError,   setStockFormError]   = useState('')
  const [stockFormLoading, setStockFormLoading] = useState(false)

  useEffect(() => { fetchAll() }, [])

  const fetchAll = async () => {
    try {
      const [summaryRes, usersRes, tradesRes,
             marginRes, marketRes, stocksRes] = await Promise.all([
        api.get('/admin/summary'),
        api.get('/admin/users'),
        api.get('/admin/trades'),
        api.get('/admin/margin/calls'),
        api.get('/market/status'),
        api.get('/stocks/all'),
      ])
      setSummary(summaryRes.data)
      setUsers(usersRes.data)
      setTrades(tradesRes.data)
      setMarginCalls(marginRes.data)
      setMarketOpen(marketRes.data.isOpen)
      setStocks(stocksRes.data)
    } catch (err) {
      console.error('Admin fetch error', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchAll()
    setTimeout(() => setRefreshing(false), 600)
  }

  const showMsg = (msg) => {
    setActionMsg(msg)
    setTimeout(() => setActionMsg(''), 3000)
  }

  const toggleMarket = async () => {
    try {
      await api.put(marketOpen ? '/market/close' : '/market/open')
      setMarketOpen(p => !p)
      showMsg(`Market ${marketOpen ? 'closed' : 'opened'} successfully`)
    } catch (err) {
      showMsg('Failed to toggle market')
    }
  }

  const toggleUser = async (userId, isActive) => {
    try {
      await api.put(`/admin/users/${userId}/${isActive ? 'deactivate' : 'activate'}`)
      setUsers(prev => prev.map(u =>
        u.id === userId ? { ...u, active: !isActive } : u))
      showMsg(`User ${isActive ? 'deactivated' : 'activated'}`)
    } catch (err) {
      const msg = err.response?.data?.message
      showMsg(msg ?? 'Failed to update user')
    }
  }

  const resolveMarginCall = async (userId) => {
    try {
      await api.put(`/admin/margin/${userId}/resolve`)
      setMarginCalls(prev => prev.filter(m => m.user?.id !== userId))
      showMsg('Margin call resolved')
    } catch (err) {
      showMsg('Failed to resolve margin call')
    }
  }

  const cancelAllPending = async () => {
    try {
      await api.put('/orders/cancel-all-pending')
      showMsg('All pending orders cancelled')
    } catch (err) {
      showMsg('Failed to cancel orders')
    }
  }

  const handleDelistStock = async (ticker) => {
    try {
      await api.put(`/stocks/${ticker}/delist`)
      setStocks(prev => prev.map(s =>
        s.ticker === ticker ? { ...s, active: false } : s))
      showMsg(`${ticker} delisted successfully`)
    } catch (err) {
      showMsg('Failed to delist stock')
    }
  }

  const handleStockFormChange = (e) => {
    setStockForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
    setStockFormError('')
  }

  const handleAddStock = async (e) => {
    e.preventDefault()
    setStockFormError('')

    // Basic validation
    const price = parseFloat(stockForm.currentPrice)
    const shares = parseInt(stockForm.totalSharesAvailable)
    if (!stockForm.ticker || !stockForm.companyName) {
      return setStockFormError('Ticker and company name are required')
    }
    if (isNaN(price) || price <= 0) {
      return setStockFormError('Enter a valid price')
    }
    if (isNaN(shares) || shares <= 0) {
      return setStockFormError('Enter valid total shares')
    }

    setStockFormLoading(true)
    try {
      const payload = {
        ticker:               stockForm.ticker.toUpperCase(),
        companyName:          stockForm.companyName,
        currentPrice:         price,
        openPrice:            parseFloat(stockForm.openPrice)           || price,
        highPrice:            parseFloat(stockForm.highPrice)           || price,
        lowPrice:             parseFloat(stockForm.lowPrice)            || price,
        previousClosePrice:   parseFloat(stockForm.previousClosePrice)  || price,
        totalSharesAvailable: shares,
        active:               true,
      }
      const res = await api.post('/stocks/add', payload)
      // Add to local stocks list immediately — visible to admin right away
      setStocks(prev => [...prev, res.data])
      // Reset form
      setStockForm({
        ticker: '', companyName: '', currentPrice: '',
        openPrice: '', highPrice: '', lowPrice: '',
        previousClosePrice: '', totalSharesAvailable: '',
      })
      showMsg(`${res.data.ticker} listed successfully`)
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message
      setStockFormError(msg || 'Failed to add stock')
    } finally {
      setStockFormLoading(false)
    }
  }

  if (loading) return <AdminSkeleton />

  const filteredUsers = users.filter(u =>
    u.username.toLowerCase().includes(userSearch.toLowerCase()) ||
    u.email.toLowerCase().includes(userSearch.toLowerCase()) ||
    String(u.id).includes(userSearch)
  )

  const filteredTrades = trades.filter(t => {
    const q = tradeSearch.toLowerCase()
    const matchesSearch = q === '' || (
      t.ticker?.toLowerCase().includes(q) ||
      t.companyName?.toLowerCase().includes(q) ||
      (t.buyerUsername !== 'EXCHANGE' && t.buyerUsername?.toLowerCase().includes(q)) ||
      (t.sellerUsername !== 'EXCHANGE' && t.sellerUsername?.toLowerCase().includes(q))
    )
    const matchesSide =
      tradeSideFilter === 'ALL'  ? true :
      tradeSideFilter === 'BUY'  ? t.side === 'BUY' :
      tradeSideFilter === 'SELL' ? t.side === 'SELL' : true
    return matchesSearch && matchesSide
  })

  const tabs = [
    { id: 'overview', label: 'Overview',     icon: <BarChart2 size={14} /> },
    { id: 'users',    label: 'Users',        icon: <Users size={14} />,
      badge: users.length },
    { id: 'trades',   label: 'Trades',       icon: <TrendingUp size={14} />,
      badge: trades.length },
    { id: 'margin',   label: 'Margin Calls', icon: <AlertTriangle size={14} />,
      badge: marginCalls.length, warn: marginCalls.length > 0 },
    { id: 'stocks',   label: 'Stocks',       icon: <BarChart2 size={14} />,
      badge: stocks.length },
  ]

  return (
    <div style={s.root}>
      <Navbar isAdmin />
      <div style={s.page}>

        {/* ── Header ── */}
        <div style={s.header}>
          <div>
            <h1 style={s.title}>Admin Panel</h1>
            <p style={s.subtitle}>Platform management and monitoring</p>
          </div>
          <div style={s.headerActions}>
            {actionMsg && (
              <div style={s.actionMsg} className="animate-fadeIn">
                ✓ {actionMsg}
              </div>
            )}
            <button
              onClick={toggleMarket}
              className="btn"
              style={{
                background:  marketOpen ? 'var(--loss-dim)' : 'var(--gain-dim)',
                color:       marketOpen ? 'var(--loss)'     : 'var(--gain)',
                border:      `1px solid ${marketOpen ? 'var(--loss)' : 'var(--gain)'}`,
              }}
            >
              <Power size={14} />
              {marketOpen ? 'CLOSE MARKET' : 'OPEN MARKET'}
            </button>
            <button onClick={cancelAllPending} className="btn btn-ghost">
              Cancel All Pending
            </button>
            <button onClick={handleRefresh} className="btn btn-ghost">
              <RefreshCw size={14} style={{
                animation: refreshing ? 'spin 0.6s linear infinite' : 'none'
              }} />
            </button>
          </div>
        </div>

        {/* ── Market status banner ── */}
        <div style={{
          ...s.marketBanner,
          background:  marketOpen ? 'var(--gain-dim)' : 'var(--loss-dim)',
          borderColor: marketOpen ? 'var(--gain)'     : 'var(--loss)',
        }}>
          <div style={{
            width: 10, height: 10, borderRadius: '50%',
            background: marketOpen ? 'var(--gain)' : 'var(--loss)',
            boxShadow:  marketOpen ? '0 0 8px var(--gain)' : 'none',
            animation:  marketOpen ? 'pulse-red 1.5s infinite' : 'none',
          }} />
          <span style={{
            fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 700,
            color: marketOpen ? 'var(--gain)' : 'var(--loss)',
            letterSpacing: '0.08em',
          }}>
            MARKET {marketOpen ? 'OPEN' : 'CLOSED'}
          </span>
        </div>

        {/* ── Tabs ── */}
        <div style={s.tabRow}>
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{ ...s.tab, ...(activeTab === tab.id ? s.tabActive : {}) }}
            >
              {tab.icon}
              {tab.label}
              {tab.badge > 0 && (
                <span style={{
                  ...s.tabBadge,
                  background: tab.warn ? 'var(--loss)' : 'var(--accent-amber)',
                }}>
                  {tab.badge}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* ── Overview Tab ── */}
        {activeTab === 'overview' && summary && (
          <div className="animate-fadeIn">
            <div style={s.summaryGrid} className="stagger">
              <SummaryTile label="Total Users"    value={summary.totalUsers}
                icon={<Users size={20} />}        color="var(--accent-cyan)" />
              <SummaryTile label="Active Users"   value={summary.activeUsers}
                icon={<CheckCircle size={20} />}  color="var(--gain)" />
              <SummaryTile label="Total Trades"   value={summary.totalTrades}
                icon={<TrendingUp size={20} />}   color="var(--accent-amber)" />
              <SummaryTile label="Active Stocks"  value={summary.activeStocks}
                icon={<BarChart2 size={20} />}    color="var(--accent-cyan)" />
              <SummaryTile label="Margin Calls"   value={summary.activeMarginCalls}
                icon={<AlertTriangle size={20} />}
                color={summary.activeMarginCalls > 0 ? 'var(--loss)' : 'var(--gain)'}
                warn={summary.activeMarginCalls > 0} />
            </div>
            <h3 style={{ ...s.sectionTitle, marginTop: 32 }}>Recent Trades</h3>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>STOCK</th><th>TYPE</th><th>BUYER</th><th>SELLER</th>
                    <th>QTY</th><th>PRICE</th><th>VALUE</th><th>TIME</th>
                  </tr>
                </thead>
                <tbody>
                  {trades.slice(0, 10).map(t => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 700, color: 'var(--text-primary)',
                                   letterSpacing: '0.04em' }}>{t.ticker}</td>
                      <td>
                        <span style={{ fontSize: 10, fontFamily: 'var(--font-mono)',
                          fontWeight: 700,
                          color: t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                            ? 'var(--accent-cyan)' : 'var(--text-muted)' }}>
                          {t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                            ? 'USER↔USER' : 'EXCHANGE'}
                        </span>
                      </td>
                      <td style={{ color: 'var(--gain)' }}>{t.buyerUsername}</td>
                      <td style={{ color: 'var(--loss)' }}>{t.sellerUsername}</td>
                      <td>{t.quantity}</td>
                      <td>₹{parseFloat(t.executedPrice).toLocaleString('en-IN')}</td>
                      <td style={{ color: 'var(--accent-amber)', fontWeight: 600 }}>
                        ₹{parseFloat(t.totalTradeValue).toLocaleString('en-IN')}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                        {new Date(t.executedAt).toLocaleString('en-IN')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ── Users Tab ── */}
        {activeTab === 'users' && (
          <div className="animate-fadeIn">
            <div style={{ marginBottom: 12 }}>
              <input type="text" placeholder="Search by username, email or ID..."
                value={userSearch} onChange={e => setUserSearch(e.target.value)}
                className="input"
                style={{ width: 320, fontSize: 13, fontFamily: 'var(--font-mono)' }} />
            </div>
            <div className="table-wrapper">
              {filteredUsers.length === 0
                ? <div style={s.emptyMargin}>
                    <p style={{ fontFamily: 'var(--font-mono)',
                                color: 'var(--text-muted)', fontSize: 13 }}>
                      No users found for "{userSearch}"
                    </p>
                  </div>
                : <table>
                    <thead>
                      <tr>
                        <th>ID</th><th>USER</th><th>EMAIL</th><th>ROLE</th>
                        <th>BALANCE</th><th>STATUS</th><th>JOINED</th><th>ACTION</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.map(u => (
                        <tr key={u.id}>
                          <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>#{u.id}</td>
                          <td><div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{u.username}</div></td>
                          <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>{u.email}</td>
                          <td>
                            <span style={{
                              ...s.roleBadge,
                              background: u.role === 'ROLE_ADMIN' ? 'var(--accent-amber-dim)' : 'var(--accent-cyan-dim)',
                              color:      u.role === 'ROLE_ADMIN' ? 'var(--accent-amber)'     : 'var(--accent-cyan)',
                              border:     `1px solid ${u.role === 'ROLE_ADMIN' ? 'var(--accent-amber)' : 'var(--accent-cyan)'}`,
                            }}>
                              {u.role === 'ROLE_ADMIN' ? 'ADMIN' : 'USER'}
                            </span>
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                            ₹{parseFloat(u.balance).toLocaleString('en-IN')}
                          </td>
                          <td>
                            <span className={`badge badge-${u.active ? 'executed' : 'rejected'}`}>
                              {u.active ? 'ACTIVE' : 'INACTIVE'}
                            </span>
                          </td>
                          <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                            {new Date(u.createdAt).toLocaleDateString('en-IN')}
                          </td>
                          <td>
                            <button onClick={() => toggleUser(u.id, u.active)}
                              className="btn btn-ghost"
                              style={{ padding: '4px 10px', fontSize: 11,
                                color:       u.active ? 'var(--loss)' : 'var(--gain)',
                                borderColor: u.active ? 'var(--loss)' : 'var(--gain)' }}>
                              {u.active ? <XCircle size={12} /> : <CheckCircle size={12} />}
                              {u.active ? 'Deactivate' : 'Activate'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
              }
            </div>
          </div>
        )}

        {/* ── Trades Tab ── */}
        {activeTab === 'trades' && (
          <div className="animate-fadeIn">
            <div style={{ display: 'flex', gap: 10, marginBottom: 12,
                          alignItems: 'center', flexWrap: 'wrap' }}>
              <input type="text"
                placeholder="Search by ticker, company, buyer or seller..."
                value={tradeSearch} onChange={e => setTradeSearch(e.target.value)}
                className="input"
                style={{ width: 340, fontSize: 13, fontFamily: 'var(--font-mono)' }} />
              {['ALL', 'BUY', 'SELL'].map(f => (
                <button key={f} onClick={() => setTradeSideFilter(f)}
                  className="btn btn-ghost"
                  style={{ padding: '4px 14px', fontSize: 11,
                    fontFamily: 'var(--font-mono)', letterSpacing: '0.06em',
                    color:       tradeSideFilter === f ? 'var(--accent-cyan)' : 'var(--text-muted)',
                    borderColor: tradeSideFilter === f ? 'var(--accent-cyan)' : 'var(--border-primary)' }}>
                  {f}
                </button>
              ))}
              <span style={{ fontSize: 12, fontFamily: 'var(--font-mono)',
                             color: 'var(--text-muted)', marginLeft: 'auto' }}>
                {filteredTrades.length} trades
              </span>
            </div>
            <div className="table-wrapper">
              {filteredTrades.length === 0
                ? <div style={s.emptyMargin}>
                    <p style={{ fontFamily: 'var(--font-mono)',
                                color: 'var(--text-muted)', fontSize: 13 }}>No trades found</p>
                  </div>
                : <table>
                    <thead>
                      <tr>
                        <th>#</th><th>STOCK</th><th>TYPE</th><th>BUYER</th>
                        <th>SELLER</th><th>QTY</th><th>EXEC PRICE</th>
                        <th>TOTAL VALUE</th><th>TIME</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredTrades.map((t, i) => (
                        <tr key={t.id}>
                          <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>#{i + 1}</td>
                          <td style={{ fontWeight: 700, color: 'var(--text-primary)',
                                       letterSpacing: '0.04em' }}>
                            {t.ticker}
                            <div style={{ fontSize: 11, color: 'var(--text-muted)', fontWeight: 400 }}>
                              {t.companyName}
                            </div>
                          </td>
                          <td>
                            <span style={{ fontSize: 10, fontFamily: 'var(--font-mono)',
                              fontWeight: 700, padding: '2px 8px', borderRadius: 20,
                              letterSpacing: '0.06em',
                              background: t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                                ? 'var(--accent-cyan-dim)' : 'var(--bg-input)',
                              color: t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                                ? 'var(--accent-cyan)' : 'var(--text-muted)',
                              border: `1px solid ${t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                                ? 'var(--accent-cyan)' : 'var(--border-primary)'}` }}>
                              {t.buyerUsername !== 'EXCHANGE' && t.sellerUsername !== 'EXCHANGE'
                                ? 'USER↔USER' : 'EXCHANGE'}
                            </span>
                          </td>
                          <td style={{ color: 'var(--gain)', fontWeight: 600 }}>{t.buyerUsername}</td>
                          <td style={{ color: 'var(--loss)', fontWeight: 600 }}>{t.sellerUsername}</td>
                          <td style={{ fontWeight: 600 }}>{t.quantity}</td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                            ₹{parseFloat(t.executedPrice).toLocaleString('en-IN')}
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700,
                                       color: 'var(--accent-amber)' }}>
                            ₹{parseFloat(t.totalTradeValue).toLocaleString('en-IN')}
                          </td>
                          <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                            {new Date(t.executedAt).toLocaleString('en-IN')}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
              }
            </div>
          </div>
        )}

        {/* ── Margin Calls Tab ── */}
        {activeTab === 'margin' && (
          <div className="animate-fadeIn">
            {marginCalls.length === 0
              ? <div style={s.emptyMargin}>
                  <CheckCircle size={40} color="var(--gain)" />
                  <p style={{ fontFamily: 'var(--font-mono)', color: 'var(--gain)',
                               marginTop: 12, fontSize: 14 }}>No active margin calls</p>
                </div>
              : <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        <th>USER</th><th>MARGIN LIMIT</th><th>MARGIN USED</th>
                        <th>MARGIN AVAILABLE</th><th>USAGE %</th><th>ACTION</th>
                      </tr>
                    </thead>
                    <tbody>
                      {marginCalls.map(m => {
                        const usagePct = ((m.marginUsed / (m.marginUsed + m.marginAvailable)) * 100).toFixed(1)
                        return (
                          <tr key={m.id} style={{ background: 'var(--loss-dim)' }}>
                            <td style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                              {m.user?.username ?? `User #${m.id}`}
                            </td>
                            <td>₹{parseFloat(m.marginLimit).toLocaleString('en-IN')}</td>
                            <td style={{ color: 'var(--loss)', fontWeight: 700 }}>
                              ₹{parseFloat(m.marginUsed).toLocaleString('en-IN')}
                            </td>
                            <td style={{ color: 'var(--gain)' }}>
                              ₹{parseFloat(m.marginAvailable).toLocaleString('en-IN')}
                            </td>
                            <td>
                              <span style={{ color: 'var(--loss)', fontFamily: 'var(--font-mono)',
                                             fontWeight: 700 }} className="pulse-red">
                                {usagePct}%
                              </span>
                            </td>
                            <td>
                              <button onClick={() => resolveMarginCall(m.user?.id)}
                                className="btn btn-ghost"
                                style={{ padding: '4px 12px', fontSize: 11,
                                         color: 'var(--gain)', borderColor: 'var(--gain)' }}>
                                <CheckCircle size={12} /> Resolve
                              </button>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
            }
          </div>
        )}

        {/* ── Stocks Tab ── */}
        {activeTab === 'stocks' && (
          <div className="animate-fadeIn">

            {/* Add Stock Form */}
            <h3 style={s.sectionTitle}>List New Stock</h3>
            <div style={s.stockFormCard}>
              <div style={s.stockFormGrid}>
                {[
                  { name: 'ticker',               label: 'TICKER',         placeholder: 'e.g. TCS' },
                  { name: 'companyName',           label: 'COMPANY NAME',   placeholder: 'e.g. Tata Consultancy Services' },
                  { name: 'currentPrice',          label: 'CURRENT PRICE',  placeholder: '0.00', type: 'number' },
                  { name: 'totalSharesAvailable',  label: 'TOTAL SHARES',   placeholder: '1000000', type: 'number' },
                  { name: 'previousClosePrice',    label: 'PREV CLOSE',     placeholder: '0.00 (optional)', type: 'number' },
                  { name: 'openPrice',             label: 'OPEN PRICE',     placeholder: '0.00 (optional)', type: 'number' },
                  { name: 'highPrice',             label: 'HIGH PRICE',     placeholder: '0.00 (optional)', type: 'number' },
                  { name: 'lowPrice',              label: 'LOW PRICE',      placeholder: '0.00 (optional)', type: 'number' },
                ].map(field => (
                  <div key={field.name} style={s.stockFormField}>
                    <label style={s.stockFormLabel}>{field.label}</label>
                    <input
                      name={field.name}
                      type={field.type || 'text'}
                      placeholder={field.placeholder}
                      value={stockForm[field.name]}
                      onChange={handleStockFormChange}
                      className="input"
                      style={{ width: '100%', fontSize: 13,
                               fontFamily: 'var(--font-mono)' }}
                      min={field.type === 'number' ? '0' : undefined}
                      step={field.type === 'number' ? '0.01' : undefined}
                    />
                  </div>
                ))}
              </div>

              {stockFormError && (
                <div style={s.stockFormError}>{stockFormError}</div>
              )}

              <button
                onClick={handleAddStock}
                className="btn"
                disabled={stockFormLoading}
                style={{ marginTop: 16, display: 'flex',
                         alignItems: 'center', gap: 6 }}
              >
                <PlusCircle size={14} />
                {stockFormLoading ? 'Listing...' : 'List Stock'}
              </button>
            </div>

            {/* Stocks Table */}
            <h3 style={{ ...s.sectionTitle, marginTop: 32 }}>
              All Stocks ({stocks.length})
            </h3>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>TICKER</th><th>COMPANY</th><th>PRICE</th>
                    <th>OPEN</th><th>HIGH</th><th>LOW</th>
                    <th>PREV CLOSE</th><th>SHARES AVAILABLE</th>
                    <th>STATUS</th><th>ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  {stocks.map(stock => (
                    <tr key={stock.id}>
                      <td style={{ fontWeight: 700, color: 'var(--text-primary)',
                                   letterSpacing: '0.06em' }}>
                        {stock.ticker}
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                        {stock.companyName}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700,
                                   color: 'var(--accent-cyan)' }}>
                        ₹{parseFloat(stock.currentPrice).toLocaleString('en-IN')}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                        ₹{parseFloat(stock.openPrice).toLocaleString('en-IN')}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12,
                                   color: 'var(--gain)' }}>
                        ₹{parseFloat(stock.highPrice).toLocaleString('en-IN')}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12,
                                   color: 'var(--loss)' }}>
                        ₹{parseFloat(stock.lowPrice).toLocaleString('en-IN')}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                        ₹{parseFloat(stock.previousClosePrice).toLocaleString('en-IN')}
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                        {parseInt(stock.totalSharesAvailable).toLocaleString('en-IN')}
                      </td>
                      <td>
                        <span className={`badge badge-${stock.active ? 'executed' : 'rejected'}`}>
                          {stock.active ? 'ACTIVE' : 'DELISTED'}
                        </span>
                      </td>
                      <td>
                        {stock.active && (
                          <button
                            onClick={() => handleDelistStock(stock.ticker)}
                            className="btn btn-ghost"
                            style={{ padding: '4px 10px', fontSize: 11,
                                     color: 'var(--loss)', borderColor: 'var(--loss)' }}
                          >
                            <XCircle size={12} /> Delist
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

          </div>
        )}

      </div>
    </div>
  )
}

// ── Sub-components and styles (unchanged) ────────────────────────────────────

function SummaryTile({ label, value, icon, color, warn }) {
  return (
    <div style={{
      ...st.card,
      borderColor: warn ? 'var(--loss)' : 'var(--border-primary)',
      boxShadow:   warn ? '0 0 16px var(--loss-dim)' : 'var(--shadow-card)',
    }} className="card animate-fadeInUp">
      <div style={{ ...st.icon, color, background: `${color}22` }}>{icon}</div>
      <div style={{ ...st.value, color }}>{value}</div>
      <div style={st.label}>{label}</div>
    </div>
  )
}

const st = {
  card:  { padding: '24px', flex: '1 1 160px', transition: 'var(--transition)' },
  icon:  { width: 44, height: 44, borderRadius: 'var(--radius-md)', display: 'flex',
           alignItems: 'center', justifyContent: 'center', marginBottom: 16 },
  value: { fontSize: 32, fontFamily: 'var(--font-mono)', fontWeight: 700,
           marginBottom: 6, letterSpacing: '-0.03em' },
  label: { fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)',
           letterSpacing: '0.08em', textTransform: 'uppercase' },
}

function AdminSkeleton() {
  return (
    <div style={{ padding: 24, marginLeft: 240 }}>
      <div style={{ height: 40, width: 200, marginBottom: 24 }} className="skeleton" />
      <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        {[1,2,3,4,5].map(i => (
          <div key={i} style={{ flex: 1, height: 120, borderRadius: 12 }} className="skeleton" />
        ))}
      </div>
      <div style={{ height: 300, borderRadius: 12 }} className="skeleton" />
    </div>
  )
}

const s = {
  root:       { minHeight: '100vh', background: 'var(--bg-primary)' },
  page:       { marginLeft: 240, padding: '32px 28px', maxWidth: 1400 },
  header:     { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
                marginBottom: 16, flexWrap: 'wrap', gap: 16 },
  title:      { fontSize: 28, letterSpacing: '-0.02em', marginBottom: 4 },
  subtitle:   { fontSize: 13, fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' },
  headerActions: { display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' },
  actionMsg:  { background: 'var(--gain-dim)', border: '1px solid var(--gain)',
                borderRadius: 'var(--radius-md)', padding: '8px 14px',
                color: 'var(--gain)', fontSize: 12, fontFamily: 'var(--font-mono)' },
  marketBanner: { display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px',
                  borderRadius: 'var(--radius-md)', border: '1px solid',
                  marginBottom: 24, width: 'fit-content' },
  tabRow:     { display: 'flex', gap: 4, marginBottom: 20,
                borderBottom: '1px solid var(--border-primary)' },
  tab:        { display: 'flex', alignItems: 'center', gap: 6, padding: '10px 18px',
                background: 'none', border: 'none', borderBottom: '2px solid transparent',
                cursor: 'pointer', fontFamily: 'var(--font-ui)', fontSize: 13, fontWeight: 700,
                color: 'var(--text-muted)', letterSpacing: '0.05em', textTransform: 'uppercase',
                transition: 'var(--transition)', marginBottom: '-1px' },
  tabActive:  { color: 'var(--accent-cyan)', borderBottomColor: 'var(--accent-cyan)' },
  tabBadge:   { color: 'var(--bg-primary)', borderRadius: '50%', width: 18, height: 18,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 10, fontWeight: 700 },
  summaryGrid:  { display: 'flex', gap: 16, flexWrap: 'wrap' },
  sectionTitle: { fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700,
                  letterSpacing: '-0.01em', marginBottom: 16, color: 'var(--text-primary)' },
  roleBadge:  { display: 'inline-flex', padding: '2px 8px', borderRadius: 20,
                fontSize: 10, fontFamily: 'var(--font-mono)', fontWeight: 700,
                letterSpacing: '0.08em' },
  emptyMargin:{ display: 'flex', flexDirection: 'column', alignItems: 'center',
                justifyContent: 'center', padding: '60px', background: 'var(--bg-card)',
                border: '1px solid var(--border-primary)', borderRadius: 'var(--radius-lg)' },

  // ── Stocks form styles ──
  stockFormCard: {
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-lg)',
    padding:      '24px',
  },
  stockFormGrid: {
    display:             'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
    gap:                 16,
  },
  stockFormField: {
    display:       'flex',
    flexDirection: 'column',
    gap:           6,
  },
  stockFormLabel: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    letterSpacing: '0.08em',
    fontWeight:    700,
  },
  stockFormError: {
    marginTop:    12,
    padding:      '8px 12px',
    background:   'var(--loss-dim)',
    border:       '1px solid var(--loss)',
    borderRadius: 'var(--radius-md)',
    color:        'var(--loss)',
    fontSize:     12,
    fontFamily:   'var(--font-mono)',
  },
}