import { useQuery } from '@tanstack/react-query'
import api from '../services/api'
import type { BehavioralProfile, ApiResponse } from '../types'

export const useBehavioralProfile = () => {
  return useQuery({
    queryKey: ['behavioral-profile'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<BehavioralProfile>>('/behavioral-profile')
      return response.data.data
    },
  })
}
