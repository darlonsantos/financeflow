import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
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
import PersonIcon from '@mui/icons-material/Person'
import EmailIcon from '@mui/icons-material/Email'
import LockIcon from '@mui/icons-material/Lock'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import PersonAddIcon from '@mui/icons-material/PersonAdd'

export default function Register() {
  const navigate = useNavigate()
  const { handleError, showSuccess } = useErrorHandler()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      await api.post('/auth/register', { name, email, password })
      showSuccess('Conta criada com sucesso! Verifique seu email para continuar.')
      navigate(`/email-verification-pending?email=${encodeURIComponent(email)}`)
    } catch (err) {
      handleError(err, 'Erro ao criar conta')
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
            Crie sua conta e comece a gerenciar suas finanças
          </Typography>
        </Box>

        {/* Card de Registro */}
        <Card elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Criar Conta
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Preencha os dados abaixo para começar
            </Typography>

            <form onSubmit={handleSubmit}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                <TextField
                  label="Nome Completo"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  fullWidth
                  size="small"
                  placeholder="Seu nome completo"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <PersonIcon fontSize="small" color="action" />
                      </InputAdornment>
                    ),
                  }}
                />
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
                  placeholder="Mínimo 8 caracteres"
                  inputProps={{ minLength: 8 }}
                  helperText="A senha deve ter no mínimo 8 caracteres"
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
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  size="large"
                  disabled={loading}
                  startIcon={loading ? <CircularProgress size={20} color="inherit" /> : undefined}
                  endIcon={!loading ? <PersonAddIcon /> : undefined}
                  sx={{ py: 1.5, textTransform: 'none', fontWeight: 600 }}
                >
                  {loading ? 'Criando conta...' : 'Criar Conta'}
                </Button>
              </Box>
            </form>

            <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
              Já tem uma conta?{' '}
              <Typography
                component={Link}
                to="/login"
                variant="body2"
                color="primary"
                sx={{ textDecoration: 'none', fontWeight: 600, '&:hover': { textDecoration: 'underline' } }}
              >
                Faça login
              </Typography>
            </Typography>
          </CardContent>
        </Card>

        {/* Footer */}
        <Typography variant="caption" color="text.disabled" sx={{ display: 'block', textAlign: 'center', mt: 3 }}>
          © {new Date().getFullYear()} FinanceFlow. Todos os direitos reservados.
        </Typography>
      </Box>
    </Box>
  )
}
