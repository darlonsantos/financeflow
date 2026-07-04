export type AccountSharePermission = 'VIEW' | 'EDIT'

export interface Account {
  id: string
  name: string
  type: 'BANK' | 'CASH' | 'CREDIT'
  balance: number
  initialBalance: number
  color?: string
  icon?: string
  createdAt: string
  updatedAt: string
  /** Código da moeda da conta (ex: BRL, USD). Valores na moeda da conta. */
  currencyCode?: string
  /** Se true, a conta foi compartilhada com o usuário atual */
  sharedWithMe?: boolean
  /** Permissão quando sharedWithMe é true */
  sharedPermission?: AccountSharePermission
  /** Nome do dono da conta quando sharedWithMe é true */
  ownerName?: string
}

export interface Currency {
  code: string
  name: string
  symbol: string
  decimalPlaces: number
}

export interface CurrencyRate {
  id: string
  fromCurrencyCode: string
  toCurrencyCode: string
  rate: number
  effectiveAt: string
  createdAt: string
}

export interface CurrencyRateRequest {
  fromCurrencyCode: string
  toCurrencyCode: string
  rate: number
}

/** Card PTAX na tela de taxa de câmbio (fonte BCB) */
export interface TaxaCambioCard {
  moeda: string
  nomeMoeda: string
  valor: number
  variacaoPercentual: number | null
  dataCotacao: string
  sparkline: number[]
}

/** Resumo da tela de taxa de câmbio (última atualização + cards) */
export interface TaxaCambioResumo {
  ultimaAtualizacao: string | null
  cards: TaxaCambioCard[]
}

/** Item do histórico PTAX (tabela/gráfico) */
export interface TaxaCambioItem {
  id: string
  moeda: string
  valor: number
  variacaoPercentual: number | null
  dataCotacao: string
  criadoEm: string
}

export interface AccountShareResponse {
  id: string
  accountId: string
  sharedWithUserId: string
  sharedWithUserName: string
  sharedWithUserEmail: string
  permission: AccountSharePermission
  createdAt: string
}

export interface AccountShareRequest {
  sharedWithEmail: string
  permission: AccountSharePermission
}

export interface AccountRequest {
  name: string
  type: 'BANK' | 'CASH' | 'CREDIT'
  initialBalance: number
  color?: string
  icon?: string
  /** Código da moeda (3 letras, ex: BRL, USD). Obrigatório na criação; opcional na edição. */
  currencyCode?: string
}

export interface Category {
  id: string
  name: string
  type: 'INCOME' | 'EXPENSE'
  color?: string
  icon?: string
  parentId?: string
  createdAt: string
  updatedAt: string
}

export interface CategoryRequest {
  name: string
  type: 'INCOME' | 'EXPENSE'
  color?: string
  icon?: string
  parentId?: string
}

export interface Transaction {
  id: string
  accountId: string
  accountName: string
  categoryId: string
  categoryName: string
  amount: number
  type: 'INCOME' | 'EXPENSE'
  date: string
  /** Data de vencimento (opcional). Ex.: para contas a pagar. */
  dueDate?: string
  description?: string
  tags?: string[]
  recurring: boolean
  recurringPattern?: string
  createdAt: string
  updatedAt: string
  clientId?: string
  /** Moeda da conta (ex: BRL, USD). O valor amount está nesta moeda. */
  currencyCode?: string
}

export interface TransactionRequest {
  accountId: string
  categoryId: string
  amount: number
  type: 'INCOME' | 'EXPENSE'
  date: string
  /** Data de vencimento (opcional). Ex.: para contas a pagar. */
  dueDate?: string
  description?: string
  tags?: string[]
  recurring?: boolean
  recurringPattern?: string
  clientId?: string
}

export interface ApiResponse<T> {
  data: T
  message: string
  timestamp: string
}

export interface PaginatedResponse<T> {
  data: T[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
  message: string
  timestamp: string
}

export interface Notification {
  id: string
  type: 'BUDGET_EXCEEDED' | 'LOW_BALANCE' | 'BILLS_DUE' | 'GOAL_DUE_SOON' | 'RULE_ALERT'
  title: string
  message: string
  read: boolean
  entityType?: string
  entityId?: string
  metadata?: Record<string, unknown>
  createdAt: string
}

export interface NotificationPreferences {
  id: string
  budgetExceededEnabled: boolean
  lowBalanceEnabled: boolean
  billsDueEnabled: boolean
  goalDueEnabled: boolean
  emailEnabled: boolean
  lowBalanceThreshold: number
}

export interface CategorySuggestion {
  categoryId: string
  categoryName: string
  type: 'INCOME' | 'EXPENSE'
  color?: string
  icon?: string
  confidence: number
  source: 'USER_HISTORY_EXACT' | 'USER_HISTORY_SIMILAR' | 'RECURRING_PATTERN' | 'KEYWORD_MATCH' | 'MOST_USED'
}

export interface CategorySuggestionRequest {
  type: 'INCOME' | 'EXPENSE'
  description?: string
}

export interface ChatRequest {
  message: string
}

export interface ChatResponse {
  message: string
  amount?: number
  period?: string
  category?: string
}

export interface Budget {
  id: string
  categoryId: string
  categoryName: string
  categoryColor?: string
  month: string
  limitAmount: number
  spentAmount: number
  percentUsed: number
  createdAt: string
  updatedAt: string
}

export interface BudgetRequest {
  categoryId: string
  month: string
  limitAmount: number
}

export interface Goal {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  dueDate?: string
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
  percentComplete: number
  createdAt: string
  updatedAt: string
}

export interface GoalRequest {
  name: string
  targetAmount: number
  dueDate?: string
}

export interface GoalContributeRequest {
  amount: number
}

export interface BalanceProjection {
  currentBalance: number
  projectionStartDate: string
  monthsProjected: number
  projections: MonthProjection[]
}

export interface MonthProjection {
  monthLabel: string
  monthStart: string
  balance: number
  projectedIncome: number
  projectedExpense: number
}

export interface NotificationPreferencesRequest {
  budgetExceededEnabled?: boolean
  lowBalanceEnabled?: boolean
  billsDueEnabled?: boolean
  goalDueEnabled?: boolean
  emailEnabled?: boolean
  lowBalanceThreshold?: number
}

export interface GamificationAchievement {
  code: string
  title: string
  description: string
  unlockedAt: string
}

export interface GamificationSummary {
  healthScore: number
  currentStreak: number
  achievements: GamificationAchievement[]
  recentAchievements: GamificationAchievement[]
}

export type AutomationConditionType = 'TRANSACTION_CATEGORY_AMOUNT' | 'ACCOUNT_BALANCE'
export type AutomationActionType = 'MARK_REVIEW' | 'URGENT_ALERT'

export interface AutomationRule {
  id: string
  name: string
  active: boolean
  conditionType: AutomationConditionType
  conditionConfig: Record<string, unknown>
  actionType: AutomationActionType
  actionConfig?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface AutomationRuleRequest {
  name: string
  active?: boolean
  conditionType: AutomationConditionType
  conditionConfig: Record<string, unknown>
  actionType: AutomationActionType
  actionConfig?: Record<string, unknown>
}

export interface AIReport {
  title: string
  content: string
  generatedAt: string
  fromAi: boolean
}

// Inteligência financeira preditiva
export interface PredictiveAlert {
  riskType: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  message: string
  suggestion: string
  entityType?: string | null
  entityId?: string | null
}

export interface PredictiveReport {
  generatedAt: string
  summary: string
  scenarioNextMonths: string
  alerts: PredictiveAlert[]
  historicalMonths: number
  projectionMonths: number
}

// Perfil financeiro comportamental
export interface BehavioralProfile {
  profileType: string
  riskLevel: string
  patterns: string[]
  criticalPoints: string[]
  suggestions: string[]
  generatedAt: string
  fromAi: boolean
}

// Sistema avançado de parcelamento
export type InstallmentGroupType = 'FIXED' | 'VARIABLE' | 'RECURRING'
export type InstallmentGroupStatus = 'ACTIVE' | 'PAID_OFF' | 'CANCELLED'
export type InstallmentItemStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export interface InstallmentItemResponse {
  id: string
  installmentGroupId: string
  installmentNumber: number
  dueDate: string
  amount: number
  status: InstallmentItemStatus
  transactionId?: string
  paidAt?: string
  createdAt: string
  updatedAt: string
}

export interface InstallmentGroupResponse {
  id: string
  accountId: string
  accountName: string
  categoryId: string
  categoryName: string
  description?: string
  totalAmount: number
  installmentType: InstallmentGroupType
  status: InstallmentGroupStatus
  firstDueDate: string
  numberOfInstallments: number
  paidAmount: number
  remainingAmount: number
  paidCount: number
  pendingCount: number
  items: InstallmentItemResponse[]
  createdAt: string
  updatedAt: string
  currencyCode?: string
}

export interface InstallmentGroupRequest {
  accountId: string
  categoryId: string
  description?: string
  totalAmount: number
  installmentType: InstallmentGroupType
  firstDueDate: string
  numberOfInstallments: number
  variableAmounts?: number[]
}

export interface PayInstallmentRequest {
  installmentItemId: string
  paymentDate?: string
}

export interface EarlySettlementRequest {
  installmentGroupId: string
  settlementDate?: string
}

export interface RenegotiateRequest {
  installmentGroupId: string
  newTotalAmount: number
  newFirstDueDate: string
  newNumberOfInstallments: number
  newVariableAmounts?: number[]
  newInstallmentType?: InstallmentGroupType
}

export interface TransferRequest {
  originAccountId: string
  destinationAccountId: string
  transferDate: string
  description?: string
  amount: number
}

export interface TransferResponse {
  id: string
  originAccountName?: string
  destinationAccountName?: string
  transferDate?: string
  amount?: number
  description?: string
}

export interface TransferListItem {
  id: string
  originAccountName: string
  destinationAccountName: string
  transferDate: string
  amount: number
  description: string
}

export interface OpenFinanceConnection {
  id: string
  provider: string
  providerConnectionId: string
  status: 'PENDING' | 'ACTIVE' | 'REVOKED' | 'ERROR'
  expiraEm?: string
  dataCriacao: string
}

export interface OpenFinanceAccount {
  id: string
  providerAccountId: string
  nome: string
  banco?: string
  saldoAtual: number
  tipoConta?: string
}

export interface OpenFinanceImportedTransaction {
  id: string
  providerTransactionId: string
  descricao?: string
  valor: number
  dataTransacao: string
  categoriaSugerida?: string
  statusConciliacao: 'PENDENTE' | 'CONCILIADO' | 'CONFLITO'
  transactionId?: string
}

export interface OpenFinanceSyncHistory {
  id: string
  dataInicio: string
  dataFim?: string
  totalImportado: number
  conflitos: number
  status: 'SUCCESS' | 'ERROR'
  mensagemErro?: string
}

export interface OpenFinanceConnectResponse {
  connectionId: string
  linkToken: string
  providerConnectionId: string
  status: 'PENDING' | 'ACTIVE' | 'REVOKED' | 'ERROR'
}

export interface OpenFinanceCreditCardSummary {
  providerAccountId: string
  nomeConta: string
  moeda?: string
  totalFatura: number
  totalFaturaMesCorrente: number
  pagamentoMinimo?: number
  vencimentoFatura?: string
  fechamentoFatura?: string
  limiteDisponivel?: number
  limiteTotal?: number
}
