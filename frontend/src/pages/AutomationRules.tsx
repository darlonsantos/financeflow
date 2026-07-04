import { useState } from 'react'
import Layout from '../components/Layout'
import { useAutomationRules, useCreateAutomationRule, useUpdateAutomationRule, useDeleteAutomationRule } from '../hooks/useAutomationRules'
import { useCategories } from '../hooks/useCategories'
import { useAccounts } from '../hooks/useAccounts'
import AutomationRuleForm from '../components/AutomationRuleForm'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import RuleIcon from '@mui/icons-material/Rule'
import type { AutomationRule, AutomationRuleRequest } from '../types'

export default function AutomationRules() {
  const [showForm, setShowForm] = useState(false)
  const [editingRule, setEditingRule] = useState<AutomationRule | null>(null)
  const { data: rules, isLoading } = useAutomationRules()
  const { data: categories } = useCategories()
  const { data: accounts } = useAccounts()
  const createMutation = useCreateAutomationRule()
  const updateMutation = useUpdateAutomationRule()
  const deleteMutation = useDeleteAutomationRule()

  const handleSubmit = async (data: AutomationRuleRequest) => {
    if (editingRule) {
      await updateMutation.mutateAsync({ id: editingRule.id, data })
    } else {
      await createMutation.mutateAsync(data)
    }
    setShowForm(false)
    setEditingRule(null)
  }

  const handleEdit = (rule: AutomationRule) => {
    setEditingRule(rule)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    if (window.confirm('Excluir esta regra?')) {
      await deleteMutation.mutateAsync(id)
    }
  }

  const getCategoryName = (id: string) => categories?.find((c) => c.id === id)?.name ?? id
  const getAccountName = (id: string) => accounts?.find((a) => a.id === id)?.name ?? id

  const describeCondition = (r: AutomationRule) => {
    const cfg = r.conditionConfig || {}
    if (r.conditionType === 'TRANSACTION_CATEGORY_AMOUNT') {
      const op = { GT: '>', GTE: '≥', LT: '<', LTE: '≤', EQ: '=' }[String(cfg.operator)] || '>'
      return `Categoria ${getCategoryName(String(cfg.categoryId ?? ''))} valor ${op} R$ ${Number(cfg.amount ?? 0).toFixed(2)}`
    }
    if (r.conditionType === 'ACCOUNT_BALANCE') {
      const op = String(cfg.operator) === 'LTE' ? '≤' : '<'
      return `Saldo ${getAccountName(String(cfg.accountId ?? ''))} ${op} R$ ${Number(cfg.amount ?? 0).toFixed(2)}`
    }
    return '-'
  }

  const getActionLabel = (r: AutomationRule) => {
    if (r.actionType === 'MARK_REVIEW') return "Marcar como 'revisar'"
    if (r.actionType === 'URGENT_ALERT') return 'Alerta urgente'
    return r.actionType
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Regras de automação
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setEditingRule(null); setShowForm(true) }}>
            Nova regra
          </Button>
        </Box>

        <Card elevation={1} sx={{ borderRadius: 2 }}>
          <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="body2" color="text.secondary">
              Regras são avaliadas ao criar/editar transações (marcar revisar) ou ao carregar notificações (alerta de saldo). Ex.: &quot;Se categoria = Mercado e valor &gt; R$ 500, marcar como revisar&quot; ou &quot;Se saldo conta X &lt; R$ 1000, alerta urgente&quot;.
            </Typography>
          </Box>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando regras..." />
            </Box>
          ) : !rules || rules.length === 0 ? (
            <Box sx={{ py: 4, textAlign: 'center' }}>
              <RuleIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
              <Typography color="text.secondary">Nenhuma regra configurada</Typography>
              <Button variant="outlined" size="small" sx={{ mt: 2 }} onClick={() => setShowForm(true)}>
                Criar primeira regra
              </Button>
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ bgcolor: 'action.hover' }}>
                    <TableCell sx={{ fontWeight: 600 }}>Nome</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Condição</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Ação</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }} />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rules.map((rule) => (
                    <TableRow
                      key={rule.id}
                      hover
                      sx={{
                        '&:hover': { bgcolor: (t) => alpha(t.palette.primary.main, 0.06) },
                      }}
                    >
                      <TableCell>{rule.name}</TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>{describeCondition(rule)}</TableCell>
                      <TableCell>{getActionLabel(rule)}</TableCell>
                      <TableCell>
                        <Chip
                          label={rule.active ? 'Ativa' : 'Inativa'}
                          size="small"
                          color={rule.active ? 'success' : 'default'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => handleEdit(rule)} aria-label="Editar">
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleDelete(rule.id)} aria-label="Excluir" color="error">
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Card>
      </Box>

      <AutomationRuleForm
        open={showForm}
        onClose={() => { setShowForm(false); setEditingRule(null) }}
        onSubmit={handleSubmit}
        rule={editingRule}
      />
    </Layout>
  )
}
