import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type {
  ApiResponse,
  TaxaCambioResumo,
  TaxaCambioItem,
} from '../types'

interface HistoricoResponse {
  data: TaxaCambioItem[]
  totalElements: number
  totalPages: number
  message: string
  timestamp: string
}

const RESUMO_KEY = ['taxa-cambio', 'resumo']
const HISTORICO_KEY = 'taxa-cambio-historico'
const GRAFICO_KEY = 'taxa-cambio-grafico'

export function useTaxaCambioResumo() {
  return useQuery({
    queryKey: RESUMO_KEY,
    queryFn: async () => {
      const res = await api.get<ApiResponse<TaxaCambioResumo>>('/taxa-cambio/resumo')
      return res.data.data
    },
  })
}

export function useTaxaCambioHistorico(params: {
  moedas?: string[]
  dataInicio?: string
  dataFim?: string
  page?: number
  size?: number
}) {
  const { moedas, dataInicio, dataFim, page = 0, size = 10 } = params
  return useQuery({
    queryKey: [HISTORICO_KEY, moedas, dataInicio, dataFim, page, size],
    queryFn: async () => {
      const searchParams = new URLSearchParams()
      if (moedas?.length) moedas.forEach((m) => searchParams.append('moedas', m))
      if (dataInicio) searchParams.set('dataInicio', dataInicio)
      if (dataFim) searchParams.set('dataFim', dataFim)
      searchParams.set('page', String(page))
      searchParams.set('size', String(size))
      const res = await api.get<HistoricoResponse>(`/taxa-cambio/historico?${searchParams.toString()}`)
      return {
        data: res.data.data,
        totalElements: res.data.totalElements,
        totalPages: res.data.totalPages,
      }
    },
  })
}

export function useTaxaCambioGrafico(moeda: string, dataInicio?: string, dataFim?: string) {
  return useQuery({
    queryKey: [GRAFICO_KEY, moeda, dataInicio, dataFim],
    queryFn: async () => {
      const searchParams = new URLSearchParams()
      searchParams.set('moeda', moeda)
      if (dataInicio) searchParams.set('dataInicio', dataInicio)
      if (dataFim) searchParams.set('dataFim', dataFim)
      const res = await api.get<ApiResponse<TaxaCambioItem[]>>(
        `/taxa-cambio/historico/grafico?${searchParams.toString()}`
      )
      return res.data.data
    },
  })
}

export function useAtualizarTaxaCambio() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiResponse<TaxaCambioResumo>>('/taxa-cambio/atualizar')
      return res.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: RESUMO_KEY })
      queryClient.invalidateQueries({ queryKey: [HISTORICO_KEY] })
      queryClient.invalidateQueries({ queryKey: [GRAFICO_KEY] })
    },
  })
}
