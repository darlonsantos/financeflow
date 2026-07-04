import { useQuery } from '@tanstack/react-query'
import api from '../services/api'
import type { BalanceProjection, ApiResponse } from '../types'

export const useBalanceProjection = (months: number = 12) => {
  return useQuery({
    queryKey: ['projections', 'balance', months],
    queryFn: async () => {
      const response = await api.get<ApiResponse<BalanceProjection>>('/projections/balance', {
        params: { months },
      })
      return response.data.data
    },
  })
}
