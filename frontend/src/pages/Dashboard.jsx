import React, { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import api from '../api/axios'
import Navbar from '../components/Navbar'
import StockCard from '../components/StockCard'
import OrderForm from '../components/OrderForm'
import MarketStatus from '../components/MarketStatus'
import {
  TrendingUp, TrendingDown, Wallet,
  BarChart2, RefreshCw, X
} from 'lucide-react'

export default function Dashboard() {
  const { user }                        = useAuth()
  const { prices, connected, flashMap } = useWebSocket()

  const [stocks,        setStocks]        = useState([])
  const [portfolio,     setPortfolio]     = useState(null)
  const [marketStatus,  setMarketStatus]  = useState(null)
  const [loading,       setLoading]       = useState(true)
  const [selectedStock, setSelectedStock] = useState(null)
  const [showOrderForm, setShowOrderForm] = useState(false)
  const [orderSide,     setOrderSide]     = useState('BUY')
  const [refreshing,    setRefreshing]    = useState(false)

  useEffect(() => { fetchAll() }, [])

  const fetchAll = async () => {
    try {
      const [stocksRes, portfolioRes, marketRes] = await Promise.all([
        api.get('/stocks/all'),
        api.get('/portfolio/me'),
        api.get('/market/status'),
      ])
      setStocks(stocksRes.data)
      setPortfolio(portfolioRes.data)
      setMarketStatus(marketRes.data)
    } catch (err) {
      console.error('Failed to fetch dashboard data', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchAll()
    setTimeout(() => setRefreshing(false), 600)
  }

  const openOrderForm = (stock, side) => {
    setSelectedStock(stock)
    setOrderSide(side)
    setShowOrderForm(true)
  }

  const handleOrderSuccess = () => {
    setShowOrderForm(false)
    fetchAll()
  }

  // Merge live WebSocket prices into stock list
  const enrichedStocks = stocks.map(s => ({
    ...s,
    currentPrice: prices[s.ticker] ?? s.currentPrice,
  }))

  const gainers = enrichedStocks
    .filter(s => s.priceChangeAmount > 0)
    .sort((a, b) => b.priceChangePercent - a.priceChangePercent)
    .slice(0, 3)

  const losers = enrichedStocks
    .filter(s => s.priceChangeAmount < 0)
    .sort((a, b) => a.priceChangePercent - b.priceChangePercent)
    .slice(0, 3)

  if (loading) return <DashboardSkeleton />

  return (
    <div style={s.root}>
      <Navbar />

      <div style={s.page}>

        {/* ── Header ── */}
        <div style={s.header}>
          <div>
            <h1 style={s.title}>
              Good {getGreeting()},{' '}
              <span style={{ color: 'var(--accent-cyan)' }}>
                {user?.username}
              </span>
            </h1>
            <p style={s.subtitle}>
              Here's what's happening in the market today
            </p>
          </div>
          <div style={s.headerRight}>
            <MarketStatus status={marketStatus} />
            <button
              onClick={handleRefresh}
              className="btn btn-ghost"
              style={{ gap: 6 }}
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
        </div>

        {/* ── WebSocket status ── */}
        <div style={s.wsStatus}>
          <div style={{
            ...s.wsDot,
            background: connected
              ? 'var(--gain)' : 'var(--loss)',
            boxShadow: connected
              ? '0 0 6px var(--gain)' : 'none',
          }} />
          <span style={s.wsText}>
            {connected ? 'Live prices connected' : 'Connecting to live feed...'}
          </span>
        </div>

        {/* ── Portfolio summary strip ── */}
        {portfolio && (
          <div style={s.summaryStrip} className="stagger">
            <SummaryCard
              label="Available Balance"
              value={`₹${portfolio.availableBalance?.toLocaleString('en-IN')}`}
              icon={<Wallet size={18} />}
              color="var(--accent-cyan)"
            />
            <SummaryCard
              label="Portfolio Value"
              value={`₹${portfolio.currentMarketValue?.toLocaleString('en-IN')}`}
              icon={<BarChart2 size={18} />}
              color="var(--accent-amber)"
            />
            <SummaryCard
              label="Total P&L"
              value={`₹${portfolio.totalProfitLoss?.toLocaleString('en-IN')}`}
              icon={portfolio.totalProfitLoss >= 0
                ? <TrendingUp size={18} />
                : <TrendingDown size={18} />
              }
              color={portfolio.totalProfitLoss >= 0
                ? 'var(--gain)' : 'var(--loss)'}
              sub={`${portfolio.totalProfitLossPercentage?.toFixed(2)}%`}
            />
            <SummaryCard
              label="Margin Available"
              value={`₹${portfolio.marginAvailable?.toLocaleString('en-IN')}`}
              icon={<TrendingUp size={18} />}
              color="var(--accent-cyan)"
              warn={portfolio.marginCallTriggered}
            />
          </div>
        )}

        {/* ── Margin call warning ── */}
        {portfolio?.marginCallTriggered && (
          <div style={s.marginCallBanner} className="pulse-red animate-fadeIn">
            <span style={{ fontSize: 18 }}>⚠</span>
            <div>
              <strong>Margin Call Triggered</strong>
              <p style={{ fontSize: 12, marginTop: 2, opacity: 0.8 }}>
                Your margin usage has exceeded 80%.
                Please add funds or close positions immediately.
              </p>
            </div>
          </div>
        )}

        {/* ── Gainers / Losers ── */}
        <div style={s.glRow}>
          <MiniList title="Top Gainers" items={gainers} dir="up" />
          <MiniList title="Top Losers"  items={losers}  dir="down" />
        </div>

        {/* ── All Stocks ── */}
        <div style={s.sectionHeader}>
          <h2 style={s.sectionTitle}>Live Market</h2>
          <span style={s.stockCount}>
            {enrichedStocks.length} stocks
          </span>
        </div>

        <div style={s.stockGrid} className="stagger">
          {enrichedStocks.map(stock => (
            <StockCard
              key={stock.ticker}
              stock={stock}
              flash={flashMap[stock.ticker]}
              onBuy={() => openOrderForm(stock, 'BUY')}
              onSell={() => openOrderForm(stock, 'SELL')}
              isMarketOpen={marketStatus?.isOpen}
            />
          ))}
        </div>

      </div>

      {/* ── Order Form Modal ── */}
      {showOrderForm && (
        <div style={s.modalOverlay}
             onClick={e => e.target === e.currentTarget
               && setShowOrderForm(false)}>
          <div style={s.modalCard} className="animate-fadeInUp">
            <div style={s.modalHeader}>
              <h3 style={s.modalTitle}>
                {orderSide === 'BUY' ? '📈 Place Buy Order' : '📉 Place Sell Order'}
              </h3>
              <button
                onClick={() => setShowOrderForm(false)}
                style={s.closeBtn}
              >
                <X size={18} />
              </button>
            </div>
            <OrderForm
              stock={selectedStock}
              initialSide={orderSide}
              onSuccess={handleOrderSuccess}
              onCancel={() => setShowOrderForm(false)}
            />
          </div>
        </div>
      )}

    </div>
  )
}

// ── Summary Card ───────────────────────────────────────────────
function SummaryCard({ label, value, icon, color, sub, warn }) {
  return (
    <div style={{
      ...sc.card,
      borderColor: warn ? 'var(--loss)' : 'var(--border-primary)',
      boxShadow: warn
        ? '0 0 16px var(--loss-dim)' : 'var(--shadow-card)',
    }}
    className="card animate-fadeInUp">
      <div style={sc.iconRow}>
        <div style={{ ...sc.icon, color, background: `${color}22` }}>
          {icon}
        </div>
        {warn && (
          <span style={{ color: 'var(--loss)', fontSize: 12 }}
                className="pulse-red">
            ⚠ MARGIN CALL
          </span>
        )}
      </div>
      <div style={{ ...sc.value, color }}>{value}</div>
      {sub && (
        <div style={{
          ...sc.sub,
          color: parseFloat(sub) >= 0 ? 'var(--gain)' : 'var(--loss)'
        }}>
          {parseFloat(sub) >= 0 ? '▲' : '▼'} {Math.abs(parseFloat(sub))}%
        </div>
      )}
      <div style={sc.label}>{label}</div>
    </div>
  )
}

const sc = {
  card: {
    padding:    '20px',
    flex:       1,
    minWidth:   160,
    transition: 'var(--transition)',
  },
  iconRow: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    marginBottom:   12,
  },
  icon: {
    width:          36,
    height:         36,
    borderRadius:   'var(--radius-md)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
  },
  value: {
    fontSize:      22,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    marginBottom:  4,
    letterSpacing: '-0.02em',
  },
  sub: {
    fontSize:   12,
    fontFamily: 'var(--font-mono)',
    fontWeight: 600,
    marginBottom: 4,
  },
  label: {
    fontSize:   11,
    color:      'var(--text-muted)',
    fontFamily: 'var(--font-mono)',
    letterSpacing: '0.06em',
    textTransform: 'uppercase',
  },
}

// ── Mini Gainers/Losers list ───────────────────────────────────
function MiniList({ title, items, dir }) {
  return (
    <div style={ml.card} className="card">
      <div style={ml.header}>
        <span style={ml.title}>{title}</span>
        <span style={{
          ...ml.dot,
          background: dir === 'up' ? 'var(--gain)' : 'var(--loss)',
          boxShadow:  dir === 'up'
            ? '0 0 6px var(--gain)' : '0 0 6px var(--loss)',
        }} />
      </div>
      {items.length === 0
        ? <p style={{ color: 'var(--text-muted)', fontSize: 12,
                       fontFamily: 'var(--font-mono)' }}>
            No data yet
          </p>
        : items.map(s => (
          <div key={s.ticker} style={ml.row}>
            <span style={ml.ticker}>{s.ticker}</span>
            <span style={ml.price}>
              ₹{parseFloat(s.currentPrice).toLocaleString('en-IN')}
            </span>
            <span style={{
              ...ml.pct,
              color: dir === 'up' ? 'var(--gain)' : 'var(--loss)',
            }}>
              {dir === 'up' ? '▲' : '▼'}{' '}
              {Math.abs(s.priceChangePercent).toFixed(2)}%
            </span>
          </div>
        ))
      }
    </div>
  )
}

const ml = {
  card: {
    flex:    1,
    padding: '20px',
  },
  header: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    marginBottom:   16,
  },
  title: {
    fontFamily:    'var(--font-display)',
    fontSize:      14,
    fontWeight:    700,
    color:         'var(--text-primary)',
    letterSpacing: '-0.01em',
  },
  dot: {
    width:        8,
    height:       8,
    borderRadius: '50%',
  },
  row: {
    display:        'flex',
    alignItems:     'center',
    padding:        '8px 0',
    borderBottom:   '1px solid var(--border-primary)',
    gap:            8,
  },
  ticker: {
    flex:          1,
    fontSize:      13,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    color:         'var(--text-primary)',
    letterSpacing: '0.04em',
  },
  price: {
    fontSize:   13,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-secondary)',
  },
  pct: {
    fontSize:      12,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    minWidth:      60,
    textAlign:     'right',
  },
}

// ── Skeleton ───────────────────────────────────────────────────
function DashboardSkeleton() {
  return (
    <div style={{ padding: 24, marginLeft: 240 }}>
      <div style={{ height: 40, width: 300, marginBottom: 24 }}
           className="skeleton" />
      <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        {[1,2,3,4].map(i => (
          <div key={i} style={{ flex: 1, height: 100, borderRadius: 12 }}
               className="skeleton" />
        ))}
      </div>
      <div style={{ display: 'grid',
                    gridTemplateColumns: 'repeat(3, 1fr)',
                    gap: 16 }}>
        {[1,2,3,4,5,6].map(i => (
          <div key={i} style={{ height: 180, borderRadius: 12 }}
               className="skeleton" />
        ))}
      </div>
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}

const s = {
  root: { minHeight: '100vh', background: 'var(--bg-primary)' },
  page: { marginLeft: 240, padding: '32px 28px', maxWidth: 1400 },
  header: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'flex-start',
    marginBottom:   8,
    flexWrap:       'wrap',
    gap:            16,
  },
  title: {
    fontSize:      28,
    letterSpacing: '-0.02em',
    marginBottom:  4,
  },
  subtitle: {
    fontSize:   13,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-secondary)',
  },
  headerRight: {
    display:    'flex',
    alignItems: 'center',
    gap:        12,
  },
  wsStatus: {
    display:     'flex',
    alignItems:  'center',
    gap:         6,
    marginBottom: 24,
  },
  wsDot: {
    width:        8,
    height:       8,
    borderRadius: '50%',
    transition:   'var(--transition)',
  },
  wsText: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    letterSpacing: '0.06em',
  },
  summaryStrip: {
    display:      'flex',
    gap:          16,
    marginBottom: 24,
    flexWrap:     'wrap',
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
  glRow: {
    display:      'flex',
    gap:          16,
    marginBottom: 24,
    flexWrap:     'wrap',
  },
  sectionHeader: {
    display:        'flex',
    alignItems:     'center',
    gap:            12,
    marginBottom:   16,
  },
  sectionTitle: {
    fontSize:      20,
    letterSpacing: '-0.02em',
  },
  stockCount: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    background:    'var(--bg-card)',
    border:        '1px solid var(--border-primary)',
    borderRadius:  20,
    padding:       '2px 10px',
    letterSpacing: '0.06em',
  },
  stockGrid: {
    display:             'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
    gap:                 16,
    paddingBottom:       40,
  },
  modalOverlay: {
    position:        'fixed',
    inset:           0,
    background:      'rgba(0,0,0,0.7)',
    backdropFilter:  'blur(4px)',
    display:         'flex',
    alignItems:      'center',
    justifyContent:  'center',
    zIndex:          1000,
    padding:         24,
  },
  modalCard: {
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-xl)',
    width:        '100%',
    maxWidth:     480,
    boxShadow:    'var(--shadow-card), var(--shadow-glow-cyan)',
    overflow:     'hidden',
  },
  modalHeader: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    padding:        '20px 24px',
    borderBottom:   '1px solid var(--border-primary)',
  },
  modalTitle: {
    fontSize:      16,
    fontFamily:    'var(--font-display)',
    letterSpacing: '-0.01em',
  },
  closeBtn: {
    background:   'var(--bg-input)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-md)',
    width:        32,
    height:       32,
    display:      'flex',
    alignItems:   'center',
    justifyContent: 'center',
    cursor:       'pointer',
    color:        'var(--text-secondary)',
    transition:   'var(--transition)',
  },
}