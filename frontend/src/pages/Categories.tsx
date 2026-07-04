import { useState } from 'react'
import Layout from '../components/Layout'
import { useCategories, useDeleteCategory } from '../hooks/useCategories'
import CategoryForm from '../components/CategoryForm'
import { LoadingSection } from '../components/Loading'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import DownloadIcon from '@mui/icons-material/Download'
import toast from 'react-hot-toast'
import api from '../services/api'
import type { Category } from '../types'

export default function Categories() {
  const [showForm, setShowForm] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [filterType, setFilterType] = useState<'INCOME' | 'EXPENSE' | ''>('')
  const [reportLoading, setReportLoading] = useState(false)
  const { data: categories, isLoading } = useCategories(filterType || undefined)
  const deleteMutation = useDeleteCategory()

  const handleEdit = (category: Category) => {
    setEditingCategory(category)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    if (window.confirm('Tem certeza que deseja excluir esta categoria?')) {
      await deleteMutation.mutateAsync(id)
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingCategory(null)
  }

  const handleGenerateReport = async () => {
    setReportLoading(true)
    try {
      const params = new URLSearchParams()
      if (filterType) params.append('type', filterType)

      const response = await api.get(`/categories/report?${params.toString()}`, {
        responseType: 'arraybuffer',
      })

      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'relatorio_categorias.pdf')
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)

      toast.success('Relatório gerado com sucesso!')
    } catch {
      toast.error('Erro ao gerar relatório')
    } finally {
      setReportLoading(false)
    }
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h4" component="h1" fontWeight={500}>
            Categorias
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={reportLoading ? <CircularProgress size={16} /> : <DownloadIcon />}
              onClick={handleGenerateReport}
              disabled={reportLoading}
            >
              Relatório
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setShowForm(true)}
            >
              Nova Categoria
            </Button>
          </Box>
        </Box>

        <Card elevation={1}>
          <CardContent>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel id="filter-type-label">Tipo</InputLabel>
              <Select
                labelId="filter-type-label"
                value={filterType}
                label="Tipo"
                onChange={(e) => setFilterType(e.target.value as '' | 'INCOME' | 'EXPENSE')}
              >
                <MenuItem value="">Todos os tipos</MenuItem>
                <MenuItem value="INCOME">Receitas</MenuItem>
                <MenuItem value="EXPENSE">Despesas</MenuItem>
              </Select>
            </FormControl>
          </CardContent>
        </Card>

        <Card elevation={1}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              <LoadingSection message="Carregando categorias..." />
            </Box>
          ) : categories && categories.length > 0 ? (
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
                      Ações
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {categories.map((category) => (
                    <TableRow
                      key={category.id}
                      hover
                      sx={{
                        '&:hover': {
                          bgcolor: (t) => alpha(t.palette.primary.main, 0.08),
                        },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 500 }}>{category.name}</TableCell>
                      <TableCell>
                        <Chip
                          label={category.type === 'INCOME' ? 'Receita' : 'Despesa'}
                          size="small"
                          color={category.type === 'INCOME' ? 'success' : 'error'}
                          sx={{ fontWeight: 500 }}
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleEdit(category)}
                          aria-label="Editar"
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(category.id)}
                          aria-label="Excluir"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
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
                  Nenhuma categoria cadastrada
                </Typography>
              </Box>
            </CardContent>
          )}
        </Card>

        {showForm && (
          <CategoryForm category={editingCategory} onClose={handleCloseForm} />
        )}
      </Box>
    </Layout>
  )
}
