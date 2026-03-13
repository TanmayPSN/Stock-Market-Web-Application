import React from 'react'
import { Clock } from 'lucide-react'

export default function MarketStatus({ status }) {
  if (!status) return null

  const isOpen      = status.isOpen
  const marketState = status.status

  const config = {
    OPEN:        { color: 'var(--gain)',         label: 'MARKET OPEN',       glow: '0 0 8px var(--gain)'  },
    CLOSED:      { color: 'var(--loss)',         label: 'MARKET CLOSED',     glow: 'none'                 },
    PRE_MARKET:  { color: 'var(--accent-amber)', label: 'PRE-MARKET',        glow: '0 0 8px var(--accent-amber)' },
    POST_MARKET: { color: 'var(--accent-amber)', label: 'POST-MARKET',       glow: 'none'                 },
  }

  const cfg = config[marketState] ?? config['CLOSED']

  return (
    <div style={{
      ...s.wrapper,
      background:  `${cfg.color}11`,
      borderColor: cfg.color,
    }}>
      {/* Pulsing dot */}
      <div style={{
        ...s.dot,
        background: cfg.color,
        boxShadow:  cfg.glow,
        animation:  isOpen
          ? 'pulse-red 1.5s ease infinite' : 'none',
      }} />

      <span style={{
        ...s.label,
        color: cfg.color,
      }}>
        {cfg.label}
      </span>

      {/* Open/Close times */}
      {status.openTime && (
        <div style={s.times}>
          <Clock size={11} color="var(--text-muted)" />
          <span style={s.timeText}>
            {status.openTime} – {status.closeTime}
          </span>
        </div>
      )}
    </div>
  )
}

const s = {
  wrapper: {
    display:      'inline-flex',
    alignItems:   'center',
    gap:          8,
    padding:      '7px 14px',
    borderRadius: 'var(--radius-md)',
    border:       '1px solid',
    transition:   'var(--transition)',
  },
  dot: {
    width:        8,
    height:       8,
    borderRadius: '50%',
    flexShrink:   0,
  },
  label: {
    fontSize:      11,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    letterSpacing: '0.1em',
    whiteSpace:    'nowrap',
  },
  times: {
    display:     'flex',
    alignItems:  'center',
    gap:         4,
    paddingLeft: 4,
    borderLeft:  '1px solid var(--border-primary)',
  },
  timeText: {
    fontSize:   10,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    whiteSpace: 'nowrap',
  },
}