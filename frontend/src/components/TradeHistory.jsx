import React, { useState, useEffect } from 'react'
import api from '../api/axios'
import { TrendingUp, TrendingDown, RefreshCw } from 'lucide-react'

export default function TradeHistory() {
  const [trades,     setTrades]     = useState([])
  const [loading,    setLoading]    = useState(true)
  const [filter,     setFilter]     = useState('ALL')
  // ALL | BUY | SELL
  const [refreshing, setRefreshing] = useState(false)

  useEffect(() => { fetchTrades() }, [])

  const fetchTrades = async () => {
    try {
      const res = await api.get('/trades/my')
      setTrades(res.data)
    } catch (err) {
      console.error('Trade history error', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchTrades()
    setTimeout(() => setRefreshing(false), 600)
  }

  const filtered = filter === 'ALL'
    ? trades
    : trades.filter(t => t.side === filter)

  const totalBuyValue = trades
    .filter(t => t.side === 'BUY')
    .reduce((sum, t) => sum + parseFloat(t.totalTradeValue ?? 0), 0)

  const totalSellValue = trades
    .filter(t => t.side === 'SELL')
    .reduce((sum, t) => sum + parseFloat(t.totalTradeValue ?? 0), 0)

  if (loading) return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {[1,2,3].map(i => (
        <div key={i} style={{ height: 56, borderRadius: 8 }}
             className="skeleton" />
      ))}
    </div>
  )

  return (
    <div>

      {/* ── Summary strip ── */}
      <div style={s.summaryRow}>
        <div style={s.summaryCard}>
          <TrendingUp size={14} color="var(--gain)" />
          <div>
            <div style={s.summaryLabel}>Total Bought</div>
            <div style={{ ...s.summaryValue,
                           color: 'var(--gain)' }}>
              ₹{totalBuyValue.toLocaleString('en-IN', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
            </div>
          </div>
        </div>
        <div style={s.summaryCard}>
          <TrendingDown size={14} color="var(--loss)" />
          <div>
            <div style={s.summaryLabel}>Total Sold</div>
            <div style={{ ...s.summaryValue,
                           color: 'var(--loss)' }}>
              ₹{totalSellValue.toLocaleString('en-IN', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
            </div>
          </div>
        </div>
        <div style={s.summaryCard}>
          <TrendingUp size={14} color="var(--accent-cyan)" />
          <div>
            <div style={s.summaryLabel}>Total Trades</div>
            <div style={{ ...s.summaryValue,
                           color: 'var(--accent-cyan)' }}>
              {trades.length}
            </div>
          </div>
        </div>
      </div>

      {/* ── Filter + refresh ── */}
      <div style={s.filterRow}>
        <div style={s.filterBtns}>
          {['ALL', 'BUY', 'SELL'].map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{
                ...s.filterBtn,
                ...(filter === f ? {
                  background: f === 'BUY'   ? 'var(--gain-dim)'
                             : f === 'SELL' ? 'var(--loss-dim)'
                             :                'var(--accent-cyan-dim)',
                  color:      f === 'BUY'   ? 'var(--gain)'
                             : f === 'SELL' ? 'var(--loss)'
                             :                'var(--accent-cyan)',
                  borderColor:f === 'BUY'   ? 'var(--gain)'
                             : f === 'SELL' ? 'var(--loss)'
                             :                'var(--accent-cyan)',
                } : {}),
              }}
            >
              {f}
            </button>
          ))}
        </div>
        <button
          onClick={handleRefresh}
          className="btn btn-ghost"
          style={{ padding: '6px 12px', fontSize: 12 }}
        >
          <RefreshCw size={12} style={{
            animation: refreshing
              ? 'spin 0.6s linear infinite' : 'none',
          }} />
        </button>
      </div>

      {/* ── Trade list ── */}
      {filtered.length === 0
        ? (
          <div style={s.empty}>
            No {filter === 'ALL' ? '' : filter} trades yet
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>STOCK</th>
                  <th>SIDE</th>
                  <th>QTY</th>
                  <th>PRICE</th>
                  <th>TOTAL</th>
                  <th>COUNTERPARTY</th>
                  <th>TIME</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(t => (
                  <tr key={t.id}>
                    <td>
                      <div style={{ fontWeight: 700,
                                    color: 'var(--text-primary)',
                                    letterSpacing: '0.04em' }}>
                        {t.ticker}
                      </div>
                      <div style={{ fontSize: 11,
                                    color: 'var(--text-muted)' }}>
                        {t.companyName}
                      </div>
                    </td>
                    <td>
                      <span style={{
                        display:    'inline-flex',
                        alignItems: 'center',
                        gap:        4,
                        fontWeight: 700,
                        color: t.side === 'BUY'
                          ? 'var(--gain)' : 'var(--loss)',
                      }}>
                        {t.side === 'BUY'
                          ? <TrendingUp  size={12} />
                          : <TrendingDown size={12} />
                        }
                        {t.side}
                      </span>
                    </td>
                    <td style={{ fontWeight: 600 }}>
                      {t.quantity}
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)',
                                 fontWeight: 600 }}>
                      ₹{parseFloat(t.executedPrice)
                          .toLocaleString('en-IN', {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                    </td>
                    <td style={{
                      fontFamily: 'var(--font-mono)',
                      fontWeight: 700,
                      color: t.side === 'BUY'
                        ? 'var(--loss)' : 'var(--gain)',
                    }}>
                      {t.side === 'BUY' ? '−' : '+'}
                      ₹{parseFloat(t.totalTradeValue)
                          .toLocaleString('en-IN', {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                    </td>
                    <td style={{ color: 'var(--text-muted)',
                                 fontSize: 12 }}>
                      {t.side === 'BUY'
                        ? t.sellerUsername : t.buyerUsername}
                    </td>
                    <td style={{ color: 'var(--text-muted)',
                                 fontSize: 12 }}>
                      {new Date(t.executedAt)
                        .toLocaleString('en-IN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      }
    </div>
  )
}

const s = {
  summaryRow: {
    display:      'flex',
    gap:          12,
    marginBottom: 16,
    flexWrap:     'wrap',
  },
  summaryCard: {
    display:      'flex',
    alignItems:   'center',
    gap:          10,
    flex:         '1 1 140px',
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-md)',
    padding:      '12px 16px',
  },
  summaryLabel: {
    fontSize:   10,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    marginBottom: 2,
  },
  summaryValue: {
    fontSize:   14,
    fontFamily: 'var(--font-mono)',
    fontWeight: 700,
  },
  filterRow: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    marginBottom:   12,
  },
  filterBtns: {
    display: 'flex',
    gap:     6,
  },
  filterBtn: {
    padding:      '6px 14px',
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-md)',
    fontFamily:   'var(--font-ui)',
    fontSize:     11,
    fontWeight:   700,
    letterSpacing:'0.08em',
    cursor:       'pointer',
    color:        'var(--text-muted)',
    transition:   'var(--transition)',
  },
  empty: {
    padding:    '32px',
    textAlign:  'center',
    color:      'var(--text-muted)',
    fontFamily: 'var(--font-mono)',
    fontSize:   13,
    background: 'var(--bg-card)',
    border:     '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-lg)',
  },
}