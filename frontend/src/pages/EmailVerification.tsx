import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { useErrorHandler } from '../hooks/useErrorHandler'
import api from '../services/api'

export default function EmailVerification() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { handleError, showSuccess } = useErrorHandler()
  const [loading, setLoading] = useState(true)
  const [verified, setVerified] = useState(false)
  const token = searchParams.get('token')

  useEffect(() => {
    if (!token) {
      handleError(new Error('Token não fornecido'), 'Link inválido.')
      navigate('/login')
      return
    }

    verifyEmail()
  }, [token, navigate, handleError])

  const verifyEmail = async () => {
    try {
      await api.post('/auth/email-verification/verify', null, {
        params: { token }
      })
      setVerified(true)
      showSuccess('Email verificado com sucesso!')
      setTimeout(() => {
        navigate('/login')
      }, 3000)
    } catch (err) {
      handleError(err, 'Erro ao verificar email. O token pode ter expirado.')
      setLoading(false)
    }
  }

  if (loading && !verified) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
        <div className="text-center">
          <svg className="animate-spin h-12 w-12 text-indigo-600 mx-auto mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p className="text-gray-600 dark:text-gray-400">Verificando email...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 relative overflow-hidden">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-purple-300 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-300 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob animation-delay-2000"></div>
      </div>

      <div className="relative z-10 w-full max-w-md px-6 py-8">
        <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-2xl shadow-2xl border border-white/20 dark:border-gray-700/50 p-8 text-center">
          {verified ? (
            <>
              <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 dark:bg-green-900/30 rounded-full mb-4">
                <svg className="w-10 h-10 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                Email verificado!
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                Seu email foi verificado com sucesso. Você será redirecionado para o login em instantes.
              </p>
              <Link
                to="/login"
                className="inline-flex items-center justify-center px-6 py-3 border border-transparent rounded-xl text-white font-semibold bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 transition-all duration-200"
              >
                Ir para login
              </Link>
            </>
          ) : (
            <>
              <div className="inline-flex items-center justify-center w-20 h-20 bg-red-100 dark:bg-red-900/30 rounded-full mb-4">
                <svg className="w-10 h-10 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                Falha na verificação
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                Não foi possível verificar seu email. O link pode ter expirado ou ser inválido.
              </p>
              <Link
                to="/login"
                className="inline-flex items-center justify-center px-6 py-3 border border-transparent rounded-xl text-white font-semibold bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 transition-all duration-200"
              >
                Voltar para login
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
