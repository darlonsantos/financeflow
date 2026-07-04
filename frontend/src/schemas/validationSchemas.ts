import { z } from 'zod'

/**
 * Schema de validação para transação
 */
export const transactionSchema = z.object({
  accountId: z.string().uuid('Conta inválida').min(1, 'Selecione uma conta'),
  categoryId: z.string().uuid('Categoria inválida').min(1, 'Selecione uma categoria'),
  amount: z
    .number({ invalid_type_error: 'Valor deve ser um número' })
    .positive('Valor deve ser maior que zero')
    .max(999999999, 'Valor muito alto'),
  type: z.enum(['INCOME', 'EXPENSE'], {
    errorMap: () => ({ message: 'Tipo deve ser INCOME ou EXPENSE' }),
  }),
  date: z.string().min(1, 'Data é obrigatória'),
  description: z.string().max(500, 'Descrição muito longa').optional(),
  tags: z.array(z.string()).optional(),
  recurring: z.boolean().optional(),
  recurringPattern: z.string().optional(),
})

export type TransactionFormData = z.infer<typeof transactionSchema>

/**
 * Schema de validação para conta
 */
export const accountSchema = z.object({
  name: z
    .string()
    .min(1, 'Nome é obrigatório')
    .min(3, 'Nome deve ter no mínimo 3 caracteres')
    .max(100, 'Nome muito longo'),
  type: z.enum(['BANK', 'CASH', 'CREDIT'], {
    errorMap: () => ({ message: 'Tipo deve ser BANK, CASH ou CREDIT' }),
  }),
  initialBalance: z
    .number({ invalid_type_error: 'Saldo inicial deve ser um número' })
    .finite('Saldo inicial inválido'),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Cor inválida (use formato hex)').optional(),
  icon: z.string().max(50, 'Ícone muito longo').optional(),
})

export type AccountFormData = z.infer<typeof accountSchema>

/**
 * Schema de validação para categoria
 */
export const categorySchema = z.object({
  name: z
    .string()
    .min(1, 'Nome é obrigatório')
    .min(2, 'Nome deve ter no mínimo 2 caracteres')
    .max(100, 'Nome muito longo'),
  type: z.enum(['INCOME', 'EXPENSE'], {
    errorMap: () => ({ message: 'Tipo deve ser INCOME ou EXPENSE' }),
  }),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Cor inválida (use formato hex)').optional(),
  icon: z.string().max(50, 'Ícone muito longo').optional(),
  parentId: z.string().uuid('Categoria pai inválida').optional().nullable(),
})

export type CategoryFormData = z.infer<typeof categorySchema>

/**
 * Schema de validação para login
 */
export const loginSchema = z.object({
  email: z.string().min(1, 'Email é obrigatório').email('Email inválido'),
  password: z.string().min(1, 'Senha é obrigatória').min(8, 'Senha deve ter no mínimo 8 caracteres'),
})

export type LoginFormData = z.infer<typeof loginSchema>

/**
 * Schema de validação para registro
 */
export const registerSchema = z.object({
  name: z
    .string()
    .min(1, 'Nome é obrigatório')
    .min(2, 'Nome deve ter no mínimo 2 caracteres')
    .max(100, 'Nome muito longo'),
  email: z.string().min(1, 'Email é obrigatório').email('Email inválido'),
  password: z
    .string()
    .min(1, 'Senha é obrigatória')
    .min(8, 'Senha deve ter no mínimo 8 caracteres')
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
      'Senha deve conter maiúscula, minúscula, número e caractere especial'
    ),
})

export type RegisterFormData = z.infer<typeof registerSchema>
