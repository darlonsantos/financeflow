import { useState, useEffect } from 'react'
import { useCreateTransaction, useUpdateTransaction, useSuggestCategory } from '../hooks/useTransactions'
import { useDebounce } from '../hooks/useDebounce'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Transaction, TransactionRequest, Account, Category } from '../types'
import {
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'

interface TransactionFormProps {
  transaction?: Transaction | null
  onClose: () => void
  accounts: Account[]
  categories: Category[]
}

export default function TransactionForm({
  transaction,
  onClose,
  accounts,
  categories,
}: TransactionFormProps) {
  const createMutation = useCreateTransaction()
  const updateMutation = useUpdateTransaction()
  const { handleError, showSuccess } = useErrorHandler()

  const [formData, setFormData] = useState<TransactionRequest>({
    accountId: '',
    categoryId: '',
    amount: 0,
    type: 'EXPENSE',
    date: new Date().toISOString().split('T')[0],
    dueDate: '',
    description: '',
    tags: [],
    recurring: false,
    recurringPattern: 'MONTHLY',
  })

  const debouncedDescription = useDebounce(formData.description ?? '', 600)
  const { data: suggestion, isLoading: suggestionLoading } = useSuggestCategory(
    formData.type,
    debouncedDescription
  )

  useEffect(() => {
    if (suggestion && suggestion.confidence >= 0.9 && !formData.categoryId) {
      setFormData((prev) => ({ ...prev, categoryId: suggestion.categoryId }))
    }
  }, [suggestion])

  useEffect(() => {
    if (transaction) {
      setFormData({
        accountId: transaction.accountId,
        categoryId: transaction.categoryId,
        amount: transaction.amount,
        type: transaction.type,
        date: transaction.date,
        dueDate: transaction.dueDate || '',
        description: transaction.description || '',
        tags: transaction.tags || [],
        recurring: transaction.recurring,
        recurringPattern: transaction.recurringPattern,
      })
    }
  }, [transaction])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const payload: TransactionRequest = {
      ...formData,
      dueDate: formData.dueDate?.trim() ? formData.dueDate : undefined,
    }
    try {
      if (transaction && transaction.id && transaction.id.trim() !== '') {
        await updateMutation.mutateAsync({ id: transaction.id, data: payload })
        showSuccess('Transação atualizada com sucesso!')
      } else {
        const created = await createMutation.mutateAsync(payload)
        const isPendingSync = created?.id?.toString().startsWith('pending-')
        showSuccess(
          isPendingSync
            ? 'Transação salva localmente. Será sincronizada quando estiver online.'
            : 'Transação criada com sucesso!'
        )
      }
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  const filteredCategories = categories.filter((cat) => cat.type === formData.type)
  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Dialog
      open={true}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{ sx: { borderRadius: 2, overflow: 'visible' } }}
    >
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
        {transaction && transaction.id && transaction.id.trim() !== ''
          ? 'Editar Transação'
          : 'Nova Transação'}
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 3, overflow: 'visible', '& .MuiInputLabel-root': { overflow: 'visible' } }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, overflow: 'visible' }}>
            <TextField
              select
              label="Tipo"
              value={formData.type}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  type: e.target.value as 'INCOME' | 'EXPENSE',
                  categoryId: '',
                })
              }
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
            >
              <MenuItem value="EXPENSE">Despesa</MenuItem>
              <MenuItem value="INCOME">Receita</MenuItem>
            </TextField>

            <TextField
              select
              label="Conta"
              value={formData.accountId}
              onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
            >
              <MenuItem value="">
                <em>Selecione uma conta</em>
              </MenuItem>
              {accounts.map((acc) => (
                <MenuItem key={acc.id} value={acc.id}>
                  {acc.name}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="Categoria"
              value={formData.categoryId}
              onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
            >
              <MenuItem value="">
                <em>Selecione uma categoria</em>
              </MenuItem>
              {filteredCategories.map((cat) => (
                <MenuItem key={cat.id} value={cat.id}>
                  {cat.name}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              label="Valor"
              type="number"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
              inputProps={{ step: 0.01, min: 0.01 }}
              value={formData.amount || ''}
              onChange={(e) =>
                setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })
              }
              required
              fullWidth
              size="small"
            />

            <TextField
              label="Data"
              type="date"
              value={formData.date}
              onChange={(e) => setFormData({ ...formData, date: e.target.value })}
              required
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
            />

            <TextField
              label="Data de Vencimento"
              type="date"
              value={formData.dueDate || ''}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value || undefined })}
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true, sx: { overflow: 'visible' } }}
            />

            <TextField
              label="Descrição"
              value={formData.description}
              InputLabelProps={{ sx: { overflow: 'visible' } }}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              fullWidth
              size="small"
              multiline
              rows={3}
            />

            {suggestion && suggestion.categoryId !== formData.categoryId && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                <Typography variant="caption" color="text.secondary">
                  Sugestão de categoria:
                </Typography>
                <Chip
                  icon={<AutoAwesomeIcon sx={{ fontSize: 16 }} />}
                  label={suggestion.categoryName}
                  color="primary"
                  variant="outlined"
                  size="small"
                  onClick={() =>
                    setFormData((prev) => ({ ...prev, categoryId: suggestion.categoryId }))
                  }
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      setFormData((prev) => ({ ...prev, categoryId: suggestion.categoryId }))
                    }
                  }}
                  tabIndex={0}
                  role="button"
                  sx={{ cursor: 'pointer' }}
                />
                {suggestion.confidence >= 0.9 && (
                  <Typography variant="caption" color="success.main">
                    Alta confiança
                  </Typography>
                )}
              </Box>
            )}

            {suggestionLoading && debouncedDescription.length >= 2 && (
              <Typography variant="caption" color="text.secondary">
                Analisando...
              </Typography>
            )}

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.recurring}
                    onChange={(e) =>
                      setFormData({ ...formData, recurring: e.target.checked })
                    }
                    size="small"
                  />
                }
                label="Transação recorrente"
              />
              {formData.recurring && (
                <FormControl fullWidth size="small" required={formData.recurring} sx={{ '& .MuiInputLabel-root': { overflow: 'visible' } }}>
                  <InputLabel id="transaction-pattern-label" shrink sx={{ overflow: 'visible', zIndex: 1 }}>Periodicidade</InputLabel>
                  <Select
                    labelId="transaction-pattern-label"
                    value={formData.recurringPattern || 'MONTHLY'}
                    label="Periodicidade"
                    onChange={(e) =>
                      setFormData({ ...formData, recurringPattern: e.target.value })
                    }
                  >
                    <MenuItem value="DAILY">Diária</MenuItem>
                    <MenuItem value="WEEKLY">Semanal</MenuItem>
                    <MenuItem value="MONTHLY">Mensal</MenuItem>
                    <MenuItem value="YEARLY">Anual</MenuItem>
                  </Select>
                </FormControl>
              )}
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
