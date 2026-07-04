import { useState, useEffect } from 'react'
import { useCreateCategory, useUpdateCategory, useCategories } from '../hooks/useCategories'
import { useErrorHandler } from '../hooks/useErrorHandler'
import type { Category, CategoryRequest } from '../types'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface CategoryFormProps {
  category?: Category | null
  onClose: () => void
}

export default function CategoryForm({ category, onClose }: CategoryFormProps) {
  const createMutation = useCreateCategory()
  const updateMutation = useUpdateCategory()
  const { data: allCategories } = useCategories()
  const { handleError, showSuccess } = useErrorHandler()

  const [formData, setFormData] = useState<CategoryRequest>({
    name: '',
    type: 'EXPENSE',
    color: '#3B82F6',
    icon: undefined,
    parentId: undefined,
  })

  useEffect(() => {
    if (category) {
      setFormData({
        name: category.name,
        type: category.type,
        color: category.color || '#3B82F6',
        icon: category.icon || undefined,
        parentId: category.parentId,
      })
    }
  }, [category])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const dataToSend: CategoryRequest = {
        name: formData.name,
        type: formData.type,
        color: formData.color && formData.color.trim() !== '' ? formData.color : undefined,
        icon: formData.icon && formData.icon.trim() !== '' ? formData.icon : undefined,
        parentId: formData.parentId && formData.parentId.trim() !== '' ? formData.parentId : undefined,
      }

      if (category) {
        await updateMutation.mutateAsync({ id: category.id, data: dataToSend })
        showSuccess('Categoria atualizada com sucesso!')
      } else {
        await createMutation.mutateAsync(dataToSend)
        showSuccess('Categoria criada com sucesso!')
      }
      onClose()
    } catch (error) {
      handleError(error)
    }
  }

  const parentCategories = allCategories?.filter(
    (cat) => cat.type === formData.type && cat.id !== category?.id
  )

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Dialog open={true} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 2 } }}>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
        {category ? 'Editar Categoria' : 'Nova Categoria'}
        <IconButton onClick={onClose} size="small" aria-label="fechar">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 0 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <TextField
              label="Nome"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              fullWidth
              size="small"
              autoFocus
            />
            <FormControl fullWidth size="small" required>
              <InputLabel>Tipo</InputLabel>
              <Select
                value={formData.type}
                label="Tipo"
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    type: e.target.value as 'INCOME' | 'EXPENSE',
                    parentId: undefined,
                  })
                }
              >
                <MenuItem value="EXPENSE">Despesa</MenuItem>
                <MenuItem value="INCOME">Receita</MenuItem>
              </Select>
            </FormControl>
            {parentCategories && parentCategories.length > 0 && (
              <FormControl fullWidth size="small">
                <InputLabel>Categoria Pai (opcional)</InputLabel>
                <Select
                  value={formData.parentId || ''}
                  label="Categoria Pai (opcional)"
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      parentId: e.target.value || undefined,
                    })
                  }
                >
                  <MenuItem value="">Nenhuma</MenuItem>
                  {parentCategories.map((cat) => (
                    <MenuItem key={cat.id} value={cat.id}>
                      {cat.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
            <Box>
              <InputLabel shrink sx={{ fontSize: '0.875rem', fontWeight: 500, color: 'text.secondary', mb: 0.5, display: 'block' }}>
                Cor
              </InputLabel>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Box
                  component="input"
                  type="color"
                  value={formData.color}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, color: e.target.value })}
                  sx={{
                    width: 48,
                    height: 40,
                    p: 0.5,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    cursor: 'pointer',
                    bgcolor: 'background.paper',
                  }}
                />
                <TextField
                  value={formData.color}
                  onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                  size="small"
                  sx={{ flex: 1 }}
                  inputProps={{ maxLength: 7 }}
                  placeholder="#3B82F6"
                />
              </Box>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={isPending}>
            {isPending ? 'Salvando...' : 'Salvar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
