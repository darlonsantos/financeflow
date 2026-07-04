import { useState, useMemo } from 'react'
import Layout from '../components/Layout'
import { Link } from 'react-router-dom'
import { useAccounts } from '../hooks/useAccounts'
import { useTransactions } from '../hooks/useTransactions'
import { useCurrencies, formatWithCurrency } from '../hooks/useCurrencies'
import { useBudgets } from '../hooks/useBudgets'
import { useGoals } from '../hooks/useGoals'
import { useBalanceProjection } from '../hooks/useProjections'
import { useGamification } from '../hooks/useGamification'
import { useInsights } from '../hooks/useInsights'
import { ChartSkeleton, LoadingSection } from '../components/Loading'
import InsightsWidget from '../components/InsightsWidget'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Chip,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Typography,
  useTheme,
} from '@mui/material'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import FavoriteIcon from '@mui/icons-material/Favorite'
import CloseIcon from '@mui/icons-material/Close'
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'

// Cores semânticas Material Design
const COLORS = {
  income: '#4CAF50',
  expense: '#F44336',
  balance: '#2196F3',
}

const PERIOD_MONTH = 'month'
const PERIOD_YEAR = 'year'
type PeriodFilter = typeof PERIOD_MONTH | typeof PERIOD_YEAR

export default function Dashboard() {
  const theme = useTheme()
  const [periodFilter, setPeriodFilter] = useState<PeriodFilter>(PERIOD_MONTH)
  const [achievementsDialogOpen, setAchievementsDialogOpen] = useState(false)
  const now = new Date()

  const handlePeriodChange = (_e: React.MouseEvent<HTMLElement>, newValue: string | null) => {
    if (newValue === PERIOD_MONTH || newValue === PERIOD_YEAR) {
      setPeriodFilter(newValue)
    }
  }

  const { data: accounts, isLoading: accountsLoading } = useAccounts()
  const { data: currencies } = useCurrencies()
  const { data: transactionsData, isLoading: transactionsLoading } = useTransactions({ size: 10 })
  const { data: allTransactionsData } = useTransactions({ size: 10000 })
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const { data: budgets } = useBudgets(currentMonth)
  const { data: goals } = useGoals('ACTIVE')
  const { data: balanceProjection, isLoading: projectionLoading } = useBalanceProjection(12)
  const { data: gamification, isLoading: gamificationLoading } = useGamification()

  const allTransactions = allTransactionsData?.data || []
  const recentTransactions = transactionsData?.data || []
  const insights = useInsights(allTransactions, goals ?? undefined)
  const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1)
  const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0)
  const currentYearStart = new Date(now.getFullYear(), 0, 1)
  const lastYearStart = new Date(now.getFullYear() - 1, 0, 1)
  const lastYearEnd = new Date(now.getFullYear() - 1, 11, 31)

  const currentPeriodTransactions = useMemo(() => {
    const startDate = periodFilter === 'month' ? currentMonthStart : currentYearStart
    return allTransactions.filter((t) => new Date(t.date) >= startDate)
  }, [allTransactions, periodFilter, currentMonthStart, currentYearStart])

  const previousPeriodTransactions = useMemo(() => {
    const startDate = periodFilter === 'month' ? lastMonthStart : lastYearStart
    const endDate = periodFilter === 'month' ? lastMonthEnd : lastYearEnd
    return allTransactions.filter(
      (t) => new Date(t.date) >= startDate && new Date(t.date) <= endDate
    )
  }, [allTransactions, periodFilter, lastMonthStart, lastMonthEnd, lastYearStart, lastYearEnd])

  // Chaves de série recorrente: para receitas/despesas recorrentes, nos cards mostramos só o valor do mês atual
  const recurringSeriesKeys = useMemo(() => {
    const set = new Set<string>()
    allTransactions.forEach((t) => {
      if (t.recurring) {
        const key = `${t.description ?? ''}|${t.accountId}|${t.categoryId}|${t.amount}|${t.type}`
        set.add(key)
      }
    })
    return set
  }, [allTransactions])

  const isRecurringSeries = (t: { description?: string; accountId: string; categoryId: string; amount: number; type: string; recurring?: boolean }) => {
    const key = `${t.description ?? ''}|${t.accountId}|${t.categoryId}|${t.amount}|${t.type}`
    return t.recurring === true || recurringSeriesKeys.has(key)
  }

  const currentMonthEnd = useMemo(
    () => new Date(now.getFullYear(), now.getMonth() + 1, 0),
    [now]
  )

  // Lista de Transações Recentes: despesas/receitas recorrentes só do mês atual; demais todas
  const displayRecentTransactions = useMemo(() => {
    return recentTransactions.filter((t) => {
      const isRecurringIncomeOrExpense =
        (t.type === 'INCOME' || t.type === 'EXPENSE') &&
        (t.recurring === true || recurringSeriesKeys.has(`${t.description ?? ''}|${t.accountId}|${t.categoryId}|${t.amount}|${t.type}`))
      if (isRecurringIncomeOrExpense) {
        const d = new Date(t.date)
        return d >= currentMonthStart && d <= currentMonthEnd
      }
      return true
    })
  }, [recentTransactions, currentMonthStart, currentMonthEnd, recurringSeriesKeys])

  // Receitas/Despesas dos cards: recorrentes contam só no mês atual; demais no período selecionado
  const totalIncome = useMemo(() => {
    return currentPeriodTransactions
      .filter((t) => t.type === 'INCOME')
      .reduce((sum, t) => {
        const date = new Date(t.date)
        const inCurrentMonth = date >= currentMonthStart && date <= currentMonthEnd
        if (isRecurringSeries(t)) {
          return sum + (inCurrentMonth ? t.amount : 0)
        }
        return sum + t.amount
      }, 0)
  }, [currentPeriodTransactions, currentMonthStart, currentMonthEnd, recurringSeriesKeys])

  const totalExpense = useMemo(() => {
    return currentPeriodTransactions
      .filter((t) => t.type === 'EXPENSE')
      .reduce((sum, t) => {
        const date = new Date(t.date)
        const inCurrentMonth = date >= currentMonthStart && date <= currentMonthEnd
        if (isRecurringSeries(t)) {
          return sum + (inCurrentMonth ? t.amount : 0)
        }
        return sum + t.amount
      }, 0)
  }, [currentPeriodTransactions, currentMonthStart, currentMonthEnd, recurringSeriesKeys])

  const totalBalance = useMemo(() => {
    if (!accounts || accounts.length === 0) return 0
    return accounts.reduce((sum, account) => sum + account.balance, 0)
  }, [accounts])

  const previousIncome = previousPeriodTransactions
    .filter((t) => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0)

  const previousExpense = previousPeriodTransactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0)

  const incomeVariation =
    previousIncome > 0 ? ((totalIncome - previousIncome) / previousIncome) * 100 : 0
  const expenseVariation =
    previousExpense > 0 ? ((totalExpense - previousExpense) / previousExpense) * 100 : 0

  const monthlyData = useMemo(() => {
    const months: Record<
      string,
      { income: number; expense: number; balance: number }
    > = {}

    for (let i = 5; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      months[monthKey] = { income: 0, expense: 0, balance: 0 }
    }

    allTransactions.forEach((transaction) => {
      const date = new Date(transaction.date)
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

      if (months[monthKey]) {
        if (transaction.type === 'INCOME') {
          months[monthKey].income += transaction.amount
        } else {
          months[monthKey].expense += transaction.amount
        }
        months[monthKey].balance =
          months[monthKey].income - months[monthKey].expense
      }
    })

    return Object.entries(months).map(([month, data]) => ({
      month: new Date(month + '-01').toLocaleDateString('pt-BR', {
        month: 'short',
        year: 'numeric',
      }),
      ...data,
    }))
  }, [allTransactions, now])

  const categoryData = useMemo(() => {
    const categoryMap: Record<
      string,
      { name: string; income: number; expense: number }
    > = {}

    currentPeriodTransactions.forEach((transaction) => {
      const key = transaction.categoryId
      if (!categoryMap[key]) {
        categoryMap[key] = {
          name: transaction.categoryName,
          income: 0,
          expense: 0,
        }
      }

      if (transaction.type === 'INCOME') {
        categoryMap[key].income += transaction.amount
      } else {
        categoryMap[key].expense += transaction.amount
      }
    })

    return Object.values(categoryMap)
      .map((cat) => ({
        name: cat.name,
        value: cat.expense || cat.income,
        type: cat.expense > 0 ? 'Despesa' : 'Receita',
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5)
  }, [currentPeriodTransactions])

  const typeDistribution = useMemo(
    () => [
      { name: 'Receitas', value: totalIncome, color: COLORS.income },
      { name: 'Despesas', value: totalExpense, color: COLORS.expense },
    ],
    [totalIncome, totalExpense]
  )

  const budgetChartData = useMemo(() => {
    if (!budgets || budgets.length === 0) return []
    return budgets.map((b) => ({
      name: b.categoryName,
      limit: b.limitAmount,
      spent: b.spentAmount,
      percent: b.percentUsed,
    }))
  }, [budgets])

  const goalChartData = useMemo(() => {
    if (!goals || goals.length === 0) return []
    return goals.map((g) => ({
      name: g.name,
      value: g.percentComplete,
      percent: g.percentComplete,
    }))
  }, [goals])

  const goalChartMax = useMemo(() => {
    if (goalChartData.length === 0) return 100
    const max = Math.max(...goalChartData.map((g) => g.value))
    return Math.max(100, Math.ceil(max / 10) * 10)
  }, [goalChartData])

  const projectionChartData = useMemo(() => {
    if (!balanceProjection) return []
    const current = {
      month: 'Atual',
      balance: balanceProjection.currentBalance,
      saldo: balanceProjection.currentBalance,
    }
    const projected = balanceProjection.projections.map((p) => ({
      month: p.monthLabel,
      balance: p.balance,
      saldo: p.balance,
    }))
    return [current, ...projected]
  }, [balanceProjection])

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value)
  }

  const formatPercent = (value: number) => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`
  }

  // 4.5 Gráficos - Tooltip: Surface container com elevation 2 (Recharts contentStyle exige CSS string para boxShadow)
  const tooltipBg = theme.palette.background.paper
  const tooltipBorder = theme.palette.divider
  const tooltipColor = theme.palette.text.primary
  const tooltipShadow = theme.shadows[2] as string

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}> {/* 5. Gap entre cards: 24px */}
        {/* 4.4 Filtros de Período - Abaixo do título Dashboard */}
        <Box>
          <Typography variant="h4" component="h1" fontWeight={500} sx={{ mb: 2 }}>
            Dashboard
          </Typography>
          <ToggleButtonGroup
            value={periodFilter}
            exclusive
            onChange={handlePeriodChange}
            size="small"
            sx={{
              '& .MuiToggleButton-root': {
                textAlign: 'center',
                textTransform: 'none',
                fontWeight: 500,
                typography: 'subtitle1',
                border: '1px solid',
                borderColor: 'divider',
                transition: 'all 0.2s ease',
                '&:hover': {
                  bgcolor: 'action.hover',
                },
                '&.Mui-selected': {
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  borderColor: 'primary.main',
                  '&:hover': {
                    bgcolor: 'primary.dark',
                    borderColor: 'primary.dark',
                  },
                },
              },
            }}
          >
            <ToggleButton value={PERIOD_MONTH} aria-label="Filtrar por mês atual">Mês Atual</ToggleButton>
            <ToggleButton value={PERIOD_YEAR} aria-label="Filtrar por ano atual">Ano Atual</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {/* Cards de Resumo - 4.3 | 5. Grid 12 colunas, gap 24px */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Card
              elevation={1}
              sx={{
                height: '100%',
                borderRadius: 2,
                transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': { boxShadow: 2 },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      bgcolor: (t) => alpha(t.palette.primary.main, 0.12),
                      color: 'primary.main',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <AccountBalanceWalletIcon sx={{ fontSize: 28 }} />
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" color="text.secondary">
                      Saldo Total
                    </Typography>
                    <Typography
                      variant="h5"
                      fontWeight={600}
                      sx={{
                        color:
                          totalBalance >= 0 ? 'text.primary' : 'error.main',
                      }}
                    >
                      {formatCurrency(totalBalance)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total de todas as contas
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Card
              elevation={1}
              sx={{
                height: '100%',
                borderRadius: 2,
                transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': { boxShadow: 2 },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      bgcolor: (t) => alpha(t.palette.success.main, 0.12),
                      color: 'success.main',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <TrendingUpIcon sx={{ fontSize: 28 }} />
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" color="text.secondary">
                      Receitas
                    </Typography>
                    <Typography variant="h5" fontWeight={600} color="success.main">
                      {formatCurrency(totalIncome)}
                    </Typography>
                    {previousIncome > 0 && (
                      <Box sx={{ mt: 0.5, display: 'flex', gap: 0.5, flexWrap: 'wrap', alignItems: 'center' }}>
                        <Chip
                          label={formatPercent(incomeVariation)}
                          size="small"
                          color={incomeVariation >= 0 ? 'success' : 'error'}
                          sx={{ height: 20, fontSize: '0.7rem' }}
                        />
                        <Typography variant="body2" color="text.secondary">
                          vs {periodFilter === 'month' ? 'mês anterior' : 'ano anterior'}
                        </Typography>
                      </Box>
                    )}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Card
              elevation={1}
              sx={{
                height: '100%',
                borderRadius: 2,
                transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': { boxShadow: 2 },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      bgcolor: (t) => alpha(t.palette.error.main, 0.12),
                      color: 'error.main',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <TrendingDownIcon sx={{ fontSize: 28 }} />
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" color="text.secondary">
                      Despesas
                    </Typography>
                    <Typography variant="h5" fontWeight={600} color="error.main">
                      {formatCurrency(totalExpense)}
                    </Typography>
                    {previousExpense > 0 && (
                      <Box sx={{ mt: 0.5, display: 'flex', gap: 0.5, flexWrap: 'wrap', alignItems: 'center' }}>
                        <Chip
                          label={formatPercent(expenseVariation)}
                          size="small"
                          color={expenseVariation <= 0 ? 'success' : 'error'}
                          sx={{ height: 20, fontSize: '0.7rem' }}
                        />
                        <Typography variant="body2" color="text.secondary">
                          vs {periodFilter === 'month' ? 'mês anterior' : 'ano anterior'}
                        </Typography>
                      </Box>
                    )}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Insights automáticos */}
        {insights.length > 0 && (
          <InsightsWidget insights={insights} />
        )}

        {/* Gamificação - Score, Streak e Conquistas */}
        <Card
          elevation={1}
          sx={{
            borderRadius: 2,
            transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            '&:hover': { boxShadow: 2 },
          }}
        >
          <CardHeader
            avatar={
              <Box
                sx={{
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  bgcolor: (t) => alpha(t.palette.secondary.main, 0.12),
                  color: 'secondary.main',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <EmojiEventsIcon />
              </Box>
            }
            title="Sua Jornada Financeira"
            subheader="Score de saúde, streak e conquistas"
            titleTypographyProps={{ variant: 'h6' }}
            action={
              gamification && gamification.achievements && gamification.achievements.length > 0 ? (
                <Button
                  size="small"
                  onClick={() => setAchievementsDialogOpen(true)}
                  sx={{ textTransform: 'none' }}
                >
                  Ver histórico
                </Button>
              ) : null
            }
            sx={{ px: 3, pt: 3, pb: 0 }}
          />
          <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
            {gamificationLoading ? (
              <LoadingSection />
            ) : gamification ? (
              <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <Box
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      bgcolor: (t) => alpha(t.palette.primary.main, 0.06),
                      border: '1px solid',
                      borderColor: (t) => alpha(t.palette.primary.main, 0.2),
                      textAlign: 'center',
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5, mb: 1 }}>
                      <FavoriteIcon sx={{ color: 'primary.main', fontSize: 20 }} />
                      <Typography variant="overline" color="text.secondary">
                        Saúde Financeira
                      </Typography>
                    </Box>
                    <Typography variant="h4" fontWeight={700} color="primary.main">
                      {gamification.healthScore}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      de 100
                    </Typography>
                  </Box>
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <Box
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      bgcolor: (t) => alpha(t.palette.warning.main, 0.06),
                      border: '1px solid',
                      borderColor: (t) => alpha(t.palette.warning.main, 0.2),
                      textAlign: 'center',
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5, mb: 1 }}>
                      <LocalFireDepartmentIcon sx={{ color: 'warning.main', fontSize: 20 }} />
                      <Typography variant="overline" color="text.secondary">
                        Streak
                      </Typography>
                    </Box>
                    <Typography variant="h4" fontWeight={700} color="warning.main">
                      {gamification.currentStreak}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      dias consecutivos
                    </Typography>
                  </Box>
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <Box
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      bgcolor: (t) => alpha(t.palette.secondary.main, 0.06),
                      border: '1px solid',
                      borderColor: (t) => alpha(t.palette.secondary.main, 0.2),
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1.5 }}>
                      <EmojiEventsIcon sx={{ color: 'secondary.main', fontSize: 20 }} />
                      <Typography variant="overline" color="text.secondary">
                        Conquistas Recentes
                      </Typography>
                    </Box>
                    {gamification.recentAchievements && gamification.recentAchievements.length > 0 ? (
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        {gamification.recentAchievements.slice(0, 3).map((a) => (
                          <Box
                            key={a.code}
                            sx={{
                              py: 0.75,
                              px: 1.5,
                              borderRadius: 1,
                              bgcolor: 'background.default',
                              border: '1px solid',
                              borderColor: 'divider',
                            }}
                          >
                            <Typography variant="body2" fontWeight={600}>
                              {a.title}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {a.description}
                            </Typography>
                          </Box>
                        ))}
                        {gamification.achievements && gamification.achievements.length > 3 && (
                          <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                            +{Math.max(0, gamification.achievements.length - 3)} outras conquistas
                          </Typography>
                        )}
                      </Box>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        Complete ações para desbloquear conquistas
                      </Typography>
                    )}
                  </Box>
                </Grid>
              </Grid>
            ) : (
              <Typography variant="body2" color="text.secondary">
                Carregando sua jornada...
              </Typography>
            )}
          </CardContent>
        </Card>

        {/* Dialog de histórico de conquistas */}
        <Dialog
          open={achievementsDialogOpen}
          onClose={() => setAchievementsDialogOpen(false)}
          maxWidth="sm"
          fullWidth
          PaperProps={{ sx: { borderRadius: 2 } }}
        >
          <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <EmojiEventsIcon color="secondary" />
              <span>Histórico de Conquistas</span>
            </Box>
            <IconButton onClick={() => setAchievementsDialogOpen(false)} size="small">
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent dividers sx={{ pt: 0 }}>
            {gamification?.achievements && gamification.achievements.length > 0 ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {gamification.achievements.map((a) => (
                  <Box
                    key={a.code}
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      bgcolor: 'action.hover',
                      border: '1px solid',
                      borderColor: 'divider',
                    }}
                  >
                    <Typography variant="subtitle1" fontWeight={600}>
                      {a.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {a.description}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                      Desbloqueada em {new Date(a.unlockedAt).toLocaleDateString('pt-BR', {
                        day: '2-digit',
                        month: 'long',
                        year: 'numeric',
                      })}
                    </Typography>
                  </Box>
                ))}
              </Box>
            ) : (
              <Typography color="text.secondary">
                Nenhuma conquista desbloqueada ainda. Continue registrando transações e atingindo metas!
              </Typography>
            )}
          </DialogContent>
        </Dialog>

        {/* 4.5 Gráficos | 5. Gap 24px */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card elevation={1}>
              <CardHeader
                title="Evolução Financeira (Últimos 6 Meses)"
                titleTypographyProps={{ variant: 'h6' }}
                sx={{ px: 3, pt: 3, pb: 0 }}
              />
              <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
                {transactionsLoading ? (
                  <ChartSkeleton />
                ) : (
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={monthlyData}>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke={theme.palette.divider}
                      />
                      <XAxis dataKey="month" tick={{ fill: theme.palette.text.secondary }} />
                      <YAxis
                        tick={{ fill: theme.palette.text.secondary }}
                        tickFormatter={(v) => `R$ ${(v / 1000).toFixed(0)}k`}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          border: `1px solid ${tooltipBorder}`,
                          borderRadius: 12,
                          color: tooltipColor,
                          boxShadow: tooltipShadow,
                        }}
                        formatter={(value: number) => formatCurrency(value)}
                      />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey="income"
                        stroke={COLORS.income}
                        strokeWidth={2}
                        name="Receitas"
                        dot={{ r: 4 }}
                      />
                      <Line
                        type="monotone"
                        dataKey="expense"
                        stroke={COLORS.expense}
                        strokeWidth={2}
                        name="Despesas"
                        dot={{ r: 4 }}
                      />
                      <Line
                        type="monotone"
                        dataKey="balance"
                        stroke={COLORS.balance}
                        strokeWidth={2}
                        name="Saldo"
                        strokeDasharray="5 5"
                        dot={{ r: 4 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card elevation={1}>
              <CardHeader
                title="Distribuição Receitas vs Despesas"
                titleTypographyProps={{ variant: 'h6' }}
                sx={{ px: 3, pt: 3, pb: 0 }}
              />
              <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
                {transactionsLoading ? (
                  <ChartSkeleton />
                ) : (
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={typeDistribution}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent }) =>
                          `${name}: ${(percent * 100).toFixed(0)}%`
                        }
                        outerRadius={100}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {typeDistribution.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
<Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          border: `1px solid ${tooltipBorder}`,
                          borderRadius: 12,
                          color: tooltipColor,
                          boxShadow: tooltipShadow,
                        }}
                        formatter={(value: number) => formatCurrency(value)}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Projeção de Saldo Futuro */}
        <Card elevation={1}>
          <CardHeader
            title="Projeção de Saldo (Próximos 12 Meses)"
            titleTypographyProps={{ variant: 'h6' }}
            subheader="Baseado em transações recorrentes e média histórica"
            sx={{ px: 3, pt: 3, pb: 0 }}
          />
          <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
            {projectionLoading ? (
              <ChartSkeleton />
            ) : projectionChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={projectionChartData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={theme.palette.divider}
                  />
                  <XAxis dataKey="month" tick={{ fill: theme.palette.text.secondary }} />
                  <YAxis
                    tick={{ fill: theme.palette.text.secondary }}
                    tickFormatter={(v) => `R$ ${(v / 1000).toFixed(0)}k`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: tooltipBg,
                      border: `1px solid ${tooltipBorder}`,
                      borderRadius: 12,
                      color: tooltipColor,
                      boxShadow: tooltipShadow,
                    }}
                    formatter={(value: number) => formatCurrency(value)}
                    labelFormatter={(label) => (label === 'Atual' ? 'Saldo atual' : label)}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="balance"
                    stroke={COLORS.balance}
                    strokeWidth={2}
                    name="Saldo projetado"
                    dot={{ r: 4 }}
                    connectNulls
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  Nenhum dado para projeção
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>

        {/* Top 5 Categorias */}
        {categoryData.length > 0 && (
          <Card elevation={1}>
            <CardHeader
              title={`Top 5 Categorias (${periodFilter === 'month' ? 'Este Mês' : 'Este Ano'})`}
              titleTypographyProps={{ variant: 'h6' }}
              sx={{ px: 3, pt: 3, pb: 0 }}
            />
            <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
              {transactionsLoading ? (
                <ChartSkeleton />
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={categoryData} layout="vertical">
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={theme.palette.divider}
                    />
                    <XAxis
                      type="number"
                      tick={{ fill: theme.palette.text.secondary }}
                      tickFormatter={(v) => `R$ ${(v / 1000).toFixed(0)}k`}
                    />
                    <YAxis
                      type="category"
                      dataKey="name"
                      tick={{ fill: theme.palette.text.secondary }}
                      width={120}
                    />
                    <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          border: `1px solid ${tooltipBorder}`,
                          borderRadius: 12,
                          color: tooltipColor,
                          boxShadow: tooltipShadow,
                        }}
                        formatter={(value: number) => formatCurrency(value)}
                    />
                    <Bar
                      dataKey="value"
                      fill={COLORS.balance}
                      radius={[0, 8, 8, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        )}

        {/* Orçamentos e Metas - Gráficos */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card elevation={1}>
              <CardHeader
                title="Orçamentos do Mês"
                titleTypographyProps={{ variant: 'h6' }}
                action={
                  <Button component={Link} to="/budgets" size="small" color="primary">
                    Ver todos
                  </Button>
                }
                sx={{ px: 3, pt: 3, pb: 0 }}
              />
              <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
                {budgetChartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={budgetChartData} layout="vertical" margin={{ left: 8, right: 8 }}>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke={theme.palette.divider}
                      />
                      <XAxis
                        type="number"
                        tick={{ fill: theme.palette.text.secondary }}
                        tickFormatter={(v) => `R$ ${(v / 1000).toFixed(0)}k`}
                      />
                      <YAxis
                        type="category"
                        dataKey="name"
                        tick={{ fill: theme.palette.text.secondary }}
                        width={120}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          border: `1px solid ${tooltipBorder}`,
                          borderRadius: 12,
                          color: tooltipColor,
                          boxShadow: tooltipShadow,
                        }}
                        formatter={(value: number) => formatCurrency(value)}
                        content={({ active, payload }) => {
                          if (active && payload?.length && payload[0].payload) {
                            const p = payload[0].payload
                            return (
                              <Box
                                sx={{
                                  p: 2,
                                  bgcolor: tooltipBg,
                                  border: `1px solid ${tooltipBorder}`,
                                  borderRadius: 2,
                                  boxShadow: 2,
                                }}
                              >
                                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                                  {p.name}
                                </Typography>
                                <Typography variant="body2">
                                  Limite: {formatCurrency(p.limit)}
                                </Typography>
                                <Typography variant="body2">
                                  Gasto: {formatCurrency(p.spent)}
                                </Typography>
                                <Typography variant="body2" fontWeight={600}>
                                  {p.percent.toFixed(0)}%
                                </Typography>
                              </Box>
                            )
                          }
                          return null
                        }}
                      />
                      <Legend />
                      <Bar
                        dataKey="limit"
                        fill={theme.palette.mode === 'dark' ? 'rgba(33, 150, 243, 0.3)' : 'rgba(33, 150, 243, 0.2)'}
                        radius={[0, 8, 8, 0]}
                        name="Limite"
                      />
                      <Bar
                        dataKey="spent"
                        fill={COLORS.expense}
                        radius={[0, 8, 8, 0]}
                        name="Gasto"
                      />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <Box sx={{ py: 4, textAlign: 'center' }}>
                    <Typography color="text.secondary">Nenhum orçamento este mês</Typography>
                    <Button component={Link} to="/budgets" variant="outlined" size="small" sx={{ mt: 2 }}>
                      Criar orçamento
                    </Button>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card elevation={1}>
              <CardHeader
                title="Metas Ativas"
                titleTypographyProps={{ variant: 'h6' }}
                action={
                  <Button component={Link} to="/goals" size="small" color="primary">
                    Ver todas
                  </Button>
                }
                sx={{ px: 3, pt: 3, pb: 0 }}
              />
              <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
                {goalChartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart
                      data={goalChartData}
                      layout="vertical"
                      margin={{ left: 8, right: 8 }}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke={theme.palette.divider}
                      />
                      <XAxis
                        type="number"
                        domain={[0, goalChartMax]}
                        tick={{ fill: theme.palette.text.secondary }}
                        tickFormatter={(v) => `${v}%`}
                      />
                      <YAxis
                        type="category"
                        dataKey="name"
                        tick={{ fill: theme.palette.text.secondary }}
                        width={120}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          border: `1px solid ${tooltipBorder}`,
                          borderRadius: 12,
                          color: tooltipColor,
                          boxShadow: tooltipShadow,
                        }}
                        formatter={(value: number) => `${value}%`}
                        content={({ active, payload }) => {
                          if (active && payload?.length && payload[0].payload) {
                            const p = payload[0].payload
                            return (
                              <Box
                                sx={{
                                  p: 2,
                                  bgcolor: tooltipBg,
                                  border: `1px solid ${tooltipBorder}`,
                                  borderRadius: 2,
                                  boxShadow: 2,
                                }}
                              >
                                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                                  {p.name}
                                </Typography>
                                <Typography variant="body2" fontWeight={600}>
                                  {p.percent.toFixed(0)}% concluído
                                </Typography>
                              </Box>
                            )
                          }
                          return null
                        }}
                      />
                      <Bar
                        dataKey="value"
                        fill={COLORS.balance}
                        radius={[0, 8, 8, 0]}
                        name="Progresso"
                      />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <Box sx={{ py: 4, textAlign: 'center' }}>
                    <Typography color="text.secondary">Nenhuma meta ativa</Typography>
                    <Button component={Link} to="/goals" variant="outlined" size="small" sx={{ mt: 2 }}>
                      Criar meta
                    </Button>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Contas */}
        <Card elevation={1}>
          <CardHeader
            title="Contas"
            titleTypographyProps={{ variant: 'h6' }}
            sx={{ px: 3, pt: 3, pb: 0 }}
          />
          <CardContent sx={{ px: 3, pb: 3 }}>
            {accountsLoading ? (
              <LoadingSection message="Carregando contas..." />
            ) : accounts && accounts.length > 0 ? (
              <Grid container spacing={3}>
                {accounts.map((account) => (
                  <Grid size={{ xs: 12, md: 6, lg: 4 }} key={account.id}>
                    <Card
                      variant="outlined"
                      elevation={0}
                      sx={{
                        transition: 'box-shadow 0.2s ease-in-out, border-color 0.2s ease-in-out',
                        '&:hover': {
                          boxShadow: 2,
                          borderColor: 'primary.main',
                        },
                      }}
                    >
                      <CardContent>
                        <Box
                          sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'flex-start',
                          }}
                        >
                          <Box>
                            <Typography variant="h6" fontWeight={600}>
                              {account.name}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {account.type.toLowerCase()}
                            </Typography>
                          </Box>
                          <Typography
                            variant="h6"
                            fontWeight={600}
                            color={
                              account.balance >= 0 ? 'text.primary' : 'error.main'
                            }
                          >
                            {formatWithCurrency(account.balance, account.currencyCode || 'BRL', currencies)}
                          </Typography>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            ) : (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  Nenhuma conta cadastrada
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>

        {/* 4.6 Tabela de Transações */}
        <Card elevation={1}>
          <CardHeader
            title="Transações Recentes"
            titleTypographyProps={{ variant: 'h6' }}
            sx={{ px: 3, pt: 3, pb: 0 }}
          />
          <CardContent sx={{ p: 0 }}>
            {transactionsLoading ? (
              <Box sx={{ p: 3 }}>
                <LoadingSection message="Carregando transações..." />
              </Box>
            ) : displayRecentTransactions.length > 0 ? (
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
                      <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                        Data
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                        Descrição
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                        Categoria
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                        Conta
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }} align="right">
                        Valor
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {displayRecentTransactions.map((transaction) => (
                      <TableRow
                        key={transaction.id}
                        hover
                        sx={{
                          '&:hover': {
                            bgcolor: (t) => alpha(t.palette.primary.main, 0.08),
                          },
                        }}
                      >
                        <TableCell sx={{ color: 'text.secondary' }}>
                          {new Date(transaction.date).toLocaleDateString('pt-BR', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                          })}
                        </TableCell>
                        <TableCell sx={{ fontWeight: 500 }}>
                          {transaction.description || '-'}
                        </TableCell>
                        <TableCell sx={{ color: 'text.secondary' }}>
                          {transaction.categoryName}
                        </TableCell>
                        <TableCell sx={{ color: 'text.secondary' }}>
                          {transaction.accountName}
                        </TableCell>
                        <TableCell
                          align="right"
                          sx={{
                            typography: 'body1',
                            fontWeight: 500,
                            color:
                              transaction.type === 'INCOME'
                                ? 'success.main'
                                : 'error.main',
                          }}
                        >
                          {transaction.type === 'INCOME' ? '+' : '-'}
                          {formatWithCurrency(transaction.amount, transaction.currencyCode || 'BRL', currencies)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  Nenhuma transação encontrada
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </Box>
    </Layout>
  )
}
