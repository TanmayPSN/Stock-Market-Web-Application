import React, { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import TradeHistory from '../components/TradeHistory'
import api from '../api/axios'
import { useWebSocket } from '../hooks/useWebSocket'
import {
  TrendingUp, TrendingDown, Wallet,
  BarChart2, Clock, RefreshCw,
  PlusCircle, MinusCircle
} from 'lucide-react'
import {
  PieChart, Pie, Cell, Tooltip,
  ResponsiveContainer, Legend
} from 'recharts'

const CHART_COLORS = [
  '#00d4ff', '#f5a623', '#00e676', '#ff4757',
  '#a29bfe', '#fd79a8', '#55efc4', '#fdcb6e'
]

export default function Portfolio() {
  const { prices } = useWebSocket()
  const [holdingSearch,  setHoldingSearch]  = useState('')
  const [orderFilter,    setOrderFilter]    = useState('ALL')

  const [portfolio, setPortfolio] = useState(null)
  const [orders,    setOrders]    = useState([])
  const [loading,   setLoading]   = useState(true)
  const [activeTab, setActiveTab] = useState('holdings')
  const [refreshing,setRefreshing]= useState(false)

  // ── Balance modal state ──
  const [balanceModal,   setBalanceModal]   = useState(null)
  // null | 'add' | 'withdraw'
  const [balanceAmount,  setBalanceAmount]  = useState('')
  const [balanceError,   setBalanceError]   = useState('')
  const [balanceSuccess, setBalanceSuccess] = useState('')
  const [balanceLoading, setBalanceLoading] = useState(false)

  useEffect(() => { fetchData() }, [])

  const fetchData = async () => {
    try {
      const [portRes, ordersRes] = await Promise.all([
        api.get('/portfolio/me'),
        api.get('/orders/my'),
      ])
      setPortfolio(portRes.data)
      setOrders(ordersRes.data)
    } catch (err) {
      console.error('Portfolio fetch error', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchData()
    setTimeout(() => setRefreshing(false), 600)
  }

  const handleCancelOrder = async (orderId) => {
    try {
      await api.put(`/orders/${orderId}/cancel`)
      fetchData()
    } catch (err) {
      console.error('Cancel order error', err)
    }
  }

  const handleBalanceAction = async () => {
    setBalanceError('')
    setBalanceSuccess('')
    const amount = parseFloat(balanceAmount)
    if (isNaN(amount) || amount <= 0) {
      return setBalanceError('Enter a valid amount greater than 0')
    }

    setBalanceLoading(true)
    try {
      const endpoint = balanceModal === 'add'
        ? '/user/balance/add'
        : '/user/balance/withdraw'
      const res = await api.put(endpoint, { amount })

      // Update available balance in portfolio state immediately
      setPortfolio(prev => ({
        ...prev,
        availableBalance: res.data.newBalance,
      }))
      setBalanceSuccess(res.data.message)
      setBalanceAmount('')
      setTimeout(() => {
        setBalanceModal(null)
        setBalanceSuccess('')
      }, 1500)
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message
      setBalanceError(msg || 'Failed to update balance')
    } finally {
      setBalanceLoading(false)
    }
  }

  // Enrich holdings with live prices
  const enrichedHoldings = portfolio?.holdings?.map(h => ({
    ...h,
    currentPrice:  prices[h.ticker] ?? h.currentPrice,
    currentValue:  (prices[h.ticker] ?? h.currentPrice) * h.quantityOwned,
    profitLoss:    ((prices[h.ticker] ?? h.currentPrice) - h.averageBuyPrice)
                   * h.quantityOwned,
  })) ?? []

  // Pie chart data from holdings
  const pieData = enrichedHoldings
    .filter(h => h.quantityOwned > 0)
    .map(h => ({
      name:  h.ticker,
      value: parseFloat(h.currentValue.toFixed(2)),
    }))

  const filteredHoldings = enrichedHoldings.filter(h =>
    h.ticker.toLowerCase().includes(holdingSearch.toLowerCase()) ||
    h.companyName.toLowerCase().includes(holdingSearch.toLowerCase())
  )

  const filteredOrders = orderFilter === 'ALL'
    ? orders
    : orders.filter(o => o.status === orderFilter)

  const totalPortfolioValue =
    (portfolio?.availableBalance ?? 0) +
    (portfolio?.currentMarketValue ?? 0)

  if (loading) return <PortfolioSkeleton />

  return (
    <div style={s.root}>
      <Navbar />

      <div style={s.page}>

        {/* ── Header ── */}
        <div style={s.header}>
          <div>
            <h1 style={s.title}>My Portfolio</h1>
            <p style={s.subtitle}>
              Track your investments and performance
            </p>
          </div>
          <button
            onClick={handleRefresh}
            className="btn btn-ghost"
          >
            <RefreshCw
              size={14}
              style={{
                animation: refreshing
                  ? 'spin 0.6s linear infinite' : 'none'
              }}
            />
            Refresh
          </button>
        </div>

        {/* ── Margin call warning ── */}
        {portfolio?.marginCallTriggered && (
          <div style={s.marginCallBanner} className="pulse-red animate-fadeIn">
            <span style={{ fontSize: 20 }}>⚠</span>
            <div>
              <strong>Margin Call Triggered</strong>
              <p style={{ fontSize: 12, marginTop: 2, opacity: 0.8 }}>
                Your margin usage exceeds 80%.
                Add funds or close positions immediately.
              </p>
            </div>
          </div>
        )}

        {/* ── Top stats ── */}
        <div style={s.statsRow} className="stagger">
          <StatCard
            label="Total Portfolio Value"
            value={`₹${totalPortfolioValue.toLocaleString('en-IN')}`}
            icon={<Wallet size={20} />}
            color="var(--accent-cyan)"
            large
          />

          {/* Available Balance + Add/Withdraw buttons */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <StatCard
              label="Available Balance"
              value={`₹${portfolio?.availableBalance?.toLocaleString('en-IN') ?? 0}`}
              icon={<Wallet size={18} />}
              color="var(--accent-amber)"
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                onClick={() => {
                  setBalanceModal('add')
                  setBalanceAmount('')
                  setBalanceError('')
                  setBalanceSuccess('')
                }}
                className="btn btn-ghost"
                style={{
                  flex: 1, justifyContent: 'center', fontSize: 11,
                  color: 'var(--gain)', borderColor: 'var(--gain)',
                  padding: '6px 8px',
                }}
              >
                <PlusCircle size={12} /> Add Funds
              </button>
              <button
                onClick={() => {
                  setBalanceModal('withdraw')
                  setBalanceAmount('')
                  setBalanceError('')
                  setBalanceSuccess('')
                }}
                className="btn btn-ghost"
                style={{
                  flex: 1, justifyContent: 'center', fontSize: 11,
                  color: 'var(--loss)', borderColor: 'var(--loss)',
                  padding: '6px 8px',
                }}
              >
                <MinusCircle size={12} /> Withdraw
              </button>
            </div>
          </div>

          <StatCard
            label="Invested Value"
            value={`₹${portfolio?.totalInvestedValue?.toLocaleString('en-IN') ?? 0}`}
            icon={<BarChart2 size={18} />}
            color="var(--accent-cyan)"
          />
          <StatCard
            label="Overall P&L"
            value={`₹${portfolio?.totalProfitLoss?.toLocaleString('en-IN') ?? 0}`}
            icon={portfolio?.totalProfitLoss >= 0
              ? <TrendingUp size={18} />
              : <TrendingDown size={18} />
            }
            color={portfolio?.totalProfitLoss >= 0
              ? 'var(--gain)' : 'var(--loss)'}
            sub={`${portfolio?.totalProfitLossPercentage?.toFixed(2)}%`}
          />
        </div>

        {/* ── Main content: chart + holdings ── */}
        <div style={s.mainRow}>

          {/* Pie chart */}
          {pieData.length > 0 && (
            <div style={s.chartCard} className="card">
              <h3 style={s.cardTitle}>Holdings Breakdown</h3>
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={110}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {pieData.map((_, i) => (
                      <Cell
                        key={i}
                        fill={CHART_COLORS[i % CHART_COLORS.length]}
                        stroke="var(--bg-card)"
                        strokeWidth={2}
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background:   'var(--bg-card)',
                      border:       '1px solid var(--border-primary)',
                      borderRadius: 8,
                      fontFamily:   'var(--font-mono)',
                      fontSize:     12,
                      color:        'var(--text-primary)',
                    }}
                    formatter={(v) =>
                      [`₹${v.toLocaleString('en-IN')}`, 'Value']}
                  />
                  <Legend
                    formatter={(value) => (
                      <span style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize:   11,
                        color:      'var(--text-secondary)',
                      }}>
                        {value}
                      </span>
                    )}
                  />
                </PieChart>
              </ResponsiveContainer>

              {/* Centre label */}
              <div style={s.pieCenter}>
                <span style={s.pieCenterValue}>
                  ₹{portfolio?.currentMarketValue?.toLocaleString('en-IN')}
                </span>
                <span style={s.pieCenterLabel}>Market Value</span>
              </div>
            </div>
          )}

          {/* Margin info */}
          {portfolio?.marginEnabled && (
            <div style={s.marginCard} className="card">
              <h3 style={s.cardTitle}>Margin Account</h3>
              <div style={s.marginRows}>
                <MarginRow
                  label="Margin Used"
                  value={`₹${portfolio.marginUsed?.toLocaleString('en-IN')}`}
                  color="var(--loss)"
                />
                <MarginRow
                  label="Margin Available"
                  value={`₹${portfolio.marginAvailable?.toLocaleString('en-IN')}`}
                  color="var(--gain)"
                />
              </div>

              {/* Usage bar */}
              <div style={s.marginBarTrack}>
                <div style={{
                  ...s.marginBarFill,
                  width: `${Math.min(
                    (portfolio.marginUsed /
                    (portfolio.marginUsed + portfolio.marginAvailable)) * 100,
                    100
                  )}%`,
                  background: portfolio.marginCallTriggered
                    ? 'var(--loss)' : 'var(--accent-cyan)',
                }} />
              </div>
              <p style={s.marginBarLabel}>
                {((portfolio.marginUsed /
                  (portfolio.marginUsed + portfolio.marginAvailable))
                  * 100).toFixed(1)}% used
              </p>
            </div>
          )}

        </div>

        {/* ── Tabs ── */}
        <div style={s.tabRow}>
          {['holdings', 'orders', 'trades'].map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                ...s.tab,
                ...(activeTab === tab ? s.tabActive : {})
              }}
            >
              {tab === 'holdings' && <BarChart2  size={14} />}
              {tab === 'orders'   && <Clock      size={14} />}
              {tab === 'trades'   && <TrendingUp size={14} />}
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
              {tab === 'orders' && orders.filter(
                o => o.status === 'PENDING' || o.status === 'PARTIAL').length > 0 && (
                <span style={s.tabBadge}>
                  {orders.filter(o =>
                    o.status === 'PENDING' || o.status === 'PARTIAL').length}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* ── Holdings tab ── */}
        {activeTab === 'holdings' && (
          <div style={s.tableWrapper} className="table-wrapper animate-fadeIn">
            <div style={{ marginBottom: 12 }}>
              <input
                type="text"
                placeholder="Search holdings by ticker or company..."
                value={holdingSearch}
                onChange={e => setHoldingSearch(e.target.value)}
                className="input"
                style={{ width: 300, fontSize: 13, fontFamily: 'var(--font-mono)' }}
              />
            </div>
            {filteredHoldings.length === 0
              ? <EmptyState message={
                  holdingSearch
                    ? `No holdings found for "${holdingSearch}"`
                    : 'No holdings yet. Start trading!'
                } />
              : (
                <table>
                  <thead>
                    <tr>
                      <th>STOCK</th><th>QTY</th><th>AVG BUY</th>
                      <th>CURRENT</th><th>VALUE</th><th>P&L</th><th>P&L %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredHoldings.map(h => {
                      const pl    = h.profitLoss
                      const plPct = ((pl / h.investedAmount) * 100).toFixed(2)
                      return (
                        <tr key={h.ticker}>
                          <td>
                            <div>
                              <div style={{ fontWeight: 700,
                                            color: 'var(--text-primary)',
                                            letterSpacing: '0.04em' }}>
                                {h.ticker}
                              </div>
                              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                                {h.companyName}
                              </div>
                            </div>
                          </td>
                          <td style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                            {h.quantityOwned}
                          </td>
                          <td>₹{parseFloat(h.averageBuyPrice).toLocaleString('en-IN')}</td>
                          <td style={{
                            color: h.priceDirection === 'UP'
                              ? 'var(--gain)' : 'var(--loss)',
                            fontWeight: 600,
                          }}>
                            ₹{parseFloat(h.currentPrice).toLocaleString('en-IN')}
                            <span style={{ fontSize: 10, marginLeft: 4 }}>
                              {h.priceDirection === 'UP' ? '▲' : '▼'}
                            </span>
                          </td>
                          <td style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                            ₹{parseFloat(h.currentValue).toLocaleString('en-IN')}
                          </td>
                          <td style={{
                            color: pl >= 0 ? 'var(--gain)' : 'var(--loss)',
                            fontWeight: 700,
                          }}>
                            {pl >= 0 ? '+' : ''}₹{parseFloat(pl).toLocaleString('en-IN')}
                          </td>
                          <td>
                            <span className={`badge badge-${
                              parseFloat(plPct) >= 0 ? 'executed' : 'rejected'}`}>
                              {parseFloat(plPct) >= 0 ? '+' : ''}{plPct}%
                            </span>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )
            }
          </div>
        )}

        {/* ── Orders tab ── */}
        {activeTab === 'orders' && (
          <div style={s.tableWrapper} className="table-wrapper animate-fadeIn">
            <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
              {['ALL', 'PENDING', 'PARTIAL', 'EXECUTED', 'CANCELLED', 'REJECTED'].map(f => (
                <button
                  key={f}
                  onClick={() => setOrderFilter(f)}
                  className="btn btn-ghost"
                  style={{
                    padding:     '4px 12px',
                    fontSize:    11,
                    fontFamily:  'var(--font-mono)',
                    letterSpacing: '0.06em',
                    color:       orderFilter === f
                      ? 'var(--accent-cyan)' : 'var(--text-muted)',
                    borderColor: orderFilter === f
                      ? 'var(--accent-cyan)' : 'var(--border-primary)',
                  }}
                >
                  {f}
                </button>
              ))}
            </div>
            {filteredOrders.length === 0
              ? <EmptyState message="No orders found." />
              : (
                <table>
                  <thead>
                    <tr>
                      <th>STOCK</th><th>SIDE</th><th>TYPE</th><th>QTY</th>
                      <th>FILLED</th><th>LIMIT PRICE</th><th>EXEC PRICE</th>
                      <th>VALUE</th><th>STATUS</th><th>PLACED</th><th>ACTION</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredOrders.map(o => (
                      <tr key={o.id}>
                        <td style={{ fontWeight: 700,
                                     color: 'var(--text-primary)',
                                     letterSpacing: '0.04em' }}>
                          {o.ticker}
                        </td>
                        <td>
                          <span style={{
                            color: o.side === 'BUY'
                              ? 'var(--gain)' : 'var(--loss)',
                            fontWeight: 700,
                          }}>
                            {o.side}
                          </span>
                        </td>
                        <td>{o.type}</td>
                        <td>{o.quantity}</td>
                        <td style={{
                          fontFamily: 'var(--font-mono)',
                          fontSize:   12,
                          color: o.filledQuantity > 0
                            ? 'var(--accent-cyan)' : 'var(--text-muted)',
                        }}>
                          {o.filledQuantity ?? 0}/{o.quantity}
                        </td>
                        <td>{o.limitPrice
                          ? `₹${parseFloat(o.limitPrice).toLocaleString('en-IN')}`
                          : '—'}
                        </td>
                        <td>{o.executedPrice
                          ? `₹${parseFloat(o.executedPrice).toLocaleString('en-IN')}`
                          : '—'}
                        </td>
                        <td>{o.totalOrderValue
                          ? `₹${parseFloat(o.totalOrderValue).toLocaleString('en-IN')}`
                          : '—'}
                        </td>
                        <td>
                          <span className={`badge badge-${o.status.toLowerCase()}`}>
                            {o.status}
                          </span>
                        </td>
                        <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                          {new Date(o.placedAt).toLocaleString('en-IN')}
                        </td>
                        <td>
                          {(o.status === 'PENDING' || o.status === 'PARTIAL') && (
                            <button
                              onClick={() => handleCancelOrder(o.id)}
                              className="btn btn-ghost"
                              style={{ padding: '4px 10px', fontSize: 11 }}
                            >
                              Cancel
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            }
          </div>
        )}

        {/* ── Trades tab ── */}
        {activeTab === 'trades' && (
          <div className="animate-fadeIn">
            <TradeHistory />
          </div>
        )}

      </div>{/* end s.page */}

      {/* ── Balance Modal ── */}
      {balanceModal && (
        <div style={s.modalOverlay} onClick={() => setBalanceModal(null)}>
          <div style={s.modalCard} onClick={e => e.stopPropagation()}>

            <div style={{ display: 'flex', alignItems: 'center',
                          gap: 10, marginBottom: 6 }}>
              {balanceModal === 'add'
                ? <PlusCircle  size={20} color="var(--gain)" />
                : <MinusCircle size={20} color="var(--loss)" />}
              <h3 style={{ ...s.cardTitle, marginBottom: 0 }}>
                {balanceModal === 'add' ? 'Add Funds' : 'Withdraw Funds'}
              </h3>
            </div>

            <p style={{ fontSize: 12, color: 'var(--text-muted)',
                        fontFamily: 'var(--font-mono)', marginBottom: 20 }}>
              Current balance: ₹{portfolio?.availableBalance
                ?.toLocaleString('en-IN')}
            </p>

            <label style={s.fieldLabel}>AMOUNT (₹)</label>
            <input
              type="number"
              min="1"
              step="0.01"
              placeholder="Enter amount..."
              value={balanceAmount}
              onChange={e => {
                setBalanceAmount(e.target.value)
                setBalanceError('')
                setBalanceSuccess('')
              }}
              className="input"
              style={{ width: '100%', fontSize: 14,
                       fontFamily: 'var(--font-mono)', marginTop: 6 }}
              autoFocus
            />

            {balanceError && (
              <div style={s.errorBox}>{balanceError}</div>
            )}
            {balanceSuccess && (
              <div style={s.successBox}>{balanceSuccess}</div>
            )}

            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button
                onClick={handleBalanceAction}
                disabled={balanceLoading}
                className="btn"
                style={{
                  flex:           1,
                  justifyContent: 'center',
                  background: balanceModal === 'add'
                    ? 'var(--gain-dim)' : 'var(--loss-dim)',
                  color:      balanceModal === 'add'
                    ? 'var(--gain)' : 'var(--loss)',
                  border:     `1px solid ${balanceModal === 'add'
                    ? 'var(--gain)' : 'var(--loss)'}`,
                }}
              >
                {balanceLoading
                  ? 'Processing...'
                  : balanceModal === 'add'
                    ? '+ Add Funds'
                    : '− Withdraw'}
              </button>
              <button
                onClick={() => setBalanceModal(null)}
                className="btn btn-ghost"
                style={{ flex: 1, justifyContent: 'center' }}
              >
                Cancel
              </button>
            </div>

          </div>
        </div>
      )}

    </div> // end s.root
  )
}

// ── Sub-components ────────────────────────────────────────────────────────────

function StatCard({ label, value, icon, color, sub, large }) {
  return (
    <div style={{
      ...stc.card,
      flex: large ? '2 1 200px' : '1 1 140px',
    }} className="card animate-fadeInUp">
      <div style={stc.iconRow}>
        <div style={{ ...stc.icon, color, background: `${color}22` }}>
          {icon}
        </div>
      </div>
      <div style={{ ...stc.value, color, fontSize: large ? 26 : 20 }}>
        {value}
      </div>
      {sub && (
        <div style={{
          fontSize:     12,
          fontFamily:   'var(--font-mono)',
          fontWeight:   700,
          marginBottom: 4,
          color: parseFloat(sub) >= 0 ? 'var(--gain)' : 'var(--loss)',
        }}>
          {parseFloat(sub) >= 0 ? '▲' : '▼'}{' '}
          {Math.abs(parseFloat(sub))}%
        </div>
      )}
      <div style={stc.label}>{label}</div>
    </div>
  )
}

const stc = {
  card:    { padding: '20px', transition: 'var(--transition)' },
  iconRow: { marginBottom: 12 },
  icon: {
    width:          36,
    height:         36,
    borderRadius:   'var(--radius-md)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
  },
  value: {
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    marginBottom:  4,
    letterSpacing: '-0.02em',
  },
  label: {
    fontSize:      11,
    color:         'var(--text-muted)',
    fontFamily:    'var(--font-mono)',
    letterSpacing: '0.06em',
    textTransform: 'uppercase',
  },
}

function MarginRow({ label, value, color }) {
  return (
    <div style={{
      display:        'flex',
      justifyContent: 'space-between',
      alignItems:     'center',
      padding:        '8px 0',
      borderBottom:   '1px solid var(--border-primary)',
    }}>
      <span style={{ fontSize: 12, fontFamily: 'var(--font-mono)',
                     color: 'var(--text-muted)' }}>{label}</span>
      <span style={{ fontSize: 14, fontFamily: 'var(--font-mono)',
                     fontWeight: 700, color }}>{value}</span>
    </div>
  )
}

function EmptyState({ message }) {
  return (
    <div style={{
      padding:    '40px',
      textAlign:  'center',
      color:      'var(--text-muted)',
      fontFamily: 'var(--font-mono)',
      fontSize:   13,
    }}>
      {message}
    </div>
  )
}

function PortfolioSkeleton() {
  return (
    <div style={{ padding: 24, marginLeft: 240 }}>
      <div style={{ height: 40, width: 200, marginBottom: 24 }}
           className="skeleton" />
      <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        {[1,2,3,4].map(i => (
          <div key={i} style={{ flex: 1, height: 100, borderRadius: 12 }}
               className="skeleton" />
        ))}
      </div>
      <div style={{ height: 300, borderRadius: 12 }}
           className="skeleton" />
    </div>
  )
}

// ── Styles ────────────────────────────────────────────────────────────────────

const s = {
  root:    { minHeight: '100vh', background: 'var(--bg-primary)' },
  page:    { marginLeft: 240, padding: '32px 28px', maxWidth: 1400 },
  header: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'flex-start',
    marginBottom:   24,
  },
  title:    { fontSize: 28, letterSpacing: '-0.02em', marginBottom: 4 },
  subtitle: {
    fontSize:   13,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-secondary)',
  },
  marginCallBanner: {
    display:      'flex',
    alignItems:   'flex-start',
    gap:          12,
    background:   'var(--loss-dim)',
    border:       '1px solid var(--loss)',
    borderRadius: 'var(--radius-lg)',
    padding:      '16px 20px',
    color:        'var(--loss)',
    marginBottom: 24,
    fontFamily:   'var(--font-mono)',
    fontSize:     14,
  },
  statsRow: {
    display:      'flex',
    gap:          16,
    marginBottom: 24,
    flexWrap:     'wrap',
  },
  mainRow: {
    display:      'flex',
    gap:          16,
    marginBottom: 24,
    flexWrap:     'wrap',
  },
  chartCard: {
    flex:     '2 1 300px',
    padding:  '24px',
    position: 'relative',
  },
  cardTitle: {
    fontFamily:    'var(--font-display)',
    fontSize:      15,
    fontWeight:    700,
    letterSpacing: '-0.01em',
    marginBottom:  16,
  },
  pieCenter: {
    position:      'absolute',
    top:           '50%',
    left:          '35%',
    transform:     'translate(-50%, -10%)',
    textAlign:     'center',
    pointerEvents: 'none',
  },
  pieCenterValue: {
    display:       'block',
    fontFamily:    'var(--font-mono)',
    fontSize:      14,
    fontWeight:    700,
    color:         'var(--text-primary)',
  },
  pieCenterLabel: {
    display:       'block',
    fontFamily:    'var(--font-mono)',
    fontSize:      10,
    color:         'var(--text-muted)',
    letterSpacing: '0.06em',
  },
  marginCard:  { flex: '1 1 220px', padding: '24px' },
  marginRows:  { display: 'flex', flexDirection: 'column', marginBottom: 16 },
  marginBarTrack: {
    height:       6,
    background:   'var(--bg-input)',
    borderRadius: 3,
    overflow:     'hidden',
    marginBottom: 6,
  },
  marginBarFill: {
    height:       '100%',
    borderRadius: 3,
    transition:   'var(--transition-slow)',
  },
  marginBarLabel: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    textAlign:  'right',
  },
  tabRow: {
    display:       'flex',
    gap:           4,
    marginBottom:  16,
    borderBottom:  '1px solid var(--border-primary)',
    paddingBottom: 0,
  },
  tab: {
    display:       'flex',
    alignItems:    'center',
    gap:           6,
    padding:       '10px 18px',
    background:    'none',
    border:        'none',
    borderBottom:  '2px solid transparent',
    cursor:        'pointer',
    fontFamily:    'var(--font-ui)',
    fontSize:      13,
    fontWeight:    700,
    color:         'var(--text-muted)',
    letterSpacing: '0.05em',
    textTransform: 'uppercase',
    transition:    'var(--transition)',
    marginBottom:  '-1px',
  },
  tabActive: {
    color:             'var(--accent-cyan)',
    borderBottomColor: 'var(--accent-cyan)',
  },
  tabBadge: {
    background:     'var(--accent-amber)',
    color:          'var(--bg-primary)',
    borderRadius:   '50%',
    width:          18,
    height:         18,
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    fontSize:       10,
    fontWeight:     700,
  },
  tableWrapper: { marginBottom: 40 },

  // ── Modal styles ──
  modalOverlay: {
    position:       'fixed',
    inset:          0,
    background:     'rgba(0,0,0,0.6)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    zIndex:         1000,
  },
  modalCard: {
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-lg)',
    padding:      '28px',
    width:        '100%',
    maxWidth:     '400px',
  },
  fieldLabel: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    letterSpacing: '0.08em',
    fontWeight:    700,
  },
  errorBox: {
    marginTop:    10,
    padding:      '8px 12px',
    background:   'var(--loss-dim)',
    border:       '1px solid var(--loss)',
    borderRadius: 'var(--radius-md)',
    color:        'var(--loss)',
    fontSize:     12,
    fontFamily:   'var(--font-mono)',
  },
  successBox: {
    marginTop:    10,
    padding:      '8px 12px',
    background:   'var(--gain-dim)',
    border:       '1px solid var(--gain)',
    borderRadius: 'var(--radius-md)',
    color:        'var(--gain)',
    fontSize:     12,
    fontFamily:   'var(--font-mono)',
  },
}