import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type {
  Account,
  AccountRequest,
  AccountShareRequest,
  AccountShareResponse,
  ApiResponse,
} from '../types'

export const useAccounts = () => {
  return useQuery({
    queryKey: ['accounts'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Account[]>>('/accounts')
      return response.data.data
    },
  })
}

export const useAccount = (id: string) => {
  return useQuery({
    queryKey: ['accounts', id],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Account>>(`/accounts/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export const useCreateAccount = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (data: AccountRequest) => {
      const response = await api.post<ApiResponse<Account>>('/accounts', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

export const useUpdateAccount = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: AccountRequest }) => {
      const response = await api.put<ApiResponse<Account>>(`/accounts/${id}`, data)
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({ queryKey: ['accounts', variables.id] })
    },
  })
}

export const useDeleteAccount = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/accounts/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
    },
  })
}

// --- Compartilhamento de contas ---

export const useAccountShares = (accountId: string) => {
  return useQuery({
    queryKey: ['accounts', accountId, 'shares'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<AccountShareResponse[]>>(
        `/accounts/${accountId}/shares`
      )
      return response.data.data
    },
    enabled: !!accountId,
  })
}

export const useShareAccount = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      accountId,
      data,
    }: {
      accountId: string
      data: AccountShareRequest
    }) => {
      const response = await api.post<ApiResponse<AccountShareResponse>>(
        `/accounts/${accountId}/shares`,
        data
      )
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({
        queryKey: ['accounts', variables.accountId, 'shares'],
      })
    },
  })
}

export const useUpdateAccountSharePermission = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      accountId,
      sharedWithUserId,
      permission,
    }: {
      accountId: string
      sharedWithUserId: string
      permission: 'VIEW' | 'EDIT'
    }) => {
      const response = await api.put<ApiResponse<AccountShareResponse>>(
        `/accounts/${accountId}/shares/${sharedWithUserId}`,
        null,
        { params: { permission } }
      )
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({
        queryKey: ['accounts', variables.accountId, 'shares'],
      })
    },
  })
}

export const useRevokeAccountShare = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      accountId,
      sharedWithUserId,
    }: {
      accountId: string
      sharedWithUserId: string
    }) => {
      await api.delete(`/accounts/${accountId}/shares/${sharedWithUserId}`)
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({
        queryKey: ['accounts', variables.accountId, 'shares'],
      })
    },
  })
}
