import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { AutomationRule, AutomationRuleRequest, ApiResponse } from '../types'

export function useAutomationRules() {
  return useQuery({
    queryKey: ['automation-rules'],
    queryFn: async () => {
      const response = await api.get<{ data: AutomationRule[] }>('/automation-rules')
      return response.data.data
    },
  })
}

export function useAutomationRule(id: string | null) {
  return useQuery({
    queryKey: ['automation-rules', id],
    queryFn: async () => {
      const response = await api.get<{ data: AutomationRule }>(`/automation-rules/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export function useCreateAutomationRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: AutomationRuleRequest) => {
      const response = await api.post<{ data: AutomationRule }>('/automation-rules', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] })
    },
  })
}

export function useUpdateAutomationRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: AutomationRuleRequest }) => {
      const response = await api.put<{ data: AutomationRule }>(`/automation-rules/${id}`, data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] })
    },
  })
}

export function useDeleteAutomationRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/automation-rules/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] })
    },
  })
}
