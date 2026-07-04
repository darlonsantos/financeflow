import { useMemo } from 'react'
import type { Transaction, Goal } from '../types'

export type InsightType = 'CATEGORY_VARIATION' | 'GOALS_DUE' | 'RECURRING_NEXT_MONTH'

export interface Insight {
  id: string
  type: InsightType
  title: string
  message: string
  variant: 'info' | 'warning' | 'success'
  link?: string
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)
}

/**
 * Gera insights automáticos a partir de transações e metas (sem IA externa).
 */
export function useInsights(
  transactions: Transaction[],
  goals: Goal[] | undefined
): Insight[] {
  return useMemo(() => {
    const insights: Insight[] = []
    const now = new Date()

    const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1)
    const currentMonthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0)

    const currentMonthExpenses = transactions.filter(
      (t) =>
        t.type === 'EXPENSE' &&
        new Date(t.date) >= currentMonthStart &&
        new Date(t.date) <= currentMonthEnd
    )
    const lastMonthExpenses = transactions.filter(
      (t) =>
        t.type === 'EXPENSE' &&
        new Date(t.date) >= lastMonthStart &&
        new Date(t.date) <= lastMonthEnd
    )

    const byCategoryThisMonth = new Map<string, { name: string; total: number }>()
    currentMonthExpenses.forEach((t) => {
      const cur = byCategoryThisMonth.get(t.categoryId) ?? {
        name: t.categoryName,
        total: 0,
      }
      cur.total += t.amount
      byCategoryThisMonth.set(t.categoryId, cur)
    })
    const byCategoryLastMonth = new Map<string, number>()
    lastMonthExpenses.forEach((t) => {
      byCategoryLastMonth.set(
        t.categoryId,
        (byCategoryLastMonth.get(t.categoryId) ?? 0) + t.amount
      )
    })

    byCategoryThisMonth.forEach((data, categoryId) => {
      const lastTotal = byCategoryLastMonth.get(categoryId) ?? 0
      if (lastTotal > 0 && data.total > lastTotal) {
        const percent = Math.round(((data.total - lastTotal) / lastTotal) * 100)
        insights.push({
          id: `cat-${categoryId}`,
          type: 'CATEGORY_VARIATION',
          title: 'Gasto acima do mês passado',
          message: `Gasto com ${data.name} ${percent}% acima do mês passado (${formatCurrency(data.total)} este mês).`,
          variant: percent > 50 ? 'warning' : 'info',
          link: '/transactions',
        })
      }
    })

    if (goals && goals.length > 0) {
      const in30Days = now.getTime() + 30 * 24 * 60 * 60 * 1000
      const goalsDueSoon = goals.filter((g) => {
        if (!g.dueDate || g.status !== 'ACTIVE') return false
        const due = new Date(g.dueDate).getTime()
        return due >= now.getTime() && due <= in30Days
      })
      if (goalsDueSoon.length > 0) {
        insights.push({
          id: 'goals-due',
          type: 'GOALS_DUE',
          title: 'Metas vencendo em 30 dias',
          message:
            goalsDueSoon.length === 1
              ? `1 meta vencendo em 30 dias: ${goalsDueSoon[0].name}.`
              : `${goalsDueSoon.length} metas vencendo em 30 dias: ${goalsDueSoon.map((g) => g.name).join(', ')}.`,
          variant: 'warning',
          link: '/goals',
        })
      }
    }

    const recurringExpenses = transactions.filter(
      (t) => t.type === 'EXPENSE' && t.recurring === true
    )
    const recurringSum = recurringExpenses.reduce((sum, t) => sum + t.amount, 0)
    if (recurringSum > 0) {
      insights.push({
        id: 'recurring-next',
        type: 'RECURRING_NEXT_MONTH',
        title: 'Transações recorrentes',
        message: `Transações recorrentes somam ${formatCurrency(recurringSum)} no próximo mês.`,
        variant: 'info',
        link: '/transactions',
      })
    }

    return insights
  }, [transactions, goals])
}
