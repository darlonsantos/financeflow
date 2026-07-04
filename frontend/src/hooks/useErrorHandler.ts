import { useCallback } from 'react'
import axios, { AxiosError } from 'axios'
import toast from 'react-hot-toast'

/**
 * Estrutura de erro padronizada da API
 */
interface ApiErrorResponse {
  error?: {
    code: string
    message: string
    // Backend pode retornar details como objeto Map<String, String> ou array
    details?: Array<{
      field: string
      message: string
    }> | Record<string, string>
  }
  timestamp: string
}

/**
 * Categorias de erros para tratamento específico
 */
export enum ErrorCategory {
  VALIDATION = 'VALIDATION',
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  NOT_FOUND = 'NOT_FOUND',
  NETWORK = 'NETWORK',
  SERVER = 'SERVER',
  RATE_LIMIT = 'RATE_LIMIT',
  UNKNOWN = 'UNKNOWN',
}

/**
 * Hook para tratamento robusto de erros
 */
export const useErrorHandler = () => {
  /**
   * Categoriza o erro baseado no código HTTP e código da API
   */
  const categorizeError = useCallback((error: unknown): ErrorCategory => {
    if (!axios.isAxiosError(error)) {
      return ErrorCategory.UNKNOWN
    }

    const axiosError = error as AxiosError<ApiErrorResponse>
    const status = axiosError.response?.status
    const errorCode = axiosError.response?.data?.error?.code

    // Erro de rede
    if (!status) {
      return ErrorCategory.NETWORK
    }

    // Mapeamento de status HTTP para categoria
    switch (status) {
      case 400:
        // Verificar se é erro de validação pelo código ou pela presença de details
        const hasDetails = axiosError.response?.data?.error?.details
        if (errorCode?.includes('VALIDATION') || errorCode === 'VALIDATION_ERROR' || hasDetails) {
          return ErrorCategory.VALIDATION
        }
        return ErrorCategory.UNKNOWN
      case 401:
        return ErrorCategory.AUTHENTICATION
      case 403:
        return ErrorCategory.AUTHORIZATION
      case 404:
        return ErrorCategory.NOT_FOUND
      case 429:
        return ErrorCategory.RATE_LIMIT
      case 500:
      case 502:
      case 503:
      case 504:
        return ErrorCategory.SERVER
      default:
        return ErrorCategory.UNKNOWN
    }
  }, [])

  /**
   * Extrai mensagem amigável do erro
   */
  const getErrorMessage = useCallback((error: unknown): string => {
    if (!axios.isAxiosError(error)) {
      if (error instanceof Error) {
        return error.message
      }
      return 'Ocorreu um erro inesperado'
    }

    const axiosError = error as AxiosError<ApiErrorResponse>
    const apiError = axiosError.response?.data?.error

    // Mensagem específica da API
    if (apiError?.message) {
      return apiError.message
    }

    // Mensagens padrão por status
    const status = axiosError.response?.status
    switch (status) {
      case 400:
        return 'Dados inválidos. Verifique os campos e tente novamente.'
      case 401:
        return 'Sessão expirada. Faça login novamente.'
      case 403:
        return 'Você não tem permissão para realizar esta ação.'
      case 404:
        return 'Recurso não encontrado.'
      case 429:
        return 'Muitas requisições. Aguarde alguns instantes antes de tentar novamente.'
      case 500:
        return 'Erro no servidor. Tente novamente mais tarde.'
      case 502:
      case 503:
        return 'Serviço temporariamente indisponível.'
      case 504:
        return 'Tempo de resposta esgotado. Verifique sua conexão.'
      default:
        return 'Ocorreu um erro. Tente novamente.'
    }
  }, [])

  /**
   * Extrai detalhes de validação (erros por campo)
   * Suporta dois formatos:
   * 1. Array: [{ field: "email", message: "Email inválido" }]
   * 2. Objeto: { "email": "Email inválido" } (formato do backend Spring)
   */
  const getValidationErrors = useCallback((error: unknown): Record<string, string> | null => {
    if (!axios.isAxiosError(error)) {
      return null
    }

    const axiosError = error as AxiosError<ApiErrorResponse>
    const details = axiosError.response?.data?.error?.details

    if (!details) {
      return null
    }

    // Se details é um objeto (Map<String, String> do backend)
    if (!Array.isArray(details)) {
      // Verificar se é um objeto não vazio
      if (typeof details === 'object' && Object.keys(details).length > 0) {
        return details as Record<string, string>
      }
      return null
    }

    // Se details é um array
    if (details.length === 0) {
      return null
    }

    // Converter array para objeto
    return details.reduce((acc, detail) => {
      acc[detail.field] = detail.message
      return acc
    }, {} as Record<string, string>)
  }, [])

  /**
   * Trata erro com toast e retorna informações estruturadas
   */
  const handleError = useCallback((error: unknown, customMessage?: string) => {
    const category = categorizeError(error)
    // Priorizar mensagem da API quando disponível (ex: "Email já está em uso")
    const message = getErrorMessage(error) || customMessage || 'Ocorreu um erro.'
    const validationErrors = getValidationErrors(error)

    // Log detalhado para debug (apenas em desenvolvimento)
    if (import.meta.env.DEV) {
      console.error('Error details:', {
        category,
        message,
        customMessage,
        validationErrors,
        originalError: error,
      })
    }

    // Verificar se é erro de email não verificado (deve mostrar toast)
    // Verificar tanto na mensagem customizada quanto na mensagem original do erro
    const messageLower = message.toLowerCase()
    const originalMessage = axios.isAxiosError(error) 
      ? (error.response?.data?.error?.message || '').toLowerCase()
      : ''
    
    const isEmailNotVerified = messageLower.includes('email não verificado') || 
                              messageLower.includes('email nao verificado') ||
                              messageLower.includes('verifique seu email') ||
                              messageLower.includes('email não foi verificado') ||
                              messageLower.includes('email nao foi verificado') ||
                              originalMessage.includes('email não verificado') ||
                              originalMessage.includes('email nao verificado') ||
                              originalMessage.includes('verifique seu email')

    // Log para debug
    if (import.meta.env.DEV) {
      console.log('Email verification check:', {
        message,
        messageLower,
        originalMessage,
        isEmailNotVerified,
        category
      })
    }

    // Se for erro de email não verificado, sempre mostrar toast
    if (isEmailNotVerified) {
      console.log('Showing email verification toast:', message)
      toast.error(message, {
        icon: '📧',
        duration: 5000,
        id: 'email-not-verified', // ID único para evitar duplicatas
      })
      return {
        category,
        message,
        validationErrors,
      }
    }

    // Não mostrar toast para erros de autenticação (tratados pelo interceptor)
    if (category !== ErrorCategory.AUTHENTICATION) {
      // Toast diferenciado por categoria
      switch (category) {
        case ErrorCategory.VALIDATION:
          if (validationErrors) {
            // Mapear nomes de campos para português amigável
            const fieldNames: Record<string, string> = {
              name: 'Nome',
              email: 'Email',
              password: 'Senha',
              confirmPassword: 'Confirmação de senha',
              description: 'Descrição',
              amount: 'Valor',
              date: 'Data',
              type: 'Tipo',
              accountId: 'Conta',
              categoryId: 'Categoria',
            }
            
            // Mostrar todos os erros de validação
            Object.entries(validationErrors).forEach(([field, msg]) => {
              const fieldLabel = fieldNames[field] || field
              toast.error(`${fieldLabel}: ${msg}`, {
                duration: 4000,
                id: `validation-${field}`, // Evita toasts duplicados
              })
            })
          } else {
            toast.error(message, { duration: 4000 })
          }
          break

        case ErrorCategory.NETWORK:
          toast.error(message, {
            icon: '🌐',
            duration: 5000,
          })
          break

        case ErrorCategory.SERVER:
          toast.error(message, {
            icon: '⚠️',
            duration: 5000,
          })
          break

        case ErrorCategory.NOT_FOUND:
          toast.error(message, {
            icon: '🔍',
            duration: 4000,
          })
          break

        case ErrorCategory.AUTHORIZATION:
          toast.error(message, {
            icon: '🔒',
            duration: 4000,
          })
          break

        case ErrorCategory.RATE_LIMIT:
          toast.error(message, {
            icon: '⏱️',
            duration: 6000, // Duração maior para rate limit
          })
          break

        default:
          toast.error(message, { duration: 4000 })
      }
    }
    // Erros de autenticação não mostram toast aqui (tratados pelo interceptor)
    // Email não verificado já foi tratado acima com return antecipado

    return {
      category,
      message,
      validationErrors,
    }
  }, [categorizeError, getErrorMessage, getValidationErrors])

  /**
   * Versão simplificada que apenas mostra toast
   */
  const showError = useCallback((message: string) => {
    toast.error(message)
  }, [])

  /**
   * Mostra mensagem de sucesso
   */
  const showSuccess = useCallback((message: string) => {
    toast.success(message)
  }, [])

  /**
   * Mostra mensagem informativa
   */
  const showInfo = useCallback((message: string) => {
    toast(message, {
      icon: 'ℹ️',
    })
  }, [])

  return {
    handleError,
    showError,
    showSuccess,
    showInfo,
    categorizeError,
    getErrorMessage,
    getValidationErrors,
  }
}

export default useErrorHandler
