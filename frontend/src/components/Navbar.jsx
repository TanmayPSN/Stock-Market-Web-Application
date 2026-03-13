import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  TrendingUp, LayoutDashboard, Briefcase,
  LogOut, Sun, Moon, Menu, X,
  ShieldCheck, Users, BarChart2,
  ChevronDown
} from 'lucide-react'

export default function Navbar({ isAdmin }) {
  const { user, logout, toggleTheme, theme } = useAuth()
  const location = useLocation()
  const navigate  = useNavigate()
  const [collapsed,    setCollapsed]    = useState(false)
  const [showUserMenu, setShowUserMenu] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const userLinks = [
    {
      to:    '/dashboard',
      label: 'Dashboard',
      icon:  <LayoutDashboard size={16} />,
    },
    {
      to:    '/portfolio',
      label: 'Portfolio',
      icon:  <Briefcase size={16} />,
    },
  ]

  const adminLinks = [
    {
      to:    '/admin',
      label: 'Overview',
      icon:  <BarChart2 size={16} />,
    },
    {
      to:    '/admin?tab=users',
      label: 'Users',
      icon:  <Users size={16} />,
    },
  ]

  const links = isAdmin ? adminLinks : userLinks
  const isActive = (to) => location.pathname === to.split('?')[0]

  return (
    <nav style={{
      ...s.nav,
      width: collapsed ? 64 : 240,
    }}>

      {/* ── Logo ── */}
      <div style={s.logoArea}>
        {!collapsed && (
          <div style={s.logoRow}>
            <div style={s.logoIcon}>
              <TrendingUp size={18} color="var(--bg-primary)" />
            </div>
            <span style={s.logoText}>StockX</span>
            {isAdmin && (
              <span style={s.adminBadge}>
                <ShieldCheck size={10} />
                ADMIN
              </span>
            )}
          </div>
        )}
        {collapsed && (
          <div style={{ ...s.logoIcon, margin: '0 auto' }}>
            <TrendingUp size={18} color="var(--bg-primary)" />
          </div>
        )}

        {/* Collapse toggle */}
        <button
          onClick={() => setCollapsed(p => !p)}
          style={s.collapseBtn}
          title={collapsed ? 'Expand' : 'Collapse'}
        >
          {collapsed
            ? <Menu size={16} color="var(--text-muted)" />
            : <X    size={16} color="var(--text-muted)" />
          }
        </button>
      </div>

      <div style={s.divider} />

      {/* ── Nav links ── */}
      <div style={s.linksSection}>
        {!collapsed && (
          <span style={s.sectionLabel}>
            {isAdmin ? 'MANAGEMENT' : 'NAVIGATION'}
          </span>
        )}
        {links.map(link => (
          <Link
            key={link.to}
            to={link.to}
            style={{
              ...s.navLink,
              ...(isActive(link.to) ? s.navLinkActive : {}),
              justifyContent: collapsed ? 'center' : 'flex-start',
              padding:        collapsed ? '12px' : '11px 16px',
            }}
            title={collapsed ? link.label : ''}
          >
            <span style={{
              color: isActive(link.to)
                ? 'var(--accent-cyan)' : 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              flexShrink: 0,
            }}>
              {link.icon}
            </span>
            {!collapsed && (
              <span style={{
                color: isActive(link.to)
                  ? 'var(--text-primary)' : 'var(--text-secondary)',
                fontSize:      13,
                fontWeight:    isActive(link.to) ? 700 : 400,
                letterSpacing: '0.01em',
              }}>
                {link.label}
              </span>
            )}
            {/* Active indicator bar */}
            {isActive(link.to) && (
              <div style={s.activeBar} />
            )}
          </Link>
        ))}
      </div>

      {/* ── Spacer ── */}
      <div style={{ flex: 1 }} />

      <div style={s.divider} />

      {/* ── Bottom section ── */}
      <div style={s.bottomSection}>

        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          style={{
            ...s.themeBtn,
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding:        collapsed ? '12px' : '11px 16px',
          }}
          title="Toggle theme"
        >
          <span style={{ display: 'flex', alignItems: 'center',
                         flexShrink: 0 }}>
            {theme === 'dark'
              ? <Sun  size={16} color="var(--accent-amber)" />
              : <Moon size={16} color="var(--accent-cyan)"  />
            }
          </span>
          {!collapsed && (
            <span style={s.themeBtnLabel}>
              {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
            </span>
          )}
        </button>

        {/* User card */}
        <div
          style={{
            ...s.userCard,
            padding:        collapsed ? '12px' : '12px 16px',
            justifyContent: collapsed ? 'center' : 'space-between',
            cursor:         'pointer',
          }}
          onClick={() => !collapsed && setShowUserMenu(p => !p)}
        >
          <div style={{
            display:    'flex',
            alignItems: 'center',
            gap:        collapsed ? 0 : 10,
          }}>
            {/* Avatar */}
            <div style={s.avatar}>
              {user?.username?.[0]?.toUpperCase() ?? 'U'}
            </div>
            {!collapsed && (
              <div>
                <div style={s.userName}>{user?.username}</div>
                <div style={s.userRole}>
                  {isAdmin ? '⚡ Admin' : '👤 Trader'}
                </div>
              </div>
            )}
          </div>
          {!collapsed && (
            <ChevronDown
              size={14}
              color="var(--text-muted)"
              style={{
                transform:  showUserMenu
                  ? 'rotate(180deg)' : 'rotate(0)',
                transition: 'var(--transition)',
              }}
            />
          )}
        </div>

        {/* User dropdown */}
        {showUserMenu && !collapsed && (
          <div style={s.userDropdown} className="animate-fadeIn">
            <button
              onClick={handleLogout}
              style={s.logoutBtn}
            >
              <LogOut size={14} />
              Sign Out
            </button>
          </div>
        )}

      </div>
    </nav>
  )
}

const s = {
  nav: {
    position:      'fixed',
    top:           0,
    left:          0,
    height:        '100vh',
    background:    'var(--bg-secondary)',
    borderRight:   '1px solid var(--border-primary)',
    display:       'flex',
    flexDirection: 'column',
    zIndex:        100,
    transition:    'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    overflow:      'hidden',
  },
  logoArea: {
    padding:        '20px 16px',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'space-between',
    minHeight:      72,
  },
  logoRow: {
    display:    'flex',
    alignItems: 'center',
    gap:        10,
    flex:       1,
    minWidth:   0,
  },
  logoIcon: {
    width:          36,
    height:         36,
    background:     'var(--accent-cyan)',
    borderRadius:   'var(--radius-md)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    boxShadow:      'var(--shadow-glow-cyan)',
    flexShrink:     0,
  },
  logoText: {
    fontFamily:    'var(--font-display)',
    fontSize:      20,
    fontWeight:    800,
    color:         'var(--text-primary)',
    letterSpacing: '-0.03em',
    whiteSpace:    'nowrap',
  },
  adminBadge: {
    display:       'inline-flex',
    alignItems:    'center',
    gap:           3,
    background:    'var(--accent-amber-dim)',
    border:        '1px solid var(--accent-amber)',
    color:         'var(--accent-amber)',
    borderRadius:  20,
    padding:       '1px 6px',
    fontSize:      9,
    fontFamily:    'var(--font-mono)',
    fontWeight:    700,
    letterSpacing: '0.08em',
    whiteSpace:    'nowrap',
  },
  collapseBtn: {
    background:   'none',
    border:       'none',
    cursor:       'pointer',
    padding:      4,
    display:      'flex',
    alignItems:   'center',
    flexShrink:   0,
    transition:   'var(--transition)',
    borderRadius: 'var(--radius-sm)',
  },
  divider: {
    height:     1,
    background: 'var(--border-primary)',
    margin:     '0 16px',
    flexShrink: 0,
  },
  linksSection: {
    display:       'flex',
    flexDirection: 'column',
    padding:       '12px 0',
    gap:           2,
  },
  sectionLabel: {
    fontSize:      10,
    fontFamily:    'var(--font-mono)',
    color:         'var(--text-muted)',
    letterSpacing: '0.12em',
    padding:       '4px 20px 8px',
    textTransform: 'uppercase',
  },
  navLink: {
    display:        'flex',
    alignItems:     'center',
    gap:            12,
    textDecoration: 'none',
    borderRadius:   'var(--radius-md)',
    margin:         '0 8px',
    position:       'relative',
    transition:     'var(--transition)',
    whiteSpace:     'nowrap',
  },
  navLinkActive: {
    background: 'var(--accent-cyan-dim)',
  },
  activeBar: {
    position:     'absolute',
    right:        0,
    top:          '50%',
    transform:    'translateY(-50%)',
    width:        3,
    height:       20,
    background:   'var(--accent-cyan)',
    borderRadius: '3px 0 0 3px',
    boxShadow:    'var(--shadow-glow-cyan)',
  },
  bottomSection: {
    display:       'flex',
    flexDirection: 'column',
    padding:       '12px 0',
    gap:           2,
  },
  themeBtn: {
    display:     'flex',
    alignItems:  'center',
    gap:         12,
    background:  'none',
    border:      'none',
    cursor:      'pointer',
    borderRadius:'var(--radius-md)',
    margin:      '0 8px',
    transition:  'var(--transition)',
    whiteSpace:  'nowrap',
  },
  themeBtnLabel: {
    fontSize:   13,
    fontFamily: 'var(--font-ui)',
    color:      'var(--text-secondary)',
  },
  userCard: {
    display:      'flex',
    alignItems:   'center',
    margin:       '0 8px',
    borderRadius: 'var(--radius-md)',
    border:       '1px solid var(--border-primary)',
    background:   'var(--bg-card)',
    transition:   'var(--transition)',
  },
  avatar: {
    width:          32,
    height:         32,
    borderRadius:   '50%',
    background:     'linear-gradient(135deg, var(--accent-cyan), var(--accent-amber))',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    fontSize:       13,
    fontWeight:     700,
    color:          'var(--bg-primary)',
    fontFamily:     'var(--font-mono)',
    flexShrink:     0,
  },
  userName: {
    fontSize:   13,
    fontWeight: 700,
    color:      'var(--text-primary)',
    whiteSpace: 'nowrap',
    overflow:   'hidden',
    textOverflow:'ellipsis',
    maxWidth:   120,
  },
  userRole: {
    fontSize:   11,
    fontFamily: 'var(--font-mono)',
    color:      'var(--text-muted)',
    whiteSpace: 'nowrap',
  },
  userDropdown: {
    margin:       '4px 8px',
    background:   'var(--bg-card)',
    border:       '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-md)',
    overflow:     'hidden',
  },
  logoutBtn: {
    display:     'flex',
    alignItems:  'center',
    gap:         8,
    width:       '100%',
    padding:     '10px 16px',
    background:  'none',
    border:      'none',
    cursor:      'pointer',
    fontFamily:  'var(--font-ui)',
    fontSize:    13,
    color:       'var(--loss)',
    fontWeight:  700,
    transition:  'var(--transition)',
    letterSpacing: '0.04em',
  },
}