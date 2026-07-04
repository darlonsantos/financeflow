import { useState } from 'react'
import { useContributeGoal } from '../hooks/useGoals'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Goal } from '../types'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  TextField,
  Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface GoalContributeDialogProps {
  goal: Goal
  onClose: () => void
}

export default function GoalContributeDialog({ goal, onClose }: GoalContributeDialogProps) {
  const contributeMutation = useContributeGoal()
  const { handleError, showSuccess } = useErrorHandler()
  const [amount, setAmount] = useState<number>(0)

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  const remaining = goal.targetAmount - goal.currentAmount

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (amount <= 0) return
    try {
      await contributeMutation.mutateAsync({ id: goal.id, data: { amount } })
      showSuccess('Contribuição registrada com sucesso!')
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  return (
    <Dialog open={true} onClose={onClose} maxWidth="xs" fullWidth PaperProps={{ sx: { borderRadius: 2 } }}>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
        Contribuir para "{goal.name}"
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Progresso: {formatCurrency(goal.currentAmount)} / {formatCurrency(goal.targetAmount)} (
              {goal.percentComplete.toFixed(0)}%)
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Restante: {formatCurrency(remaining)}
            </Typography>
            <TextField
              label="Valor da contribuição (R$)"
              type="number"
              inputProps={{ step: 0.01, min: 0.01 }}
              value={amount || ''}
              onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
              required
              fullWidth
              size="small"
              autoFocus
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={contributeMutation.isPending || amount <= 0}>
            {contributeMutation.isPending ? 'Salvando...' : 'Contribuir'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
