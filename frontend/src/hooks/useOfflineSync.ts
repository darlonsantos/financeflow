/**
 * Hook para estado online/offline e sincronização da fila de transações ao reconectar.
 * Usa sync_logs/client_id no backend para resolver conflitos (idempotência).
 */

import { useEffect, useState, useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import api from '../services/api'
import {
  getPendingTransactions,
  removePendingTransaction,
} from '../services/offlineQueue'

export function useOfflineSync() {
  const [isOnline, setIsOnline] = useState(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  )
  const [pendingCount, setPendingCount] = useState(0)
  const [isSyncing, setIsSyncing] = useState(false)
  const [lastSyncError, setLastSyncError] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const updatePendingCount = useCallback(() => {
    setPendingCount(getPendingTransactions().length)
  }, [])

  const syncQueue = useCallback(async () => {
    const pending = getPendingTransactions()
    if (pending.length === 0) {
      updatePendingCount()
      return
    }
    if (!navigator.onLine) return
    setIsSyncing(true)
    setLastSyncError(null)
    let failed = 0
    for (const item of pending) {
      try {
        const payload = { ...item.payload, clientId: item.clientId }
        await api.post('/transactions', payload)
        removePendingTransaction(item.clientId)
      } catch (err: unknown) {
        failed++
        const message = err instanceof Error ? err.message : 'Erro ao sincronizar'
        setLastSyncError(message)
        console.warn('Offline sync failed for', item.clientId, err)
      }
    }
    updatePendingCount()
    if (failed === 0) {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({ queryKey: ['gamification'] })
    }
    setIsSyncing(false)
  }, [queryClient, updatePendingCount])

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true)
      syncQueue()
    }
    const handleOffline = () => setIsOnline(false)
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    updatePendingCount()
    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [syncQueue, updatePendingCount])

  // Atualizar contagem quando a janela ganha foco (ex.: voltou da aba)
  useEffect(() => {
    const handleFocus = () => updatePendingCount()
    window.addEventListener('focus', handleFocus)
    return () => window.removeEventListener('focus', handleFocus)
  }, [updatePendingCount])

  return {
    isOnline,
    pendingCount,
    isSyncing,
    lastSyncError,
    syncQueue,
    updatePendingCount,
  }
}
