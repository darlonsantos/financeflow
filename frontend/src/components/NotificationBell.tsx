import { useState, useRef, useEffect } from 'react'
import type { NotificationPreferencesRequest } from '../types'
import { useAuthStore } from '../store/authStore'
import {
  useNotifications,
  useNotificationCount,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  useRefreshNotifications,
  useNotificationPreferences,
  useUpdateNotificationPreferences,
} from '../hooks/useNotifications'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Notification } from '../types'
import {
  alpha,
  Badge,
  Box,
  Button,
  Chip,
  FormControlLabel,
  IconButton,
  Paper,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import NotificationsIcon from '@mui/icons-material/Notifications'
import RefreshIcon from '@mui/icons-material/Refresh'
import SettingsIcon from '@mui/icons-material/Settings'

const TYPE_LABELS: Record<string, string> = {
  BUDGET_EXCEEDED: 'Orçamento',
  LOW_BALANCE: 'Saldo',
  BILLS_DUE: 'Despesas',
  GOAL_DUE_SOON: 'Meta',
  PREDICTIVE_RISK: 'Inteligência Preditiva',
  RULE_ALERT: 'Regra',
}

const TYPE_CHIP_COLORS: Record<string, 'warning' | 'error' | 'info' | 'success'> = {
  BUDGET_EXCEEDED: 'warning',
  LOW_BALANCE: 'error',
  BILLS_DUE: 'info',
  GOAL_DUE_SOON: 'success',
  PREDICTIVE_RISK: 'warning',
  RULE_ALERT: 'error',
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'Agora'
  if (diffMins < 60) return `${diffMins}min atrás`
  if (diffHours < 24) return `${diffHours}h atrás`
  if (diffDays < 7) return `${diffDays}d atrás`
  return date.toLocaleDateString('pt-BR')
}

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [showPreferences, setShowPreferences] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const { handleError } = useErrorHandler()
  const accessToken = useAuthStore((s) => s.accessToken)

  const { data: notificationsData, isLoading, refetch } = useNotifications({
    enabled: !!accessToken,
  })
  const { data: countFromApi } = useNotificationCount({
    enabled: !!accessToken,
  })
  const unreadFromList = notificationsData?.data?.filter((n: Notification) => !n.read).length ?? 0
  const unreadCount = Math.max(
    notificationsData?.unreadCount ?? 0,
    countFromApi ?? 0,
    unreadFromList
  )
  const markReadMutation = useMarkNotificationRead()
  const markAllReadMutation = useMarkAllNotificationsRead()
  const refreshMutation = useRefreshNotifications()

  useEffect(() => {
    if (open && useAuthStore.getState().accessToken) {
      refreshMutation
        .mutateAsync()
        .then(() => refetch())
        .catch((err) => handleError(err, 'Erro ao atualizar notificações'))
    }
  }, [open])

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleMarkRead = async (id: string) => {
    try {
      await markReadMutation.mutateAsync(id)
    } catch (err) {
      handleError(err, 'Erro ao marcar notificação')
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllReadMutation.mutateAsync()
    } catch (err) {
      handleError(err, 'Erro ao marcar todas como lidas')
    }
  }

  const handleRefresh = async () => {
    try {
      await refreshMutation.mutateAsync()
      refetch()
    } catch (err) {
      handleError(err, 'Erro ao atualizar notificações')
    }
  }

  return (
    <Box ref={dropdownRef} sx={{ position: 'relative', flexShrink: 0 }}>
      <IconButton
        onClick={() => setOpen(!open)}
        aria-label="Notificações"
        color="inherit"
        sx={{ '&:hover': { bgcolor: 'action.hover' } }}
      >
        <Badge badgeContent={unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : 0} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      {open && (
        <Paper
          elevation={8}
          sx={{
            position: { xs: 'fixed', sm: 'absolute' },
            right: { xs: 'auto', sm: 0 },
            left: { xs: '50%', sm: 'auto' },
            top: { xs: '50%', sm: '100%' },
            mt: { xs: 0, sm: 1.5 },
            transform: { xs: 'translate(-50%, -50%)', sm: 'none' },
            width: { xs: 'min(92vw, 28rem)', sm: 448 },
            maxWidth: { xs: 'calc(100vw - 24px)', sm: 'none' },
            minWidth: { xs: 0, sm: 320 },
            maxHeight: { xs: 'calc(100vh - 32px)', sm: 448 },
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            zIndex: 1300,
            borderRadius: 2,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider', bgcolor: 'action.hover' }}>
            <Typography variant="h6" component="h3" fontWeight={600}>
              Notificações
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <IconButton size="small" onClick={handleRefresh} disabled={refreshMutation.isPending} title="Atualizar">
                <RefreshIcon fontSize="small" />
              </IconButton>
              <IconButton
                size="small"
                onClick={() => setShowPreferences(!showPreferences)}
                color={showPreferences ? 'primary' : 'inherit'}
                title="Preferências"
              >
                <SettingsIcon fontSize="small" />
              </IconButton>
              {unreadCount > 0 && (
                <Button
                  size="small"
                  onClick={handleMarkAllRead}
                  disabled={markAllReadMutation.isPending}
                  sx={{ textTransform: 'none', fontSize: '0.75rem' }}
                >
                  Marcar todas lidas
                </Button>
              )}
            </Box>
          </Box>

          {showPreferences && (
            <NotificationPreferencesPanel onClose={() => setShowPreferences(false)} />
          )}

          <Box sx={{ overflowY: 'auto', flex: 1 }}>
            {isLoading ? (
              <Box sx={{ p: 4, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Carregando...
                </Typography>
              </Box>
            ) : !notificationsData?.data?.length ? (
              <Box sx={{ p: 4, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Nenhuma notificação
                </Typography>
              </Box>
            ) : (
              <Box sx={{ '& > *': { borderBottom: 1, borderColor: 'divider' } }}>
                {(notificationsData?.data ?? []).map((notification: Notification) => (
                  <Box
                    key={notification.id}
                    onClick={() => !notification.read && handleMarkRead(notification.id)}
                    sx={{
                      p: 2,
                      cursor: notification.read ? 'default' : 'pointer',
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: 1.5,
                      transition: 'background-color 0.2s',
                      '&:hover': { bgcolor: 'action.hover' },
                      ...(!notification.read && { bgcolor: (t) => alpha(t.palette.primary.main, 0.08) }),
                    }}
                  >
                    <Chip
                      size="small"
                      label={TYPE_LABELS[notification.type] || notification.type}
                      color={TYPE_CHIP_COLORS[notification.type] || undefined}
                      sx={!TYPE_CHIP_COLORS[notification.type] ? { bgcolor: 'action.selected' } : undefined}
                    />
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="subtitle2" fontWeight={600}>
                        {notification.title}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, wordBreak: 'break-word' }}>
                        {notification.message}
                      </Typography>
                      <Typography variant="caption" color="text.disabled" sx={{ mt: 0.5, display: 'block' }}>
                        {formatDate(notification.createdAt)}
                      </Typography>
                    </Box>
                    {!notification.read && (
                      <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'primary.main', mt: 1.5, flexShrink: 0 }} />
                    )}
                  </Box>
                ))}
              </Box>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  )
}

function NotificationPreferencesPanel({ onClose }: { onClose: () => void }) {
  const [threshold, setThreshold] = useState<number>(100)
  const { data: prefs, isLoading } = useNotificationPreferences()
  const updateMutation = useUpdateNotificationPreferences()
  const { handleError, showSuccess } = useErrorHandler()

  useEffect(() => {
    if (prefs) setThreshold(prefs.lowBalanceThreshold)
  }, [prefs])

  const handleToggle = async (key: keyof NotificationPreferencesRequest, value: boolean) => {
    if (!prefs) return
    try {
      await updateMutation.mutateAsync({ [key]: value })
      showSuccess('Preferência atualizada')
    } catch (err) {
      handleError(err, 'Erro ao atualizar preferência')
    }
  }

  const handleThresholdBlur = async () => {
    try {
      await updateMutation.mutateAsync({ lowBalanceThreshold: threshold })
      showSuccess('Limite atualizado')
    } catch (err) {
      handleError(err, 'Erro ao atualizar limite')
    }
  }

  if (isLoading || !prefs) return null

  return (
    <Box sx={{ p: 2, bgcolor: 'action.hover', borderBottom: 1, borderColor: 'divider' }}>
      <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 2 }}>
        Preferências de notificação
      </Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        <FormControlLabel
          control={
            <Switch
              checked={prefs.budgetExceededEnabled}
              onChange={(_, v) => handleToggle('budgetExceededEnabled', v)}
              size="small"
              color="primary"
            />
          }
          label="Orçamento excedido"
          slotProps={{ typography: { variant: 'body2' } }}
        />
        <FormControlLabel
          control={
            <Switch
              checked={prefs.lowBalanceEnabled}
              onChange={(_, v) => handleToggle('lowBalanceEnabled', v)}
              size="small"
              color="primary"
            />
          }
          label="Saldo baixo"
          slotProps={{ typography: { variant: 'body2' } }}
        />
        <FormControlLabel
          control={
            <Switch
              checked={prefs.billsDueEnabled}
              onChange={(_, v) => handleToggle('billsDueEnabled', v)}
              size="small"
              color="primary"
            />
          }
          label="Despesas a vencer"
          slotProps={{ typography: { variant: 'body2' } }}
        />
        <FormControlLabel
          control={
            <Switch
              checked={prefs.goalDueEnabled}
              onChange={(_, v) => handleToggle('goalDueEnabled', v)}
              size="small"
              color="primary"
            />
          }
          label="Metas próximas do prazo"
          slotProps={{ typography: { variant: 'body2' } }}
        />
        <FormControlLabel
          control={
            <Switch
              checked={prefs.emailEnabled}
              onChange={(_, v) => handleToggle('emailEnabled', v)}
              size="small"
              color="primary"
            />
          }
          label="Receber por e-mail"
          slotProps={{ typography: { variant: 'body2' } }}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mt: 0.5 }}>
          <Typography variant="body2">Limite saldo baixo (R$)</Typography>
          <TextField
            type="number"
            size="small"
            value={threshold}
            onChange={(e) => setThreshold(Number(e.target.value) || 0)}
            onBlur={handleThresholdBlur}
            inputProps={{ min: 0, step: 10 }}
            sx={{ width: 90 }}
          />
        </Box>
      </Box>
      <Button size="small" onClick={onClose} sx={{ mt: 2, textTransform: 'none' }}>
        Fechar preferências
      </Button>
    </Box>
  )
}
