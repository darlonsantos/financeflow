import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { Category, CategoryRequest, ApiResponse } from '../types'

export const useCategories = (type?: 'INCOME' | 'EXPENSE') => {
  return useQuery({
    queryKey: ['categories', type],
    queryFn: async () => {
      const params = type ? { type } : {}
      const response = await api.get<ApiResponse<Category[]>>('/categories', { params })
      return response.data.data
    },
  })
}

export const useCategory = (id: string) => {
  return useQuery({
    queryKey: ['categories', id],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Category>>(`/categories/${id}`)
      return response.data.data
    },
    enabled: !!id,
  })
}

export const useCreateCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (data: CategoryRequest) => {
      // Remover campos undefined/null antes de enviar
      const cleanData: any = {
        name: data.name,
        type: data.type,
      }
      
      if (data.color) cleanData.color = data.color
      if (data.icon) cleanData.icon = data.icon
      if (data.parentId) cleanData.parentId = data.parentId
      
      const response = await api.post<ApiResponse<Category>>('/categories', cleanData)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
  })
}

export const useUpdateCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: CategoryRequest }) => {
      // Remover campos undefined/null antes de enviar
      const cleanData: any = {
        name: data.name,
        type: data.type,
      }
      
      if (data.color) cleanData.color = data.color
      if (data.icon) cleanData.icon = data.icon
      if (data.parentId) cleanData.parentId = data.parentId
      
      const response = await api.put<ApiResponse<Category>>(`/categories/${id}`, cleanData)
      return response.data.data
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.invalidateQueries({ queryKey: ['categories', variables.id] })
    },
  })
}

export const useDeleteCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/categories/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
  })
}
