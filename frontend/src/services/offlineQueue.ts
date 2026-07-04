/**
 * Fila de transações criadas offline. Persistida em localStorage.
 * Cada item tem clientId para idempotência no backend ao reconectar.
 */

import type { TransactionRequest } from '../types'

const STORAGE_KEY = 'financeflow_offline_transactions'

export interface PendingTransaction {
  clientId: string
  payload: TransactionRequest
  createdAt: string
}

function loadQueue(): PendingTransaction[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as PendingTransaction[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function saveQueue(queue: PendingTransaction[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue))
  } catch (e) {
    console.error('offlineQueue: save failed', e)
  }
}

export function getPendingTransactions(): PendingTransaction[] {
  return loadQueue()
}

export function addPendingTransaction(clientId: string, payload: TransactionRequest): void {
  const queue = loadQueue()
  queue.push({
    clientId,
    payload: { ...payload, clientId },
    createdAt: new Date().toISOString(),
  })
  saveQueue(queue)
}

export function removePendingTransaction(clientId: string): void {
  const queue = loadQueue().filter((t) => t.clientId !== clientId)
  saveQueue(queue)
}

export function clearPendingTransactions(): void {
  saveQueue([])
}

export function getPendingCount(): number {
  return loadQueue().length
}
