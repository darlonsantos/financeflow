import { useState } from 'react'
import Layout from '../components/Layout'
import { useBudgets, useDeleteBudget } from '../hooks/useBudgets'
import BudgetForm from '../components/BudgetForm'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  IconButton,
  InputLabel,
  LinearProgress,
  MenuItem,
  Select,
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
import type { Budget } from '../types'

export default function Budgets() {
  const now = new Date()
  const defaultMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const [monthFilter, setMonthFilter] = useState(defaultMonth)
  const [showForm, setShowForm] = useState(false)
  const [editingBudget, setEditingBudget] = useState<Budget | null>(null)
  const { data: budgets, isLoading } = useBudgets(monthFilter)
  const deleteMutation = useDeleteBudget()

  const handleEdit = (budget: Budget) => {
    setEditingBudget(budget)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    if (window.confirm('Tem certeza que deseja excluir este orçamento?')) {
      await deleteMutation.mutateAsync(id)
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingBudget(null)
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  const formatMonth = (dateStr: string) => {
    if (!dateStr) return ''
    const [year, month] = dateStr.split('-')
    const months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
    return `${months[parseInt(month, 10) - 1]}/${year}`
  }

  const getMonthOptions = () => {
    const options: string[] = []
    for (let i = -6; i <= 6; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() + i, 1)
      options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`)
    }
    return options
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Orçamentos
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Mês</InputLabel>
              <Select
                value={monthFilter}
                label="Mês"
                onChange={(e) => setMonthFilter(e.target.value)}
              >
                {getMonthOptions().map((m) => (
                  <MenuItem key={m} value={m}>
                    {formatMonth(m)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
              Novo Orçamento
            </Button>
          </Box>
        </Box>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando orçamentos..." />
            </Box>
          ) : budgets && budgets.length > 0 ? (
            <TableContainer>
              <Table
                sx={{
                  '& .MuiTableCell-root': {
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                  },
                }}
              >
                <TableHead>
                  <TableRow sx={{ bgcolor: 'action.hover' }}>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Categoria</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Limite</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Gasto</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>%</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {budgets.map((budget) => (
                    <TableRow
                      key={budget.id}
                      hover
                      sx={{
                        '&:hover': {
                          bgcolor: (t) => alpha(t.palette.primary.main, 0.08),
                        },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 500, display: 'flex', alignItems: 'center', gap: 1 }}>
                        {budget.categoryColor && (
                          <Box
                            sx={{
                              width: 12,
                              height: 12,
                              borderRadius: '50%',
                              bgcolor: budget.categoryColor,
                            }}
                          />
                        )}
                        {budget.categoryName}
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {formatCurrency(budget.limitAmount)}
                      </TableCell>
                      <TableCell
                        sx={{
                          fontWeight: 600,
                          color:
                            budget.percentUsed > 100
                              ? 'error.main'
                              : budget.percentUsed > 80
                                ? 'warning.main'
                                : 'text.primary',
                        }}
                      >
                        {formatCurrency(budget.spentAmount)}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 120 }}>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(budget.percentUsed, 100)}
                            sx={{
                              flex: 1,
                              height: 8,
                              borderRadius: 1,
                              bgcolor: 'action.hover',
                              '& .MuiLinearProgress-bar': {
                                bgcolor:
                                  budget.percentUsed > 100
                                    ? 'error.main'
                                    : budget.percentUsed > 80
                                      ? 'warning.main'
                                      : 'primary.main',
                              },
                            }}
                          />
                          <Typography variant="caption" sx={{ minWidth: 36 }}>
                            {budget.percentUsed.toFixed(0)}%
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleEdit(budget)} aria-label="Editar">
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(budget.id)}
                          aria-label="Excluir"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <CardContent>
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  Nenhum orçamento cadastrado para este mês
                </Typography>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => setShowForm(true)}
                  sx={{ mt: 2 }}
                >
                  Criar primeiro orçamento
                </Button>
              </Box>
            </CardContent>
          )}
        </Card>

        {showForm && <BudgetForm budget={editingBudget} onClose={handleCloseForm} />}
      </Box>
    </Layout>
  )
}
