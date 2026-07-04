import { useState, useMemo } from 'react'
import Layout from '../components/Layout'
import {
  useTaxaCambioResumo,
  useTaxaCambioHistorico,
  useTaxaCambioGrafico,
  useAtualizarTaxaCambio,
} from '../hooks/useTaxaCambio'
import { useCurrencies, useCurrencyRates, useCreateCurrencyRate } from '../hooks/useCurrencies'
import { useErrorHandler } from '../hooks/useErrorHandler'
import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Skeleton,
  ToggleButtonGroup,
  ToggleButton,
  TablePagination,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Collapse,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import EuroIcon from '@mui/icons-material/Euro'
import AddIcon from '@mui/icons-material/Add'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts'
import type { TaxaCambioCard, TaxaCambioItem } from '../types'
import type { CurrencyRateRequest } from '../types'

const PERIOD_7 = 7
const PERIOD_30 = 30
const PERIOD_90 = 90
const PAGE_SIZES = [10, 20, 50]
const INITIAL_RATE_FORM: CurrencyRateRequest = {
  fromCurrencyCode: 'BRL',
  toCurrencyCode: 'USD',
  rate: 0.19,
}

function formatDateBR(dateStr: string | null): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function formatDateTimeBR(dateStr: string | null): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(value)
}

function CardMoeda({ card }: { card: TaxaCambioCard }) {
  const subiu = card.variacaoPercentual != null && card.variacaoPercentual >= 0
  const Icon = card.moeda === 'EUR' ? EuroIcon : AttachMoneyIcon
  return (
    <Card
      elevation={1}
      sx={{
        borderRadius: 2,
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': { boxShadow: '0 4px 12px rgba(0,0,0,0.08)' },
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Icon sx={{ color: 'text.secondary', fontSize: 28 }} />
            <Box>
              <Typography variant="subtitle1" fontWeight={600}>
                {card.nomeMoeda}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {card.moeda}
              </Typography>
            </Box>
          </Box>
          {card.variacaoPercentual != null && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {subiu ? (
                <TrendingUpIcon sx={{ color: 'success.main', fontSize: 20 }} />
              ) : (
                <TrendingDownIcon sx={{ color: 'error.main', fontSize: 20 }} />
              )}
              <Typography
                variant="body2"
                fontWeight={600}
                color={subiu ? 'success.main' : 'error.main'}
              >
                {subiu ? '+' : ''}
                {card.variacaoPercentual.toFixed(2)}%
              </Typography>
            </Box>
          )}
        </Box>
        <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>
          {formatMoney(card.valor)}
        </Typography>
        {card.sparkline && card.sparkline.length > 1 && (
          <Box sx={{ height: 40, mt: 1 }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={card.sparkline.map((v, i) => ({ x: i, valor: v }))}>
                <Area
                  type="monotone"
                  dataKey="valor"
                  stroke={subiu ? '#2e7d32' : '#d32f2f'}
                  fill={subiu ? 'rgba(46, 125, 50, 0.15)' : 'rgba(211, 47, 47, 0.15)'}
                  strokeWidth={1.5}
                />
              </AreaChart>
            </ResponsiveContainer>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

export default function CurrencyRates() {
  const {
    data: resumo,
    isLoading: loadingResumo,
    refetch: refetchResumo,
  } = useTaxaCambioResumo()
  const atualizarMutation = useAtualizarTaxaCambio()
  const { handleError, showError, showInfo, showSuccess } = useErrorHandler()

  const [periodoDias, setPeriodoDias] = useState<number>(PERIOD_30)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [expandManual, setExpandManual] = useState(false)

  const dataFim = useMemo(() => new Date().toISOString().slice(0, 10), [])
  const dataInicio = useMemo(
    () =>
      new Date(Date.now() - periodoDias * 24 * 60 * 60 * 1000)
        .toISOString()
        .slice(0, 10),
    [periodoDias]
  )

  const {
    data: graficoData,
    isLoading: loadingGrafico,
    refetch: refetchGrafico,
  } = useTaxaCambioGrafico(
    'USD',
    dataInicio,
    dataFim
  )
  const {
    data: historicoPage,
    isLoading: loadingHistorico,
    refetch: refetchHistorico,
  } = useTaxaCambioHistorico({
    dataInicio,
    dataFim,
    page,
    size: pageSize,
  })

  const handleAtualizar = async () => {
    try {
      await atualizarMutation.mutateAsync()

      const [resumoResult, graficoResult, historicoResult] = await Promise.all([
        refetchResumo(),
        refetchGrafico(),
        refetchHistorico(),
      ])
      const hasReloadError = [resumoResult, graficoResult, historicoResult].some(
        (result) => result.error
      )

      if (hasReloadError) {
        showError('Taxas atualizadas, mas houve falha ao recarregar alguns dados da tela.')
      } else {
        const ultimaAtualizacao = resumoResult.data?.ultimaAtualizacao ?? resumo?.ultimaAtualizacao ?? null
        const dataAtualizacao = ultimaAtualizacao
          ? new Date(ultimaAtualizacao).toISOString().slice(0, 10)
          : null
        const dataHoje = new Date().toISOString().slice(0, 10)

        if (dataAtualizacao && dataAtualizacao !== dataHoje) {
          showInfo(
            `BCB ainda não publicou cotação de hoje. Exibindo última disponível de ${formatDateBR(ultimaAtualizacao)}.`
          )
        } else {
          showSuccess('Taxas atualizadas com sucesso.')
        }
      }
    } catch (err) {
      handleError(err)
    }
  }

  const chartPoints = useMemo(() => {
    if (!graficoData?.length) return []
    return graficoData.map((item) => ({
      data: item.dataCotacao,
      valor: item.valor,
      name: formatDateBR(item.dataCotacao),
    }))
  }, [graficoData])

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <Box>
            <Typography variant="h4" component="h1" fontWeight={500}>
              Taxa de Câmbio
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Última atualização: {loadingResumo ? '…' : formatDateTimeBR(resumo?.ultimaAtualizacao ?? null)}
            </Typography>
          </Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleAtualizar}
            disabled={atualizarMutation.isPending}
          >
            Atualizar agora
          </Button>
        </Box>

        {/* Cards */}
        {loadingResumo ? (
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Skeleton variant="rounded" width={280} height={140} />
            <Skeleton variant="rounded" width={280} height={140} />
          </Box>
        ) : resumo?.cards?.length ? (
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            {resumo.cards.map((card) => (
              <CardMoeda key={card.moeda} card={card} />
            ))}
          </Box>
        ) : (
          <Card elevation={1} sx={{ p: 3 }}>
            <Typography color="text.secondary">
              Nenhuma cotação PTAX disponível. Use &quot;Atualizar agora&quot; para buscar cotações
              do Banco Central.
            </Typography>
          </Card>
        )}

        {/* Gráfico histórico */}
        <Card elevation={1} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Gráfico histórico
            </Typography>
            <ToggleButtonGroup
              value={periodoDias}
              exclusive
              onChange={(_, v) => v != null && setPeriodoDias(v)}
              size="small"
              sx={{ mb: 2 }}
            >
              <ToggleButton value={PERIOD_7}>7 dias</ToggleButton>
              <ToggleButton value={PERIOD_30}>30 dias</ToggleButton>
              <ToggleButton value={PERIOD_90}>90 dias</ToggleButton>
            </ToggleButtonGroup>
            {loadingGrafico ? (
              <Skeleton variant="rectangular" height={280} />
            ) : chartPoints.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={chartPoints} margin={{ top: 8, right: 16, left: 8, bottom: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="data" tickFormatter={(v) => formatDateBR(v)} />
                  <YAxis tickFormatter={(v) => `R$ ${Number(v).toFixed(2)}`} />
                  <Tooltip
                    formatter={(value: number) => [formatMoney(value), 'Valor']}
                    labelFormatter={(label) => `Data: ${label}`}
                  />
                  <Line
                    type="monotone"
                    dataKey="valor"
                    stroke="#1976d2"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                    name="Valor"
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="text.secondary">Sem dados para o período.</Typography>
            )}
          </CardContent>
        </Card>

        {/* Tabela histórico */}
        <Card elevation={1} sx={{ borderRadius: 2 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Tabela de histórico
            </Typography>
            {loadingHistorico ? (
              <Skeleton variant="rectangular" height={200} />
            ) : historicoPage?.data?.length ? (
              <>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow sx={{ bgcolor: 'action.hover' }}>
                        <TableCell sx={{ fontWeight: 600 }}>Data</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Moeda</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Valor (R$)</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Variação %</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {historicoPage.data.map((row: TaxaCambioItem) => (
                        <TableRow key={row.id}>
                          <TableCell>{formatDateBR(row.dataCotacao)}</TableCell>
                          <TableCell>{row.moeda}</TableCell>
                          <TableCell>{formatMoney(row.valor)}</TableCell>
                          <TableCell>
                            {row.variacaoPercentual != null ? (
                              <Typography
                                component="span"
                                color={
                                  row.variacaoPercentual >= 0 ? 'success.main' : 'error.main'
                                }
                                fontWeight={500}
                              >
                                {row.variacaoPercentual >= 0 ? '+' : ''}
                                {row.variacaoPercentual.toFixed(2)}%
                              </Typography>
                            ) : (
                              '—'
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <TablePagination
                  component="div"
                  count={historicoPage.totalElements ?? 0}
                  page={page}
                  onPageChange={(_, p) => setPage(p)}
                  rowsPerPage={pageSize}
                  onRowsPerPageChange={(e) => {
                    setPageSize(Number(e.target.value))
                    setPage(0)
                  }}
                  rowsPerPageOptions={PAGE_SIZES}
                  labelRowsPerPage="Registros por página"
                />
              </>
            ) : (
              <Typography color="text.secondary">Nenhum registro no período.</Typography>
            )}
          </CardContent>
        </Card>

        {/* Seção taxas manuais (colapsável) */}
        <Card elevation={1} sx={{ borderRadius: 2 }}>
          <Button
            fullWidth
            onClick={() => setExpandManual(!expandManual)}
            endIcon={expandManual ? <ExpandLessIcon /> : <ExpandMoreIcon />}
            sx={{ justifyContent: 'space-between', px: 2, py: 1.5 }}
          >
            <Typography variant="subtitle1">Configuração de taxas (manuais)</Typography>
          </Button>
          <Collapse in={expandManual}>
            <CurrencyRatesManual />
          </Collapse>
        </Card>
      </Box>
    </Layout>
  )
}

function CurrencyRatesManual() {
  const { data: currencies } = useCurrencies()
  const { data: rates, isLoading } = useCurrencyRates()
  const createMutation = useCreateCurrencyRate()
  const { handleError, showError, showSuccess } = useErrorHandler()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CurrencyRateRequest>({ ...INITIAL_RATE_FORM })

  const sameCurrency = form.fromCurrencyCode === form.toCurrencyCode
  const invalidRate = !(form.rate > 0)
  const isInvalidForm = sameCurrency || invalidRate

  const resetForm = () => setForm({ ...INITIAL_RATE_FORM })
  const handleOpenDialog = () => {
    resetForm()
    setOpen(true)
  }
  const handleCloseDialog = () => {
    setOpen(false)
    resetForm()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (sameCurrency) {
      showError('As moedas de origem e destino devem ser diferentes.')
      return
    }
    if (invalidRate) {
      showError('Informe uma taxa maior que zero.')
      return
    }
    try {
      await createMutation.mutateAsync(form)
      showSuccess('Taxa registrada com sucesso.')
      handleCloseDialog()
    } catch (err) {
      handleError(err)
    }
  }

  const uniquePairs = rates
    ? (() => {
        const getTimestamp = (value?: string) => {
          if (!value) return 0
          const parsed = new Date(value).getTime()
          return Number.isNaN(parsed) ? 0 : parsed
        }
        const byEffective = [...rates].sort(
          (a, b) =>
            getTimestamp(b.effectiveAt) - getTimestamp(a.effectiveAt) ||
            getTimestamp(b.createdAt) - getTimestamp(a.createdAt)
        )
        const latestByPair = new Map<string, (typeof byEffective)[number]>()
        byEffective.forEach((rate) => {
          const pairKey = `${rate.fromCurrencyCode}-${rate.toCurrencyCode}`
          // Como a lista já está do mais recente para o mais antigo, mantemos apenas a primeira ocorrência.
          if (!latestByPair.has(pairKey)) {
            latestByPair.set(pairKey, rate)
          }
        })
        return Array.from(latestByPair.values()).sort(
          (a, b) =>
            a.fromCurrencyCode.localeCompare(b.fromCurrencyCode) ||
            a.toCurrencyCode.localeCompare(b.toCurrencyCode)
        )
      })()
    : []

  return (
    <CardContent>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Taxas configuráveis para conversão entre moedas. Valores históricos não são alterados.
      </Typography>
      <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenDialog} size="small">
        Nova taxa
      </Button>
      {isLoading ? (
        <Typography color="text.secondary" sx={{ mt: 2 }}>
          Carregando...
        </Typography>
      ) : uniquePairs.length === 0 ? (
        <Typography color="text.secondary" sx={{ mt: 2 }}>
          Nenhuma taxa cadastrada.
        </Typography>
      ) : (
        <TableContainer sx={{ mt: 2 }}>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'action.hover' }}>
                <TableCell sx={{ fontWeight: 600 }}>De</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Para</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Taxa</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Vigente em</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {uniquePairs.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{r.fromCurrencyCode}</TableCell>
                  <TableCell>{r.toCurrencyCode}</TableCell>
                  <TableCell>
                    {Number(r.rate).toLocaleString('pt-BR', {
                      minimumFractionDigits: 4,
                      maximumFractionDigits: 6,
                    })}{' '}
                    {r.toCurrencyCode}
                  </TableCell>
                  <TableCell>{new Date(r.effectiveAt).toLocaleString('pt-BR')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={open} onClose={handleCloseDialog} maxWidth="xs" fullWidth>
        <DialogTitle>Nova taxa de câmbio</DialogTitle>
        <form onSubmit={handleSubmit}>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <FormControl size="small" fullWidth required error={sameCurrency}>
                <InputLabel>De (moeda origem)</InputLabel>
                <Select
                  value={form.fromCurrencyCode}
                  label="De (moeda origem)"
                  onChange={(e) => setForm({ ...form, fromCurrencyCode: e.target.value })}
                >
                  {(currencies || []).map((c) => (
                    <MenuItem key={c.code} value={c.code}>
                      {c.code} - {c.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" fullWidth required error={sameCurrency}>
                <InputLabel>Para (moeda destino)</InputLabel>
                <Select
                  value={form.toCurrencyCode}
                  label="Para (moeda destino)"
                  onChange={(e) => setForm({ ...form, toCurrencyCode: e.target.value })}
                >
                  {(currencies || []).map((c) => (
                    <MenuItem
                      key={c.code}
                      value={c.code}
                      disabled={c.code === form.fromCurrencyCode}
                    >
                      {c.code} - {c.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField
                label="Taxa (1 unidade de origem = X na destino)"
                type="number"
                inputProps={{ step: 'any', min: 0.00000001 }}
                value={form.rate}
                onChange={(e) => setForm({ ...form, rate: parseFloat(e.target.value) || 0 })}
                required
                fullWidth
                size="small"
                error={invalidRate}
                helperText={invalidRate ? 'A taxa deve ser maior que zero.' : ''}
              />
              {sameCurrency && (
                <Typography variant="caption" color="error.main">
                  As moedas de origem e destino devem ser diferentes.
                </Typography>
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog}>Cancelar</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending || isInvalidForm}
            >
              Salvar
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </CardContent>
  )
}
