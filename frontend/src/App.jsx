import React from 'react'
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate
} from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'

import Login      from './pages/Login'
import Register   from './pages/Register'
import Dashboard  from './pages/Dashboard'
import Portfolio  from './pages/Portfolio'
import AdminPanel from './pages/AdminPanel'

// ── Route Guards ───────────────────────────────────────────────
function PrivateRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()
  if (loading) return <FullScreenLoader />
  return isAuthenticated() ? children : <Navigate to="/login" replace />
}

function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  if (loading) return <FullScreenLoader />
  if (!isAuthenticated()) return <Navigate to="/login"  replace />
  if (!isAdmin())         return <Navigate to="/dashboard" replace />
  return children
}

function PublicRoute({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  if (loading) return <FullScreenLoader />
  if (isAuthenticated()) {
    return <Navigate to={isAdmin() ? '/admin' : '/dashboard'} replace />
  }
  return children
}

function FullScreenLoader() {
  return (
    <div style={{
      display:        'flex',
      alignItems:     'center',
      justifyContent: 'center',
      minHeight:      '100vh',
      background:     'var(--bg-primary)'
    }}>
      <div style={{
        width:        40,
        height:       40,
        border:       '3px solid var(--border-primary)',
        borderTop:    '3px solid var(--accent-cyan)',
        borderRadius: '50%',
        animation:    'spin 0.8s linear infinite'
      }} />
    </div>
  )
}

// ── App ────────────────────────────────────────────────────────
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>

          {/* Public routes */}
          <Route path="/login" element={
            <PublicRoute><Login /></PublicRoute>
          } />
          <Route path="/register" element={
            <PublicRoute><Register /></PublicRoute>
          } />

          {/* User routes */}
          <Route path="/dashboard" element={
            <PrivateRoute><Dashboard /></PrivateRoute>
          } />
          <Route path="/portfolio" element={
            <PrivateRoute><Portfolio /></PrivateRoute>
          } />

          {/* Admin routes */}
          <Route path="/admin" element={
            <AdminRoute><AdminPanel /></AdminRoute>
          } />

          {/* Default redirect */}
          <Route path="*" element={
            <Navigate to="/login" replace />
          } />

        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}