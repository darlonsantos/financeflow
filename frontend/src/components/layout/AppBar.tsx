import { useNavigate } from 'react-router-dom'
import {
  AppBar as MuiAppBar,
  Box,
  IconButton,
  Toolbar,
  Typography,
  Button,
  useTheme,
  useMediaQuery,
  Tooltip,
  Chip,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import LightModeIcon from '@mui/icons-material/LightMode'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import CloudOffIcon from '@mui/icons-material/CloudOff'
import SyncIcon from '@mui/icons-material/Sync'
import GetAppIcon from '@mui/icons-material/GetApp'
import { useAuthStore } from '../../store/authStore'
import { useThemeMode } from '../../contexts/ThemeContext'
import { useOfflineSync } from '../../hooks/useOfflineSync'
import { useInstallPWA } from '../../hooks/useInstallPWA'
import NotificationBell from '../NotificationBell'

const HEADER_HEIGHT = 64

export { HEADER_HEIGHT }

export interface AppBarProps {
  onDrawerToggle: () => void
  sidebarCollapsed?: boolean
  isMobile?: boolean
  currentPageLabel?: string
}

export default function AppBar({
  onDrawerToggle,
  sidebarCollapsed = false,
  isMobile = false,
  currentPageLabel,
}: AppBarProps) {
  const theme = useTheme()
  const isMobileQuery = useMediaQuery(theme.breakpoints.down('sm'))
  const navigate = useNavigate()
  const { clearTokens } = useAuthStore()
  const { mode, toggleColorMode } = useThemeMode()
  const { isOnline, pendingCount, isSyncing } = useOfflineSync()
  // const { canInstall, isInstalled, install } = useInstallPWA()

  const handleLogout = () => {
    clearTokens()
    navigate('/login')
  }

  return (
    <MuiAppBar
      position="fixed"
      elevation={1}
      sx={{
        width: '100%',
        top: 'auto',
        left: 'auto',
        right: 'auto',
        bgcolor: 'background.paper',
        color: 'text.primary',
        boxShadow: theme.shadows[1],
      }}
    >
      <Toolbar
        sx={{
          minHeight: `${HEADER_HEIGHT}px !important`,
          height: HEADER_HEIGHT,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-start',
          px: { xs: 2, sm: 3 },
          py: 0,
        }}
      >
        <IconButton
          color="inherit"
          aria-label={isMobileQuery ? 'abrir menu' : (sidebarCollapsed ? 'expandir menu' : 'recolher menu')}
          onClick={onDrawerToggle}
          edge="start"
          sx={{
            mr: 2,
            transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            transform: sidebarCollapsed && !isMobileQuery ? 'rotate(0deg)' : 'rotate(0deg)',
            '&:hover': {
              backgroundColor: 'action.hover',
            },
          }}
        >
          <MenuIcon />
        </IconButton>
        <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <Typography
            variant="h6"
            component="span"
            sx={{
              fontWeight: 600,
              color: 'primary.main',
              lineHeight: 1.2,
            }}
          >
            FinanceFlow
          </Typography>
          {currentPageLabel && (
            <Typography
              variant="caption"
              sx={{
                color: 'text.secondary',
                fontWeight: 500,
                mt: 0.25,
              }}
            >
              {currentPageLabel}
            </Typography>
          )}
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {(!isOnline || pendingCount > 0) && (
            <Tooltip
              title={
                !isOnline
                  ? 'Você está offline. Transações serão sincronizadas ao reconectar.'
                  : isSyncing
                    ? 'Sincronizando transações...'
                    : `${pendingCount} transação(ões) na fila de sincronização`
              }
            >
              <Chip
                size="small"
                icon={!isOnline ? <CloudOffIcon /> : <SyncIcon />}
                label={
                  !isOnline
                    ? 'Offline'
                    : isSyncing
                      ? 'Sincronizando...'
                      : `${pendingCount} pendente(s)`
                }
                color={!isOnline ? 'warning' : 'default'}
                variant="outlined"
                sx={{ height: 28 }}
              />
            </Tooltip>
          )}
          {/* {canInstall && !isInstalled && (
            <Tooltip title="Instalar FinanceFlow como app">
              <Button
                color="inherit"
                size="small"
                startIcon={<GetAppIcon />}
                onClick={() => install()}
                sx={{ fontWeight: 500 }}
              >
                Instalar app
              </Button>
            </Tooltip>
          )} */}
          <NotificationBell />
          <IconButton
            color="inherit"
            onClick={toggleColorMode}
            aria-label="alternar tema"
            sx={{ '&:hover': { backgroundColor: 'action.hover' } }}
          >
            {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
          </IconButton>
          <Button color="inherit" onClick={handleLogout} sx={{ fontWeight: 500 }}>
            Sair
          </Button>
        </Box>
      </Toolbar>
    </MuiAppBar>
  )
}
