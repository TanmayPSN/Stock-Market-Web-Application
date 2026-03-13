import React, { useEffect, useRef, useState } from 'react'
import { TrendingUp, TrendingDown, ShoppingCart, ArrowUpDown } from 'lucide-react'

export default function StockCard({
  stock, flash, onBuy, onSell, isMarketOpen
}) {
  const prevPriceRef = useRef(stock.currentPrice)
  const [animClass,  setAnimClass]  = useState('')
  const [displayed,  setDisplayed]  = useState(stock.currentPrice)

  // Trigger flash animation when price changes
  useEffect(() => {
    if (stock.currentPrice !== prevPriceRef.current) {
      const dir = stock.currentPrice > prevPriceRef.current
        ? 'flash-green' : 'flash-red'
      setAnimClass(dir)
      setDisplayed(stock.currentPrice)
      prevPriceRef.current = stock.currentPrice
      setTimeout(() => setAnimClass(''), 600)
    }
  }, [stock.currentPrice])

  const isGain   = stock.priceChangeAmount >= 0
  const changeColor = isGain ? 'var(--gain)' : 'var(--loss)'
  const changePct   = parseFloat(stock.priceChangePercent ?? 0)
    .toFixed(2)

  return (
    <div
      style={{
        ...s.card,
        ...(flash === 'green'
          ? { borderColor: 'var(--gain)',
              boxShadow: '0 0 16px var(--gain-dim)' }
          : flash === 'red'
          ? { borderColor: 'var(--loss)',
              boxShadow: '0 0 16px var(--loss-dim)' }
          : {}),
      }}
      className={`card animate-fadeInUp ${animClass}`}
    >

      {/* ── Header ── */}
      <div style={s.header}>
        <div>
          <div style={s.ticker}>{stock.ticker}</div>
          <div style={s.company}>{stock.companyName}</div>
        </div>
        <div style={{
          ...s.directionBadge,
          background:  isGain ? 'var(--gain-dim)' : 'var(--loss-dim)',
          color:       changeColor,
          border:      `1px solid ${changeColor}`,
        }}>
          {isGain
            ? <TrendingUp  size={12} />
            : <TrendingDown size={12} />
          }
          {isGain ? '+' : ''}{changePct}%
        </div>
      </div>

      {/* ── Price ── */}
      <div style={{ ...s.price, color: changeColor }}
           className={animClass}>
        ₹{parseFloat(displayed).toLocaleString('en-IN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}
      </div>

      <div style={s.changeRow}>
        <span style={{ color: changeColor, fontSize: 12,
                        fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
          {isGain ? '+' : ''}
          ₹{parseFloat(stock.priceChangeAmount ?? 0)
              .toLocaleString('en-IN', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
        </span>
        <span style={s.vsClose}>vs prev close</span>
      </div>

      {/* ── OHLC strip ── */}
      <div style={s.ohlcRow}>
        <OhlcItem label="O" value={stock.openPrice} />
        <OhlcItem label="H" value={stock.highPrice}
                  color="var(--gain)" />
        <OhlcItem label="L" value={stock.lowPrice}
                  color="var(--loss)" />
        <OhlcItem label="P" value={stock.previousClosePrice} />
      </div>

      {/* ── Availability ── */}
      <div style={s.availRow}>
        <span style={s.availLabel}>Available</span>
        <span style={s.availValue}>
          {stock.totalSharesAvailable?.toLocaleString('en-IN')} shares
        </span>
      </div>

      {/* ── Action buttons ── */}
      {!stock.isActive
        ? (
          <div style={s.delistedBanner}>
            Delisted — trading suspended
          </div>
        ) : !isMarketOpen
        ? (
          <div style={s.closedBanner}>
            <ArrowUpDown size={12} />
            Market closed
          </div>
        ) : (
          <div style={s.btnRow}>
            <button
              onClick={onBuy}
              className="btn btn-buy"
              style={s.actionBtn}
            >
              <ShoppingCart size={13} />
              BUY
            </button>
            <button
              onClick={onSell}
              className="btn btn-sell"
              style={s.actionBtn}
            >
              <TrendingDown size={13} />
              SELL
            </button>
          </div>
        )
      }

    </div>
  )
}

function OhlcItem({ label, value, color }) {
  return (
    <div style={{
      display:       'flex',
      flexDirection: 'column',
      alignItems:    'center',
      gap:           2,
      flex:          1,
    }}>
      <span style={{
        fontSize:      9,
        fontFamily:    'var(--font-mono)',
        color:         'var(--text-muted)',
        letterSpacing: '0.1em',
        textTransform: 'uppercase',
      }}>
        {label}
      </span>
      <span style={{
        fontSize:   11,
        fontFamily: 'var(--font-mono)',
        fontWeight: 600,
        color:      color ?? 'var(--text-secondary)',
      }}>
        ₹{parseFloat(value ?? 0).toLocaleString('en-IN', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
          })}
      </span>
    </div>
  )
}

const s = {
  card: {
    padding:    '20px',
    transition: 'var(--transition)',
    cursor:     'default',
  },
  header: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'flex-start',
    marginBottom:   12,
  },
  ticker: {
    fontSize:      16,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    color:         'var(--text-primary)',
    letterSpacing: '0.06em',
  },
  company: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    marginTop:  2,
  },
  directionBadge: {
    display:       'inline-flex',
    alignItems:    'center',
    gap:           4,
    padding:       '3px 8px',
    borderRadius:  20,
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
  },
  price: {
    fontSize:      26,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    letterSpacing: '-0.02em',
    marginBottom:  4,
    transition:    'color 0.3s ease',
  },
  changeRow: {
    display:      'flex',
    alignItems:   'center',
    gap:          8,
    marginBottom: 16,
  },
  vsClose: {
    fontSize:   10,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
  },
  ohlcRow: {
    display:      'flex',
    background:   'var(--bg-input)',
    borderRadius: 'var(--radius-sm)',
    padding:      '8px 4px',
    marginBottom: 12,
    gap:          4,
  },
  availRow: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    marginBottom:   16,
  },
  availLabel: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
  },
  availValue: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-secondary)',
    fontWeight: 600,
  },
  btnRow: {
    display: 'flex',
    gap:     8,
  },
  actionBtn: {
    flex:           1,
    justifyContent: 'center',
    padding:        '9px',
    fontSize:       12,
  },
  delistedBanner: {
    textAlign:    'center',
    padding:      '8px',
    borderRadius: 'var(--radius-sm)',
    background:   'var(--bg-input)',
    color:        'var(--text-muted)',
    fontSize:     11,
    fontFamily:   'var(--font-mono)',
    letterSpacing:'0.04em',
  },
  closedBanner: {
    display:       'flex',
    alignItems:    'center',
    justifyContent:'center',
    gap:           6,
    padding:       '8px',
    borderRadius:  'var(--radius-sm)',
    background:    'var(--bg-input)',
    color:         'var(--text-muted)',
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    letterSpacing: '0.04em',
  },
}