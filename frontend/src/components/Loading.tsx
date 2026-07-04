import React from 'react'

/**
 * Skeleton para item de lista
 */
export const ListItemSkeleton = () => (
  <div className="animate-pulse flex space-x-4 p-4 border-b">
    <div className="rounded-full bg-gray-300 h-12 w-12"></div>
    <div className="flex-1 space-y-2 py-1">
      <div className="h-4 bg-gray-300 rounded w-3/4"></div>
      <div className="h-3 bg-gray-300 rounded w-1/2"></div>
    </div>
  </div>
)

/**
 * Skeleton para tabela
 */
export const TableSkeleton = ({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) => (
  <div className="animate-pulse">
    {Array.from({ length: rows }).map((_, rowIndex) => (
      <div key={rowIndex} className="flex space-x-4 p-4 border-b">
        {Array.from({ length: columns }).map((_, colIndex) => (
          <div
            key={colIndex}
            className={`h-4 bg-gray-300 rounded ${
              colIndex === 0 ? 'w-1/4' : colIndex === columns - 1 ? 'w-1/6' : 'w-1/3'
            }`}
          ></div>
        ))}
      </div>
    ))}
  </div>
)

/**
 * Skeleton para card
 */
export const CardSkeleton = () => (
  <div className="animate-pulse bg-white rounded-lg shadow-md p-6 space-y-4">
    <div className="h-6 bg-gray-300 rounded w-3/4"></div>
    <div className="space-y-2">
      <div className="h-4 bg-gray-300 rounded"></div>
      <div className="h-4 bg-gray-300 rounded w-5/6"></div>
    </div>
    <div className="h-10 bg-gray-300 rounded w-1/3"></div>
  </div>
)

/**
 * Skeleton para dashboard/estatísticas
 */
export const StatCardSkeleton = () => (
  <div className="animate-pulse bg-white rounded-lg shadow-md p-6">
    <div className="flex items-center justify-between mb-4">
      <div className="h-5 bg-gray-300 rounded w-1/3"></div>
      <div className="h-8 w-8 bg-gray-300 rounded"></div>
    </div>
    <div className="h-8 bg-gray-300 rounded w-2/3 mb-2"></div>
    <div className="h-3 bg-gray-300 rounded w-1/2"></div>
  </div>
)

/**
 * Skeleton para gráfico
 */
export const ChartSkeleton = () => (
  <div className="animate-pulse bg-white rounded-lg shadow-md p-6">
    <div className="h-6 bg-gray-300 rounded w-1/3 mb-4"></div>
    <div className="h-64 bg-gray-200 rounded flex items-end justify-around p-4">
      {Array.from({ length: 7 }).map((_, index) => (
        <div
          key={index}
          className="bg-gray-300 rounded-t w-12"
          style={{ height: `${Math.random() * 80 + 20}%` }}
        ></div>
      ))}
    </div>
  </div>
)

/**
 * Spinner para botões e formulários
 */
export const Spinner = ({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8',
  }

  return (
    <div
      className={`${sizeClasses[size]} border-2 border-gray-300 border-t-indigo-600 rounded-full animate-spin`}
    ></div>
  )
}

/**
 * Loading overlay para tela cheia
 */
export const LoadingOverlay = ({ message = 'Carregando...' }: { message?: string }) => (
  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div className="bg-white rounded-lg p-6 flex flex-col items-center space-y-4">
      <Spinner size="lg" />
      <p className="text-gray-700 font-medium">{message}</p>
    </div>
  </div>
)

/**
 * Loading inline para seções
 */
export const LoadingSection = ({ message = 'Carregando...' }: { message?: string }) => (
  <div className="flex flex-col items-center justify-center py-12 space-y-4">
    <Spinner size="lg" />
    <p className="text-gray-500">{message}</p>
  </div>
)

/**
 * Skeleton para transações (específico)
 */
export const TransactionListSkeleton = ({ count = 5 }: { count?: number }) => (
  <div className="space-y-2">
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="animate-pulse bg-white rounded-lg shadow p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 bg-gray-300 rounded-full"></div>
            <div className="space-y-2">
              <div className="h-4 bg-gray-300 rounded w-32"></div>
              <div className="h-3 bg-gray-300 rounded w-24"></div>
            </div>
          </div>
          <div className="text-right space-y-2">
            <div className="h-5 bg-gray-300 rounded w-20 ml-auto"></div>
            <div className="h-3 bg-gray-300 rounded w-16 ml-auto"></div>
          </div>
        </div>
      </div>
    ))}
  </div>
)

/**
 * Skeleton para contas (específico)
 */
export const AccountListSkeleton = ({ count = 3 }: { count?: number }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="animate-pulse bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="h-5 bg-gray-300 rounded w-1/2"></div>
          <div className="h-4 w-4 bg-gray-300 rounded"></div>
        </div>
        <div className="h-8 bg-gray-300 rounded w-3/4 mb-2"></div>
        <div className="h-3 bg-gray-300 rounded w-1/3"></div>
      </div>
    ))}
  </div>
)

/**
 * Empty State - quando não há dados
 */
export const EmptyState = ({
  icon = '📭',
  title = 'Nenhum item encontrado',
  description = 'Comece criando um novo item',
  action,
}: {
  icon?: string
  title?: string
  description?: string
  action?: React.ReactNode
}) => (
  <div className="flex flex-col items-center justify-center py-12 text-center">
    <div className="text-6xl mb-4">{icon}</div>
    <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
    <p className="text-gray-500 mb-4">{description}</p>
    {action && <div>{action}</div>}
  </div>
)
