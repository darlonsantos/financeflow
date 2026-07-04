import { useState, useMemo } from 'react'
import { useCreateInstallment } from '../hooks/useInstallments'
import { useAccounts } from '../hooks/useAccounts'
import { useCategories } from '../hooks/useCategories'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { InstallmentGroupRequest } from '../types'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Checkbox,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface InstallmentFormProps {
  onClose: () => void
}

const INSTALLMENT_TYPES = [
  { value: 'FIXED' as const, label: 'Parcelas fixas' },
  { value: 'VARIABLE' as const, label: 'Parcelas variáveis' },
  { value: 'RECURRING' as const, label: 'Recorrente (assinatura)' },
]

const MIN_AMOUNT = 0.01
const MAX_INSTALLMENTS = 360

export default function InstallmentForm({ onClose }: InstallmentFormProps) {
  const createMutation = useCreateInstallment()
  const { handleError, showSuccess } = useErrorHandler()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories('EXPENSE')
  const todayIso = new Date().toISOString().slice(0, 10)

  const [formData, setFormData] = useState<InstallmentGroupRequest & { variableAmountsStr: string; splitBetweenInstallments: boolean }>({
    accountId: '',
    categoryId: '',
    description: '',
    totalAmount: 0,
    installmentType: 'FIXED',
    firstDueDate: todayIso,
    numberOfInstallments: 12,
    variableAmountsStr: '',
    splitBetweenInstallments: true,
  })

  const variableAmounts = useMemo(() => {
    if (formData.installmentType !== 'VARIABLE' || !formData.variableAmountsStr.trim()) return undefined
    return formData.variableAmountsStr
      .replace(/\n/g, ';')
      .split(/[;\s]+/)
      .map((s) => s.trim())
      .filter(Boolean)
      .map((s) => parseFloat(s.replace(/\./g, '').replace(',', '.')))
      .filter((n) => !Number.isNaN(n))
  }, [formData.installmentType, formData.variableAmountsStr])

  const variableAmountsSum = useMemo(
    () => (variableAmounts || []).reduce((acc, value) => acc + value, 0),
    [variableAmounts],
  )

  const validationErrors = useMemo(() => {
    const errors: string[] = []
    if (!formData.accountId) errors.push('Selecione a conta.')
    if (!formData.categoryId) errors.push('Selecione a categoria.')
    if (!(formData.totalAmount >= MIN_AMOUNT)) errors.push('Valor total deve ser maior que zero.')
    if (
      !Number.isInteger(formData.numberOfInstallments)
      || formData.numberOfInstallments < 1
      || formData.numberOfInstallments > MAX_INSTALLMENTS
    ) {
      errors.push(`Número de parcelas deve estar entre 1 e ${MAX_INSTALLMENTS}.`)
    }
    if (!formData.firstDueDate || formData.firstDueDate < todayIso) {
      errors.push('Data da primeira parcela não pode ser no passado.')
    }
    if (formData.installmentType === 'VARIABLE' && formData.splitBetweenInstallments) {
      if (!variableAmounts?.length) {
        errors.push('Informe os valores por parcela para parcelamento variável.')
      } else {
        if (variableAmounts.length !== formData.numberOfInstallments) {
          errors.push(`Informe exatamente ${formData.numberOfInstallments} valores de parcela.`)
        }
        if (variableAmounts.some((amount) => amount <= 0)) {
          errors.push('Todos os valores de parcela devem ser maiores que zero.')
        }
        if (Math.abs(variableAmountsSum - formData.totalAmount) > 0.01) {
          errors.push('A soma dos valores por parcela deve ser igual ao valor total.')
        }
      }
    }
    return errors
  }, [formData, todayIso, variableAmounts, variableAmountsSum])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (validationErrors.length > 0) return
    try {
      const payload: InstallmentGroupRequest = {
        accountId: formData.accountId,
        categoryId: formData.categoryId,
        description: formData.description || undefined,
        totalAmount: formData.totalAmount,
        installmentType: formData.installmentType,
        firstDueDate: formData.firstDueDate,
        numberOfInstallments: formData.numberOfInstallments,
      }
      if (!formData.splitBetweenInstallments) {
        // Repetir o mesmo valor em todas as parcelas: enviar como VARIABLE com N vezes o valor
        payload.installmentType = 'VARIABLE'
        payload.totalAmount = formData.totalAmount * formData.numberOfInstallments
        payload.variableAmounts = Array(formData.numberOfInstallments).fill(formData.totalAmount)
      } else if (formData.installmentType === 'VARIABLE' && variableAmounts?.length) {
        payload.variableAmounts = variableAmounts
      }
      await createMutation.mutateAsync(payload)
      showSuccess('Parcelamento criado com sucesso!')
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  const isPending = createMutation.isPending
  const isSubmitDisabled = isPending || validationErrors.length > 0

  return (
    <Dialog open={true} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 2 } }}>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
        Novo parcelamento
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <FormControl fullWidth required>
              <InputLabel>Conta</InputLabel>
              <Select
                value={formData.accountId}
                label="Conta"
                onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
              >
                {accounts?.map((a) => (
                  <MenuItem key={a.id} value={a.id}>
                    {a.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth required>
              <InputLabel>Categoria</InputLabel>
              <Select
                value={formData.categoryId}
                label="Categoria"
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
              >
                {categories?.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Descrição"
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              fullWidth
              placeholder="Ex: TV 55 polegadas"
            />
            <TextField
              label="Valor total"
              type="number"
              required
              fullWidth
              inputProps={{ min: MIN_AMOUNT, step: 0.01 }}
              InputProps={{
                startAdornment: <InputAdornment position="start">R$</InputAdornment>,
              }}
              value={formData.totalAmount || ''}
              onChange={(e) => setFormData({ ...formData, totalAmount: parseFloat(e.target.value) || 0 })}
              error={formData.totalAmount > 0 && formData.totalAmount < MIN_AMOUNT}
              helperText={formData.totalAmount > 0 && formData.totalAmount < MIN_AMOUNT ? 'Valor mínimo: R$ 0,01.' : ' '}
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={formData.splitBetweenInstallments}
                  onChange={(e) =>
                    setFormData({ ...formData, splitBetweenInstallments: e.target.checked })
                  }
                />
              }
              label="Dividir entre Parcelas"
            />
            <FormControl fullWidth required>
              <InputLabel>Tipo</InputLabel>
              <Select
                value={formData.installmentType}
                label="Tipo"
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    installmentType: e.target.value as InstallmentGroupRequest['installmentType'],
                  })
                }
              >
                {INSTALLMENT_TYPES.map((opt) => (
                  <MenuItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Data da primeira parcela"
              type="date"
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={formData.firstDueDate}
              onChange={(e) => setFormData({ ...formData, firstDueDate: e.target.value })}
              inputProps={{ min: todayIso }}
              error={Boolean(formData.firstDueDate && formData.firstDueDate < todayIso)}
              helperText={formData.firstDueDate < todayIso ? 'A data não pode ser no passado.' : ' '}
            />
            <TextField
              label="Número de parcelas"
              type="number"
              required
              fullWidth
              inputProps={{ min: 1, max: MAX_INSTALLMENTS }}
              value={formData.numberOfInstallments || ''}
              onChange={(e) => {
                const parsed = Number.parseInt(e.target.value, 10)
                setFormData({
                  ...formData,
                  numberOfInstallments: Number.isNaN(parsed) ? 0 : parsed,
                })
              }}
              error={formData.numberOfInstallments < 1 || formData.numberOfInstallments > MAX_INSTALLMENTS}
              helperText={`Mínimo 1 e máximo ${MAX_INSTALLMENTS} parcelas.`}
            />
            {formData.installmentType === 'VARIABLE' && formData.splitBetweenInstallments && (
              <TextField
                label="Valores por parcela (separados por ; ou espaço)"
                value={formData.variableAmountsStr}
                onChange={(e) => setFormData({ ...formData, variableAmountsStr: e.target.value })}
                fullWidth
                placeholder="Ex: 100,50; 150,00; 200,25"
                error={
                  Boolean(variableAmounts?.length)
                  && ((variableAmounts?.length ?? 0) !== formData.numberOfInstallments || Math.abs(variableAmountsSum - formData.totalAmount) > 0.01)
                }
                helperText={`Informe exatamente ${formData.numberOfInstallments} valores. Soma atual: R$ ${variableAmountsSum.toFixed(2)} | Esperada: R$ ${formData.totalAmount?.toFixed(2) || '0,00'}.`}
              />
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          {validationErrors.length > 0 && (
            <Box sx={{ flex: 1, mr: 2, color: 'error.main', fontSize: 12 }}>
              {validationErrors[0]}
            </Box>
          )}
          <Button onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={isSubmitDisabled}>
            {isPending ? 'Criando...' : 'Criar parcelamento'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
