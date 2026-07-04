import { useState } from 'react'
import toast from 'react-hot-toast'
import { PluggyConnect } from 'react-pluggy-connect'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import SyncIcon from '@mui/icons-material/Sync'
import LinkIcon from '@mui/icons-material/Link'
import LinkOffIcon from '@mui/icons-material/LinkOff'
import Layout from '../components/Layout'
import { LoadingSection } from '../components/Loading'
import {
  useAccounts,
} from '../hooks/useAccounts'
import {
  useCategories,
} from '../hooks/useCategories'
import {
  useCreateTransaction,
} from '../hooks/useTransactions'
import {
  useConnectOpenFinance,
  useConfirmOpenFinanceConnection,
  useOpenFinanceAccounts,
  useOpenFinanceConnections,
  useOpenFinanceCreditSummary,
  useOpenFinanceHistory,
  useOpenFinanceTransactions,
  useRevokeOpenFinanceConnection,
  useSyncOpenFinanceConnection,
} from '../hooks/useOpenFinance'
import type { OpenFinanceCreditCardSummary } from '../types'

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('pt-BR')
}

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

const getAccountTypeLabel = (tipoConta?: string) => {
  if (!tipoConta) return '-'
  const normalized = tipoConta.toUpperCase()
  if (normalized === 'CHECKING_ACCOUNT') return 'Conta Corrente'
  if (normalized === 'SAVINGS_ACCOUNT') return 'Conta Poupança'
  if (normalized === 'CREDIT_CARD') return 'Cartão de Crédito'
  return tipoConta.split('_').join(' ')
}

const getApiErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'object' && error !== null) {
    const maybeAxios = error as {
      response?: { data?: { error?: { message?: string }; message?: string } }
      message?: string
    }
    return maybeAxios.response?.data?.error?.message || maybeAxios.response?.data?.message || maybeAxios.message || fallback
  }
  return fallback
}

export default function OpenFinance() {
  const [selectedConnectionId, setSelectedConnectionId] = useState<string>('')
  const [widgetConnectToken, setWidgetConnectToken] = useState<string>('')
  const [pendingConnectionId, setPendingConnectionId] = useState<string>('')
  const { data: connections, isLoading } = useOpenFinanceConnections()
  const connectMutation = useConnectOpenFinance()
  const confirmMutation = useConfirmOpenFinanceConnection()
  const syncMutation = useSyncOpenFinanceConnection()
  const revokeMutation = useRevokeOpenFinanceConnection()

  const activeConnectionId = selectedConnectionId || connections?.[0]?.id || ''
  const { data: history } = useOpenFinanceHistory(activeConnectionId)
  const { data: importedTransactions } = useOpenFinanceTransactions(activeConnectionId)
  const { data: importedAccounts } = useOpenFinanceAccounts(activeConnectionId)
  const { data: creditSummaries } = useOpenFinanceCreditSummary(activeConnectionId)
  const { data: accounts } = useAccounts()
  const { data: expenseCategories } = useCategories('EXPENSE')
  const createTransactionMutation = useCreateTransaction()
  const latestSync = history?.[0]
  const [invoiceDialogOpen, setInvoiceDialogOpen] = useState(false)
  const [selectedInvoice, setSelectedInvoice] = useState<OpenFinanceCreditCardSummary | null>(null)
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [selectedCategoryId, setSelectedCategoryId] = useState('')

  const connect = async () => {
    try {
      const response = await connectMutation.mutateAsync('pluggy')
      toast.success('Conectando no Pluggy...')
      setSelectedConnectionId(response.connectionId)
      setPendingConnectionId(response.connectionId)
      setWidgetConnectToken(response.linkToken)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Falha ao conectar banco'))
    }
  }

  const handleWidgetSuccess = async (data: unknown) => {
    const payload = data as { item?: { id?: string } }
    const itemId = payload?.item?.id
    if (!itemId || !pendingConnectionId) {
      toast.error('Pluggy não retornou itemId da conexão')
      return
    }
    try {
      await confirmMutation.mutateAsync({ connectionId: pendingConnectionId, providerConnectionId: itemId })
      setWidgetConnectToken('')
      toast.success('Conexão confirmada. Sincronizando...')
      await syncMutation.mutateAsync(pendingConnectionId)
      toast.success('Sincronização concluída')
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Falha ao confirmar conexão'))
    }
  }

  const sync = async (connectionId: string) => {
    try {
      await syncMutation.mutateAsync(connectionId)
      toast.success('Sincronização concluída')
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Falha na sincronização'))
    }
  }

  const revoke = async (connectionId: string) => {
    if (!window.confirm('Deseja revogar esta conexão bancária?')) return
    try {
      await revokeMutation.mutateAsync(connectionId)
      toast.success('Conexão revogada com sucesso')
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Falha ao revogar conexão'))
    }
  }

  const openInvoiceDialog = (summary: OpenFinanceCreditCardSummary) => {
    setSelectedInvoice(summary)
    setSelectedAccountId(accounts?.[0]?.id || '')
    setSelectedCategoryId(expenseCategories?.[0]?.id || '')
    setInvoiceDialogOpen(true)
  }

  const addInvoiceAsExpense = async () => {
    if (!selectedInvoice) return
    if (!selectedAccountId || !selectedCategoryId) {
      toast.error('Selecione conta e categoria para lançar a despesa')
      return
    }
    if (!selectedInvoice.totalFaturaMesCorrente || selectedInvoice.totalFaturaMesCorrente <= 0) {
      toast.error('Não há valor de fatura do mês para lançar')
      return
    }

    const now = new Date()
    const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const monthLabel = `${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`
    try {
      await createTransactionMutation.mutateAsync({
        accountId: selectedAccountId,
        categoryId: selectedCategoryId,
        amount: selectedInvoice.totalFaturaMesCorrente,
        type: 'EXPENSE',
        date,
        description: `Fatura cartão ${selectedInvoice.nomeConta} - ${monthLabel}`,
      })
      toast.success('Fatura do mês adicionada como despesa')
      setInvoiceDialogOpen(false)
      setSelectedInvoice(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Falha ao lançar despesa da fatura'))
    }
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Open Finance
          </Typography>
          <Button
            variant="contained"
            startIcon={<LinkIcon />}
            onClick={connect}
            disabled={connectMutation.isPending}
          >
            Conectar Banco
          </Button>
        </Box>

        <Alert severity="info">
          MVP em modo leitura: sincroniza contas, saldo e transações importadas com conciliação automática.
          {' '}
          Última execução: {formatDateTime(latestSync?.dataFim || latestSync?.dataInicio)}
        </Alert>

        {widgetConnectToken && (
          <PluggyConnect
            connectToken={widgetConnectToken}
            includeSandbox={import.meta.env.DEV}
            onSuccess={handleWidgetSuccess}
            onError={(error: unknown) => {
              toast.error(getApiErrorMessage(error, 'Falha no widget da Pluggy'))
              setWidgetConnectToken('')
            }}
            onClose={() => {
              setWidgetConnectToken('')
            }}
          />
        )}

        <Card elevation={1}>
          <CardContent>
            {isLoading ? (
              <LoadingSection message="Carregando conexões..." />
            ) : !connections?.length ? (
              <Typography color="text.secondary">Nenhuma conexão cadastrada.</Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Provider</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Expira em</TableCell>
                    <TableCell>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {connections.map((connection) => (
                    <TableRow
                      key={connection.id}
                      hover
                      selected={activeConnectionId === connection.id}
                      onClick={() => setSelectedConnectionId(connection.id)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{connection.provider}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={connection.status}
                          color={connection.status === 'ACTIVE' ? 'success' : connection.status === 'PENDING' ? 'warning' : 'default'}
                        />
                      </TableCell>
                      <TableCell>{formatDateTime(connection.expiraEm)}</TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<SyncIcon />}
                            onClick={(event) => {
                              event.stopPropagation()
                              sync(connection.id)
                            }}
                            disabled={connection.status !== 'ACTIVE' || syncMutation.isPending}
                          >
                            Sincronizar
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            variant="outlined"
                            startIcon={<LinkOffIcon />}
                            onClick={(event) => {
                              event.stopPropagation()
                              revoke(connection.id)
                            }}
                            disabled={revokeMutation.isPending}
                          >
                            Desconectar
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card elevation={1}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Fatura do cartão
            </Typography>
            {!creditSummaries?.length ? (
              <Typography color="text.secondary">Nenhuma fatura de cartão disponível para esta conexão.</Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Conta</TableCell>
                    <TableCell>Fatura mês corrente</TableCell>
                    <TableCell>Saldo total fatura</TableCell>
                    <TableCell>Pagamento mínimo</TableCell>
                    <TableCell>Vencimento</TableCell>
                    <TableCell>Fechamento</TableCell>
                    <TableCell>Limite disponível</TableCell>
                    <TableCell>Ação</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {creditSummaries.map((summary) => (
                    <TableRow key={summary.providerAccountId}>
                      <TableCell>{summary.nomeConta}</TableCell>
                      <TableCell>{formatCurrency(summary.totalFaturaMesCorrente || 0)}</TableCell>
                      <TableCell>{formatCurrency(summary.totalFatura || 0)}</TableCell>
                      <TableCell>
                        {summary.pagamentoMinimo != null ? formatCurrency(summary.pagamentoMinimo) : '-'}
                      </TableCell>
                      <TableCell>
                        {summary.vencimentoFatura
                          ? new Date(summary.vencimentoFatura).toLocaleDateString('pt-BR')
                          : '-'}
                      </TableCell>
                      <TableCell>
                        {summary.fechamentoFatura
                          ? new Date(summary.fechamentoFatura).toLocaleDateString('pt-BR')
                          : '-'}
                      </TableCell>
                      <TableCell>
                        {summary.limiteDisponivel != null ? formatCurrency(summary.limiteDisponivel) : '-'}
                      </TableCell>
                      <TableCell>
                        <Tooltip title="adicionar despesas">
                          <span>
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => openInvoiceDialog(summary)}
                              disabled={!summary.totalFaturaMesCorrente || summary.totalFaturaMesCorrente <= 0}
                              aria-label="adicionar despesas"
                            >
                              <AddCircleOutlineIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card elevation={1}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Contas importadas
            </Typography>
            {!importedAccounts?.length ? (
              <Typography color="text.secondary">Sem contas importadas para esta conexão.</Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Nome</TableCell>
                    <TableCell>Banco</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell>Saldo</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {importedAccounts.map((account) => (
                    <TableRow key={account.id}>
                      <TableCell>{account.nome}</TableCell>
                      <TableCell>{account.banco || '-'}</TableCell>
                      <TableCell>{getAccountTypeLabel(account.tipoConta)}</TableCell>
                      <TableCell>{formatCurrency(account.saldoAtual)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card elevation={1}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Transações importadas
            </Typography>
            {!importedTransactions?.length ? (
              <Typography color="text.secondary">Sem transações importadas.</Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Data</TableCell>
                    <TableCell>Descrição</TableCell>
                    <TableCell>Valor</TableCell>
                    <TableCell>Conciliação</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {importedTransactions.map((transaction) => (
                    <TableRow key={transaction.id}>
                      <TableCell>{new Date(transaction.dataTransacao).toLocaleDateString('pt-BR')}</TableCell>
                      <TableCell>{transaction.descricao || '-'}</TableCell>
                      <TableCell>{formatCurrency(transaction.valor)}</TableCell>
                      <TableCell>{transaction.statusConciliacao}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
        <Dialog open={invoiceDialogOpen} onClose={() => setInvoiceDialogOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>Adicionar fatura mensal como despesa</DialogTitle>
          <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <Typography variant="body2" color="text.secondary">
              {selectedInvoice
                ? `Cartão: ${selectedInvoice.nomeConta} | Valor do mês: ${formatCurrency(selectedInvoice.totalFaturaMesCorrente || 0)}`
                : '-'}
            </Typography>
            <FormControl fullWidth>
              <InputLabel id="invoice-account-label">Conta de destino</InputLabel>
              <Select
                labelId="invoice-account-label"
                label="Conta de destino"
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
              >
                {(accounts || []).map((account) => (
                  <MenuItem key={account.id} value={account.id}>
                    {account.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel id="invoice-category-label">Categoria de despesa</InputLabel>
              <Select
                labelId="invoice-category-label"
                label="Categoria de despesa"
                value={selectedCategoryId}
                onChange={(e) => setSelectedCategoryId(e.target.value)}
              >
                {(expenseCategories || []).map((category) => (
                  <MenuItem key={category.id} value={category.id}>
                    {category.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setInvoiceDialogOpen(false)}>Cancelar</Button>
            <Button
              variant="contained"
              onClick={addInvoiceAsExpense}
              disabled={createTransactionMutation.isPending}
            >
              Confirmar
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </Layout>
  )
}
