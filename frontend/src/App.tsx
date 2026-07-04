import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { useAuthStore } from './store/authStore'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import Accounts from './pages/Accounts'
import Categories from './pages/Categories'
import Budgets from './pages/Budgets'
import Goals from './pages/Goals'
import AutomationRules from './pages/AutomationRules'
import CurrencyRates from './pages/CurrencyRates'
import Installments from './pages/Installments'
import Predictive from './pages/Predictive'
import BehavioralProfile from './pages/BehavioralProfile'
import TransferBetweenAccounts from './pages/TransferBetweenAccounts'
import OpenFinance from './pages/OpenFinance'
import PasswordResetRequest from './pages/PasswordResetRequest'
import PasswordResetConfirm from './pages/PasswordResetConfirm'
import EmailVerification from './pages/EmailVerification'
import EmailVerificationPending from './pages/EmailVerificationPending'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />
}

function App() {
  return (
    <BrowserRouter>
      <Toaster
        position="top-right"
        reverseOrder={false}
        gutter={8}
        toastOptions={{
          duration: 3000,
          style: {
            background: '#363636',
            color: '#fff',
          },
          success: {
            duration: 3000,
            iconTheme: {
              primary: '#10b981',
              secondary: '#fff',
            },
          },
          error: {
            duration: 4000,
            iconTheme: {
              primary: '#ef4444',
              secondary: '#fff',
            },
          },
        }}
      />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/password-reset" element={<PasswordResetRequest />} />
        <Route path="/reset-password" element={<PasswordResetConfirm />} />
        <Route path="/verify-email" element={<EmailVerification />} />
        <Route path="/email-verification-pending" element={<EmailVerificationPending />} />
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route
          path="/transactions"
          element={
            <PrivateRoute>
              <Transactions />
            </PrivateRoute>
          }
        />
        <Route
          path="/accounts"
          element={
            <PrivateRoute>
              <Accounts />
            </PrivateRoute>
          }
        />
        <Route
          path="/transfer"
          element={
            <PrivateRoute>
              <TransferBetweenAccounts />
            </PrivateRoute>
          }
        />
        <Route
          path="/open-finance"
          element={
            <PrivateRoute>
              <OpenFinance />
            </PrivateRoute>
          }
        />
        <Route
          path="/categories"
          element={
            <PrivateRoute>
              <Categories />
            </PrivateRoute>
          }
        />
        <Route
          path="/budgets"
          element={
            <PrivateRoute>
              <Budgets />
            </PrivateRoute>
          }
        />
        <Route
          path="/goals"
          element={
            <PrivateRoute>
              <Goals />
            </PrivateRoute>
          }
        />
        <Route
          path="/automation-rules"
          element={
            <PrivateRoute>
              <AutomationRules />
            </PrivateRoute>
          }
        />
        <Route
          path="/currency-rates"
          element={
            <PrivateRoute>
              <CurrencyRates />
            </PrivateRoute>
          }
        />
        <Route
          path="/installments"
          element={
            <PrivateRoute>
              <Installments />
            </PrivateRoute>
          }
        />
        <Route
          path="/predictive"
          element={
            <PrivateRoute>
              <Predictive />
            </PrivateRoute>
          }
        />
        <Route
          path="/behavioral-profile"
          element={
            <PrivateRoute>
              <BehavioralProfile />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
