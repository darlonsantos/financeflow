import { useMutation } from '@tanstack/react-query'
import api from '../services/api'
import type { ChatRequest, ChatResponse } from '../types'

export const useAssistantChat = () => {
  return useMutation({
    mutationFn: async (data: ChatRequest): Promise<ChatResponse> => {
      const response = await api.post<{ data: ChatResponse }>('/assistant/chat', data)
      return response.data.data
    },
  })
}
