import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { Notification, NotificationPreferences, NotificationPreferencesRequest } from '../types'

interface NotificationsResponse {
  data: Notification[]
  pagination: { page: number; size: number; totalElements: number; totalPages: number }
  unreadCount: number
}

export function useNotifications(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const { data } = await api.get<NotificationsResponse>('/notifications?size=20')
      return data
    },
    enabled: options?.enabled !== false,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
    retry: (failureCount, error) => {
      const status = (error as { response?: { status?: number } })?.response?.status
      return failureCount < 2 && status !== 429 && status !== 403
    },
  })
}

export function useNotificationCount(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['notificationCount'],
    queryFn: async () => {
      const { data } = await api.get<{ data: { unreadCount: number } }>('/notifications/count')
      return data.data?.unreadCount ?? 0
    },
    enabled: options?.enabled !== false,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
    retry: (failureCount, error) => {
      const status = (error as { response?: { status?: number } })?.response?.status
      return failureCount < 2 && status !== 429 && status !== 403
    },
  })
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.post(`/notifications/${id}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notificationCount'] })
    },
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post('/notifications/read-all'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notificationCount'] })
    },
  })
}

export function useRefreshNotifications() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post('/notifications/refresh'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notificationCount'] })
    },
  })
}

export function useNotificationPreferences() {
  return useQuery({
    queryKey: ['notificationPreferences'],
    queryFn: async () => {
      const { data } = await api.get<{ data: NotificationPreferences }>('/notifications/preferences')
      return data.data
    },
  })
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: NotificationPreferencesRequest) =>
      api.put<{ data: NotificationPreferences }>('/notifications/preferences', request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationPreferences'] })
    },
  })
}
