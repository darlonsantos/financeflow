import { useState } from 'react'
import Layout from '../components/Layout'
import { useGoals, useDeleteGoal, useUpdateGoalStatus } from '../hooks/useGoals'
import GoalForm from '../components/GoalForm'
import GoalContributeDialog from '../components/GoalContributeDialog'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
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
import SavingsIcon from '@mui/icons-material/Savings'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import type { Goal } from '../types'

export default function Goals() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [showForm, setShowForm] = useState(false)
  const [editingGoal, setEditingGoal] = useState<Goal | null>(null)
  const [contributingGoal, setContributingGoal] = useState<Goal | null>(null)
  const { data: goals, isLoading } = useGoals(statusFilter || undefined)
  const deleteMutation = useDeleteGoal()
  const updateStatusMutation = useUpdateGoalStatus()

  const handleEdit = (goal: Goal) => {
    setEditingGoal(goal)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    if (window.confirm('Tem certeza que deseja excluir esta meta?')) {
      await deleteMutation.mutateAsync(id)
    }
  }

  const handleComplete = async (goal: Goal) => {
    await updateStatusMutation.mutateAsync({ id: goal.id, status: 'COMPLETED' })
  }

  const handleCancel = async (goal: Goal) => {
    await updateStatusMutation.mutateAsync({ id: goal.id, status: 'CANCELLED' })
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingGoal(null)
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('pt-BR')
  }

  const getStatusLabel = (status: string) => {
    if (status === 'ACTIVE') return 'Ativa'
    if (status === 'COMPLETED') return 'Concluída'
    if (status === 'CANCELLED') return 'Cancelada'
    return status
  }

  const getStatusColor = (status: string) => {
    if (status === 'ACTIVE') return 'primary'
    if (status === 'COMPLETED') return 'success'
    if (status === 'CANCELLED') return 'default'
    return 'default'
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Metas
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">Todas</MenuItem>
                <MenuItem value="ACTIVE">Ativas</MenuItem>
                <MenuItem value="COMPLETED">Concluídas</MenuItem>
                <MenuItem value="CANCELLED">Canceladas</MenuItem>
              </Select>
            </FormControl>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
              Nova Meta
            </Button>
          </Box>
        </Box>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando metas..." />
            </Box>
          ) : goals && goals.length > 0 ? (
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
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Meta</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Progresso</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Valor alvo</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Data limite</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {goals.map((goal) => (
                    <TableRow
                      key={goal.id}
                      hover
                      sx={{
                        '&:hover': {
                          bgcolor: (t) => alpha(t.palette.primary.main, 0.08),
                        },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 500 }}>{goal.name}</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 140 }}>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(goal.percentComplete, 100)}
                            sx={{
                              flex: 1,
                              height: 8,
                              borderRadius: 1,
                              bgcolor: 'action.hover',
                              '& .MuiLinearProgress-bar': {
                                bgcolor:
                                  goal.percentComplete >= 100
                                    ? 'success.main'
                                    : 'primary.main',
                              },
                            }}
                          />
                          <Typography variant="caption" sx={{ minWidth: 50 }}>
                            {formatCurrency(goal.currentAmount)}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {formatCurrency(goal.targetAmount)}
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {formatDate(goal.dueDate)}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusLabel(goal.status)}
                          color={getStatusColor(goal.status) as any}
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        {goal.status === 'ACTIVE' && (
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => setContributingGoal(goal)}
                            aria-label="Contribuir"
                            title="Contribuir"
                          >
                            <SavingsIcon fontSize="small" />
                          </IconButton>
                        )}
                        {goal.status === 'ACTIVE' && (
                          <IconButton
                            size="small"
                            onClick={() => handleEdit(goal)}
                            aria-label="Editar"
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        )}
                        {goal.status === 'ACTIVE' && (
                          <IconButton
                            size="small"
                            color="success"
                            onClick={() => handleComplete(goal)}
                            aria-label="Marcar como concluída"
                            title="Concluir"
                          >
                            <CheckCircleIcon fontSize="small" />
                          </IconButton>
                        )}
                        {goal.status === 'ACTIVE' && (
                          <IconButton
                            size="small"
                            color="warning"
                            onClick={() => handleCancel(goal)}
                            aria-label="Cancelar meta"
                            title="Cancelar"
                          >
                            <CancelIcon fontSize="small" />
                          </IconButton>
                        )}
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(goal.id)}
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
                  Nenhuma meta cadastrada
                </Typography>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => setShowForm(true)}
                  sx={{ mt: 2 }}
                >
                  Criar primeira meta
                </Button>
              </Box>
            </CardContent>
          )}
        </Card>

        {showForm && <GoalForm goal={editingGoal} onClose={handleCloseForm} />}
        {contributingGoal && (
          <GoalContributeDialog
            goal={contributingGoal}
            onClose={() => setContributingGoal(null)}
          />
        )}
      </Box>
    </Layout>
  )
}
