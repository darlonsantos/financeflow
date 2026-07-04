import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { Budget, BudgetRequest, ApiResponse } from '../types'

export const useBudgets = (month?: string) => {
  return useQuery({
    queryKey: ['budgets', month],
    queryFn: async () => {
      const params = month ? { month } : {}
      const response = await api.get<ApiResponse<Budget[]>>('/budgets', { params })
      return response.data.data
    },
  })
}

export const useBudget = (id: string) => {
  return useQuery({
    queryKey: ['budgets', id],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Budget>>(`/budgets/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export const useCreateBudget = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: BudgetRequest) => {
      const response = await api.post<ApiResponse<Budget>>('/budgets', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
    },
  })
}

export const useUpdateBudget = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: BudgetRequest }) => {
      const response = await api.put<ApiResponse<Budget>>(`/budgets/${id}`, data)
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      queryClient.invalidateQueries({ queryKey: ['budgets', variables.id] })
    },
  })
}

export const useDeleteBudget = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/budgets/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
    },
  })
}
