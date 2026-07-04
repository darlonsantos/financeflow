import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import { addPendingTransaction } from '../services/offlineQueue'
import type { Transaction, TransactionRequest, PaginatedResponse, CategorySuggestion } from '../types'

function generateClientId(): string {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `client-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

function isNetworkError(err: unknown): boolean {
  if (err && typeof err === 'object' && 'isAxiosError' in err) {
    const ax = err as { response?: unknown; code?: string }
    return !ax.response || ax.code === 'ERR_NETWORK'
  }
  return false
}

interface TransactionFilters {
  page?: number
  size?: number
  sort?: string
  accountId?: string
  categoryId?: string
  type?: 'INCOME' | 'EXPENSE'
  dateFrom?: string
  dateTo?: string
}

export const useTransactions = (filters: TransactionFilters = {}) => {
  return useQuery({
    queryKey: ['transactions', filters],
    queryFn: async () => {
      const params = {
        page: filters.page || 0,
        size: filters.size || 20,
        sort: filters.sort || 'date,desc',
        ...(filters.accountId && { accountId: filters.accountId }),
        ...(filters.categoryId && { categoryId: filters.categoryId }),
        ...(filters.type && { type: filters.type }),
        ...(filters.dateFrom && { dateFrom: filters.dateFrom }),
        ...(filters.dateTo && { dateTo: filters.dateTo }),
      }
      const response = await api.get<PaginatedResponse<Transaction>>('/transactions', { params })
      return response.data
    },
  })
}

export const useTransaction = (id: string) => {
  return useQuery({
    queryKey: ['transactions', id],
    queryFn: async () => {
      const response = await api.get(`/transactions/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export const useCreateTransaction = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: TransactionRequest) => {
      const clientId = generateClientId()
      const dataWithClientId: TransactionRequest = { ...data, clientId }

      if (typeof navigator !== 'undefined' && !navigator.onLine) {
        addPendingTransaction(clientId, data)
        return {
          id: `pending-${clientId}`,
          clientId,
          ...data,
          accountName: '...',
          categoryName: '...',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        } as Transaction
      }

      try {
        const response = await api.post('/transactions', dataWithClientId)
        return response.data.data
      } catch (err) {
        if (isNetworkError(err)) {
          addPendingTransaction(clientId, data)
          return {
            id: `pending-${clientId}`,
            clientId,
            ...data,
            accountName: '...',
            categoryName: '...',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          } as Transaction
        }
        throw err
      }
    },
    onMutate: async (newTransaction) => {
      await queryClient.cancelQueries({ queryKey: ['transactions'] })
      const previousTransactions = queryClient.getQueryData(['transactions'])
      queryClient.setQueryData(['transactions'], (old: any) => {
        if (!old) return old
        const optimisticTransaction = {
          id: 'temp-' + Date.now(),
          ...newTransaction,
          accountName: 'Carregando...',
          categoryName: 'Carregando...',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
        return { ...old, data: [optimisticTransaction, ...old.data] }
      })
      return { previousTransactions }
    },
    onError: (err, newTransaction, context) => {
      if (!isNetworkError(err) && context?.previousTransactions) {
        queryClient.setQueryData(['transactions'], context.previousTransactions)
      }
    },
    onSuccess: (data) => {
      const isPending = data?.id?.toString().startsWith('pending-')
      if (!isPending) {
        queryClient.invalidateQueries({ queryKey: ['transactions'] })
        queryClient.invalidateQueries({ queryKey: ['accounts'] })
        queryClient.invalidateQueries({ queryKey: ['gamification'] })
      }
    },
  })
}

export const useUpdateTransaction = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: TransactionRequest }) => {
      const response = await api.put(`/transactions/${id}`, data)
      return response.data.data
    },
    onMutate: async ({ id, data }) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['transactions'] })
      await queryClient.cancelQueries({ queryKey: ['transactions', id] })
      
      // Snapshot the previous value
      const previousTransactions = queryClient.getQueryData(['transactions'])
      const previousTransaction = queryClient.getQueryData(['transactions', id])
      
      // Optimistically update to the new value
      queryClient.setQueryData(['transactions'], (old: any) => {
        if (!old) return old
        
        return {
          ...old,
          data: old.data.map((transaction: any) =>
            transaction.id === id
              ? { ...transaction, ...data, updatedAt: new Date().toISOString() }
              : transaction
          ),
        }
      })
      
      queryClient.setQueryData(['transactions', id], (old: any) => {
        if (!old) return old
        return { ...old, ...data, updatedAt: new Date().toISOString() }
      })
      
      return { previousTransactions, previousTransaction }
    },
    onError: (err, { id }, context) => {
      // If the mutation fails, rollback
      if (context?.previousTransactions) {
        queryClient.setQueryData(['transactions'], context.previousTransactions)
      }
      if (context?.previousTransaction) {
        queryClient.setQueryData(['transactions', id], context.previousTransaction)
      }
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['transactions', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

export const useDeleteTransaction = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/transactions/${id}`)
    },
    onMutate: async (id) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['transactions'] })
      
      // Snapshot the previous value
      const previousTransactions = queryClient.getQueryData(['transactions'])
      
      // Optimistically remove the transaction
      queryClient.setQueryData(['transactions'], (old: any) => {
        if (!old) return old
        
        return {
          ...old,
          data: old.data.filter((transaction: any) => transaction.id !== id),
        }
      })
      
      return { previousTransactions }
    },
    onError: (err, id, context) => {
      // If the mutation fails, rollback
      if (context?.previousTransactions) {
        queryClient.setQueryData(['transactions'], context.previousTransactions)
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

export const useDeleteTransactionsBatch = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (ids: string[]) => {
      const response = await api.delete('/transactions/batch', { data: ids })
      return response.data
    },
    onMutate: async (ids) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['transactions'] })
      
      // Snapshot the previous value
      const previousTransactions = queryClient.getQueryData(['transactions'])
      
      // Optimistically remove the transactions
      queryClient.setQueryData(['transactions'], (old: any) => {
        if (!old) return old
        
        const idsSet = new Set(ids)
        return {
          ...old,
          data: old.data.filter((transaction: any) => !idsSet.has(transaction.id)),
        }
      })
      
      return { previousTransactions }
    },
    onError: (err, ids, context) => {
      // If the mutation fails, rollback
      if (context?.previousTransactions) {
        queryClient.setQueryData(['transactions'], context.previousTransactions)
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

export const useSuggestCategory = (type: 'INCOME' | 'EXPENSE', description: string) => {
  const hasValidInput = !!type && !!description && description.trim().length >= 2

  return useQuery({
    queryKey: ['categorySuggestion', type, description.trim()],
    queryFn: async () => {
      const response = await api.post<{ data: CategorySuggestion | null }>('/transactions/suggest-category', {
        type,
        description: description.trim(),
      })
      return response.data.data
    },
    enabled: hasValidInput,
    staleTime: 30_000,
  })
}

export const useProcessRecurringTransactions = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async () => {
      const response = await api.post('/transactions/process-recurring')
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}
