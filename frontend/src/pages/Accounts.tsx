import { useState } from 'react'
import Layout from '../components/Layout'
import {
  useAccounts,
  useDeleteAccount,
} from '../hooks/useAccounts'
import { useCurrencies, formatWithCurrency } from '../hooks/useCurrencies'
import AccountForm from '../components/AccountForm'
import AccountShareDialog from '../components/AccountShareDialog'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import ShareIcon from '@mui/icons-material/Share'
import type { Account } from '../types'

export default function Accounts() {
  const [showForm, setShowForm] = useState(false)
  const [editingAccount, setEditingAccount] = useState<Account | null>(null)
  const [shareAccount, setShareAccount] = useState<Account | null>(null)
  const { data: accounts, isLoading } = useAccounts()
  const { data: currencies } = useCurrencies()
  const deleteMutation = useDeleteAccount()

  const handleEdit = (account: Account) => {
    if (account.sharedWithMe && account.sharedPermission === 'VIEW') return
    setEditingAccount(account)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    if (window.confirm('Tem certeza que deseja excluir esta conta?')) {
      await deleteMutation.mutateAsync(id)
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingAccount(null)
  }

  const formatAccountBalance = (value: number, currencyCode?: string) =>
    formatWithCurrency(value, currencyCode || 'BRL', currencies)

  const getTypeLabel = (type: string) => {
    if (type === 'BANK') return 'Banco'
    if (type === 'CASH') return 'Dinheiro'
    if (type === 'CREDIT') return 'Crédito'
    return type
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Contas
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setShowForm(true)}
          >
            Nova Conta
          </Button>
        </Box>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando contas..." />
            </Box>
          ) : accounts && accounts.length > 0 ? (
            <TableContainer>
              <Table
                sx={{
                  '& .MuiTableCell-root': {
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                  },
                }}
              >
                <TableHead>
                  <TableRow sx={{ bgcolor: 'action.hover' }}>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                      Nome
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                      Tipo
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                      Saldo
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600, typography: 'subtitle1' }}>
                      Ações
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {accounts.map((account) => (
                    <TableRow
                      key={account.id}
                      hover
                      sx={{
                        '&:hover': {
                          bgcolor: (t) => alpha(t.palette.primary.main, 0.08),
                        },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 500 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                          {account.name}
                          {account.sharedWithMe && (
                            <Chip
                              size="small"
                              label={account.ownerName ? `Compartilhada por ${account.ownerName}` : 'Compartilhada'}
                              color="default"
                              variant="outlined"
                            />
                          )}
                        </Box>
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {getTypeLabel(account.type)}
                      </TableCell>
                      <TableCell
                        sx={{
                          fontWeight: 600,
                          color: account.balance >= 0 ? 'text.primary' : 'error.main',
                        }}
                      >
                        {formatAccountBalance(account.balance, account.currencyCode)}
                      </TableCell>
                      <TableCell>
                        {!account.sharedWithMe && (
                          <Tooltip title="Compartilhar conta">
                            <IconButton
                              size="small"
                              onClick={() => setShareAccount(account)}
                              aria-label="Compartilhar"
                            >
                              <ShareIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        <Tooltip
                          title={
                            account.sharedWithMe && account.sharedPermission === 'VIEW'
                              ? 'Somente leitura'
                              : 'Editar'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              onClick={() => handleEdit(account)}
                              aria-label="Editar"
                              disabled={account.sharedWithMe && account.sharedPermission === 'VIEW'}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        {!account.sharedWithMe && (
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDelete(account.id)}
                            aria-label="Excluir"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <CardContent>
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  Nenhuma conta cadastrada
                </Typography>
              </Box>
            </CardContent>
          )}
        </Card>

        {showForm && (
          <AccountForm account={editingAccount} onClose={handleCloseForm} />
        )}
        {shareAccount && (
          <AccountShareDialog
            account={shareAccount}
            open={!!shareAccount}
            onClose={() => setShareAccount(null)}
          />
        )}
      </Box>
    </Layout>
  )
}
