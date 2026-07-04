import { useQuery } from '@tanstack/react-query'
import api from '../services/api'
import type { GamificationSummary, ApiResponse } from '../types'

export const useGamification = () => {
  return useQuery({
    queryKey: ['gamification', 'summary'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<GamificationSummary>>('/gamification/summary')
      return response.data.data
    },
  })
}
