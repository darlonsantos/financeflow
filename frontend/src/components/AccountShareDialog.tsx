import { useState } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  SelectChangeEvent,
  TextField,
  Typography,
} from '@mui/material'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import PersonOffIcon from '@mui/icons-material/PersonOff'
import {
  useAccountShares,
  useRevokeAccountShare,
  useShareAccount,
  useUpdateAccountSharePermission,
} from '../hooks/useAccounts'
import type { Account, AccountSharePermission, AccountShareResponse } from '../types'

interface AccountShareDialogProps {
  account: Account
  open: boolean
  onClose: () => void
}

export default function AccountShareDialog({ account, open, onClose }: AccountShareDialogProps) {
  const [email, setEmail] = useState('')
  const [permission, setPermission] = useState<AccountSharePermission>('VIEW')
  const [emailError, setEmailError] = useState('')

  const { data: shares = [], isLoading } = useAccountShares(account.id)
  const shareMutation = useShareAccount()
  const revokeMutation = useRevokeAccountShare()
  const updatePermissionMutation = useUpdateAccountSharePermission()

  const handlePermissionChange = (e: SelectChangeEvent<AccountSharePermission>) => {
    setPermission(e.target.value as AccountSharePermission)
  }

  const handleShare = async () => {
    const trimmed = email.trim().toLowerCase()
    if (!trimmed) {
      setEmailError('Informe o e-mail do usuário')
      return
    }
    setEmailError('')
    try {
      await shareMutation.mutateAsync({
        accountId: account.id,
        data: { sharedWithEmail: trimmed, permission },
      })
      setEmail('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message
      setEmailError(msg || 'Não foi possível compartilhar. Verifique o e-mail.')
    }
  }

  const handleRevoke = async (share: AccountShareResponse) => {
    if (!window.confirm(`Remover acesso de ${share.sharedWithUserName}?`)) return
    await revokeMutation.mutateAsync({
      accountId: account.id,
      sharedWithUserId: share.sharedWithUserId,
    })
  }

  const handleUpdatePermission = async (share: AccountShareResponse, newPermission: AccountSharePermission) => {
    await updatePermissionMutation.mutateAsync({
      accountId: account.id,
      sharedWithUserId: share.sharedWithUserId,
      permission: newPermission,
    })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Compartilhar conta: {account.name}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          <Typography variant="subtitle2" color="text.secondary">
            Adicionar pessoa por e-mail
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'flex-start' }}>
            <TextField
              size="small"
              label="E-mail"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                setEmailError('')
              }}
              error={!!emailError}
              helperText={emailError}
              placeholder="email@exemplo.com"
              sx={{ flex: 1, minWidth: 200 }}
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Permissão</InputLabel>
              <Select value={permission} label="Permissão" onChange={handlePermissionChange}>
                <MenuItem value="VIEW">Visualizar</MenuItem>
                <MenuItem value="EDIT">Editar</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={<PersonAddIcon />}
              onClick={handleShare}
              disabled={shareMutation.isPending}
            >
              Compartilhar
            </Button>
          </Box>

          <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 2 }}>
            Pessoas com acesso
          </Typography>
          {isLoading ? (
            <Typography variant="body2" color="text.secondary">
              Carregando...
            </Typography>
          ) : shares.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              Ninguém além de você tem acesso a esta conta.
            </Typography>
          ) : (
            <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
              {shares.map((share) => (
                <Box
                  component="li"
                  key={share.id}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    flexWrap: 'wrap',
                    py: 0.5,
                  }}
                >
                  <Typography variant="body2" sx={{ flex: 1, minWidth: 120 }}>
                    {share.sharedWithUserName} ({share.sharedWithUserEmail})
                  </Typography>
                  <FormControl size="small" sx={{ minWidth: 110 }}>
                    <Select
                      value={share.permission}
                      onChange={(e) =>
                        handleUpdatePermission(share, e.target.value as AccountSharePermission)
                      }
                      disabled={updatePermissionMutation.isPending}
                    >
                      <MenuItem value="VIEW">Visualizar</MenuItem>
                      <MenuItem value="EDIT">Editar</MenuItem>
                    </Select>
                  </FormControl>
                  <Button
                    size="small"
                    color="error"
                    startIcon={<PersonOffIcon />}
                    onClick={() => handleRevoke(share)}
                    disabled={revokeMutation.isPending}
                  >
                    Remover
                  </Button>
                </Box>
              ))}
            </Box>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Fechar</Button>
      </DialogActions>
    </Dialog>
  )
}
