import { useState, useEffect } from 'react'
import { useCreateGoal, useUpdateGoal } from '../hooks/useGoals'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Goal, GoalRequest } from '../types'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface GoalFormProps {
  goal?: Goal | null
  onClose: () => void
}

export default function GoalForm({ goal, onClose }: GoalFormProps) {
  const createMutation = useCreateGoal()
  const updateMutation = useUpdateGoal()
  const { handleError, showSuccess } = useErrorHandler()

  const [formData, setFormData] = useState<GoalRequest>({
    name: '',
    targetAmount: 0,
    dueDate: '',
  })

  useEffect(() => {
    if (goal) {
      setFormData({
        name: goal.name,
        targetAmount: goal.targetAmount,
        dueDate: goal.dueDate ? goal.dueDate.substring(0, 10) : '',
      })
    }
  }, [goal])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const data: GoalRequest = {
        name: formData.name,
        targetAmount: formData.targetAmount,
        dueDate: formData.dueDate || undefined,
      }
      if (goal) {
        await updateMutation.mutateAsync({ id: goal.id, data })
        showSuccess('Meta atualizada com sucesso!')
      } else {
        await createMutation.mutateAsync(data)
        showSuccess('Meta criada com sucesso!')
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
        {goal ? 'Editar Meta' : 'Nova Meta'}
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
              placeholder="Ex: Viagem, Carro, Reserva emergência"
            />
            <TextField
              label="Valor alvo (R$)"
              type="number"
              inputProps={{ step: 0.01, min: 0.01 }}
              value={formData.targetAmount || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  targetAmount: parseFloat(e.target.value) || 0,
                })
              }
              required
              fullWidth
              size="small"
            />
            <TextField
              label="Data limite"
              type="date"
              value={formData.dueDate || ''}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
            />
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
