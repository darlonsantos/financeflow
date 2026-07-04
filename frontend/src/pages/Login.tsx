import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useErrorHandler } from '../hooks/useErrorHandler'
import api from '../services/api'
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  IconButton,
  InputAdornment,
  TextField,
  Typography,
} from '@mui/material'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import EmailIcon from '@mui/icons-material/Email'
import LockIcon from '@mui/icons-material/Lock'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'

export default function Login() {
  const navigate = useNavigate()
  const setTokens = useAuthStore((state) => state.setTokens)
  const { handleError, showSuccess } = useErrorHandler()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await api.post('/auth/login', { email, password })
      const { accessToken, refreshToken } = response.data.data
      setTokens(accessToken, refreshToken)
      showSuccess('Login realizado com sucesso!')
      navigate('/dashboard')
    } catch (err: any) {
      const errorMessage = err?.response?.data?.error?.message || ''
      const errorMessageLower = errorMessage.toLowerCase()

      if (
        errorMessageLower.includes('email não verificado') ||
        errorMessageLower.includes('email nao verificado') ||
        errorMessageLower.includes('verifique seu email')
      ) {
        handleError(err, 'Email não verificado. Verifique seu email antes de fazer login.')
        setTimeout(() => {
          navigate(`/email-verification-pending?email=${encodeURIComponent(email)}`)
        }, 500)
      } else {
        handleError(err, 'Erro ao fazer login. Verifique suas credenciais.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 448 }}>
        {/* Logo e Header */}
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Box
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 72,
              height: 72,
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              borderRadius: 2,
              mb: 2,
              boxShadow: 2,
            }}
          >
            <AccountBalanceWalletIcon sx={{ fontSize: 40 }} />
          </Box>
          <Typography variant="h4" fontWeight={700} color="primary.main" gutterBottom>
            FinanceFlow
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gerencie suas finanças de forma inteligente
          </Typography>
        </Box>

        {/* Card de Login */}
        <Card elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Bem-vindo de volta
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Faça login para continuar
            </Typography>

            <form onSubmit={handleSubmit}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                <TextField
                  label="Email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  fullWidth
                  size="small"
                  placeholder="seu@email.com"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <EmailIcon fontSize="small" color="action" />
                      </InputAdornment>
                    ),
                  }}
                />
                <TextField
                  label="Senha"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  fullWidth
                  size="small"
                  placeholder="••••••••"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <LockIcon fontSize="small" color="action" />
                      </InputAdornment>
                    ),
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowPassword(!showPassword)}
                          edge="end"
                          size="small"
                          aria-label={showPassword ? 'ocultar senha' : 'mostrar senha'}
                        >
                          {showPassword ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
                <Box sx={{ textAlign: 'right' }}>
                  <Typography
                    component={Link}
                    to="/password-reset"
                    variant="body2"
                    color="primary"
                    sx={{ textDecoration: 'none', fontWeight: 500, '&:hover': { textDecoration: 'underline' } }}
                  >
                    Esqueceu sua senha?
                  </Typography>
                </Box>
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  size="large"
                  disabled={loading}
                  startIcon={loading ? <CircularProgress size={20} color="inherit" /> : undefined}
                  endIcon={!loading ? <ArrowForwardIcon /> : undefined}
                  sx={{ py: 1.5, textTransform: 'none', fontWeight: 600 }}
                >
                  {loading ? 'Entrando...' : 'Entrar'}
                </Button>
              </Box>
            </form>

            <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
              Não tem uma conta?{' '}
              <Typography
                component={Link}
                to="/register"
                variant="body2"
                color="primary"
                sx={{ textDecoration: 'none', fontWeight: 600, '&:hover': { textDecoration: 'underline' } }}
              >
                Registre-se agora
              </Typography>
            </Typography>
          </CardContent>
        </Card>

        {/* Footer */}
        <Typography variant="caption" color="text.disabled" sx={{ display: 'block', textAlign: 'center', mt: 3 }}>
          © {new Date().getFullYear()} FinanceFlow. Todos os direitos reservados.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: 'block', textAlign: 'center', mt: 0.5 }}>
          Versão {import.meta.env.VITE_APP_VERSION || '1.0.0'}
        </Typography>
      </Box>
    </Box>
  )
}
