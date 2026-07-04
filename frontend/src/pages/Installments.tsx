import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import {
  useInstallments,
  useInstallment,
  usePayInstallment,
  useEarlySettlement,
  useRenegotiate,
  useCancelInstallment,
} from '../hooks/useInstallments'
import InstallmentForm from '../components/InstallmentForm'
import { useErrorHandler } from '../hooks/useErrorHandler'
import { LoadingSection } from '../components/Loading'
import { useDebounce } from '../hooks/useDebounce'
import type {
  InstallmentGroupResponse,
  InstallmentItemResponse,
  InstallmentGroupStatus,
  InstallmentGroupType,
  RenegotiateRequest,
} from '../types'
import {
  alpha,
  Box,
  Button,
  Card,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  LinearProgress,
  Menu,
  MenuItem,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import PaymentIcon from '@mui/icons-material/Payment'
import PayOffIcon from '@mui/icons-material/Paid'
import RenegotiateIcon from '@mui/icons-material/SwapHoriz'
import CloseIcon from '@mui/icons-material/Close'

const PAGE_SIZE = 10

function formatCurrency(value: number, currencyCode?: string) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: currencyCode || 'BRL',
  }).format(value)
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('pt-BR')
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'Ativo',
    PAID_OFF: 'Quitado',
    CANCELLED: 'Cancelado',
    PENDING: 'Pendente',
    PAID: 'Pago',
  }
  return map[status] || status
}

function statusColor(status: string): 'success' | 'warning' | 'default' | 'primary' {
  if (status === 'ACTIVE' || status === 'PENDING') return 'primary'
  if (status === 'PAID_OFF' || status === 'PAID') return 'success'
  if (status === 'CANCELLED') return 'default'
  return 'default'
}

export default function Installments() {
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [selectedGroup, setSelectedGroup] = useState<InstallmentGroupResponse | null>(null)
  const [payItemId, setPayItemId] = useState<string | null>(null)
  const [earlySettleId, setEarlySettleId] = useState<string | null>(null)
  const [earlySettleRemaining, setEarlySettleRemaining] = useState<number | null>(null)
  const [renegotiateId, setRenegotiateId] = useState<string | null>(null)
  const [renegotiateForm, setRenegotiateForm] = useState<Partial<RenegotiateRequest>>({})
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<InstallmentGroupStatus | 'ALL'>('ALL')
  const [typeFilter, setTypeFilter] = useState<InstallmentGroupType | 'ALL'>('ALL')
  const debouncedSearch = useDebounce(search, 400)

  const { data: paginated, isLoading } = useInstallments(page, PAGE_SIZE, {
    search: debouncedSearch,
    status: statusFilter,
    installmentType: typeFilter,
  })
  const { data: detail, isLoading: loadingDetail } = useInstallment(detailId)
  const payMutation = usePayInstallment()
  const earlyMutation = useEarlySettlement()
  const renegotiateMutation = useRenegotiate()
  const cancelMutation = useCancelInstallment()
  const { handleError, showSuccess } = useErrorHandler()

  const list = paginated?.data ?? []
  const pagination = paginated?.pagination ?? { page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 }

  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, statusFilter, typeFilter])

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, group: InstallmentGroupResponse) => {
    setAnchorEl(event.currentTarget)
    setSelectedGroup(group)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
    setSelectedGroup(null)
  }

  const handlePayInstallment = async () => {
    if (!payItemId) return
    try {
      await payMutation.mutateAsync({ installmentItemId: payItemId })
      showSuccess('Parcela paga com sucesso!')
      setPayItemId(null)
      if (detailId) setDetailId(null)
    } catch (e) {
      handleError(e)
    }
  }

  const handleEarlySettlement = async () => {
    if (!earlySettleId) return
    try {
      await earlyMutation.mutateAsync({ installmentGroupId: earlySettleId })
      showSuccess('Quitação antecipada realizada!')
      setEarlySettleId(null)
      setEarlySettleRemaining(null)
      setDetailId(null)
    } catch (e) {
      handleError(e)
    }
  }

  const handleRenegotiate = async () => {
    if (!renegotiateId || !renegotiateForm.newTotalAmount || !renegotiateForm.newFirstDueDate || !renegotiateForm.newNumberOfInstallments) return
    try {
      await renegotiateMutation.mutateAsync({
        installmentGroupId: renegotiateId,
        newTotalAmount: renegotiateForm.newTotalAmount,
        newFirstDueDate: renegotiateForm.newFirstDueDate,
        newNumberOfInstallments: renegotiateForm.newNumberOfInstallments,
        newVariableAmounts: renegotiateForm.newVariableAmounts,
        newInstallmentType: renegotiateForm.newInstallmentType,
      })
      showSuccess('Renegociação realizada!')
      setRenegotiateId(null)
      setRenegotiateForm({})
      setDetailId(null)
    } catch (e) {
      handleError(e)
    }
  }

  const handleCancel = async () => {
    if (!selectedGroup) return
    if (!window.confirm('Cancelar este parcelamento? Parcelas pendentes serão canceladas.')) {
      handleMenuClose()
      return
    }
    try {
      await cancelMutation.mutateAsync(selectedGroup.id)
      showSuccess('Parcelamento cancelado.')
      handleMenuClose()
    } catch (e) {
      handleError(e)
    }
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Parcelamentos
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
            Novo parcelamento
          </Button>
        </Box>

        <Card elevation={1} sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
            <TextField
              label="Buscar"
              placeholder="Descrição, conta ou categoria"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              sx={{ minWidth: 260, flex: 1 }}
            />
            <TextField
              select
              label="Status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as InstallmentGroupStatus | 'ALL')}
              size="small"
              sx={{ minWidth: 180 }}
            >
              <MenuItem value="ALL">Todos os status</MenuItem>
              <MenuItem value="ACTIVE">Ativo</MenuItem>
              <MenuItem value="PAID_OFF">Quitado</MenuItem>
              <MenuItem value="CANCELLED">Cancelado</MenuItem>
            </TextField>
            <TextField
              select
              label="Tipo"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as InstallmentGroupType | 'ALL')}
              size="small"
              sx={{ minWidth: 200 }}
            >
              <MenuItem value="ALL">Todos os tipos</MenuItem>
              <MenuItem value="FIXED">Parcelas fixas</MenuItem>
              <MenuItem value="VARIABLE">Parcelas variáveis</MenuItem>
              <MenuItem value="RECURRING">Recorrente</MenuItem>
            </TextField>
          </Box>
        </Card>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando parcelamentos..." />
            </Box>
          ) : list.length === 0 ? (
            <Box sx={{ p: 4, textAlign: 'center' }}>
              <Typography color="text.secondary">
                {debouncedSearch || statusFilter !== 'ALL' || typeFilter !== 'ALL'
                  ? 'Nenhum parcelamento encontrado para os filtros selecionados.'
                  : 'Nenhum parcelamento cadastrado.'}
              </Typography>
              {!debouncedSearch && statusFilter === 'ALL' && typeFilter === 'ALL' && (
                <Button sx={{ mt: 2 }} variant="outlined" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
                  Criar primeiro parcelamento
                </Button>
              )}
            </Box>
          ) : (
            <>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ bgcolor: 'action.hover' }}>
                      <TableCell sx={{ fontWeight: 600 }}>Descrição</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Conta / Categoria</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Total</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Parcelas</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 600 }} align="right">Ações</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {list.map((row) => (
                      <TableRow
                        key={row.id}
                        hover
                        sx={{
                          '&:hover': { bgcolor: (t) => alpha(t.palette.primary.main, 0.08) },
                        }}
                      >
                        <TableCell>
                          <Typography fontWeight={500}>
                            {row.description || `Parcelamento ${row.id.slice(0, 8)}`}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {row.firstDueDate && formatDate(row.firstDueDate)} · {row.installmentType}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">{row.accountName}</Typography>
                          <Typography variant="caption" color="text.secondary">{row.categoryName}</Typography>
                        </TableCell>
                        <TableCell>
                          {formatCurrency(row.totalAmount, row.currencyCode)}
                          {row.pendingCount > 0 && (
                            <Typography variant="caption" display="block" color="text.secondary">
                              Restante: {formatCurrency(row.remainingAmount, row.currencyCode)}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 120 }}>
                            <LinearProgress
                              variant="determinate"
                              value={row.numberOfInstallments ? (row.paidCount / row.numberOfInstallments) * 100 : 0}
                              sx={{
                                flex: 1,
                                height: 6,
                                borderRadius: 1,
                                bgcolor: 'action.hover',
                                '& .MuiLinearProgress-bar': {
                                  bgcolor: row.status === 'PAID_OFF' ? 'success.main' : 'primary.main',
                                },
                              }}
                            />
                            <Typography variant="caption">
                              {row.paidCount}/{row.numberOfInstallments}
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={statusLabel(row.status)}
                            color={statusColor(row.status)}
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Button size="small" onClick={() => setDetailId(row.id)}>
                            Ver parcelas
                          </Button>
                          {row.status === 'ACTIVE' && (
                            <IconButton size="small" onClick={(e) => handleMenuOpen(e, row)}>
                              <MoreVertIcon />
                            </IconButton>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              {pagination.totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                  <Pagination
                    count={pagination.totalPages}
                    page={pagination.page + 1}
                    onChange={(_, p) => setPage(p - 1)}
                    color="primary"
                  />
                </Box>
              )}
            </>
          )}
        </Card>
      </Box>

      {showForm && <InstallmentForm onClose={() => setShowForm(false)} />}

      {/* Detalhe: lista de parcelas */}
      <Dialog
        open={!!detailId}
        onClose={() => setDetailId(null)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 2 } }}
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
          Parcelas
          <IconButton onClick={() => setDetailId(null)} size="small">
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          {loadingDetail || !detail ? (
            <LoadingSection message="Carregando..." />
          ) : (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                {detail.description || 'Parcelamento'} · {detail.accountName} · {detail.categoryName}
              </Typography>
              <Typography variant="h6" gutterBottom>
                Total: {formatCurrency(detail.totalAmount, detail.currencyCode)} · Restante:{' '}
                {formatCurrency(detail.remainingAmount, detail.currencyCode)}
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>#</TableCell>
                      <TableCell>Vencimento</TableCell>
                      <TableCell>Valor</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell align="right">Ação</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {detail.items?.map((item: InstallmentItemResponse) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.installmentNumber}</TableCell>
                        <TableCell>{formatDate(item.dueDate)}</TableCell>
                        <TableCell>{formatCurrency(item.amount, detail.currencyCode)}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={statusLabel(item.status)}
                            color={statusColor(item.status)}
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell align="right">
                          {item.status === 'PENDING' && (
                            <Button
                              size="small"
                              startIcon={<PaymentIcon />}
                              onClick={() => setPayItemId(item.id)}
                            >
                              Pagar
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              {detail.status === 'ACTIVE' && detail.pendingCount > 0 && (
                <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<PayOffIcon />}
                    onClick={() => {
                      setEarlySettleId(detail.id)
                      setEarlySettleRemaining(detail.remainingAmount)
                    }}
                  >
                    Quitação antecipada
                  </Button>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<RenegotiateIcon />}
                    onClick={() => {
                      setRenegotiateId(detail.id)
                      setRenegotiateForm({
                        installmentGroupId: detail.id,
                        newTotalAmount: detail.remainingAmount,
                        newFirstDueDate: new Date().toISOString().slice(0, 10),
                        newNumberOfInstallments: detail.pendingCount,
                      })
                    }}
                  >
                    Renegociar
                  </Button>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
      </Dialog>

      {/* Confirmar pagamento de uma parcela */}
      <Dialog open={!!payItemId} onClose={() => setPayItemId(null)}>
        <DialogTitle>Pagar parcela</DialogTitle>
        <DialogContent>
          <Typography>Será criada uma transação de despesa com a data de hoje. Confirma?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPayItemId(null)}>Cancelar</Button>
          <Button variant="contained" onClick={handlePayInstallment} disabled={payMutation.isPending}>
            {payMutation.isPending ? 'Processando...' : 'Confirmar'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Quitação antecipada */}
      <Dialog open={!!earlySettleId} onClose={() => { setEarlySettleId(null); setEarlySettleRemaining(null) }}>
        <DialogTitle>Quitação antecipada</DialogTitle>
        <DialogContent>
          <Typography>
            Será criada uma única transação de despesa com o valor restante (
            {detail ? formatCurrency(detail.remainingAmount, detail.currencyCode) : earlySettleRemaining != null ? formatCurrency(earlySettleRemaining) : '...'}).
            Todas as parcelas pendentes serão marcadas como pagas. Confirma?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setEarlySettleId(null); setEarlySettleRemaining(null) }}>Cancelar</Button>
          <Button variant="contained" onClick={handleEarlySettlement} disabled={earlyMutation.isPending}>
            {earlyMutation.isPending ? 'Processando...' : 'Quitar'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Renegociar */}
      <Dialog
        open={!!renegotiateId}
        onClose={() => { setRenegotiateId(null); setRenegotiateForm({}) }}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Renegociar parcelamento</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              label="Novo valor total"
              type="number"
              fullWidth
              inputProps={{ min: 0.01, step: 0.01 }}
              InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }}
              value={renegotiateForm.newTotalAmount ?? ''}
              onChange={(e) =>
                setRenegotiateForm({ ...renegotiateForm, newTotalAmount: parseFloat(e.target.value) || 0 })
              }
            />
            <TextField
              label="Nova data da primeira parcela"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={renegotiateForm.newFirstDueDate ?? ''}
              onChange={(e) => setRenegotiateForm({ ...renegotiateForm, newFirstDueDate: e.target.value })}
            />
            <TextField
              label="Número de parcelas"
              type="number"
              fullWidth
              inputProps={{ min: 1 }}
              value={renegotiateForm.newNumberOfInstallments ?? ''}
              onChange={(e) =>
                setRenegotiateForm({
                  ...renegotiateForm,
                  newNumberOfInstallments: parseInt(e.target.value, 10) || 1,
                })
              }
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setRenegotiateId(null); setRenegotiateForm({}) }}>Cancelar</Button>
          <Button variant="contained" onClick={handleRenegotiate} disabled={renegotiateMutation.isPending}>
            {renegotiateMutation.isPending ? 'Processando...' : 'Renegociar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        <MenuItem
          onClick={() => {
            if (selectedGroup) {
              setEarlySettleId(selectedGroup.id)
              setEarlySettleRemaining(selectedGroup.remainingAmount)
            }
            handleMenuClose()
          }}
        >
          Quitação antecipada
        </MenuItem>
        <MenuItem
          onClick={() => {
            if (selectedGroup) {
              setRenegotiateId(selectedGroup.id)
              setRenegotiateForm({
                installmentGroupId: selectedGroup.id,
                newTotalAmount: selectedGroup.remainingAmount,
                newFirstDueDate: new Date().toISOString().slice(0, 10),
                newNumberOfInstallments: selectedGroup.pendingCount,
              })
            }
            handleMenuClose()
          }}
        >
          Renegociar
        </MenuItem>
        <MenuItem onClick={handleCancel} sx={{ color: 'error.main' }}>
          Cancelar parcelamento
        </MenuItem>
      </Menu>
    </Layout>
  )
}
