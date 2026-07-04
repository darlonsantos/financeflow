import { useState } from 'react'
import { useAccounts } from '../hooks/useAccounts'
import { useCreateTransfer } from '../hooks/useTransfers'
import { useErrorHandler } from '../hooks/useErrorHandler'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'

const DEFAULT_DESCRIPTION = 'Transferência entre Contas'

interface TransferFormProps {
  onClose: () => void
}

export default function TransferForm({ onClose }: TransferFormProps) {
  const { data: accounts } = useAccounts()
  const createTransfer = useCreateTransfer()
  const { handleError, showSuccess } = useErrorHandler()

  const [formData, setFormData] = useState({
    originAccountId: '',
    destinationAccountId: '',
    transferDate: new Date().toISOString().slice(0, 10),
    description: DEFAULT_DESCRIPTION,
    amount: 0 as number,
  })
  const [attemptedSubmit, setAttemptedSubmit] = useState(false)
  const [touched, setTouched] = useState({
    originAccountId: false,
    destinationAccountId: false,
    amount: false,
  })

  const canSubmit =
    !!formData.originAccountId &&
    !!formData.destinationAccountId &&
    formData.originAccountId !== formData.destinationAccountId &&
    formData.amount > 0

  const showOriginRequired =
    (attemptedSubmit || touched.originAccountId) && !formData.originAccountId
  const showDestinationRequired =
    (attemptedSubmit || touched.destinationAccountId) &&
    !formData.destinationAccountId
  const showSameAccount =
    !!formData.originAccountId &&
    !!formData.destinationAccountId &&
    formData.originAccountId === formData.destinationAccountId &&
    (attemptedSubmit || touched.originAccountId || touched.destinationAccountId)
  const showAmountInvalid = (attemptedSubmit || touched.amount) && formData.amount <= 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setAttemptedSubmit(true)
    if (!canSubmit) return
    try {
      await createTransfer.mutateAsync({
        originAccountId: formData.originAccountId,
        destinationAccountId: formData.destinationAccountId,
        transferDate: formData.transferDate,
        description: formData.description || undefined,
        amount: formData.amount,
      })
      showSuccess('Transferência realizada com sucesso!')
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: 2 } }}
    >
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          pr: 1,
        }}
      >
        Nova Transferência
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <FormControl
              fullWidth
              required
              size="small"
              error={showOriginRequired || showSameAccount}
            >
              <InputLabel>Conta de Origem</InputLabel>
              <Select
                value={formData.originAccountId}
                label="Conta de Origem"
                onChange={(e) => {
                  setTouched((prev) => ({ ...prev, originAccountId: true }))
                  setFormData({ ...formData, originAccountId: e.target.value })
                }}
                onBlur={() =>
                  setTouched((prev) => ({ ...prev, originAccountId: true }))
                }
                displayEmpty
                renderValue={(v) => {
                  if (!v) return null
                  const a = accounts?.find((x) => x.id === v)
                  return a ? a.name : v
                }}
              >
                <MenuItem value="">
                  <em>Selecione a conta de origem</em>
                </MenuItem>
                {accounts?.map((a) => (
                  <MenuItem key={a.id} value={a.id}>
                    {a.name}
                  </MenuItem>
                ))}
              </Select>
              {showOriginRequired ? (
                <FormHelperText>Selecione a conta de origem.</FormHelperText>
              ) : showSameAccount ? (
                <FormHelperText>
                  A conta de origem deve ser diferente da conta de destino.
                </FormHelperText>
              ) : null}
            </FormControl>

            <FormControl
              fullWidth
              required
              size="small"
              error={showDestinationRequired || showSameAccount}
            >
              <InputLabel>Conta de Destino</InputLabel>
              <Select
                value={formData.destinationAccountId}
                label="Conta de Destino"
                onChange={(e) => {
                  setTouched((prev) => ({ ...prev, destinationAccountId: true }))
                  setFormData({ ...formData, destinationAccountId: e.target.value })
                }}
                onBlur={() =>
                  setTouched((prev) => ({ ...prev, destinationAccountId: true }))
                }
                displayEmpty
                renderValue={(v) => {
                  if (!v) return null
                  const a = accounts?.find((x) => x.id === v)
                  return a ? a.name : v
                }}
              >
                <MenuItem value="">
                  <em>Selecione a conta de destino</em>
                </MenuItem>
                {accounts?.map((a) => (
                  <MenuItem key={a.id} value={a.id}>
                    {a.name}
                  </MenuItem>
                ))}
              </Select>
              {showDestinationRequired ? (
                <FormHelperText>Selecione a conta de destino.</FormHelperText>
              ) : showSameAccount ? (
                <FormHelperText>
                  A conta de destino deve ser diferente da conta de origem.
                </FormHelperText>
              ) : null}
            </FormControl>

            <TextField
              label="Data de Transferência"
              type="date"
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
              value={formData.transferDate}
              onChange={(e) =>
                setFormData({ ...formData, transferDate: e.target.value })
              }
            />

            <TextField
              label="Motivo Transferência"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              fullWidth
              size="small"
              placeholder={DEFAULT_DESCRIPTION}
            />

            <TextField
              label="Valor da Transferência"
              type="number"
              required
              fullWidth
              size="small"
              error={showAmountInvalid}
              helperText={
                showAmountInvalid
                  ? 'O valor da transferência deve ser maior que zero.'
                  : ' '
              }
              inputProps={{ min: 0.01, step: 0.01 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">R$</InputAdornment>
                ),
              }}
              value={formData.amount || ''}
              onChange={(e) => {
                setTouched((prev) => ({ ...prev, amount: true }))
                setFormData({
                  ...formData,
                  amount: parseFloat(e.target.value) || 0,
                })
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, amount: true }))}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={createTransfer.isPending || !canSubmit}
            startIcon={<SwapHorizIcon />}
          >
            {createTransfer.isPending ? 'Transferindo...' : 'Transferir'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
