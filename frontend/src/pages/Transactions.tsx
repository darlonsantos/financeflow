import { useState, useMemo } from 'react'
import React from 'react'
import Layout from '../components/Layout'
import { useTransactions, useDeleteTransaction, useDeleteTransactionsBatch } from '../hooks/useTransactions'
import { useAccounts } from '../hooks/useAccounts'
import { useCategories } from '../hooks/useCategories'
import { useCurrencies, formatWithCurrency } from '../hooks/useCurrencies'
import { useErrorHandler } from '../hooks/useErrorHandler'
import TransactionForm from '../components/TransactionForm'
import { TransactionListSkeleton, EmptyState } from '../components/Loading'
import { useDebounce } from '../hooks/useDebounce'
import type { Transaction, AIReport } from '../types'
import api from '../services/api'
import {
  BarChart,
  Bar,
  LineChart,
  Line,
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
import * as XLSX from 'xlsx'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputAdornment,
  InputLabel,
  Menu,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  useTheme,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import BarChartIcon from '@mui/icons-material/BarChart'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import FilterListIcon from '@mui/icons-material/FilterList'
import GridViewIcon from '@mui/icons-material/GridView'
import ListIcon from '@mui/icons-material/List'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import DownloadIcon from '@mui/icons-material/Download'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import FirstPageIcon from '@mui/icons-material/FirstPage'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import LastPageIcon from '@mui/icons-material/LastPage'

// Funções auxiliares para filtros rápidos
const getDateRange = (range: string) => {
  const today = new Date()
  const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  
  switch (range) {
    case 'today':
      return {
        dateFrom: startOfDay.toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    case 'thisWeek':
      const startOfWeek = new Date(startOfDay)
      startOfWeek.setDate(startOfDay.getDate() - startOfDay.getDay())
      return {
        dateFrom: startOfWeek.toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    case 'thisMonth':
      return {
        dateFrom: new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    case 'last30Days':
      const last30Days = new Date(startOfDay)
      last30Days.setDate(startOfDay.getDate() - 30)
      return {
        dateFrom: last30Days.toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    case 'last3Months':
      const last3Months = new Date(startOfDay)
      last3Months.setMonth(startOfDay.getMonth() - 3)
      return {
        dateFrom: last3Months.toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    case 'thisYear':
      return {
        dateFrom: new Date(today.getFullYear(), 0, 1).toISOString().split('T')[0],
        dateTo: today.toISOString().split('T')[0],
      }
    default:
      return { dateFrom: '', dateTo: '' }
  }
}

export default function Transactions() {
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null)
  
  // Filtros suportados pela API
  const [filters, setFilters] = useState({
    accountId: '',
    categoryId: '',
    type: '' as '' | 'INCOME' | 'EXPENSE',
    dateFrom: '',
    dateTo: '',
  })
  
  // Filtros adicionais (aplicados no frontend)
  const [searchText, setSearchText] = useState('')
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [minValue, setMinValue] = useState('')
  const [maxValue, setMaxValue] = useState('')
  const [onlyRecurring, setOnlyRecurring] = useState(false)
  
  // Visualização e agrupamento
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table')
  const [groupBy, setGroupBy] = useState<'none' | 'date' | 'category' | 'account' | 'type'>('none')
  const [groupByDateType, setGroupByDateType] = useState<'day' | 'week' | 'month'>('day')
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())
  const [sortConfig, setSortConfig] = useState<{ field: string; direction: 'asc' | 'desc' } | null>(null)
  const [showCharts, setShowCharts] = useState(false)
  
  // Paginação e seleção
  const [pageSize, setPageSize] = useState(20)
  const [selectedTransactions, setSelectedTransactions] = useState<Set<string>>(new Set())
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [transactionToDelete, setTransactionToDelete] = useState<Transaction | null>(null)
  const [showDuplicateModal, setShowDuplicateModal] = useState(false)
  const [transactionToDuplicate, setTransactionToDuplicate] = useState<Transaction | null>(null)
  const [exportAnchorEl, setExportAnchorEl] = useState<null | HTMLElement>(null)
  const [aiReportOpen, setAiReportOpen] = useState(false)
  const [aiReportLoading, setAiReportLoading] = useState(false)
  const [aiReport, setAiReport] = useState<AIReport | null>(null)
  
  // Debounce da busca
  const debouncedSearchText = useDebounce(searchText, 300)

  // Buscar todas as transações filtradas para calcular totais e aplicar filtros do frontend
  const { data: allFilteredData, isLoading } = useTransactions({
    size: 10000,
    ...(filters.accountId && { accountId: filters.accountId }),
    ...(filters.categoryId && { categoryId: filters.categoryId }),
    ...(filters.type && { type: filters.type }),
    ...(filters.dateFrom && { dateFrom: filters.dateFrom }),
    ...(filters.dateTo && { dateTo: filters.dateTo }),
  })
  
  // Obter todas as tags únicas das transações
  const allTags = useMemo(() => {
    const tagsSet = new Set<string>()
    allFilteredData?.data.forEach(transaction => {
      transaction.tags?.forEach(tag => tagsSet.add(tag))
    })
    return Array.from(tagsSet).sort()
  }, [allFilteredData])
  
  // Aplicar filtros do frontend
  const filteredTransactions = useMemo(() => {
    let transactions = allFilteredData?.data || []
    
    // Busca por texto
    if (debouncedSearchText) {
      const searchLower = debouncedSearchText.toLowerCase()
      transactions = transactions.filter(t => 
        t.description?.toLowerCase().includes(searchLower) ||
        t.accountName?.toLowerCase().includes(searchLower) ||
        t.categoryName?.toLowerCase().includes(searchLower) ||
        t.tags?.some(tag => tag.toLowerCase().includes(searchLower))
      )
    }
    
    // Filtro por tags
    if (selectedTags.length > 0) {
      transactions = transactions.filter(t => 
        t.tags && t.tags.some(tag => selectedTags.includes(tag))
      )
    }
    
    // Filtro por valor mínimo
    if (minValue) {
      const min = parseFloat(minValue)
      if (!isNaN(min)) {
        transactions = transactions.filter(t => t.amount >= min)
      }
    }
    
    // Filtro por valor máximo
    if (maxValue) {
      const max = parseFloat(maxValue)
      if (!isNaN(max)) {
        transactions = transactions.filter(t => t.amount <= max)
      }
    }
    
    // Filtro por transações recorrentes
    if (onlyRecurring) {
      transactions = transactions.filter(t => t.recurring)
    }
    
    // Filtrar transações recorrentes para mostrar apenas uma por mês corrente
    // Identifica séries recorrentes (mesma descrição, conta, categoria, valor, tipo)
    // e mantém apenas a transação do mês atual para cada série
    const today = new Date()
    const currentMonth = today.getMonth()
    const currentYear = today.getFullYear()
    const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    
    // Primeiro, identificar todas as transações recorrentes originais para criar o mapa de séries
    const allTransactions = allFilteredData?.data || []
    const recurringOriginals = new Set<string>()
    
    allTransactions.forEach(t => {
      if (t.recurring) {
        const seriesKey = `${t.description || ''}_${t.accountId}_${t.categoryId}_${t.amount}_${t.type}`
        recurringOriginals.add(seriesKey)
      }
    })
    
    // Criar um mapa para agrupar transações por série recorrente
    // Chave: descrição + accountId + categoryId + amount + type
    const recurringSeriesMap = new Map<string, Transaction[]>()
    const nonRecurringTransactions: Transaction[] = []
    
    transactions.forEach(transaction => {
      // Criar chave única para identificar séries recorrentes
      const seriesKey = `${transaction.description || ''}_${transaction.accountId}_${transaction.categoryId}_${transaction.amount}_${transaction.type}`
      
      // Verificar se esta transação faz parte de uma série recorrente
      const isPartOfRecurringSeries = transaction.recurring || recurringOriginals.has(seriesKey)
      
      if (isPartOfRecurringSeries) {
        // Adicionar à série recorrente
        if (!recurringSeriesMap.has(seriesKey)) {
          recurringSeriesMap.set(seriesKey, [])
        }
        recurringSeriesMap.get(seriesKey)!.push(transaction)
      } else {
        // Não faz parte de uma série recorrente, manter normalmente
        nonRecurringTransactions.push(transaction)
      }
    })
    
    // Para cada série recorrente, manter apenas a transação do mês atual
    const filteredRecurringTransactions: Transaction[] = []
    
    recurringSeriesMap.forEach((seriesTransactions) => {
      if (seriesTransactions.length === 0) return
      
      // Encontrar a transação do mês atual
      const currentMonthTransaction = seriesTransactions.find(t => {
        const transactionDate = new Date(t.date)
        return transactionDate.getMonth() === currentMonth && 
               transactionDate.getFullYear() === currentYear
      })
      
      if (currentMonthTransaction) {
        // Se encontrou transação do mês atual, usar ela
        filteredRecurringTransactions.push(currentMonthTransaction)
      } else {
        // Se não encontrou, usar a transação mais próxima do mês atual (mas não futura)
        const pastTransactions = seriesTransactions
          .map(t => ({ transaction: t, date: new Date(t.date) }))
          .filter(({ date }) => {
            const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate())
            return dateOnly <= todayOnly
          })
          .sort((a, b) => b.date.getTime() - a.date.getTime())
        
        if (pastTransactions.length > 0) {
          // Usar a transação mais recente do passado
          filteredRecurringTransactions.push(pastTransactions[0].transaction)
        } else {
          // Se não há transações passadas, usar a transação recorrente original (se existir)
          const originalRecurring = seriesTransactions.find(t => t.recurring)
          if (originalRecurring) {
            filteredRecurringTransactions.push(originalRecurring)
          } else {
            // Se não há original, usar a primeira transação da série (fallback)
            filteredRecurringTransactions.push(seriesTransactions[0])
          }
        }
      }
    })
    
    // Combinar transações não-recorrentes com transações recorrentes filtradas
    return [...nonRecurringTransactions, ...filteredRecurringTransactions]
  }, [allFilteredData, debouncedSearchText, selectedTags, minValue, maxValue, onlyRecurring])
  
  const totalPages = Math.ceil(filteredTransactions.length / pageSize)
  
  // Funções de seleção múltipla
  const toggleSelectTransaction = (id: string) => {
    setSelectedTransactions(prev => {
      const newSet = new Set(prev)
      if (newSet.has(id)) {
        newSet.delete(id)
      } else {
        newSet.add(id)
      }
      return newSet
    })
  }
  
  const toggleSelectAll = () => {
    if (selectedTransactions.size === transactionsToDisplay.length && transactionsToDisplay.length > 0) {
      setSelectedTransactions(new Set())
    } else {
      setSelectedTransactions(new Set(transactionsToDisplay.map(t => t.id)))
    }
  }
  
  const handleBulkDelete = async () => {
    if (selectedTransactions.size === 0) return
    
    if (window.confirm(`Tem certeza que deseja excluir ${selectedTransactions.size} transação(ões)?`)) {
      try {
        const ids = Array.from(selectedTransactions)
        await deleteBatchMutation.mutateAsync(ids)
        const count = ids.length
        setSelectedTransactions(new Set())
        showSuccess(`${count} transação(ões) excluída(s) com sucesso!`)
      } catch (error) {
        handleError(error)
      }
    }
  }
  
  const handleDuplicate = (transaction: Transaction) => {
    setTransactionToDuplicate(transaction)
    setShowDuplicateModal(true)
  }
  
  const handleDuplicateConfirm = () => {
    if (transactionToDuplicate) {
      const duplicated = {
        ...transactionToDuplicate,
        id: '',
        description: `${transactionToDuplicate.description || 'Transação'} (Cópia)`,
        date: new Date().toISOString().split('T')[0],
      }
      setEditingTransaction(duplicated as Transaction)
      setShowForm(true)
      setShowDuplicateModal(false)
      setTransactionToDuplicate(null)
    }
  }
  
  const goToPage = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage)
    }
  }
  
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const deleteMutation = useDeleteTransaction()
  const deleteBatchMutation = useDeleteTransactionsBatch()
  const { handleError, showSuccess } = useErrorHandler()

  // Calcular totais das transações filtradas (após aplicar todos os filtros)
  const summary = useMemo(() => {
    const totalIncome = filteredTransactions
      .filter(t => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0)
    const totalExpense = filteredTransactions
      .filter(t => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0)
    const balance = totalIncome - totalExpense
    const count = filteredTransactions.length

    return { totalIncome, totalExpense, balance, count }
  }, [filteredTransactions])
  
  // Contar filtros ativos
  const activeFiltersCount = useMemo(() => {
    let count = 0
    if (filters.accountId) count++
    if (filters.categoryId) count++
    if (filters.type) count++
    if (filters.dateFrom) count++
    if (filters.dateTo) count++
    if (debouncedSearchText) count++
    if (selectedTags.length > 0) count++
    if (minValue) count++
    if (maxValue) count++
    if (onlyRecurring) count++
    return count
  }, [filters, debouncedSearchText, selectedTags, minValue, maxValue, onlyRecurring])
  
  // Função para limpar todos os filtros
  const clearAllFilters = () => {
    setFilters({
      accountId: '',
      categoryId: '',
      type: '',
      dateFrom: '',
      dateTo: '',
    })
    setSearchText('')
    setSelectedTags([])
    setMinValue('')
    setMaxValue('')
    setOnlyRecurring(false)
    setPage(0)
  }
  
  // Função para aplicar filtro rápido
  const applyQuickFilter = (range: string) => {
    const dateRange = getDateRange(range)
    setFilters(prev => ({
      ...prev,
      dateFrom: dateRange.dateFrom,
      dateTo: dateRange.dateTo,
    }))
    setPage(0)
  }
  
  // Função para toggle de tag
  const toggleTag = (tag: string) => {
    setSelectedTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    )
    setPage(0)
  }

  const { data: currencies } = useCurrencies()
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value)
  }
  const formatTransactionAmount = (amount: number, currencyCode?: string) =>
    formatWithCurrency(amount, currencyCode || 'BRL', currencies)

  // Obter cor da categoria se disponível
  const getCategoryColor = (categoryId: string) => {
    const category = categories?.find(c => c.id === categoryId)
    return category?.color || '#6366f1'
  }

  const handleEdit = (transaction: Transaction) => {
    setEditingTransaction(transaction)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    const transaction = filteredTransactions.find(t => t.id === id)
    if (transaction) {
      setTransactionToDelete(transaction)
      setShowDeleteModal(true)
    }
  }
  
  const handleDeleteConfirm = async () => {
    if (transactionToDelete) {
      try {
        await deleteMutation.mutateAsync(transactionToDelete.id)
        showSuccess('Transação excluída com sucesso!')
        setShowDeleteModal(false)
        setTransactionToDelete(null)
      } catch (error) {
        handleError(error)
      }
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingTransaction(null)
  }

  const handleGenerateReport = async () => {
    try {
      const params = new URLSearchParams()
      
      if (filters.accountId) params.append('accountId', filters.accountId)
      if (filters.categoryId) params.append('categoryId', filters.categoryId)
      if (filters.type) params.append('type', filters.type)
      if (filters.dateFrom) params.append('dateFrom', filters.dateFrom)
      if (filters.dateTo) params.append('dateTo', filters.dateTo)
      
      const response = await api.get(`/transactions/report?${params.toString()}`, {
        responseType: 'blob',
      })
      
      // Criar URL do blob e fazer download
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'relatorio_transacoes.pdf')
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      showSuccess('Relatório gerado com sucesso!')
    } catch (error) {
      handleError(error, 'Erro ao gerar relatório')
    }
  }

  const handleGenerateAIReport = async () => {
    setExportAnchorEl(null)
    setAiReportOpen(true)
    setAiReportLoading(true)
    setAiReport(null)
    try {
      const response = await api.get<{ data: AIReport }>('/reports/ai')
      setAiReport(response.data.data)
    } catch (error) {
      handleError(error, 'Erro ao gerar relatório por IA')
      setAiReportOpen(false)
    } finally {
      setAiReportLoading(false)
    }
  }

  // Funções de agrupamento
  const getGroupKey = (transaction: Transaction): string => {
    switch (groupBy) {
      case 'date':
        const date = new Date(transaction.date)
        if (groupByDateType === 'day') {
          return date.toLocaleDateString('pt-BR')
        } else if (groupByDateType === 'week') {
          const weekStart = new Date(date)
          weekStart.setDate(date.getDate() - date.getDay())
          return `Semana ${weekStart.toLocaleDateString('pt-BR')}`
        } else {
          return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
        }
      case 'category':
        return transaction.categoryName || 'Sem categoria'
      case 'account':
        return transaction.accountName || 'Sem conta'
      case 'type':
        return transaction.type === 'INCOME' ? 'Receitas' : 'Despesas'
      default:
        return ''
    }
  }

  const groupedTransactions = useMemo(() => {
    if (groupBy === 'none') {
      return { '': filteredTransactions }
    }
    
    const groups: Record<string, Transaction[]> = {}
    filteredTransactions.forEach(transaction => {
      const key = getGroupKey(transaction)
      if (!groups[key]) {
        groups[key] = []
      }
      groups[key].push(transaction)
    })
    
    // Ordenar grupos
    const sortedKeys = Object.keys(groups).sort((a, b) => {
      if (groupBy === 'date') {
        return new Date(groups[b][0].date).getTime() - new Date(groups[a][0].date).getTime()
      }
      return a.localeCompare(b)
    })
    
    const sortedGroups: Record<string, Transaction[]> = {}
    sortedKeys.forEach(key => {
      sortedGroups[key] = groups[key]
    })
    
    return sortedGroups
  }, [filteredTransactions, groupBy, groupByDateType])

  // Função de ordenação
  const handleSort = (field: string) => {
    setSortConfig(prev => {
      if (prev?.field === field) {
        return prev.direction === 'asc' 
          ? { field, direction: 'desc' }
          : null
      }
      return { field, direction: 'asc' }
    })
  }

  const sortedTransactions = useMemo(() => {
    if (!sortConfig) return filteredTransactions
    
    return [...filteredTransactions].sort((a, b) => {
      let aValue: any
      let bValue: any
      
      switch (sortConfig.field) {
        case 'date':
          aValue = new Date(a.date).getTime()
          bValue = new Date(b.date).getTime()
          break
        case 'description':
          aValue = a.description?.toLowerCase() || ''
          bValue = b.description?.toLowerCase() || ''
          break
        case 'account':
          aValue = a.accountName?.toLowerCase() || ''
          bValue = b.accountName?.toLowerCase() || ''
          break
        case 'category':
          aValue = a.categoryName?.toLowerCase() || ''
          bValue = b.categoryName?.toLowerCase() || ''
          break
        case 'amount':
          aValue = a.amount
          bValue = b.amount
          break
        case 'dueDate':
          aValue = a.dueDate ? new Date(a.dueDate).getTime() : Infinity
          bValue = b.dueDate ? new Date(b.dueDate).getTime() : Infinity
          break
        default:
          return 0
      }
      
      if (aValue < bValue) return sortConfig.direction === 'asc' ? -1 : 1
      if (aValue > bValue) return sortConfig.direction === 'asc' ? 1 : -1
      return 0
    })
  }, [filteredTransactions, sortConfig])

  // Toggle de grupo expandido/colapsado
  const toggleGroup = (groupKey: string) => {
    setExpandedGroups(prev => {
      const newSet = new Set(prev)
      if (newSet.has(groupKey)) {
        newSet.delete(groupKey)
      } else {
        newSet.add(groupKey)
      }
      return newSet
    })
  }

  // Funções de exportação
  const exportToCSV = () => {
    const headers = ['Data', 'Data de Vencimento', 'Descrição', 'Conta', 'Categoria', 'Tipo', 'Valor', 'Recorrente', 'Tags']
    const rows = filteredTransactions.map(t => [
      new Date(t.date).toLocaleDateString('pt-BR'),
      t.dueDate ? new Date(t.dueDate).toLocaleDateString('pt-BR') : '',
      t.description || '',
      t.accountName || '',
      t.categoryName || '',
      t.type === 'INCOME' ? 'Receita' : 'Despesa',
      t.amount.toFixed(2),
      t.recurring ? 'Sim' : 'Não',
      t.tags?.join('; ') || ''
    ])
    
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n')
    
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `transacoes_${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    showSuccess('Dados exportados para CSV com sucesso!')
  }

  const exportToJSON = () => {
    const data = filteredTransactions.map(t => ({
      id: t.id,
      data: new Date(t.date).toLocaleDateString('pt-BR'),
      dataVencimento: t.dueDate ? new Date(t.dueDate).toLocaleDateString('pt-BR') : null,
      descricao: t.description,
      conta: t.accountName,
      categoria: t.categoryName,
      tipo: t.type === 'INCOME' ? 'Receita' : 'Despesa',
      valor: t.amount,
      recorrente: t.recurring,
      tags: t.tags || []
    }))
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `transacoes_${new Date().toISOString().split('T')[0]}.json`
    link.click()
    showSuccess('Dados exportados para JSON com sucesso!')
  }

  const exportToExcel = () => {
    const data = filteredTransactions.map(t => ({
      Data: new Date(t.date).toLocaleDateString('pt-BR'),
      'Data de Vencimento': t.dueDate ? new Date(t.dueDate).toLocaleDateString('pt-BR') : '',
      Descricao: t.description || '',
      Conta: t.accountName || '',
      Categoria: t.categoryName || '',
      Tipo: t.type === 'INCOME' ? 'Receita' : 'Despesa',
      Valor: t.amount,
      Recorrente: t.recurring ? 'Sim' : 'Nao',
      Tags: t.tags?.join('; ') || '',
    }))

    const worksheet = XLSX.utils.json_to_sheet(data)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Transacoes')

    const excelArrayBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelArrayBuffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })

    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `transacoes_${new Date().toISOString().split('T')[0]}.xlsx`
    link.click()
    showSuccess('Dados exportados para Excel com sucesso!')
  }

  // Dados para gráficos
  const chartDataByCategory = useMemo(() => {
    const categoryMap = new Map<string, { income: number; expense: number }>()
    
    filteredTransactions.forEach(t => {
      const catName = t.categoryName || 'Sem categoria'
      if (!categoryMap.has(catName)) {
        categoryMap.set(catName, { income: 0, expense: 0 })
      }
      const data = categoryMap.get(catName)!
      if (t.type === 'INCOME') {
        data.income += t.amount
      } else {
        data.expense += t.amount
      }
    })
    
    return Array.from(categoryMap.entries()).map(([name, data]) => ({
      name,
      receitas: data.income,
      despesas: data.expense
    }))
  }, [filteredTransactions])

  const chartDataByDate = useMemo(() => {
    const dateMap = new Map<string, { income: number; expense: number }>()
    
    filteredTransactions.forEach(t => {
      const date = new Date(t.date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
      if (!dateMap.has(date)) {
        dateMap.set(date, { income: 0, expense: 0 })
      }
      const data = dateMap.get(date)!
      if (t.type === 'INCOME') {
        data.income += t.amount
      } else {
        data.expense += t.amount
      }
    })
    
    return Array.from(dateMap.entries())
      .sort((a, b) => new Date(a[0].split('/').reverse().join('-')).getTime() - new Date(b[0].split('/').reverse().join('-')).getTime())
      .map(([date, data]) => ({
        date,
        receitas: data.income,
        despesas: data.expense
      }))
  }, [filteredTransactions])

  const pieChartData = useMemo(() => {
    const totalIncome = filteredTransactions
      .filter(t => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0)
    const totalExpense = filteredTransactions
      .filter(t => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0)
    
    return [
      { name: 'Receitas', value: totalIncome },
      { name: 'Despesas', value: totalExpense }
    ]
  }, [filteredTransactions])

  const COLORS = ['#10b981', '#ef4444', '#6366f1', '#f59e0b', '#ec4899']

  // Transações finais (agrupadas e ordenadas)
  const finalTransactions = sortedTransactions
  const transactionsToDisplay = useMemo(() => {
    if (groupBy === 'none') {
      const start = page * pageSize
      const end = start + pageSize
      return finalTransactions.slice(start, end)
    }
    // Para grupos, retornar todas as transações dos grupos expandidos
    return finalTransactions.slice(page * pageSize, (page + 1) * pageSize)
  }, [finalTransactions, page, pageSize, groupBy])

  const theme = useTheme()

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Header - Material Design */}
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2 }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Transações
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <ToggleButtonGroup value={viewMode} exclusive size="small">
              <ToggleButton value="table" onClick={() => setViewMode('table')} aria-label="Tabela">
                <ListIcon />
              </ToggleButton>
              <ToggleButton value="cards" onClick={() => setViewMode('cards')} aria-label="Cards">
                <GridViewIcon />
              </ToggleButton>
            </ToggleButtonGroup>
            <Button
              variant={showCharts ? 'contained' : 'outlined'}
              size="small"
              startIcon={showCharts ? <VisibilityOffIcon /> : <BarChartIcon />}
              onClick={() => setShowCharts(!showCharts)}
            >
              Gráficos
            </Button>
            <Button
              variant="contained"
              color="info"
              size="small"
              startIcon={<DownloadIcon />}
              onClick={(e) => setExportAnchorEl(e.currentTarget)}
            >
              Exportar
            </Button>
            <Menu
              anchorEl={exportAnchorEl}
              open={!!exportAnchorEl}
              onClose={() => setExportAnchorEl(null)}
              anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
              transformOrigin={{ vertical: 'top', horizontal: 'right' }}
              slotProps={{ paper: { sx: { minWidth: 220 } } }}
              MenuListProps={{ sx: { py: 0, maxHeight: '70vh' } }}
              disableScrollLock
            >
              <MenuItem onClick={() => { exportToCSV(); setExportAnchorEl(null) }}>Exportar CSV</MenuItem>
              <MenuItem onClick={handleGenerateAIReport} sx={{ fontWeight: 500 }}>Relatório por IA</MenuItem>
              <MenuItem onClick={() => { exportToExcel(); setExportAnchorEl(null) }}>Exportar Excel</MenuItem>
              <MenuItem onClick={() => { exportToJSON(); setExportAnchorEl(null) }}>Exportar JSON</MenuItem>
              <MenuItem onClick={() => { handleGenerateReport(); setExportAnchorEl(null) }}>Exportar PDF</MenuItem>
            </Menu>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
              Nova Transação
            </Button>
          </Box>
        </Box>

        {/* Cards de Resumo - Material Design */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card elevation={1} sx={{ transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 2 } }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1" color="text.secondary">Total de Receitas</Typography>
                    <Typography variant="h5" color="success.main" fontWeight={600}>{formatCurrency(summary.totalIncome)}</Typography>
                  </Box>
                  <Box sx={{ p: 1.5, borderRadius: '50%', bgcolor: (t) => alpha(t.palette.success.main, 0.12), color: 'success.main', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <TrendingUpIcon />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card elevation={1} sx={{ transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 2 } }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1" color="text.secondary">Total de Despesas</Typography>
                    <Typography variant="h5" color="error.main" fontWeight={600}>{formatCurrency(summary.totalExpense)}</Typography>
                  </Box>
                  <Box sx={{ p: 1.5, borderRadius: '50%', bgcolor: (t) => alpha(t.palette.error.main, 0.12), color: 'error.main', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <TrendingDownIcon />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card elevation={1} sx={{ transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 2 } }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1" color="text.secondary">Saldo Líquido</Typography>
                    <Typography variant="h5" fontWeight={600} color={summary.balance >= 0 ? 'text.primary' : 'error.main'}>{formatCurrency(summary.balance)}</Typography>
                  </Box>
                  <Box sx={{ p: 1.5, borderRadius: '50%', bgcolor: (t) => alpha(t.palette.primary.main, 0.12), color: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <AttachMoneyIcon />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card elevation={1} sx={{ transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 2 } }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1" color="text.secondary">Transações</Typography>
                    <Typography variant="h5" fontWeight={600}>{summary.count}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {totalPages > 0 ? `Página ${page + 1}/${totalPages}` : 'Total'}
                    </Typography>
                  </Box>
                  <Box sx={{ p: 1.5, borderRadius: '50%', bgcolor: 'action.hover', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <RefreshIcon />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Busca e Filtros - Material Design */}
        <Card elevation={1}>
          <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              fullWidth
              size="small"
              placeholder="Buscar por descrição, tags, conta ou categoria..."
              value={searchText}
              onChange={(e) => { setSearchText(e.target.value); setPage(0) }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>,
              }}
            />
            <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1 }}>
              <FilterListIcon fontSize="small" />
              <Typography variant="body2" sx={{ mr: 1 }}>Período:</Typography>
              {[
                { key: 'today', label: 'Hoje' },
                { key: 'thisWeek', label: 'Esta Semana' },
                { key: 'thisMonth', label: 'Este Mês' },
                { key: 'last30Days', label: 'Últimos 30 dias' },
                { key: 'last3Months', label: 'Últimos 3 meses' },
                { key: 'thisYear', label: 'Este Ano' },
              ].map(({ key, label }) => {
                const range = getDateRange(key)
                const isActive = filters.dateFrom === range.dateFrom && filters.dateTo === range.dateTo
                return (
                  <Button key={key} size="small" variant={isActive ? 'contained' : 'outlined'} onClick={() => applyQuickFilter(key)}>{label}</Button>
                )
              })}
            </Box>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 3 }}>
                <FormControl fullWidth size="small">
                  <InputLabel>Conta</InputLabel>
                  <Select value={filters.accountId} label="Conta" onChange={(e) => { setFilters({ ...filters, accountId: e.target.value }); setPage(0) }}>
                    <MenuItem value="">Todas as contas</MenuItem>
                    {accounts?.map((acc) => <MenuItem key={acc.id} value={acc.id}>{acc.name}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <FormControl fullWidth size="small">
                  <InputLabel>Categoria</InputLabel>
                  <Select value={filters.categoryId} label="Categoria" onChange={(e) => { setFilters({ ...filters, categoryId: e.target.value }); setPage(0) }}>
                    <MenuItem value="">Todas as categorias</MenuItem>
                    {categories?.map((cat) => <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <FormControl fullWidth size="small">
                  <InputLabel>Tipo</InputLabel>
                  <Select value={filters.type} label="Tipo" onChange={(e) => { setFilters({ ...filters, type: e.target.value as any }); setPage(0) }}>
                    <MenuItem value="">Todos os tipos</MenuItem>
                    <MenuItem value="INCOME">Receitas</MenuItem>
                    <MenuItem value="EXPENSE">Despesas</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField type="date" size="small" fullWidth label="Data inicial" value={filters.dateFrom} onChange={(e) => { setFilters({ ...filters, dateFrom: e.target.value }); setPage(0) }} InputLabelProps={{ shrink: true }} />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField type="date" size="small" fullWidth label="Data final" value={filters.dateTo} onChange={(e) => { setFilters({ ...filters, dateTo: e.target.value }); setPage(0) }} InputLabelProps={{ shrink: true }} />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField type="number" size="small" fullWidth label="Valor Mínimo" value={minValue} onChange={(e) => { setMinValue(e.target.value); setPage(0) }} placeholder="0.00" inputProps={{ step: 0.01 }} />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField type="number" size="small" fullWidth label="Valor Máximo" value={maxValue} onChange={(e) => { setMaxValue(e.target.value); setPage(0) }} placeholder="0.00" inputProps={{ step: 0.01 }} />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="body2" sx={{ mb: 1 }}>Tags ({selectedTags.length} selecionadas)</Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, maxHeight: 80, overflow: 'auto' }}>
                  {allTags.length > 0 ? allTags.map((tag) => (
                    <Button key={tag} size="small" variant={selectedTags.includes(tag) ? 'contained' : 'outlined'} onClick={() => toggleTag(tag)}>{tag}</Button>
                  )) : <Typography variant="caption" color="text.secondary">Nenhuma tag disponível</Typography>}
                </Box>
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <FormControlLabel control={<Checkbox checked={onlyRecurring} onChange={(e) => { setOnlyRecurring(e.target.checked); setPage(0) }} />} label="Apenas recorrentes" />
              </Grid>
            </Grid>
            {activeFiltersCount > 0 && (
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pt: 1, borderTop: 1, borderColor: 'divider' }}>
                <Typography variant="body2" color="text.secondary">{activeFiltersCount} {activeFiltersCount === 1 ? 'filtro ativo' : 'filtros ativos'}</Typography>
                <Button size="small" color="error" onClick={clearAllFilters}>Limpar Filtros</Button>
              </Box>
            )}
          </CardContent>
        </Card>

        {/* Agrupamento e Ordenação - Material Design */}
        <Card elevation={1}>
          <CardContent>
            <Grid container spacing={3}>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Agrupar por</Typography>
                <ToggleButtonGroup value={groupBy} exclusive size="small" onChange={(_, v) => { if (v) { setGroupBy(v); setPage(0) } }}>
                  {[
                    { value: 'none', label: 'Nenhum' },
                    { value: 'date', label: 'Data' },
                    { value: 'category', label: 'Categoria' },
                    { value: 'account', label: 'Conta' },
                    { value: 'type', label: 'Tipo' },
                  ].map(({ value, label }) => (
                    <ToggleButton key={value} value={value}>{label}</ToggleButton>
                  ))}
                </ToggleButtonGroup>
                {groupBy === 'date' && (
                  <Box sx={{ mt: 1 }}>
                    {(['day', 'week', 'month'] as const).map((type) => (
                      <Button key={type} size="small" variant={groupByDateType === type ? 'contained' : 'outlined'} sx={{ mr: 0.5 }} onClick={() => setGroupByDateType(type)}>
                        {type === 'day' ? 'Dia' : type === 'week' ? 'Semana' : 'Mês'}
                      </Button>
                    ))}
                  </Box>
                )}
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Ordenar por</Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {[
                    { value: 'date', label: 'Data' },
                    { value: 'dueDate', label: 'Vencimento' },
                    { value: 'description', label: 'Descrição' },
                    { value: 'amount', label: 'Valor' },
                    { value: 'category', label: 'Categoria' },
                    { value: 'account', label: 'Conta' },
                  ].map(({ value, label }) => (
                    <Button key={value} size="small" variant={sortConfig?.field === value ? 'contained' : 'outlined'} onClick={() => handleSort(value)}>{label}</Button>
                  ))}
                  {sortConfig && <Button size="small" color="error" onClick={() => setSortConfig(null)}>Limpar</Button>}
                </Box>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Gráficos */}
        {showCharts && (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, lg: 6 }}>
              <Card elevation={1}>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>Por Categoria</Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={chartDataByCategory}>
                      <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                      <XAxis dataKey="name" tick={{ fill: theme.palette.text.secondary }} />
                      <YAxis tick={{ fill: theme.palette.text.secondary }} />
                      <Tooltip formatter={(value: number) => formatCurrency(value)} />
                      <Legend />
                      <Bar dataKey="receitas" fill="#4CAF50" />
                      <Bar dataKey="despesas" fill="#F44336" />
                    </BarChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, lg: 6 }}>
              <Card elevation={1}>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>Distribuição</Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie data={pieChartData} cx="50%" cy="50%" labelLine={false} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`} outerRadius={80} fill="#8884d8" dataKey="value">
                        {pieChartData.map((_, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
                      </Pie>
                      <Tooltip formatter={(value: number) => formatCurrency(value)} />
                    </PieChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12 }}>
              <Card elevation={1}>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>Evolução Temporal</Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={chartDataByDate}>
                      <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                      <XAxis dataKey="date" tick={{ fill: theme.palette.text.secondary }} />
                      <YAxis tick={{ fill: theme.palette.text.secondary }} />
                      <Tooltip formatter={(value: number) => formatCurrency(value)} />
                      <Legend />
                      <Line type="monotone" dataKey="receitas" stroke="#4CAF50" strokeWidth={2} />
                      <Line type="monotone" dataKey="despesas" stroke="#F44336" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Lista de Transações */}
        <Card elevation={1} sx={{ overflow: 'hidden' }}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <TransactionListSkeleton count={10} />
            </Box>
          ) : filteredTransactions.length > 0 ? (
            <>
              {viewMode === 'table' ? (
                <>
                  {/* Em mobile, forçar visualização em cards */}
                  <Box sx={{ display: { xs: 'block', md: 'none' }, p: 2 }}>
                    <Grid container spacing={2}>
                      {transactionsToDisplay.map((transaction) => (
                        <Grid size={{ xs: 12 }} key={transaction.id}>
                          <Card variant="outlined" sx={{ '&:hover': { boxShadow: 1 } }}>
                            <CardContent>
                              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                <Box sx={{ flex: 1 }}>
                                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                    <Checkbox
                                      size="small"
                                      checked={selectedTransactions.has(transaction.id)}
                                      onChange={() => toggleSelectTransaction(transaction.id)}
                                    />
                                    <Typography variant="subtitle1" fontWeight={600}>
                                      {transaction.description || 'Sem descrição'}
                                    </Typography>
                                  </Box>
                                  <Typography variant="body2" color="text.secondary">
                                    {new Date(transaction.date).toLocaleDateString('pt-BR')}
                                    {transaction.dueDate && (
                                      <> · Venc.: {new Date(transaction.dueDate).toLocaleDateString('pt-BR')}</>
                                    )}
                                  </Typography>
                                </Box>
                                <Typography variant="h6" fontWeight={600} color={transaction.type === 'INCOME' ? 'success.main' : 'error.main'}>
                                  {transaction.type === 'INCOME' ? '+' : '-'}{formatTransactionAmount(transaction.amount, transaction.currencyCode)}
                                </Typography>
                              </Box>
                              <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                                <Chip size="small" label={transaction.categoryName} sx={{ bgcolor: alpha(getCategoryColor(transaction.categoryId), 0.12), color: getCategoryColor(transaction.categoryId) }} />
                                <Typography variant="body2" color="text.secondary">{transaction.accountName}</Typography>
                              </Box>
                              <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
                                <IconButton size="small" onClick={() => handleDuplicate(transaction)} color="info"><ContentCopyIcon fontSize="small" /></IconButton>
                                <IconButton size="small" onClick={() => handleEdit(transaction)} color="primary"><EditIcon fontSize="small" /></IconButton>
                                <IconButton size="small" onClick={() => handleDelete(transaction.id)} color="error"><DeleteIcon fontSize="small" /></IconButton>
                              </Box>
                            </CardContent>
                          </Card>
                        </Grid>
                      ))}
                    </Grid>
                  </Box>

                  {/* Tabela para desktop */}
                  <TableContainer component={Paper} elevation={0} sx={{ display: { xs: 'none', md: 'block' } }}>
                    <Table size="small">
                      <TableHead>
                        <TableRow sx={{ bgcolor: 'action.hover' }}>
                          <TableCell padding="checkbox" sx={{ width: 48 }}>
                            <Checkbox
                              size="small"
                              checked={selectedTransactions.size === transactionsToDisplay.length && transactionsToDisplay.length > 0}
                              onChange={toggleSelectAll}
                            />
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('date')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Data
                              {sortConfig?.field === 'date' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('dueDate')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Vencimento
                              {sortConfig?.field === 'dueDate' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('description')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Descrição
                              {sortConfig?.field === 'description' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('account')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Conta
                              {sortConfig?.field === 'account' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('category')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Categoria
                              {sortConfig?.field === 'category' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }} onClick={() => handleSort('amount')}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>
                              Valor
                              {sortConfig?.field === 'amount' && (sortConfig.direction === 'asc' ? <ArrowUpwardIcon sx={{ fontSize: 14 }} /> : <ArrowDownwardIcon sx={{ fontSize: 14 }} />)}
                            </Box>
                          </TableCell>
                          <TableCell sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.75rem' }}>Ações</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {groupBy === 'none' ? (
                          transactionsToDisplay.map((transaction) => (
                            <TableRow
                              key={transaction.id}
                              hover
                              sx={{
                                bgcolor: selectedTransactions.has(transaction.id) ? (t) => alpha(t.palette.primary.main, 0.08) : 'inherit',
                              }}
                            >
                              <TableCell padding="checkbox">
                                <Checkbox
                                  size="small"
                                  checked={selectedTransactions.has(transaction.id)}
                                  onChange={() => toggleSelectTransaction(transaction.id)}
                                />
                              </TableCell>
                              <TableCell>
                                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                  <Typography variant="body2">
                                    {new Date(transaction.date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })}
                                  </Typography>
                                  {transaction.createdAt && (
                                    <Typography variant="caption" color="text.secondary" title={`Criada em: ${new Date(transaction.createdAt).toLocaleString('pt-BR')}${transaction.updatedAt && transaction.updatedAt !== transaction.createdAt ? ` | Editada em: ${new Date(transaction.updatedAt).toLocaleString('pt-BR')}` : ''}`}>
                                      {transaction.updatedAt && transaction.updatedAt !== transaction.createdAt ? '✏️' : '📅'} {new Date(transaction.createdAt).toLocaleDateString('pt-BR')}
                                    </Typography>
                                  )}
                                </Box>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2" color="text.secondary">
                                  {transaction.dueDate ? new Date(transaction.dueDate).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '-'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                                  {transaction.recurring && (
                                    <Chip size="small" label="🔄" sx={{ fontSize: '0.7rem' }} title="Transação recorrente" />
                                  )}
                                  <Typography component="span" variant="body2" fontWeight={500}>
                                    {debouncedSearchText ? (
                                      <span dangerouslySetInnerHTML={{
                                        __html: (transaction.description || '-').replace(
                                          new RegExp(`(${debouncedSearchText})`, 'gi'),
                                          '<mark style="background: rgba(255,235,59,0.5)">$1</mark>'
                                        )
                                      }} />
                                    ) : (
                                      transaction.description || '-'
                                    )}
                                  </Typography>
                                  {transaction.tags && transaction.tags.length > 0 && (
                                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                                      {transaction.tags.slice(0, 2).map((tag, idx) => (
                                        <Chip key={idx} size="small" label={tag} variant="outlined" sx={{ fontSize: '0.7rem' }} />
                                      ))}
                                      {transaction.tags.length > 2 && <Typography variant="caption" color="text.secondary">+{transaction.tags.length - 2}</Typography>}
                                    </Box>
                                  )}
                                </Box>
                              </TableCell>
                              <TableCell><Typography variant="body2" color="text.secondary">{transaction.accountName}</Typography></TableCell>
                              <TableCell>
                                <Chip size="small" label={transaction.categoryName} sx={{ bgcolor: alpha(getCategoryColor(transaction.categoryId), 0.12), color: getCategoryColor(transaction.categoryId) }} />
                              </TableCell>
                              <TableCell>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: transaction.type === 'INCOME' ? 'success.main' : 'error.main', fontWeight: 600 }}>
                                  {transaction.type === 'INCOME' ? <TrendingUpIcon fontSize="small" /> : <TrendingDownIcon fontSize="small" />}
                                  {transaction.type === 'INCOME' ? '+' : '-'}{formatTransactionAmount(transaction.amount, transaction.currencyCode)}
                                </Box>
                              </TableCell>
                              <TableCell>
                                <Box sx={{ display: 'flex', gap: 0.5 }}>
                                  <IconButton size="small" onClick={() => handleDuplicate(transaction)} color="info" title="Duplicar"><ContentCopyIcon fontSize="small" /></IconButton>
                                  <IconButton size="small" onClick={() => handleEdit(transaction)} color="primary" title="Editar"><EditIcon fontSize="small" /></IconButton>
                                  <IconButton size="small" onClick={() => handleDelete(transaction.id)} color="error" title="Excluir"><DeleteIcon fontSize="small" /></IconButton>
                                </Box>
                              </TableCell>
                            </TableRow>
                          ))
                        ) : (
                          Object.entries(groupedTransactions).map(([groupKey, groupTransactions]) => {
                            const isExpanded = expandedGroups.has(groupKey) || expandedGroups.size === 0
                            const groupTotal = (groupTransactions as Transaction[]).reduce((sum: number, t: Transaction) => sum + t.amount, 0)
                            const groupIncome = (groupTransactions as Transaction[]).filter((t: Transaction) => t.type === 'INCOME').reduce((sum: number, t: Transaction) => sum + t.amount, 0)
                            const groupExpense = (groupTransactions as Transaction[]).filter((t: Transaction) => t.type === 'EXPENSE').reduce((sum: number, t: Transaction) => sum + t.amount, 0)
                            const paginatedGroupTransactions = isExpanded 
                              ? (groupTransactions as Transaction[]).slice(page * pageSize, (page + 1) * pageSize)
                              : []
                            
                            return (
                              <React.Fragment key={groupKey}>
                                <TableRow sx={{ bgcolor: 'action.hover', cursor: 'pointer', '&:hover': { bgcolor: 'action.selected' } }} onClick={() => toggleGroup(groupKey)}>
                                  <TableCell colSpan={8} sx={{ py: 2 }}>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
                                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        {isExpanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
                                        <Typography fontWeight={600}>{groupKey}</Typography>
                                        <Typography variant="body2" color="text.secondary">
                                          ({(groupTransactions as Transaction[]).length} {(groupTransactions as Transaction[]).length === 1 ? 'transação' : 'transações'})
                                        </Typography>
                                      </Box>
                                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                        {groupIncome > 0 && <Typography variant="body2" color="success.main">Receitas: {formatCurrency(groupIncome)}</Typography>}
                                        {groupExpense > 0 && <Typography variant="body2" color="error.main">Despesas: {formatCurrency(groupExpense)}</Typography>}
                                        <Typography variant="body2" fontWeight={600}>Total: {formatCurrency(groupTotal)}</Typography>
                                      </Box>
                                    </Box>
                                  </TableCell>
                                </TableRow>
                                {isExpanded && paginatedGroupTransactions.map((transaction) => (
                                  <TableRow key={transaction.id} hover>
                                    <TableCell padding="checkbox"><Checkbox size="small" checked={selectedTransactions.has(transaction.id)} onChange={() => toggleSelectTransaction(transaction.id)} /></TableCell>
                                    <TableCell sx={{ pl: 4 }}>{new Date(transaction.date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })}</TableCell>
                                    <TableCell>{transaction.dueDate ? new Date(transaction.dueDate).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '-'}</TableCell>
                                    <TableCell>
                                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                                        {transaction.recurring && <Chip size="small" label="🔄" sx={{ fontSize: '0.7rem' }} />}
                                        <Typography variant="body2" fontWeight={500}>
                                          {debouncedSearchText ? (
                                            <span dangerouslySetInnerHTML={{
                                              __html: (transaction.description || '-').replace(new RegExp(`(${debouncedSearchText})`, 'gi'), '<mark style="background: rgba(255,235,59,0.5)">$1</mark>')
                                            }} />
                                          ) : (transaction.description || '-')}
                                        </Typography>
                                        {transaction.tags?.slice(0, 2).map((tag, idx) => <Chip key={idx} size="small" label={tag} variant="outlined" sx={{ fontSize: '0.7rem' }} />)}
                                      </Box>
                                    </TableCell>
                                    <TableCell><Typography variant="body2" color="text.secondary">{transaction.accountName}</Typography></TableCell>
                                    <TableCell><Chip size="small" label={transaction.categoryName} sx={{ bgcolor: alpha(getCategoryColor(transaction.categoryId), 0.12), color: getCategoryColor(transaction.categoryId) }} /></TableCell>
                                    <TableCell>
                                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: transaction.type === 'INCOME' ? 'success.main' : 'error.main', fontWeight: 600 }}>
                                        {transaction.type === 'INCOME' ? <TrendingUpIcon fontSize="small" /> : <TrendingDownIcon fontSize="small" />}
                                        {transaction.type === 'INCOME' ? '+' : '-'}{formatTransactionAmount(transaction.amount, transaction.currencyCode)}
                                      </Box>
                                    </TableCell>
                                    <TableCell>
                                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                                        <IconButton size="small" onClick={() => handleDuplicate(transaction)} color="info"><ContentCopyIcon fontSize="small" /></IconButton>
                                        <IconButton size="small" onClick={() => handleEdit(transaction)} color="primary"><EditIcon fontSize="small" /></IconButton>
                                        <IconButton size="small" onClick={() => handleDelete(transaction.id)} color="error"><DeleteIcon fontSize="small" /></IconButton>
                                      </Box>
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </React.Fragment>
                            )
                          })
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </>
              ) : (
                <Box sx={{ p: 2 }}>
                  <Grid container spacing={2}>
                    {transactionsToDisplay.map((transaction) => (
                      <Grid size={{ xs: 12, md: 6, lg: 4 }} key={transaction.id}>
                        <Card variant="outlined" sx={{ transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 2 } }}>
                          <CardContent>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                              <Box sx={{ flex: 1 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                  <Checkbox size="small" checked={selectedTransactions.has(transaction.id)} onChange={() => toggleSelectTransaction(transaction.id)} />
                                  <Typography variant="subtitle1" fontWeight={600}>{transaction.description || 'Sem descrição'}</Typography>
                                </Box>
                                <Typography variant="body2" color="text.secondary">
                                  {new Date(transaction.date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })}
                                  {transaction.dueDate && (
                                    <> · Venc.: {new Date(transaction.dueDate).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })}</>
                                  )}
                                </Typography>
                              </Box>
                              <Typography variant="h6" fontWeight={600} color={transaction.type === 'INCOME' ? 'success.main' : 'error.main'}>
                                {transaction.type === 'INCOME' ? '+' : '-'}{formatTransactionAmount(transaction.amount, transaction.currencyCode)}
                              </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                              <Chip size="small" label={transaction.categoryName} sx={{ bgcolor: alpha(getCategoryColor(transaction.categoryId), 0.12), color: getCategoryColor(transaction.categoryId) }} />
                              {transaction.recurring && <Chip size="small" label="🔄 Recorrente" color="info" variant="outlined" />}
                            </Box>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <Typography variant="body2" color="text.secondary">{transaction.accountName}</Typography>
                              <Box sx={{ display: 'flex', gap: 0.5 }}>
                                <IconButton size="small" onClick={() => handleDuplicate(transaction)} color="info"><ContentCopyIcon fontSize="small" /></IconButton>
                                <IconButton size="small" onClick={() => handleEdit(transaction)} color="primary"><EditIcon fontSize="small" /></IconButton>
                                <IconButton size="small" onClick={() => handleDelete(transaction.id)} color="error"><DeleteIcon fontSize="small" /></IconButton>
                              </Box>
                            </Box>
                          </CardContent>
                        </Card>
                      </Grid>
                    ))}
                  </Grid>
                </Box>
              )}
              
              {/* Paginação */}
              {totalPages > 1 && (
                <Paper variant="outlined" sx={{ p: 2, display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, alignItems: 'center', justifyContent: 'space-between', gap: 2, borderRadius: 0, borderTop: 1, borderColor: 'divider' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    <Typography variant="body2" color="text.secondary">
                      Mostrando {page * pageSize + 1} a {Math.min((page + 1) * pageSize, filteredTransactions.length)} de {filteredTransactions.length} transações
                    </Typography>
                    <FormControl size="small" sx={{ minWidth: 140 }}>
                      <InputLabel>Por página</InputLabel>
                      <Select
                        value={pageSize}
                        label="Por página"
                        onChange={(e) => { setPageSize(Number(e.target.value)); setPage(0) }}
                      >
                        <MenuItem value={10}>10 por página</MenuItem>
                        <MenuItem value={20}>20 por página</MenuItem>
                        <MenuItem value={50}>50 por página</MenuItem>
                        <MenuItem value={100}>100 por página</MenuItem>
                      </Select>
                    </FormControl>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <IconButton size="small" onClick={() => goToPage(0)} disabled={page === 0} title="Primeira página"><FirstPageIcon /></IconButton>
                    <IconButton size="small" onClick={() => goToPage(page - 1)} disabled={page === 0} title="Página anterior"><ChevronLeftIcon /></IconButton>
                    <Typography variant="body2" sx={{ mx: 1 }}>Página {page + 1} de {totalPages}</Typography>
                    <IconButton size="small" onClick={() => goToPage(page + 1)} disabled={page >= totalPages - 1} title="Próxima página"><ChevronRightIcon /></IconButton>
                    <IconButton size="small" onClick={() => goToPage(totalPages - 1)} disabled={page >= totalPages - 1} title="Última página"><LastPageIcon /></IconButton>
                  </Box>
                </Paper>
              )}
            </>
          ) : (
            <Box sx={{ p: 3 }}>
              <EmptyState
                icon="💸"
                title="Nenhuma transação encontrada"
                description={
                  activeFiltersCount > 0
                    ? "Nenhuma transação corresponde aos filtros aplicados"
                    : "Comece criando sua primeira transação"
                }
                action={
                  <Button variant="contained" startIcon={<AddIcon />} onClick={() => setShowForm(true)}>
                    Nova Transação
                  </Button>
                }
              />
            </Box>
          )}
        </Card>

        {/* Barra de Ações Flutuante para Seleção Múltipla */}
        {selectedTransactions.size > 0 && (
          <Paper
            elevation={8}
            sx={{
              position: 'fixed',
              bottom: 24,
              left: '50%',
              transform: 'translateX(-50%)',
              zIndex: 1300,
              px: 3,
              py: 2,
              display: 'flex',
              alignItems: 'center',
              gap: 2,
              borderRadius: 2,
            }}
          >
            <Typography variant="body1" fontWeight={500}>
              {selectedTransactions.size} {selectedTransactions.size === 1 ? 'transação selecionada' : 'transações selecionadas'}
            </Typography>
            <Button variant="contained" color="error" size="small" onClick={handleBulkDelete}>
              Excluir Selecionadas
            </Button>
            <Button variant="outlined" size="small" onClick={() => setSelectedTransactions(new Set())}>
              Cancelar
            </Button>
          </Paper>
        )}

        {/* Modal de Confirmação de Exclusão */}
        <Dialog open={showDeleteModal && !!transactionToDelete} onClose={() => { setShowDeleteModal(false); setTransactionToDelete(null) }} maxWidth="sm" fullWidth>
          <DialogTitle>Confirmar Exclusão</DialogTitle>
          <DialogContent>
            {transactionToDelete && (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Tem certeza que deseja excluir esta transação?
                </Typography>
                <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover' }}>
                  <Typography variant="body1" fontWeight={500}>{transactionToDelete.description || 'Sem descrição'}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {transactionToDelete.type === 'INCOME' ? 'Receita' : 'Despesa'} - {formatTransactionAmount(transactionToDelete.amount, transactionToDelete.currencyCode)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(transactionToDelete.date).toLocaleDateString('pt-BR')}
                  </Typography>
                </Paper>
              </>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { setShowDeleteModal(false); setTransactionToDelete(null) }}>Cancelar</Button>
            <Button variant="contained" color="error" onClick={handleDeleteConfirm}>Excluir</Button>
          </DialogActions>
        </Dialog>

        {/* Modal de Confirmação de Duplicação */}
        <Dialog open={showDuplicateModal && !!transactionToDuplicate} onClose={() => { setShowDuplicateModal(false); setTransactionToDuplicate(null) }} maxWidth="sm" fullWidth>
          <DialogTitle>Duplicar Transação</DialogTitle>
          <DialogContent>
            {transactionToDuplicate && (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Uma cópia desta transação será criada. Você poderá editá-la antes de salvar.
                </Typography>
                <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover' }}>
                  <Typography variant="body1" fontWeight={500}>{transactionToDuplicate.description || 'Sem descrição'}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {transactionToDuplicate.type === 'INCOME' ? 'Receita' : 'Despesa'} - {formatTransactionAmount(transactionToDuplicate.amount, transactionToDuplicate.currencyCode)}
                  </Typography>
                </Paper>
              </>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { setShowDuplicateModal(false); setTransactionToDuplicate(null) }}>Cancelar</Button>
            <Button variant="contained" onClick={handleDuplicateConfirm}>Duplicar</Button>
          </DialogActions>
        </Dialog>

        {/* Modal Relatório com IA */}
        <Dialog open={aiReportOpen} onClose={() => { setAiReportOpen(false); setAiReport(null) }} maxWidth="md" fullWidth>
          <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {aiReport ? aiReport.title : 'Relatório por IA'}
            {aiReport?.fromAi !== undefined && (
              <Chip size="small" label={aiReport.fromAi ? 'Gerado por IA' : 'Resumo automático'} color={aiReport.fromAi ? 'primary' : 'default'} variant="outlined" />
            )}
          </DialogTitle>
          <DialogContent>
            {aiReportLoading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            )}
            {!aiReportLoading && aiReport && (
              <Typography component="div" sx={{ whiteSpace: 'pre-wrap' }}>{aiReport.content}</Typography>
            )}
          </DialogContent>
          {aiReport && (
            <DialogActions>
              <Button onClick={() => { setAiReportOpen(false); setAiReport(null) }}>Fechar</Button>
            </DialogActions>
          )}
        </Dialog>

        {/* Modal de Formulário */}
        {showForm && (
          <TransactionForm
            transaction={editingTransaction}
            onClose={handleCloseForm}
            accounts={(accounts || []).filter((a) => !a.sharedWithMe || a.sharedPermission === 'EDIT')}
            categories={categories || []}
          />
        )}
      </Box>
    </Layout>
  )
}
