import { useState, useEffect } from 'react'
import { useAccount, useCreateAccount, useUpdateAccount } from '../hooks/useAccounts'
import { useCurrencies } from '../hooks/useCurrencies'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Account, AccountRequest } from '../types'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface AccountFormProps {
  account?: Account | null
  onClose: () => void
}

export default function AccountForm({ account, onClose }: AccountFormProps) {
  const createMutation = useCreateAccount()
  const updateMutation = useUpdateAccount()
  const { data: accountDetail } = useAccount(account?.id ?? '')
  const { data: currencies } = useCurrencies()
  const { handleError, showSuccess } = useErrorHandler()

  const [formData, setFormData] = useState<AccountRequest>({
    name: '',
    type: 'BANK',
    initialBalance: 0,
    color: '#3B82F6',
    icon: '',
    currencyCode: 'BRL',
  })

  const sourceAccount = account ? (accountDetail ?? account) : null

  useEffect(() => {
    if (sourceAccount) {
      const rawBalance = sourceAccount.initialBalance
      const initialBalance =
        typeof rawBalance === 'number' && !Number.isNaN(rawBalance)
          ? rawBalance
          : Number(rawBalance) || 0
      setFormData({
        name: sourceAccount.name,
        type: sourceAccount.type,
        initialBalance,
        color: sourceAccount.color || '#3B82F6',
        icon: sourceAccount.icon || '',
        currencyCode: sourceAccount.currencyCode || 'BRL',
      })
    }
  }, [sourceAccount])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      if (account) {
        await updateMutation.mutateAsync({ id: account.id, data: formData })
        showSuccess('Conta atualizada com sucesso!')
      } else {
        await createMutation.mutateAsync(formData)
        showSuccess('Conta criada com sucesso!')
      }
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Dialog open={true} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 2 } }}>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
        {account ? 'Editar Conta' : 'Nova Conta'}
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <TextField
              label="Nome"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              fullWidth
              size="small"
              autoFocus
            />
            <FormControl fullWidth size="small" required>
              <InputLabel>Tipo</InputLabel>
              <Select
                value={formData.type}
                label="Tipo"
                onChange={(e) =>
                  setFormData({ ...formData, type: e.target.value as 'BANK' | 'CASH' | 'CREDIT' })
                }
              >
                <MenuItem value="BANK">Banco</MenuItem>
                <MenuItem value="CASH">Dinheiro</MenuItem>
                <MenuItem value="CREDIT">Crédito</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Saldo Inicial"
              type="number"
              inputProps={{
                step: 0.01,
                min: 0,
                inputMode: 'decimal',
              }}
              value={formData.initialBalance}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  initialBalance: parseFloat(e.target.value) || 0,
                })
              }
              required
              fullWidth
              size="small"
              sx={{
                '& input[type=number]': { MozAppearance: 'textfield' },
                '& input[type=number]::-webkit-outer-spin-button': { WebkitAppearance: 'none', margin: 0 },
                '& input[type=number]::-webkit-inner-spin-button': { WebkitAppearance: 'none', margin: 0 },
              }}
            />
            <FormControl fullWidth size="small">
              <InputLabel>Moeda</InputLabel>
              <Select
                value={formData.currencyCode || 'BRL'}
                label="Moeda"
                onChange={(e) => setFormData({ ...formData, currencyCode: e.target.value })}
              >
                {(currencies || []).map((c) => (
                  <MenuItem key={c.code} value={c.code}>
                    {c.symbol} {c.name} ({c.code})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Box>
              <InputLabel shrink sx={{ fontSize: '0.875rem', fontWeight: 500, color: 'text.secondary', mb: 0.5, display: 'block' }}>
                Cor
              </InputLabel>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Box
                  component="input"
                  type="color"
                  value={formData.color}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, color: e.target.value })}
                  sx={{
                    width: 48,
                    height: 40,
                    p: 0.5,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    cursor: 'pointer',
                    bgcolor: 'background.paper',
                  }}
                />
                <TextField
                  value={formData.color}
                  onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                  size="small"
                  sx={{ flex: 1 }}
                  inputProps={{ maxLength: 7 }}
                  placeholder="#3B82F6"
                />
              </Box>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={isPending}>
            {isPending ? 'Salvando...' : 'Salvar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
