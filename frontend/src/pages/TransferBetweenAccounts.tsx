import { useState } from 'react'
import Layout from '../components/Layout'
import { useTransfers, useDeleteTransfer } from '../hooks/useTransfers'
import { useCurrencies, formatWithCurrency } from '../hooks/useCurrencies'
import TransferForm from '../components/TransferForm'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
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
import DeleteIcon from '@mui/icons-material/Delete'
import type { TransferListItem } from '../types'

function formatTransferDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export default function TransferBetweenAccounts() {
  const [showForm, setShowForm] = useState(false)
  const { data: transfers, isLoading } = useTransfers()
  const { data: currencies } = useCurrencies()
  const deleteMutation = useDeleteTransfer()

  const handleDelete = async (item: TransferListItem) => {
    if (window.confirm('Tem certeza que deseja excluir esta transferência?')) {
      await deleteMutation.mutateAsync(item.id)
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Typography variant="h4" component="h1" fontWeight={500}>
            Transferência entre Contas
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setShowForm(true)}
          >
            Nova Transferência
          </Button>
        </Box>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando transferências..." />
            </Box>
          ) : transfers && transfers.length > 0 ? (
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
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Conta de Origem
                    </TableCell>
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Conta de Destino
                    </TableCell>
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Data
                    </TableCell>
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Motivo
                    </TableCell>
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Valor
                    </TableCell>
                    <TableCell
                      sx={{ fontWeight: 600, typography: 'subtitle1' }}
                    >
                      Ações
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {transfers.map((item) => (
                    <TableRow
                      key={item.id}
                      hover
                      sx={{
                        '&:hover': {
                          bgcolor: (t) =>
                            alpha(t.palette.primary.main, 0.08),
                        },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 500 }}>
                        {item.originAccountName}
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {item.destinationAccountName}
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {formatTransferDate(item.transferDate)}
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>
                        {item.description || '—'}
                      </TableCell>
                      <TableCell
                        sx={{
                          fontWeight: 600,
                          color: 'text.primary',
                        }}
                      >
                        {formatWithCurrency(
                          item.amount,
                          'BRL',
                          currencies
                        )}
                      </TableCell>
                      <TableCell>
                        <Tooltip title="Excluir">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDelete(item)}
                            aria-label="Excluir"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
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
                  Nenhuma transferência cadastrada
                </Typography>
              </Box>
            </CardContent>
          )}
        </Card>

        {showForm && <TransferForm onClose={handleCloseForm} />}
      </Box>
    </Layout>
  )
}
