import { useState, useEffect } from 'react'
import { useCreateBudget, useUpdateBudget } from '../hooks/useBudgets'
import { useCategories } from '../hooks/useCategories'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Budget, BudgetRequest } from '../types'
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

interface BudgetFormProps {
  budget?: Budget | null
  onClose: () => void
}

export default function BudgetForm({ budget, onClose }: BudgetFormProps) {
  const createMutation = useCreateBudget()
  const updateMutation = useUpdateBudget()
  const { handleError, showSuccess } = useErrorHandler()
  const { data: categories } = useCategories('EXPENSE')

  const now = new Date()
  const defaultMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`

  const [formData, setFormData] = useState<BudgetRequest>({
    categoryId: '',
    month: defaultMonth,
    limitAmount: 0,
  })

  useEffect(() => {
    if (budget) {
      setFormData({
        categoryId: budget.categoryId,
        month: budget.month.startsWith('20') ? budget.month : `${budget.month}-01`,
        limitAmount: budget.limitAmount,
      })
    }
  }, [budget])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const monthDate = `${formData.month.substring(0, 7)}-01`
      const data = { ...formData, month: monthDate }
      if (budget) {
        await updateMutation.mutateAsync({ id: budget.id, data })
        showSuccess('Orçamento atualizado com sucesso!')
      } else {
        await createMutation.mutateAsync(data)
        showSuccess('Orçamento criado com sucesso!')
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
        {budget ? 'Editar Orçamento' : 'Novo Orçamento'}
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <FormControl fullWidth size="small" required>
              <InputLabel>Categoria</InputLabel>
              <Select
                value={formData.categoryId}
                label="Categoria"
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
              >
                {categories?.map((cat) => (
                  <MenuItem key={cat.id} value={cat.id}>
                    {cat.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Mês"
              type="month"
              value={formData.month.substring(0, 7)}
              onChange={(e) => setFormData({ ...formData, month: `${e.target.value}-01` })}
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Limite (R$)"
              type="number"
              inputProps={{ step: 0.01, min: 0.01 }}
              value={formData.limitAmount || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  limitAmount: parseFloat(e.target.value) || 0,
                })
              }
              required
              fullWidth
              size="small"
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
