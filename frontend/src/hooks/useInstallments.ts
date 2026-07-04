import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type {
  InstallmentGroupResponse,
  InstallmentGroupRequest,
  InstallmentGroupStatus,
  InstallmentGroupType,
  PayInstallmentRequest,
  EarlySettlementRequest,
  RenegotiateRequest,
  ApiResponse,
  PaginatedResponse,
} from '../types'

interface UseInstallmentsFilters {
  search?: string
  status?: InstallmentGroupStatus | 'ALL'
  installmentType?: InstallmentGroupType | 'ALL'
}

export function useInstallments(page: number = 0, size: number = 20, filters?: UseInstallmentsFilters) {
  const normalizedSearch = filters?.search?.trim() || undefined
  const status = filters?.status && filters.status !== 'ALL' ? filters.status : undefined
  const installmentType =
    filters?.installmentType && filters.installmentType !== 'ALL' ? filters.installmentType : undefined

  return useQuery({
    queryKey: ['installments', page, size, normalizedSearch, status, installmentType],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<InstallmentGroupResponse>>('/installments', {
        params: {
          page,
          size,
          sort: 'firstDueDate,desc',
          search: normalizedSearch,
          status,
          installmentType,
        },
      })
      return response.data as PaginatedResponse<InstallmentGroupResponse>
    },
  })
}

export function useInstallment(id: string | null) {
  return useQuery({
    queryKey: ['installments', id],
    queryFn: async () => {
      const response = await api.get<ApiResponse<InstallmentGroupResponse>>(`/installments/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export function useCreateInstallment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: InstallmentGroupRequest) => {
      const response = await api.post<ApiResponse<InstallmentGroupResponse>>('/installments', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installments'] })
    },
  })
}

export function usePayInstallment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: PayInstallmentRequest) => {
      const response = await api.post<ApiResponse<InstallmentGroupResponse>>('/installments/pay', data)
      return response.data.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['installments'] })
      queryClient.invalidateQueries({ queryKey: ['installments', data.id] })
    },
  })
}

export function useEarlySettlement() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: EarlySettlementRequest) => {
      const response = await api.post<ApiResponse<InstallmentGroupResponse>>('/installments/early-settlement', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installments'] })
    },
  })
}

export function useRenegotiate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: RenegotiateRequest) => {
      const response = await api.post<ApiResponse<InstallmentGroupResponse>>('/installments/renegotiate', data)
      return response.data.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['installments'] })
      queryClient.invalidateQueries({ queryKey: ['installments', data.id] })
    },
  })
}

export function useCancelInstallment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/installments/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installments'] })
    },
  })
}
