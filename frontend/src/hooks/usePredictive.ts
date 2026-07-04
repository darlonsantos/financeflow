import { useQuery } from '@tanstack/react-query'
import api from '../services/api'
import type { PredictiveReport, ApiResponse } from '../types'

export const usePredictiveReport = () => {
  return useQuery({
    queryKey: ['predictive', 'report'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<PredictiveReport>>('/predictive/report')
      return response.data.data
    },
  })
}
