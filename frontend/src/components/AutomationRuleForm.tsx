import { useState, useEffect } from 'react'
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import { useAccounts } from '../hooks/useAccounts'
import { useCategories } from '../hooks/useCategories'
import type { AutomationRule, AutomationRuleRequest, AutomationConditionType, AutomationActionType } from '../types'

const CONDITION_TYPES: { value: AutomationConditionType; label: string }[] = [
  { value: 'TRANSACTION_CATEGORY_AMOUNT', label: 'Transação: categoria e valor' },
  { value: 'ACCOUNT_BALANCE', label: 'Saldo da conta' },
]

const ACTION_TYPES: { value: AutomationActionType; label: string }[] = [
  { value: 'MARK_REVIEW', label: "Marcar transação como 'revisar'" },
  { value: 'URGENT_ALERT', label: 'Alerta urgente (notificação)' },
]

const AMOUNT_OPERATORS = [
  { value: 'GT', label: 'Maior que (>)' },
  { value: 'GTE', label: 'Maior ou igual (≥)' },
  { value: 'LT', label: 'Menor que (<)' },
  { value: 'LTE', label: 'Menor ou igual (≤)' },
  { value: 'EQ', label: 'Igual (=)' },
]

interface AutomationRuleFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: AutomationRuleRequest) => Promise<void>
  rule?: AutomationRule | null
}

export default function AutomationRuleForm({
  open,
  onClose,
  onSubmit,
  rule,
}: AutomationRuleFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories('EXPENSE')
  const [name, setName] = useState('')
  const [active, setActive] = useState(true)
  const [conditionType, setConditionType] = useState<AutomationConditionType>('TRANSACTION_CATEGORY_AMOUNT')
  const [actionType, setActionType] = useState<AutomationActionType>('MARK_REVIEW')
  const [categoryId, setCategoryId] = useState('')
  const [accountId, setAccountId] = useState('')
  const [operator, setOperator] = useState('GT')
  const [amount, setAmount] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (rule) {
      setName(rule.name)
      setActive(rule.active)
      setConditionType(rule.conditionType as AutomationConditionType)
      setActionType(rule.actionType as AutomationActionType)
      const cfg = rule.conditionConfig || {}
      setCategoryId((cfg.categoryId as string) ?? '')
      setAccountId((cfg.accountId as string) ?? '')
      setOperator((cfg.operator as string) ?? 'GT')
      setAmount(cfg.amount != null ? String(cfg.amount) : '')
    } else {
      setName('')
      setActive(true)
      setConditionType('TRANSACTION_CATEGORY_AMOUNT')
      setActionType('MARK_REVIEW')
      setCategoryId('')
      setAccountId('')
      setOperator('GT')
      setAmount('')
    }
  }, [rule, open])

  useEffect(() => {
    if (conditionType === 'ACCOUNT_BALANCE') {
      setActionType('URGENT_ALERT')
      if (operator !== 'LT' && operator !== 'LTE') setOperator('LT')
    } else if (conditionType === 'TRANSACTION_CATEGORY_AMOUNT') {
      setActionType('MARK_REVIEW')
    }
  }, [conditionType])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const numAmount = parseFloat(amount)
    if (isNaN(numAmount) || numAmount < 0) return
    setSubmitting(true)
    try {
      const conditionConfig =
        conditionType === 'TRANSACTION_CATEGORY_AMOUNT'
          ? { categoryId, operator, amount: numAmount }
          : { accountId, operator: operator === 'LT' || operator === 'LTE' ? operator : 'LT', amount: numAmount }
      await onSubmit({
        name,
        active,
        conditionType,
        conditionConfig,
        actionType,
      })
      onClose()
    } finally {
      setSubmitting(false)
    }
  }

  const valid =
    name.trim() &&
    (conditionType === 'TRANSACTION_CATEGORY_AMOUNT' ? categoryId && amount : accountId && amount) &&
    !isNaN(parseFloat(amount))

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 2 } }}>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{rule ? 'Editar regra' : 'Nova regra de automação'}</DialogTitle>
        <DialogContent dividers sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <TextField
            label="Nome da regra"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            required
            placeholder="Ex.: Mercado acima de R$ 500"
          />
          <FormControlLabel
            control={<Checkbox checked={active} onChange={(e) => setActive(e.target.checked)} />}
            label="Regra ativa"
          />
          <FormControl fullWidth>
            <InputLabel>Quando (condição)</InputLabel>
            <Select
              value={conditionType}
              label="Quando (condição)"
              onChange={(e) => setConditionType(e.target.value as AutomationConditionType)}
            >
              {CONDITION_TYPES.map((o) => (
                <MenuItem key={o.value} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {conditionType === 'TRANSACTION_CATEGORY_AMOUNT' && (
            <>
              <FormControl fullWidth>
                <InputLabel>Categoria</InputLabel>
                <Select
                  value={categoryId}
                  label="Categoria"
                  onChange={(e) => setCategoryId(e.target.value)}
                  required
                >
                  {(categories || []).map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      {c.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Valor (operador)</InputLabel>
                <Select value={operator} label="Valor (operador)" onChange={(e) => setOperator(e.target.value)}>
                  {AMOUNT_OPERATORS.map((o) => (
                    <MenuItem key={o.value} value={o.value}>
                      {o.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField
                label="Valor (R$)"
                type="number"
                inputProps={{ min: 0, step: 0.01 }}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                fullWidth
                required
              />
            </>
          )}

          {conditionType === 'ACCOUNT_BALANCE' && (
            <>
              <FormControl fullWidth>
                <InputLabel>Conta</InputLabel>
                <Select
                  value={accountId}
                  label="Conta"
                  onChange={(e) => setAccountId(e.target.value)}
                  required
                >
                  {(accounts || []).map((a) => (
                    <MenuItem key={a.id} value={a.id}>
                      {a.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Saldo (operador)</InputLabel>
                <Select value={operator} label="Saldo (operador)" onChange={(e) => setOperator(e.target.value)}>
                  <MenuItem value="LT">Menor que (&lt;)</MenuItem>
                  <MenuItem value="LTE">Menor ou igual (≤)</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Valor limite (R$)"
                type="number"
                inputProps={{ min: 0, step: 0.01 }}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                fullWidth
                required
                placeholder="Ex.: 1000 para alerta se saldo &lt; R$ 1000"
              />
            </>
          )}

          <FormControl fullWidth>
            <InputLabel>Então (ação)</InputLabel>
            <Select
              value={actionType}
              label="Então (ação)"
              onChange={(e) => setActionType(e.target.value as AutomationActionType)}
            >
              {conditionType === 'TRANSACTION_CATEGORY_AMOUNT' && (
                <MenuItem value="MARK_REVIEW">{ACTION_TYPES.find((a) => a.value === 'MARK_REVIEW')?.label}</MenuItem>
              )}
              {conditionType === 'ACCOUNT_BALANCE' && (
                <MenuItem value="URGENT_ALERT">{ACTION_TYPES.find((a) => a.value === 'URGENT_ALERT')?.label}</MenuItem>
              )}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={!valid || submitting}>
            {rule ? 'Salvar' : 'Criar regra'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
