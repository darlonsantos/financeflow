import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { Goal, GoalRequest, GoalContributeRequest, ApiResponse } from '../types'

export const useGoals = (status?: string) => {
  return useQuery({
    queryKey: ['goals', status],
    queryFn: async () => {
      const params = status ? { status } : {}
      const response = await api.get<ApiResponse<Goal[]>>('/goals', { params })
      return response.data.data
    },
  })
}

export const useGoal = (id: string) => {
  return useQuery({
    queryKey: ['goals', id],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Goal>>(`/goals/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export const useCreateGoal = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: GoalRequest) => {
      const response = await api.post<ApiResponse<Goal>>('/goals', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
    },
  })
}

export const useUpdateGoal = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: GoalRequest }) => {
      const response = await api.put<ApiResponse<Goal>>(`/goals/${id}`, data)
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      queryClient.invalidateQueries({ queryKey: ['goals', variables.id] })
    },
  })
}

export const useContributeGoal = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: GoalContributeRequest }) => {
      const response = await api.post<ApiResponse<Goal>>(`/goals/${id}/contribute`, data)
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      queryClient.invalidateQueries({ queryKey: ['goals', variables.id] })
    },
  })
}

export const useUpdateGoalStatus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, status }: { id: string; status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED' }) => {
      const response = await api.patch<ApiResponse<Goal>>(`/goals/${id}/status`, null, {
        params: { status },
      })
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      queryClient.invalidateQueries({ queryKey: ['goals', variables.id] })
    },
  })
}

export const useDeleteGoal = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/goals/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
    },
  })
}
