import React, { useState } from 'react'
import api from '../api/axios'
import { CheckCircle, AlertTriangle } from 'lucide-react'

export default function OrderForm({
  stock, initialSide, onSuccess, onCancel
}) {
  const [form, setForm] = useState({
    ticker:     stock?.ticker ?? '',
    side:       initialSide  ?? 'BUY',
    type:       'MARKET',
    quantity:   '',
    limitPrice: '',
    useMargin:  false,
  })

  const [loading,  setLoading]  = useState(false)
  const [result,   setResult]   = useState(null)
  // result = { success: bool, message: string }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm(p => ({
      ...p,
      [name]: type === 'checkbox' ? checked : value,
    }))
    setResult(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Frontend validation
    if (!form.quantity || parseInt(form.quantity) < 1) {
      setResult({ success: false,
                  message: 'Quantity must be at least 1' })
      return
    }
    if (form.type === 'LIMIT' && !form.limitPrice) {
      setResult({ success: false,
                  message: 'Limit price is required for LIMIT orders' })
      return
    }

    setLoading(true)
    setResult(null)

    try {
      const payload = {
        ticker:     form.ticker,
        side:       form.side,
        type:       form.type,
        quantity:   parseInt(form.quantity),
        limitPrice: form.type === 'LIMIT'
          ? parseFloat(form.limitPrice) : null,
        useMargin:  form.side === 'BUY' ? form.useMargin : false,
      }

      const res = await api.post('/orders/place', payload)
      const data = res.data

      if (data.status === 'EXECUTED' || data.status === 'PENDING') {
        setResult({
          success: true,
          message: data.status === 'EXECUTED'
            ? `Order executed at ₹${parseFloat(
                data.executedPrice).toLocaleString('en-IN')}`
            : 'Limit order placed — waiting for price condition',
        })
        setTimeout(() => onSuccess?.(), 1800)
      } else {
        setResult({
          success: false,
          message: data.rejectionReason ?? 'Order rejected',
        })
      }
    } catch (err) {
      setResult({
        success: false,
        message: err.response?.data?.message
               ?? 'Failed to place order',
      })
    } finally {
      setLoading(false)
    }
  }

  const estimatedCost = form.quantity && stock?.currentPrice
    ? (parseInt(form.quantity) *
       parseFloat(form.type === 'LIMIT' && form.limitPrice
         ? form.limitPrice : stock.currentPrice))
       .toLocaleString('en-IN', {
         minimumFractionDigits: 2,
         maximumFractionDigits: 2,
       })
    : null

  return (
    <div style={s.wrapper}>

      {/* ── Stock info strip ── */}
      <div style={s.stockStrip}>
        <div>
          <div style={s.stripTicker}>{stock?.ticker}</div>
          <div style={s.stripCompany}>{stock?.companyName}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{
            ...s.stripPrice,
            color: stock?.priceChangeAmount >= 0
              ? 'var(--gain)' : 'var(--loss)',
          }}>
            ₹{parseFloat(stock?.currentPrice ?? 0)
                .toLocaleString('en-IN', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
          </div>
          <div style={{
            fontSize: 11,
            fontFamily: 'var(--font-mono)',
            color: stock?.priceChangeAmount >= 0
              ? 'var(--gain)' : 'var(--loss)',
          }}>
            {stock?.priceChangeAmount >= 0 ? '▲' : '▼'}{' '}
            {parseFloat(stock?.priceChangePercent ?? 0)
              .toFixed(2)}%
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit} style={s.form}>

        {/* ── BUY / SELL toggle ── */}
        <div style={s.sideToggle}>
          {['BUY', 'SELL'].map(side => (
            <button
              key={side}
              type="button"
              onClick={() => setForm(p => ({ ...p, side }))}
              style={{
                ...s.sideBtn,
                ...(form.side === side
                  ? side === 'BUY'
                    ? s.sideBtnBuyActive
                    : s.sideBtnSellActive
                  : {}),
              }}
            >
              {side}
            </button>
          ))}
        </div>

        {/* ── Order type ── */}
        <div style={s.fieldGroup}>
          <label style={s.label}>ORDER TYPE</label>
          <select
            name="type"
            value={form.type}
            onChange={handleChange}
            className="input"
            style={s.select}
          >
            <option value="MARKET">Market Order</option>
            <option value="LIMIT">Limit Order</option>
          </select>
        </div>

        {/* ── Quantity ── */}
        <div style={s.fieldGroup}>
          <label style={s.label}>QUANTITY</label>
          <input
            name="quantity"
            type="number"
            min="1"
            value={form.quantity}
            onChange={handleChange}
            placeholder="No. of shares"
            required
            className="input"
          />
        </div>

        {/* ── Limit price (only if LIMIT) ── */}
        {form.type === 'LIMIT' && (
          <div style={s.fieldGroup} className="animate-fadeIn">
            <label style={s.label}>LIMIT PRICE (₹)</label>
            <input
              name="limitPrice"
              type="number"
              min="0.01"
              step="0.01"
              value={form.limitPrice}
              onChange={handleChange}
              placeholder="Target price"
              required
              className="input"
            />
            <span style={s.hint}>
              {form.side === 'BUY'
                ? '↓ Order executes when price drops to this value'
                : '↑ Order executes when price rises to this value'
              }
            </span>
          </div>
        )}

        {/* ── Margin toggle — only show for BUY ── */}
        {form.side === 'BUY' && (
          <label style={s.marginToggle}>
            <div style={s.marginToggleLeft}>
              <span style={s.marginToggleLabel}>Use Margin (5x leverage)</span>
              <span style={s.marginToggleHint}>
                Trade beyond your available balance
              </span>
            </div>
            <div
              onClick={() =>
                setForm(p => ({ ...p, useMargin: !p.useMargin }))}
              style={{
                ...s.toggle,
                background: form.useMargin
                  ? 'var(--accent-cyan)' : 'var(--bg-input)',
              }}
            >
              <div style={{
                ...s.toggleThumb,
                transform: form.useMargin
                  ? 'translateX(20px)' : 'translateX(2px)',
              }} />
            </div>
          </label>
        )}

        {/* ── Estimated cost ── */}
        {estimatedCost && (
          <div style={s.estimateBox} className="animate-fadeIn">
            <span style={s.estimateLabel}>
              Estimated {form.side === 'BUY' ? 'Cost' : 'Proceeds'}
            </span>
            <span style={{
              ...s.estimateValue,
              color: form.side === 'BUY'
                ? 'var(--loss)' : 'var(--gain)',
            }}>
              ₹{estimatedCost}
            </span>
          </div>
        )}

        {/* ── Result message ── */}
        {result && (
          <div style={{
            ...s.resultBox,
            background: result.success
              ? 'var(--gain-dim)' : 'var(--loss-dim)',
            borderColor: result.success
              ? 'var(--gain)' : 'var(--loss)',
            color: result.success
              ? 'var(--gain)' : 'var(--loss)',
          }} className="animate-fadeIn">
            {result.success
              ? <CheckCircle  size={14} />
              : <AlertTriangle size={14} />
            }
            {result.message}
          </div>
        )}

        {/* ── Actions ── */}
        <div style={s.actions}>
          <button
            type="button"
            onClick={onCancel}
            className="btn btn-ghost"
            style={{ flex: 1, justifyContent: 'center' }}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading || result?.success}
            className={`btn ${form.side === 'BUY'
              ? 'btn-buy' : 'btn-sell'}`}
            style={{ flex: 2, justifyContent: 'center' }}
          >
            {loading
              ? <span style={s.spinner} />
              : `${form.side} ${form.quantity || ''} ${
                  stock?.ticker ?? ''}`
            }
          </button>
        </div>

      </form>
    </div>
  )
}

const s = {
  wrapper: {
    padding: '24px',
  },
  stockStrip: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    background:     'var(--bg-input)',
    borderRadius:   'var(--radius-md)',
    padding:        '12px 16px',
    marginBottom:   20,
    border:         '1px solid var(--border-primary)',
  },
  stripTicker: {
    fontSize:      16,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    color:         'var(--text-primary)',
    letterSpacing: '0.06em',
  },
  stripCompany: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    marginTop:  2,
  },
  stripPrice: {
    fontSize:      18,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    letterSpacing: '-0.01em',
  },
  form: {
    display:       'flex',
    flexDirection: 'column',
    gap:           16,
  },
  sideToggle: {
    display:      'grid',
    gridTemplateColumns: '1fr 1fr',
    gap:          4,
    background:   'var(--bg-input)',
    borderRadius: 'var(--radius-md)',
    padding:      4,
  },
  sideBtn: {
    padding:       '10px',
    border:        'none',
    borderRadius:  'var(--radius-sm)',
    background:    'transparent',
    fontFamily:    'var(--font-ui)',
    fontSize:      13,
    fontWeight:    700,
    letterSpacing: '0.08em',
    cursor:        'pointer',
    color:         'var(--text-muted)',
    transition:    'var(--transition)',
  },
  sideBtnBuyActive: {
    background: 'var(--gain)',
    color:      '#000',
    boxShadow:  '0 2px 8px var(--gain-dim)',
  },
  sideBtnSellActive: {
    background: 'var(--loss)',
    color:      '#fff',
    boxShadow:  '0 2px 8px var(--loss-dim)',
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
  select: {
    width: '100%',
  },
  hint: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    marginTop:  2,
  },
  marginToggle: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    background:     'var(--bg-input)',
    border:         '1px solid var(--border-primary)',
    borderRadius:   'var(--radius-md)',
    padding:        '12px 16px',
    cursor:         'pointer',
    gap:            12,
  },
  marginToggleLeft: {
    display:       'flex',
    flexDirection: 'column',
    gap:           2,
  },
  marginToggleLabel: {
    fontSize:   13,
    fontFamily: 'var(--font-ui)',
    fontWeight: 700,
    color:      'var(--text-primary)',
  },
  marginToggleHint: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
  },
  toggle: {
    width:        44,
    height:       24,
    borderRadius: 12,
    position:     'relative',
    cursor:       'pointer',
    transition:   'var(--transition)',
    flexShrink:   0,
    border:       '1px solid var(--border-primary)',
  },
  toggleThumb: {
    position:     'absolute',
    top:          2,
    width:        18,
    height:       18,
    borderRadius: '50%',
    background:   '#fff',
    transition:   'transform 0.2s cubic-bezier(0.4,0,0.2,1)',
    boxShadow:    '0 1px 4px rgba(0,0,0,0.3)',
  },
  estimateBox: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    background:     'var(--bg-input)',
    border:         '1px solid var(--border-primary)',
    borderRadius:   'var(--radius-md)',
    padding:        '10px 16px',
  },
  estimateLabel: {
    fontSize:   12,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
  },
  estimateValue: {
    fontSize:   15,
    fontFamily: 'var(--font-mono)',
    fontWeight: 700,
  },
  resultBox: {
    display:      'flex',
    alignItems:   'center',
    gap:          8,
    padding:      '10px 14px',
    borderRadius: 'var(--radius-md)',
    border:       '1px solid',
    fontSize:     13,
    fontFamily:   'var(--font-mono)',
  },
  actions: {
    display: 'flex',
    gap:     10,
  },
  spinner: {
    width:        16,
    height:       16,
    border:       '2px solid rgba(0,0,0,0.2)',
    borderTop:    '2px solid currentColor',
    borderRadius: '50%',
    display:      'inline-block',
    animation:    'spin 0.8s linear infinite',
  },
}