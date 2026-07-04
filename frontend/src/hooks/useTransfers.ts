import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { TransferListItem, TransferRequest, TransferResponse, ApiResponse } from '../types'

export function useTransfers() {
  return useQuery({
    queryKey: ['transfers'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<TransferListItem[]>>('/transfers')
      return response.data.data ?? []
    },
  })
}

export function useCreateTransfer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: TransferRequest) => {
      const response = await api.post<ApiResponse<TransferResponse>>('/transfers', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

export function useDeleteTransfer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (transferId: string) => {
      await api.delete(`/transfers/${transferId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}
