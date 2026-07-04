import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type {
  ApiResponse,
  OpenFinanceAccount,
  OpenFinanceConnectResponse,
  OpenFinanceConnection,
  OpenFinanceCreditCardSummary,
  OpenFinanceImportedTransaction,
  OpenFinanceSyncHistory,
} from '../types'

export const useOpenFinanceConnections = () =>
  useQuery({
    queryKey: ['open-finance', 'connections'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<OpenFinanceConnection[]>>('/open-finance/connections')
      return response.data.data
    },
  })

export const useConnectOpenFinance = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (provider: string) => {
      const response = await api.post<ApiResponse<OpenFinanceConnectResponse>>('/open-finance/connect', { provider })
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'connections'] })
    },
  })
}

export const useSyncOpenFinanceConnection = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (connectionId: string) => {
      const response = await api.post<ApiResponse<OpenFinanceSyncHistory>>(`/open-finance/connections/${connectionId}/sync`, {})
      return response.data.data
    },
    onSuccess: (_, connectionId) => {
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'connections'] })
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'history', connectionId] })
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'transactions', connectionId] })
    },
  })
}

export const useRevokeOpenFinanceConnection = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (connectionId: string) => {
      await api.post(`/open-finance/connections/${connectionId}/revoke`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'connections'] })
    },
  })
}

export const useConfirmOpenFinanceConnection = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ connectionId, providerConnectionId }: { connectionId: string; providerConnectionId: string }) => {
      const response = await api.post<ApiResponse<OpenFinanceConnection>>(
        `/open-finance/connections/${connectionId}/confirm`,
        { providerConnectionId }
      )
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['open-finance', 'connections'] })
    },
  })
}

export const useOpenFinanceHistory = (connectionId: string) =>
  useQuery({
    queryKey: ['open-finance', 'history', connectionId],
    queryFn: async () => {
      const response = await api.get<ApiResponse<OpenFinanceSyncHistory[]>>(`/open-finance/connections/${connectionId}/history`)
      return response.data.data
    },
    enabled: !!connectionId,
  })

export const useOpenFinanceTransactions = (connectionId: string) =>
  useQuery({
    queryKey: ['open-finance', 'transactions', connectionId],
    queryFn: async () => {
      const response = await api.get<ApiResponse<OpenFinanceImportedTransaction[]>>(
        `/open-finance/connections/${connectionId}/transactions`
      )
      return response.data.data
    },
    enabled: !!connectionId,
  })

export const useOpenFinanceAccounts = (connectionId: string) =>
  useQuery({
    queryKey: ['open-finance', 'accounts', connectionId],
    queryFn: async () => {
      const response = await api.get<ApiResponse<OpenFinanceAccount[]>>(`/open-finance/connections/${connectionId}/accounts`)
      return response.data.data
    },
    enabled: !!connectionId,
  })

export const useOpenFinanceCreditSummary = (connectionId: string) =>
  useQuery({
    queryKey: ['open-finance', 'credit-summary', connectionId],
    queryFn: async () => {
      const response = await api.get<ApiResponse<OpenFinanceCreditCardSummary[]>>(
        `/open-finance/connections/${connectionId}/credit-summary`
      )
      return response.data.data
    },
    enabled: !!connectionId,
  })
