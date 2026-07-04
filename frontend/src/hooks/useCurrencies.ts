import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import type { ApiResponse, Currency, CurrencyRate, CurrencyRateRequest } from '../types'

export const useCurrencies = () => {
  return useQuery({
    queryKey: ['currencies'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Currency[]>>('/currencies')
      return response.data.data
    },
  })
}

export const useCurrencyRates = (from?: string, to?: string) => {
  return useQuery({
    queryKey: ['currency-rates', from, to],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (from) params.append('from', from)
      if (to) params.append('to', to)
      const response = await api.get<ApiResponse<CurrencyRate[]>>(
        `/currency-rates?${params.toString()}`
      )
      return response.data.data
    },
  })
}

export const useLatestCurrencyRate = (from: string, to: string) => {
  return useQuery({
    queryKey: ['currency-rates', 'latest', from, to],
    queryFn: async () => {
      const response = await api.get<ApiResponse<CurrencyRate>>(
        '/currency-rates/latest',
        { params: { from, to } }
      )
      return response.data.data
    },
    enabled: !!from && !!to && from !== to,
  })
}

export const useCreateCurrencyRate = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (data: CurrencyRateRequest) => {
      const response = await api.post<ApiResponse<CurrencyRate>>('/currency-rates', data)
      return response.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currency-rates'] })
    },
  })
}

export const useDeleteCurrencyRates = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ from, to }: { from: string; to: string }) => {
      await api.delete('/currency-rates', { params: { from, to } })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currency-rates'] })
    },
  })
}

export interface ConvertResult {
  fromCurrency: string
  toCurrency: string
  originalAmount: number
  convertedAmount: number
}

export const useConvertCurrency = (amount: number, from: string, to: string) => {
  return useQuery({
    queryKey: ['currency-convert', amount, from, to],
    queryFn: async () => {
      const response = await api.get<ApiResponse<ConvertResult>>(
        '/currency-rates/convert',
        { params: { amount, from, to } }
      )
      return response.data.data
    },
    enabled: amount > 0 && !!from && !!to && from !== to,
  })
}

/** Formata valor com símbolo da moeda (usa lista de moedas para símbolo). */
export function formatWithCurrency(
  value: number,
  currencyCode: string,
  currencies: Currency[] | undefined
): string {
  const code = (currencyCode || 'BRL').toUpperCase()
  const currency = currencies?.find((c) => c.code === code)
  const symbol = currency?.symbol ?? code + ' '
  const locale = 'pt-BR'
  const formatted = new Intl.NumberFormat(locale, {
    minimumFractionDigits: currency?.decimalPlaces ?? 2,
    maximumFractionDigits: currency?.decimalPlaces ?? 2,
  }).format(value)
  return symbol === 'R$' || symbol === 'US$' ? `${symbol} ${formatted}` : `${formatted} ${symbol}`
}
