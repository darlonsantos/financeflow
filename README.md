# FinanceFlow - Sistema de Gestão Financeira Pessoal

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Stack Tecnológica](#stack-tecnológica)
4. [Funcionalidades](#funcionalidades)
5. [Regras de Negócio](#regras-de-negócio)
6. [Validações](#validações)
7. [Modelo de Dados](#modelo-de-dados)
8. [Segurança](#segurança)
9. [APIs REST](#apis-rest)
10. [Instalação e Configuração](#instalação-e-configuração)
11. [Estrutura do Projeto](#estrutura-do-projeto)
12. [Inteligência Artificial](#inteligência-artificial)

---

## 📌 Visão Geral

**FinanceFlow** é um SaaS completo de controle financeiro pessoal desenvolvido com arquitetura moderna, focado em simplicidade, segurança e escalabilidade. O sistema permite que usuários gerenciem receitas, despesas, contas bancárias, categorias, orçamentos e metas financeiras através de uma interface web intuitiva.

### Características Principais

- ✅ **Gestão Completa de Transações**: Receitas e despesas com categorização avançada
- ✅ **Múltiplas Contas**: Suporte a contas bancárias, dinheiro e cartão de crédito
- ✅ **Categorias Hierárquicas**: Sistema de categorias e subcategorias
- ✅ **Orçamentos Mensais**: Controle de gastos por categoria
- ✅ **Metas Financeiras**: Acompanhamento de objetivos financeiros
- ✅ **Dashboard Analítico**: Gráficos e relatórios visuais
- ✅ **Autenticação Segura**: JWT com refresh tokens e verificação obrigatória de email
- ✅ **Verificação de Email**: Sistema completo de verificação de email antes do acesso
- ✅ **Recuperação de Senha**: Reset de senha via email com tokens seguros
- ✅ **Sistema de Auditoria**: Logs de ações críticas para rastreabilidade
- ✅ **API Versionada**: Suporte a versionamento de API
- ✅ **Rate Limiting**: Proteção contra abuso com bloqueio de IP
- ✅ **Tratamento de Erros**: Sistema robusto de tratamento e categorização de erros
- ✅ **Logging Estruturado**: Logs em formato JSON
- ✅ **Métricas e Monitoramento**: Integração com Prometheus e Grafana

---

## 🏗️ Arquitetura

### Arquitetura Geral

O projeto segue uma **arquitetura monolítica modular** com separação clara de responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │
│  │ Dashboard│  │Transactions│ │ Accounts │ │Categories││
│  └──────────┘  └──────────┘  └──────────┘  └────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Backend (Spring Boot)                      │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌──────────────┐  │
│  │  Auth  │  │Accounts│  │Transactions│ │ Categories │  │
│  └────────┘  └────────┘  └────────┘  └──────────────┘  │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌──────────────┐  │
│  │ Budgets│  │ Goals  │  │  Sync  │  │   Reports    │  │
│  └────────┘  └────────┘  └────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                         │
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL Database                        │
└─────────────────────────────────────────────────────────┘
```

### Módulos do Backend

O backend está organizado em módulos por domínio:

- **`auth`** - Autenticação e autorização (login, registro, refresh token, verificação de email, reset de senha)
- **`users`** - Gestão de usuários
- **`accounts`** - Contas financeiras (bancárias, dinheiro, crédito)
- **`categories`** - Categorias e subcategorias de transações
- **`transactions`** - Transações financeiras (receitas e despesas)
- **`budgets`** - Orçamentos mensais por categoria
- **`goals`** - Metas financeiras
- **`sync`** - Sincronização offline (preparado para mobile)
- **`reports`** - Geração de relatórios (JasperReports)
- **`security`** - Configurações de segurança, JWT, rate limiting
- **`email`** - Serviço de envio de emails (verificação, reset de senha)
- **`audit`** - Sistema de auditoria e logs de ações críticas
- **`api`** - Versionamento de API

### Padrões Arquiteturais

- **Layered Architecture**: Controller → Service → Repository → Domain
- **DTO Pattern**: Separação entre entidades de domínio e DTOs de API
- **Mapper Pattern**: MapStruct para conversão entre entidades e DTOs
- **Validator Pattern**: Validações de regras de negócio isoladas
- **Calculator Pattern**: Lógica de cálculo isolada (ex: AccountBalanceCalculator)
- **Repository Pattern**: Abstração de acesso a dados
- **Service Interface Pattern**: Interfaces para serviços (facilita testes)

---

## 💻 Stack Tecnológica

### Backend

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **Java** | 17 | Linguagem de programação |
| **Spring Boot** | 3.2.0 | Framework principal |
| **Spring Security** | 3.2.0 | Autenticação e autorização |
| **Spring Data JPA** | 3.2.0 | Persistência de dados |
| **PostgreSQL** | 15+ | Banco de dados relacional |
| **Flyway** | - | Migrations de banco de dados |
| **JWT (JJWT)** | 0.12.3 | Tokens de autenticação |
| **BCrypt** | - | Hash de senhas |
| **MapStruct** | 1.5.5 | Mapeamento DTO ↔ Entity |
| **Lombok** | 1.18.30 | Redução de boilerplate |
| **Bean Validation** | - | Validação de dados |
| **Caffeine Cache** | - | Cache para rate limiting |
| **JasperReports** | 6.20.0 | Geração de relatórios |
| **Micrometer** | - | Métricas e monitoramento |
| **Logstash Logback Encoder** | 7.4 | Logging estruturado (JSON) |
| **Spring Actuator** | - | Health checks e métricas |

### Frontend

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **React** | 18.2.0 | Biblioteca UI |
| **TypeScript** | 5.3.2 | Tipagem estática |
| **Vite** | 5.0.5 | Build tool e dev server |
| **React Router** | 6.20.0 | Roteamento |
| **Axios** | 1.6.2 | Cliente HTTP |
| **React Query** | 5.12.2 | Gerenciamento de estado servidor |
| **Zustand** | 4.4.7 | Estado global |
| **React Hook Form** | 7.48.2 | Formulários |
| **Zod** | 3.22.4 | Validação de schemas |
| **Tailwind CSS** | 3.3.6 | Estilização |
| **Recharts** | 2.10.3 | Gráficos e visualizações |
| **React Hot Toast** | 2.4.1 | Notificações |

### Infraestrutura

- **Docker** & **Docker Compose** - Containerização
- **Nginx** - Servidor web para frontend
- **Prometheus** - Coleta de métricas
- **Grafana** - Visualização de métricas

---

## 🎯 Funcionalidades

### 1. Autenticação e Autorização

#### Registro de Usuário
- Cadastro com nome, email e senha
- Validação de email único
- Hash de senha com BCrypt (12 rounds)
- **Verificação obrigatória de email**: Após registro, usuário recebe email com link de verificação
- **Não retorna tokens no registro**: Usuário precisa verificar email antes de fazer login
- Envio automático de email de verificação

#### Login
- Autenticação com email e senha
- **Validação de email verificado**: Usuário não pode fazer login sem verificar email
- Geração de access token (15 minutos) e refresh token (7 dias)
- Armazenamento seguro de refresh tokens no banco
- Rate limiting: 30 tentativas por minuto por IP (desenvolvimento), bloqueio de 5 minutos

#### Verificação de Email
- Token seguro gerado com SecureRandom e Base64
- Link de verificação válido por 72 horas (configurável)
- Reenvio de email de verificação disponível
- Endpoint público para reenvio (não requer autenticação)
- Página dedicada para usuários com email não verificado

#### Reset de Senha
- Solicitação de reset via email
- Token seguro com expiração de 24 horas
- Validação de token antes de permitir nova senha
- Endpoint público para solicitação e confirmação
- Invalidação automática de token após uso

#### Refresh Token
- Renovação automática de access tokens
- Rotação de refresh tokens
- Invalidação de tokens em logout
- Rastreamento de dispositivos e IPs

#### Logout
- Invalidação de refresh tokens
- Limpeza de sessão
- Logout de todos os dispositivos disponível

### 2. Gestão de Contas

#### Tipos de Conta
- **BANK**: Conta bancária
- **CASH**: Dinheiro em espécie
- **CREDIT**: Cartão de crédito

#### Funcionalidades
- Criar, editar e excluir contas
- Definir saldo inicial
- Cálculo automático de saldo baseado em transações
- Personalização com cores e ícones
- Soft delete (preserva histórico)

#### Regras de Negócio
- Saldo inicial pode ser negativo (débito)
- Saldo é recalculado automaticamente ao criar/editar/deletar transações
- Contas não podem ser excluídas se tiverem transações ativas (validação futura)

### 3. Gestão de Categorias

#### Estrutura Hierárquica
- Categorias principais (INCOME ou EXPENSE)
- Subcategorias (relacionamento pai-filho)
- Suporte a múltiplos níveis (preparado)

#### Funcionalidades
- Criar, editar e excluir categorias
- Associar categoria pai
- Personalização com cores e ícones
- Filtro por tipo (receita/despesa)

#### Regras de Negócio
- Categoria não pode ser pai de si mesma
- Não permite referências circulares
- Categoria com subcategorias não pode ser excluída
- Tipo da categoria deve corresponder ao tipo da transação

### 4. Gestão de Transações

#### Tipos de Transação
- **INCOME**: Receita (aumenta saldo)
- **EXPENSE**: Despesa (diminui saldo)

#### Funcionalidades
- Criar, editar e excluir transações
- Associar conta e categoria
- Definir valor, data e descrição
- Tags para organização (JSONB)
- Transações recorrentes (DAILY, WEEKLY, MONTHLY, YEARLY)
- Filtros avançados:
  - Por período (data inicial e final)
  - Por conta
  - Por categoria
  - Por tipo
  - Por tags
- Paginação e ordenação
- Busca por descrição
- Exportação de relatórios (JasperReports)

#### Regras de Negócio
- Valor deve ser maior que zero
- Tipo da transação deve corresponder ao tipo da categoria
- Data é obrigatória
- Atualização automática do saldo da conta
- Ao editar transação:
  - Reverte saldo da conta antiga
  - Aplica saldo na conta nova (se mudou)
  - Recalcula valores se valor ou tipo mudou
- Ao deletar transação:
  - Reverte o saldo da conta
  - Soft delete (preserva histórico)
- Transações recorrentes geram transações futuras automaticamente

### 5. Orçamentos

#### Funcionalidades
- Criar orçamento mensal por categoria
- Definir limite de gastos
- Cálculo automático de gastos realizados
- Visualização de status (dentro/fora do orçamento)
- Histórico mensal

#### Regras de Negócio
- Um orçamento por categoria por mês (constraint única)
- Limite deve ser maior que zero
- Gasto calculado baseado em transações do mês
- Soft delete

### 6. Metas Financeiras

#### Funcionalidades
- Criar metas com valor alvo
- Acompanhar progresso (valor atual vs. alvo)
- Definir data limite
- Status: ACTIVE, COMPLETED, CANCELLED
- Contribuições manuais

#### Regras de Negócio
- Valor alvo deve ser maior que zero
- Valor atual inicia em zero
- Status padrão: ACTIVE
- Soft delete

### 7. Dashboard

#### Visualizações
- **Resumo Financeiro**:
  - Saldo total de todas as contas
  - Total de receitas do período
  - Total de despesas do período
  - Variação percentual vs. período anterior
- **Gráficos**:
  - Evolução mensal (receitas vs. despesas)
  - Distribuição por categoria (pizza)
  - Top categorias de gastos
  - Comparativo período atual vs. anterior
- **Transações Recentes**: Lista das últimas transações

#### Filtros
- Período: Mês atual ou Ano atual
- Conta específica
- Categoria específica

### 8. Relatórios

#### Funcionalidades
- Geração de relatórios em PDF (JasperReports)
- Filtros por:
  - Período (data inicial e final)
  - Conta
  - Categoria
  - Tipo de transação
- Exportação de dados

### 9. Verificação de Email

#### Funcionalidades
- Envio automático de email de verificação após registro
- Token seguro com expiração configurável (padrão: 72 horas)
- Link de verificação único por usuário
- Reenvio de email de verificação
- Página dedicada para usuários com email não verificado
- Bloqueio de funcionalidades até verificação

#### Regras de Negócio
- Email deve ser verificado antes de fazer login
- Token de verificação expira após período configurado
- Apenas um token ativo por usuário (tokens anteriores são invalidados)
- Token é marcado como usado após verificação bem-sucedida

### 10. Recuperação de Senha

#### Funcionalidades
- Solicitação de reset via email
- Token seguro com expiração de 24 horas
- Validação de token antes de permitir nova senha
- Confirmação de nova senha com validação
- Invalidação automática de token após uso

#### Regras de Negócio
- Token de reset expira após 24 horas
- Apenas um token ativo por usuário
- Token é marcado como usado após reset bem-sucedido
- Nova senha deve seguir as mesmas regras de validação

### 11. Sistema de Auditoria

#### Funcionalidades
- Registro automático de ações críticas:
  - Criação, edição e exclusão de transações
  - Criação, edição e exclusão de contas
  - Criação, edição e exclusão de categorias
- Armazenamento de:
  - Usuário que realizou a ação
  - Tipo de ação (CREATE, UPDATE, DELETE)
  - Entidade afetada (tabela e ID)
  - Timestamp da ação
  - Dados anteriores e novos (para UPDATE)
- Consulta de histórico de auditoria

#### Regras de Negócio
- Logs são criados automaticamente sem impacto na performance
- Logs são imutáveis (não podem ser editados ou deletados)
- Logs preservam histórico completo de mudanças

---

## 📐 Regras de Negócio

### Transações

1. **Validação de Valor**
   - Valor deve ser maior que zero
   - Valor máximo: 999.999.999,99

2. **Validação de Tipo**
   - Tipo da transação (INCOME/EXPENSE) deve corresponder ao tipo da categoria
   - Categoria de receita só pode ter transações de receita
   - Categoria de despesa só pode ter transações de despesa

3. **Cálculo de Saldo**
   - Receita (INCOME): aumenta saldo da conta
   - Despesa (EXPENSE): diminui saldo da conta
   - Saldo = Saldo Inicial + Σ(Receitas) - Σ(Despesas)
   - Cálculo é automático e em tempo real

4. **Transações Recorrentes**
   - Ao criar transação recorrente, sistema gera transações futuras automaticamente
   - Padrões suportados: DAILY, WEEKLY, MONTHLY, YEARLY
   - Se padrão não especificado, usa MONTHLY por padrão

5. **Edição de Transação**
   - Ao editar, reverte impacto na conta antiga
   - Aplica impacto na conta nova (se mudou)
   - Recalcula se valor ou tipo mudou

6. **Exclusão de Transação**
   - Soft delete (marca `deleted_at`)
   - Reverte impacto no saldo da conta
   - Preserva histórico para auditoria

### Contas

1. **Saldo Inicial**
   - Pode ser negativo (débito)
   - Saldo inicial = saldo atual quando não há transações

2. **Cálculo de Saldo**
   - Saldo é recalculado automaticamente ao criar/editar/deletar transações
   - Saldo = Saldo Inicial + Σ(Receitas) - Σ(Despesas)
   - Saldo é armazenado como cache (performance)

3. **Alteração de Saldo Inicial**
   - Ao alterar saldo inicial, ajusta saldo atual proporcionalmente
   - Diferença = Novo Saldo Inicial - Saldo Inicial Antigo
   - Novo Saldo = Saldo Atual + Diferença

4. **Exclusão**
   - Contas não podem ser excluídas se tiverem transações ativas (validação futura)
   - Soft delete

### Categorias

1. **Hierarquia**
   - Categoria pode ter subcategorias
   - Categoria não pode ser pai de si mesma
   - Não permite referências circulares

2. **Tipo**
   - Categoria deve ter tipo: INCOME ou EXPENSE
   - Tipo determina quais transações podem ser associadas

3. **Exclusão**
   - Categoria com subcategorias não pode ser excluída
   - Soft delete

### Orçamentos

1. **Unicidade**
   - Um orçamento por categoria por mês (constraint única)
   - Não permite duplicatas

2. **Cálculo de Gasto**
   - Gasto = Σ(Transações de despesa da categoria no mês)
   - Atualizado automaticamente

3. **Validação**
   - Limite deve ser maior que zero

### Metas

1. **Validação**
   - Valor alvo deve ser maior que zero
   - Valor atual inicia em zero

2. **Status**
   - ACTIVE: Meta ativa
   - COMPLETED: Meta concluída (valor atual >= valor alvo)
   - CANCELLED: Meta cancelada

### Autenticação

1. **Senhas**
   - Mínimo 8 caracteres (validação futura)
   - Hash com BCrypt (12 rounds)
   - Não armazena senha em texto plano

2. **Tokens**
   - Access token: 15 minutos de validade
   - Refresh token: 7 dias de validade
   - Refresh tokens armazenados no banco (hash)
   - Rotação de refresh tokens

3. **Rate Limiting**
   - Endpoints de autenticação: 30 tentativas por minuto por IP (desenvolvimento)
   - Bloqueio: 5 minutos após exceder limite (desenvolvimento)
   - API geral: 100 requisições por minuto
   - Bloqueio de IP após múltiplas tentativas falhadas

4. **Verificação de Email**
   - Email deve ser verificado antes de fazer login
   - Token de verificação válido por 72 horas (configurável)
   - Reenvio de email disponível para usuários não autenticados

5. **Reset de Senha**
   - Token de reset válido por 24 horas
   - Apenas um token ativo por usuário
   - Token invalidado após uso bem-sucedido

### Isolamento de Dados

1. **Multi-tenancy**
   - Cada usuário só acessa seus próprios dados
   - Todas as queries verificam `user_id`
   - Validação de ownership em todas as operações

2. **Autorização**
   - Verificação de propriedade antes de qualquer operação
   - Retorna 404 se recurso não pertence ao usuário (segurança)

---

## ✅ Validações

### Backend (Bean Validation + Validators Customizados)

#### Usuário
- **Nome**: Não nulo, não vazio, máximo 255 caracteres
- **Email**: Não nulo, formato válido, único, máximo 255 caracteres
- **Senha**: Não nula, mínimo 8 caracteres (validação futura)

#### Conta
- **Nome**: Não nulo, não vazio, máximo 255 caracteres (`AccountValidator`)
- **Tipo**: Não nulo, enum válido (BANK, CASH, CREDIT)
- **Saldo Inicial**: Não nulo (`AccountValidator`)
- **Cor**: Máximo 7 caracteres (hex color)
- **Ícone**: Máximo 50 caracteres

#### Categoria
- **Nome**: Não nulo, não vazio, máximo 255 caracteres
- **Tipo**: Não nulo, enum válido (INCOME, EXPENSE)
- **Categoria Pai**: Deve existir e pertencer ao usuário (`CategoryValidator`)
- **Referência Circular**: Não permite (`CategoryValidator`)

#### Transação
- **Conta**: Não nula, deve existir e pertencer ao usuário
- **Categoria**: Não nula, deve existir e pertencer ao usuário
- **Valor**: Não nulo, maior que zero (`TransactionValidator`)
- **Tipo**: Não nulo, enum válido (INCOME, EXPENSE)
- **Data**: Não nula
- **Descrição**: Máximo 500 caracteres
- **Tags**: Array de strings (JSONB)
- **Tipo vs. Categoria**: Tipo da transação deve corresponder ao tipo da categoria (`TransactionValidator`)

#### Orçamento
- **Categoria**: Não nula, deve existir e pertencer ao usuário
- **Mês**: Não nulo, primeiro dia do mês
- **Limite**: Não nulo, maior que zero
- **Unicidade**: (user_id, category_id, month) único

#### Meta
- **Nome**: Não nulo, não vazio, máximo 255 caracteres
- **Valor Alvo**: Não nulo, maior que zero
- **Valor Atual**: Não nulo, padrão zero
- **Status**: Enum válido (ACTIVE, COMPLETED, CANCELLED)

### Frontend (Zod Schemas)

#### Transação (`transactionSchema`)
```typescript
{
  accountId: UUID válido, obrigatório
  categoryId: UUID válido, obrigatório
  amount: número positivo, máximo 999999999
  type: 'INCOME' | 'EXPENSE'
  date: string não vazia
  description: máximo 500 caracteres (opcional)
  tags: array de strings (opcional)
  recurring: boolean (opcional)
  recurringPattern: string (opcional)
}
```

#### Conta (`accountSchema`)
- Nome: obrigatório, não vazio
- Tipo: obrigatório, enum válido
- Saldo inicial: número válido

#### Categoria (`categorySchema`)
- Nome: obrigatório, não vazio
- Tipo: obrigatório, enum válido
- Categoria pai: UUID válido (opcional)

### Validações de Segurança

1. **SQL Injection**: Protegido por JPA/Hibernate (Prepared Statements)
2. **XSS**: Sanitização de inputs (preparado)
3. **CSRF**: Desabilitado para API stateless (JWT)
4. **CORS**: Configurável via environment variables
5. **Rate Limiting**: Proteção contra brute force
6. **Input Validation**: Validação em todas as camadas (frontend + backend)

---

## 🗄️ Modelo de Dados

### Entidades Principais

#### users
```sql
- id (UUID, PK)
- name (VARCHAR 255, NOT NULL)
- email (VARCHAR 255, UNIQUE, NOT NULL)
- password_hash (VARCHAR 255, NOT NULL)
- email_verified (BOOLEAN, DEFAULT false)
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL) -- Soft delete
```

**Índices**: `email`, `created_at`

#### accounts
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- name (VARCHAR 255, NOT NULL)
- type (ENUM: BANK, CASH, CREDIT, NOT NULL)
- balance (DECIMAL(15,2), DEFAULT 0.00) -- Cache calculado
- initial_balance (DECIMAL(15,2), DEFAULT 0.00)
- color (VARCHAR 7) -- Hex color
- icon (VARCHAR 50)
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL)
- version (INTEGER, DEFAULT 1) -- Optimistic locking
```

**Índices**: `user_id`, `type`, `created_at`

#### categories
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- name (VARCHAR 255, NOT NULL)
- type (ENUM: INCOME, EXPENSE, NOT NULL)
- color (VARCHAR 7)
- icon (VARCHAR 50)
- parent_id (UUID, FK → categories.id, NULL) -- Subcategorias
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL)
- version (INTEGER, DEFAULT 1)
```

**Índices**: `user_id`, `type`, `parent_id`

#### transactions
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- account_id (UUID, FK → accounts.id, NOT NULL)
- category_id (UUID, FK → categories.id, NOT NULL)
- amount (DECIMAL(15,2), NOT NULL, > 0)
- type (ENUM: INCOME, EXPENSE, NOT NULL)
- date (DATE, NOT NULL)
- description (TEXT)
- tags (JSONB) -- Array de tags
- recurring (BOOLEAN, DEFAULT false)
- recurring_pattern (VARCHAR 50) -- DAILY, WEEKLY, MONTHLY, YEARLY
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL)
- version (INTEGER, DEFAULT 1)
- client_id (VARCHAR 255) -- Para sync offline (UNIQUE quando não null)
```

**Índices**: 
- `user_id`, `account_id`, `category_id`
- `date`, `type`
- `created_at`, `updated_at`
- `client_id` (UNIQUE quando não null)

#### budgets
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- category_id (UUID, FK → categories.id, NOT NULL)
- month (DATE, NOT NULL) -- Primeiro dia do mês
- limit_amount (DECIMAL(15,2), NOT NULL, > 0)
- spent_amount (DECIMAL(15,2), DEFAULT 0.00) -- Calculado
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL)
- version (INTEGER, DEFAULT 1)
```

**Índices**: `user_id`, `category_id`, `month`
**Constraint UNIQUE**: `(user_id, category_id, month)`

#### goals
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- name (VARCHAR 255, NOT NULL)
- target_amount (DECIMAL(15,2), NOT NULL, > 0)
- current_amount (DECIMAL(15,2), DEFAULT 0.00)
- due_date (DATE)
- status (ENUM: ACTIVE, COMPLETED, CANCELLED, DEFAULT ACTIVE)
- created_at (TIMESTAMP, NOT NULL)
- updated_at (TIMESTAMP, NOT NULL)
- deleted_at (TIMESTAMP, NULL)
- version (INTEGER, DEFAULT 1)
```

**Índices**: `user_id`, `status`, `due_date`

#### refresh_tokens
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- token_hash (VARCHAR 255, NOT NULL)
- expires_at (TIMESTAMP, NOT NULL)
- revoked (BOOLEAN, DEFAULT false)
- created_at (TIMESTAMP, NOT NULL)
- last_used_at (TIMESTAMP)
- device_info (VARCHAR 255)
- ip_address (VARCHAR 45)
```

**Índices**: `user_id`, `token_hash`, `expires_at`

#### email_verification_tokens
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- token (VARCHAR 255, UNIQUE, NOT NULL)
- expires_at (TIMESTAMP, NOT NULL)
- used (BOOLEAN, DEFAULT false)
- created_at (TIMESTAMP, NOT NULL)
```

**Índices**: `user_id`, `token`, `expires_at`

#### password_reset_tokens
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- token (VARCHAR 255, UNIQUE, NOT NULL)
- expires_at (TIMESTAMP, NOT NULL)
- used (BOOLEAN, DEFAULT false)
- created_at (TIMESTAMP, NOT NULL)
```

**Índices**: `user_id`, `token`, `expires_at`

#### audit_logs
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- entity_type (VARCHAR 100, NOT NULL) -- Nome da entidade (ex: Transaction, Account)
- entity_id (UUID, NOT NULL)
- action (VARCHAR 50, NOT NULL) -- CREATE, UPDATE, DELETE
- old_data (JSONB) -- Dados anteriores (para UPDATE)
- new_data (JSONB) -- Dados novos
- created_at (TIMESTAMP, NOT NULL)
```

**Índices**: `user_id`, `entity_type`, `entity_id`, `action`, `created_at`

#### sync_logs (Preparado para sincronização offline)
```sql
- id (UUID, PK)
- user_id (UUID, FK → users.id, NOT NULL)
- entity_type (VARCHAR 50, NOT NULL)
- entity_id (UUID, NOT NULL)
- action (ENUM: CREATE, UPDATE, DELETE, NOT NULL)
- sync_status (ENUM: PENDING, SYNCED, CONFLICT, ERROR, NOT NULL)
- client_timestamp (TIMESTAMP, NOT NULL)
- server_timestamp (TIMESTAMP)
- conflict_resolution (TEXT)
- created_at (TIMESTAMP, NOT NULL)
```

**Índices**: `user_id`, `entity_type`, `sync_status`, `created_at`

### Relacionamentos

```
users (1) ──< (N) accounts
users (1) ──< (N) categories
users (1) ──< (N) transactions
users (1) ──< (N) budgets
users (1) ──< (N) goals
users (1) ──< (N) refresh_tokens

accounts (1) ──< (N) transactions
categories (1) ──< (N) transactions
categories (1) ──< (N) categories (parent_id)
categories (1) ──< (N) budgets
```

---

## 🔐 Segurança

### Autenticação

1. **JWT Tokens**
   - Access Token: Expira em 15 minutos
   - Refresh Token: Expira em 7 dias
   - Algoritmo: HS256
   - Secrets configuráveis via environment variables

2. **Refresh Tokens**
   - Armazenados no banco (hash, não o token em si)
   - Rotação automática
   - Rastreamento de dispositivo e IP
   - Invalidação em logout

3. **Senhas**
   - Hash com BCrypt (12 rounds)
   - Nunca armazenadas em texto plano
   - Validação de força (futuro)

### Autorização

1. **Isolamento de Dados**
   - Cada usuário só acessa seus próprios dados
   - Validação de ownership em todas as operações
   - Queries sempre filtram por `user_id`

2. **Endpoints Protegidos**
   - `/api/v1/auth/**` - Público
   - `/actuator/health` - Público
   - Todos os outros - Requer autenticação

### Rate Limiting

1. **Endpoints de Autenticação**
   - 30 tentativas por minuto por IP (desenvolvimento)
   - Bloqueio de 5 minutos após exceder limite (desenvolvimento)
   - Implementado com Caffeine Cache
   - Bloqueio automático de IP após múltiplas tentativas falhadas

2. **API Geral**
   - 100 requisições por minuto por IP
   - Configurável via `application.yml`

3. **Proteção**
   - Rate limiting aplicado antes da autenticação JWT
   - IP bloqueado é armazenado em cache com expiração
   - Mensagens de erro claras para usuários bloqueados

### CORS

- Origins permitidos configuráveis via `CORS_ALLOWED_ORIGINS`
- Credentials permitidos apenas para origens confiadas
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

### Headers de Segurança

- Configurados via `SecurityHeadersConfig`
- HSTS (futuro)
- X-Frame-Options
- X-Content-Type-Options
- X-XSS-Protection

### Logging e Auditoria

1. **Logs Estruturados**
   - Formato JSON (Logstash Logback Encoder)
   - Contexto MDC para rastreamento
   - Níveis: DEBUG, INFO, WARN, ERROR

2. **Sistema de Auditoria**
   - Registro automático de ações críticas:
     - Criação, edição e exclusão de transações
     - Criação, edição e exclusão de contas
     - Criação, edição e exclusão de categorias
   - Armazenamento de:
     - Usuário que realizou a ação
     - Tipo de ação (CREATE, UPDATE, DELETE)
     - Entidade afetada (tabela e ID)
     - Timestamp da ação
     - Dados anteriores e novos (para UPDATE)
   - Logs imutáveis e preservados para histórico completo
   - Rastreamento de tentativas de login
   - Logs de sincronização (futuro)

### Verificação de Email

1. **Segurança**
   - Token seguro gerado com SecureRandom e Base64
   - Token único por usuário (tokens anteriores são invalidados)
   - Expiração configurável (padrão: 72 horas)
   - Token marcado como usado após verificação

2. **Proteção**
   - Usuário não pode fazer login sem verificar email
   - Endpoints de verificação são públicos (não requerem autenticação)
   - Reenvio de email disponível para usuários não autenticados

### Reset de Senha

1. **Segurança**
   - Token seguro com expiração de 24 horas
   - Apenas um token ativo por usuário
   - Token invalidado após uso bem-sucedido
   - Validação de token antes de permitir nova senha

2. **Proteção**
   - Endpoints públicos (não requerem autenticação)
   - Mensagens genéricas para evitar enumeração de emails

---

## 🌐 APIs REST

### Base URL
```
/api/v1
```

### Padrão de Resposta

**Sucesso (200/201)**:
```json
{
  "data": { ... },
  "message": "Operação realizada com sucesso",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Erro (4xx/5xx)**:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dados inválidos",
    "details": [
      {
        "field": "email",
        "message": "Email inválido"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Endpoints Principais

#### Autenticação
- `POST /auth/register` - Cadastro de usuário (não retorna tokens, requer verificação de email)
- `POST /auth/login` - Login (retorna access + refresh token, requer email verificado)
- `POST /auth/refresh` - Renovar access token
- `POST /auth/logout` - Logout (invalida refresh token)
- `POST /auth/logout-all` - Logout de todos os dispositivos

#### Verificação de Email
- `POST /auth/email-verification/verify?token={token}` - Verificar email com token
- `POST /auth/email-verification/resend?email={email}` - Reenviar email de verificação (público)

#### Reset de Senha
- `POST /auth/password-reset/request` - Solicitar reset de senha (público)
- `POST /auth/password-reset/confirm` - Confirmar reset de senha com token (público)

#### Contas
- `GET /accounts` - Listar contas do usuário
- `GET /accounts/:id` - Detalhes da conta
- `POST /accounts` - Criar conta
- `PUT /accounts/:id` - Atualizar conta
- `DELETE /accounts/:id` - Excluir conta (soft delete)
- `GET /accounts/:id/balance` - Saldo atualizado

#### Categorias
- `GET /categories` - Listar categorias
- `GET /categories/:id` - Detalhes da categoria
- `POST /categories` - Criar categoria
- `PUT /categories/:id` - Atualizar categoria
- `DELETE /categories/:id` - Excluir categoria

#### Transações
- `GET /transactions` - Listar transações (com filtros e paginação)
- `GET /transactions/:id` - Detalhes da transação
- `POST /transactions` - Criar transação
- `PUT /transactions/:id` - Atualizar transação
- `DELETE /transactions/:id` - Excluir transação
- `DELETE /transactions/batch` - Excluir múltiplas transações
- `GET /transactions/report` - Relatório de transações (para PDF)

**Filtros de Transações**:
- `?dateFrom=2024-01-01&dateTo=2024-01-31` - Por período
- `?accountId=uuid` - Por conta
- `?categoryId=uuid` - Por categoria
- `?type=INCOME|EXPENSE` - Por tipo
- `?page=0&size=20` - Paginação
- `?sort=date,desc` - Ordenação

#### Orçamentos
- `GET /budgets` - Listar orçamentos (filtro: month)
- `GET /budgets/:id` - Detalhes do orçamento
- `POST /budgets` - Criar orçamento
- `PUT /budgets/:id` - Atualizar orçamento
- `DELETE /budgets/:id` - Excluir orçamento

#### Metas
- `GET /goals` - Listar metas (filtro: status)
- `GET /goals/:id` - Detalhes da meta
- `POST /goals` - Criar meta
- `PUT /goals/:id` - Atualizar meta
- `DELETE /goals/:id` - Excluir meta

#### Relatórios
- `GET /reports/transactions` - Gerar relatório PDF de transações

#### Monitoramento
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Métricas
- `GET /actuator/prometheus` - Métricas Prometheus

### Versionamento de API

- Versão atual: `v1`
- Header: `X-API-Version: v1` (opcional)
- URL: `/api/v1/...`
- Suporte a múltiplas versões (preparado)

---

## 🚀 Instalação e Configuração

### Pré-requisitos

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Docker e Docker Compose (opcional)
- Maven 3.8+

### Configuração Local

1. **Clone o repositório**
```bash
git clone <repository-url>
cd FinanceFlow
```

2. **Configure o banco de dados**
```bash
# Criar banco de dados PostgreSQL
createdb financeflow

# Ou usar Docker
docker run -d \
  --name financeflow-postgres \
  -e POSTGRES_DB=financeflow \
  -e POSTGRES_USER=financeflow \
  -e POSTGRES_PASSWORD=financeflow \
  -p 5432:5432 \
  postgres:15-alpine
```

3. **Configure variáveis de ambiente**

Crie arquivo `.env` na raiz:
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=financeflow
DB_USER=financeflow
DB_PASSWORD=financeflow
JWT_SECRET=your-256-bit-secret-key-change-in-production-minimum-32-characters
JWT_REFRESH_SECRET=your-256-bit-refresh-secret-key-change-in-production-minimum-32-characters
CORS_ALLOWED_ORIGINS=
SERVER_PORT=8080
FRONTEND_PORT=8081

# Configuração de Email (para verificação e reset de senha)
# Obrigatório: configure credenciais válidas ou o envio falhará
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=financeflowds@gmail.com
MAIL_PASSWORD=your-gmail-app-password
EMAIL_FROM=noreply@financeflow.com
# FRONTEND_URL: base usada nos links de email (verificação, reset de senha)
# Local: http://localhost:8081 | Produção: https://darlonsantos.com.br
FRONTEND_URL=http://localhost:8081
```

4. **Execute migrations**
```bash
cd backend
mvn flyway:migrate
```

5. **Inicie o backend**
```bash
cd backend
mvn spring-boot:run
```

6. **Inicie o frontend**
```bash
cd frontend
npm install
npm run dev
```

### Docker Compose

1. **Configure variáveis de ambiente**
```bash
cp .env.example .env
# Edite .env com suas configurações
```

**Importante – FRONTEND_URL para links em emails:**
| Ambiente | SPRING_PROFILES_ACTIVE | FRONTEND_URL | Links gerados |
|----------|------------------------|--------------|---------------|
| **Produção** | prod (padrão) | Não definir ou `https://darlonsantos.com.br` | `https://darlonsantos.com.br/verify-email?token=...` |
| **Local (Docker)** | - | `http://localhost:8081` | `http://localhost:8081/verify-email?token=...` |
| **Homologação** | - | URL do ambiente | Conforme configurado |

2. **Inicie todos os serviços**
```bash
docker-compose up -d
```

3. **Verifique logs**
```bash
docker-compose logs -f api
docker-compose logs -f frontend
```

4. **Pare os serviços**
```bash
docker-compose down
```

### Acessos

- **Frontend**: http://localhost:8081
- **Backend API**: http://localhost:8080/api/v1
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas Prometheus**: http://localhost:8080/actuator/prometheus

---

## 🎨 Frontend - Tratamento de Erros

### Sistema de Tratamento de Erros

O frontend implementa um sistema robusto de tratamento de erros com categorização automática e mensagens amigáveis ao usuário.

#### Categorias de Erros

- **VALIDATION**: Erros de validação de dados (400)
- **AUTHENTICATION**: Erros de autenticação (401)
- **AUTHORIZATION**: Erros de autorização (403)
- **NOT_FOUND**: Recurso não encontrado (404)
- **RATE_LIMIT**: Limite de requisições excedido (429)
- **NETWORK**: Erros de rede/conexão
- **SERVER**: Erros do servidor (500+)
- **UNKNOWN**: Erros desconhecidos

#### Funcionalidades

1. **Categorização Automática**
   - Detecta tipo de erro baseado no status HTTP e código da API
   - Tratamento específico para email não verificado
   - Suporte a detalhes de validação em formato objeto ou array

2. **Mensagens Amigáveis**
   - Tradução automática de mensagens técnicas
   - Mensagens específicas por tipo de erro
   - Tradução de nomes de campos para português

3. **Notificações Visuais**
   - Toasts diferenciados por categoria de erro
   - Ícones específicos para cada tipo de erro
   - Duração configurável por tipo
   - Evita toasts duplicados

4. **Validação de Formulários**
   - Exibição de erros por campo
   - Mensagens de validação traduzidas
   - Suporte a múltiplos erros simultâneos

#### Exemplo de Uso

```typescript
import { useErrorHandler } from '../hooks/useErrorHandler'

const { handleError, showSuccess } = useErrorHandler()

try {
  await api.post('/transactions', data)
  showSuccess('Transação criada com sucesso!')
} catch (err) {
  handleError(err, 'Erro ao criar transação')
}
```

#### Páginas Especiais

- **EmailVerificationPending**: Página para usuários com email não verificado
- **EmailVerification**: Página de verificação de email via token
- **PasswordResetRequest**: Solicitação de reset de senha
- **PasswordResetConfirm**: Confirmação de reset de senha

---

## 📁 Estrutura do Projeto

```
FinanceFlow/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/financeflow/
│   │   │   │   ├── accounts/          # Módulo de contas
│   │   │   │   ├── auth/              # Autenticação, verificação de email, reset de senha
│   │   │   │   ├── audit/             # Sistema de auditoria
│   │   │   │   ├── categories/        # Categorias
│   │   │   │   ├── config/            # Configurações
│   │   │   │   ├── email/             # Serviço de email
│   │   │   │   ├── goals/             # Metas
│   │   │   │   ├── budgets/           # Orçamentos
│   │   │   │   ├── reports/           # Relatórios
│   │   │   │   ├── security/          # Segurança, JWT, rate limiting
│   │   │   │   ├── sync/              # Sincronização
│   │   │   │   ├── transactions/      # Transações
│   │   │   │   ├── users/             # Usuários
│   │   │   │   └── FinanceFlowApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── logback-spring.xml
│   │   │       └── db/migration/      # Flyway migrations
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/     # Componentes reutilizáveis
│   │   ├── pages/          # Páginas/rotas
│   │   │   ├── EmailVerification.tsx
│   │   │   ├── EmailVerificationPending.tsx
│   │   │   ├── PasswordResetRequest.tsx
│   │   │   └── PasswordResetConfirm.tsx
│   │   ├── hooks/          # Custom hooks (useErrorHandler, etc.)
│   │   ├── services/       # API clients
│   │   ├── store/          # Estado global (Zustand)
│   │   ├── schemas/        # Schemas de validação (Zod)
│   │   ├── types/          # TypeScript types
│   │   └── App.tsx
│   ├── Dockerfile
│   ├── package.json
│   └── vite.config.ts
│
├── monitoring/
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── grafana/
│       ├── dashboards/
│       └── datasources/
│
├── backend/.ai/           # Workspace IA para desenvolvimento assistido
│   ├── CLAUDE.md          # Ponto de entrada para agentes
│   ├── roles/             # Architect, Developer, Reviewer, Investigator
│   ├── prompts/           # Fluxos feature, bug, review, SQL
│   ├── skills/            # Templates por tipo de tarefa
│   ├── specs/             # Especificações de features e defects
│   ├── tasks/             # Tasks de implementação
│   └── decisions/         # ADRs (Architecture Decision Records)
├── docker-compose.yml
├── .env.example
└── README.md
```

### Estrutura de Módulos (Backend)

Cada módulo segue a estrutura:
```
module/
├── controller/      # REST controllers
├── service/         # Lógica de negócio
│   ├── IModuleService.java  # Interface
│   └── ModuleService.java    # Implementação
├── repository/      # Repositórios JPA
├── domain/         # Entidades JPA
├── dto/            # DTOs de request/response
├── mapper/         # MapStruct mappers
├── validator/      # Validadores de regras de negócio
└── exception/      # Exceções customizadas
```

---

## 🤖 Inteligência Artificial

O FinanceFlow possui duas frentes de IA: **recursos no produto** (funcionalidades para o usuário final)

### Visão Geral

---

### Comparação: Planejado vs Implementado (Produto)

Com base no roadmap (`roadmap_novas_implementacoes_finance_flow.md`, `roadmap_financeflow_atualizado.md`) e no código atual:

| Funcionalidade | Planejado | Status | Como foi implementado |
|----------------|-----------|--------|------------------------|
| **Categorização automática** | Sugestão por descrição, histórico e padrões | ✅ **Implementado** (sem LLM) | `CategorySuggestionService` — histórico exato/similar, palavras-chave e categoria mais usada; auto-aplica se confiança ≥ 90% no formulário |
| **Assistente financeiro (chat)** | Perguntas em linguagem natural sobre finanças | ✅ **Implementado** | `FinancialAssistantService` — Gemini → Ollama → regras (regex/intents); widget flutuante `AssistenteChat` |
| **Projeção de saldo futuro** | Recorrências + padrões históricos | ✅ **Implementado** | `BalanceProjectionService` — média de 6 meses, recorrências e parcelas; gráfico no Dashboard |
| **Inteligência preditiva** | Alertas preventivos (orçamento, metas, parcelas) | ✅ **Implementado** | `PredictiveIntelligenceService` — algoritmos com thresholds; página `/predictive` |
| **Perfil financeiro comportamental** | Classificação por IA com padrões e sugestões | ✅ **Implementado** | `BehavioralProfileService` — Gemini (JSON estruturado) + fallback por regras; página `/behavioral-profile` |
| **Relatório financeiro por IA** | Diagnóstico estilo consultoria | ⚠️ **Parcial** | `AIReportService` — texto de 2–4 parágrafos via Gemini/Ollama; botão na tela de Transações |
| **Insights no dashboard** | Alertas contextuais | ✅ **Implementado** (sem LLM) | `useInsights` — variação de categorias, metas a vencer, recorrências (client-side) |
| **Categorização com LLM** | IA generativa para sugerir categoria | ❌ **Não implementado** | Apenas heurísticas; LLM não é usado neste fluxo |
| **Diagnóstico financeiro completo** | Relatório consultoria com seções estruturadas | ❌ **Não implementado** | Existe apenas o relatório texto básico (`GET /reports/ai`) |
| **Simulador de decisões financeiras** | Simular compra parcelada, empréstimo, etc. | ❌ **Não implementado** | Previsto na Fase 2 do roadmap estratégico |
| **Previsões e análises avançadas (ML)** | Modelos preditivos com aprendizado | ⚠️ **Parcial** | Alertas baseados em regras e médias; sem modelo de ML treinado |

#### Resumo por fase do roadmap original

| Fase | Planejado | Implementado | Parcial | Pendente |
|------|-----------|--------------|---------|----------|
| **Fase 3 — Inteligência Financeira** | 3 itens (categorização IA, projeção, assistente) | 2 | 1 | 0 |
| **Roadmap estratégico — Fase 1** | Preditiva, diagnóstico, perfil comportamental | 2 | 1 | 0 |
| **Roadmap estratégico — Fase 2** | Simulador de decisões | 0 | 0 | 1 |

> **Nota:** O item *"IA para categorização automática"* no roadmap de longo prazo está marcado como concluído na versão heurística (sem LLM). A evolução com modelo generativo permanece pendente.

---

### APIs de Inteligência Artificial

| Endpoint | Método | Descrição | Usa LLM |
|----------|--------|-----------|---------|
| `/api/v1/assistant/chat` | POST | Chat do assistente financeiro | Sim (com fallback) |
| `/api/v1/reports/ai` | GET | Relatório financeiro em texto | Sim (com fallback) |
| `/api/v1/behavioral-profile` | GET | Perfil comportamental | Sim (com fallback) |
| `/api/v1/predictive/report` | GET | Alertas preditivos | Não |
| `/api/v1/projections/balance` | GET | Projeção de saldo (1–24 meses) | Não |
| `/api/v1/transactions/suggest-category` | POST | Sugestão de categoria | Não |

---

### Provedores e Configuração

| Provedor | Papel | Padrão | Variáveis de ambiente |
|----------|-------|--------|------------------------|
| **Google Gemini** | Provedor principal | Habilitado | `GEMINI_ENABLED`, `GEMINI_API_KEY`, `GEMINI_MODEL` |
| **Ollama** | IA local (privacidade) | Desabilitado | `OLLAMA_ENABLED`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL` |

**Modelo padrão:** `gemini-2.5-flash` — temperature `0.2`, máximo `1024` tokens, timeout `30s`.

**Fluxo de prioridade** (assistente, relatório IA e perfil comportamental):

```
Pergunta/Requisição
       ↓
Gemini configurado? ──sim──→ Resposta via API Google
       ↓ não
Ollama habilitado? ──sim──→ Resposta via modelo local
       ↓ não
Regras determinísticas / mensagem de indisponibilidade
```

**Segurança:** a chave `GEMINI_API_KEY` fica apenas no backend; o contexto enviado à IA contém somente dados do usuário autenticado (`AssistantContextBuilder`).

Exemplo de variáveis no `.env`:

```env
GEMINI_ENABLED=true
GEMINI_API_KEY=sua-chave-aqui
GEMINI_MODEL=gemini-2.5-flash

# Opcional — IA local
OLLAMA_ENABLED=false
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
```

---

### Módulos Backend Relacionados

| Módulo | Pacote | Responsabilidade |
|--------|--------|------------------|
| Assistente | `assistant/` | Chat, clientes Gemini/Ollama, montagem de contexto |
| Preditiva | `predictive/` | Alertas de risco (orçamento, saldo, metas, parcelas) |
| Projeções | `projections/` | Projeção de saldo futuro |
| Comportamental | `behavioral/` | Perfil financeiro com classificação e sugestões |
| Relatórios IA | `reports/AIReportService` | Relatório narrativo gerado por LLM |
| Categorização | `transactions/CategorySuggestionService` | Sugestão heurística de categoria |

---

### Frontend — Telas e Componentes

| Componente / Página | Localização | Função |
|---------------------|-------------|--------|
| `AssistenteChat` | Widget flutuante global | Chat com sugestões de perguntas |
| `Predictive.tsx` | `/predictive` | Alertas preditivos e projeções integradas |
| `BehavioralProfile.tsx` | `/behavioral-profile` | Perfil comportamental e recomendações |
| `InsightsWidget` | Dashboard | Insights locais (sem API de IA) |
| `TransactionForm` | Formulário de transação | Sugestão automática de categoria |
| Relatório IA | `Transactions.tsx` | Download/visualização do relatório texto |

---

### Workspace de Desenvolvimento com IA (`backend/.ai/`)

Estrutura para desenvolvimento assistido por agentes (Cursor, Claude, etc.), separada das features de IA do produto:

| Componente | Status | Descrição |
|------------|--------|-----------|
| `CLAUDE.md` | ✅ | Ponto de entrada — stack, regras e skills |
| `architecture.md` | ✅ | Módulos, fluxos e integrações (inclui Gemini/Ollama) |
| `coding-standards.md` | ✅ | Padrões de código backend e frontend |
| `memory.md` | ✅ | Conhecimento acumulado e decisões de negócio |
| `debugging.md` | ✅ | Guia de investigação de problemas |
| **Roles** (`roles/`) | ✅ | Architect, Developer, Reviewer, Investigator |
| **Prompts** (`prompts/`) | ✅ | feature-lifecycle, bug-lifecycle, code-review, sql-analysis, analyze-project |
| **Skills** (`skills/`) | ✅ | new-module, flyway-migration, jasperreports, debugging, security-query, frontend-feature |
| **ADRs** (`decisions/`) | ✅ | ADR-001 (backend), ADR-002 (frontend) |
| **Specs** (`specs/`) | ✅ Parcial | FEATURE-001 (relatório de categorias) documentada e entregue |
| **Tasks** (`tasks/`) | ✅ Parcial | FEATURE-001 concluída; exemplo de bug (TASK-001) |
| **MCP** (`mcp/`) | ⏳ Em configuração | Integração MCP para Postgres e filesystem |

#### Fluxo de desenvolvimento assistido

```
Spec aprovado (.ai/specs/features/)
       ↓
Task criada (.ai/tasks/)
       ↓
Role Architect → análise arquitetural
       ↓
Role Developer → implementação (skills + coding-standards)
       ↓
Role Reviewer → code-review checklist
       ↓
Atualização memory.md + status da task
```

#### Entrega recente via workspace IA

| Task | Feature | Status | Data |
|------|---------|--------|------|
| FEATURE-001 | Relatório PDF de categorias (JasperReports) | ✅ Aprovado | 2026-06-03 |

---

### Próximos Passos em IA

| Prioridade | Item | Descrição |
|------------|------|-----------|
| Alta | Categorização com LLM | Enriquecer `CategorySuggestionService` com Gemini para descrições ambíguas |
| Alta | Diagnóstico financeiro estruturado | Expandir `AIReportService` com seções fixas (pontos fortes, riscos, plano de ação) |
| Média | Simulador de decisões | Novo módulo para simular impacto de compras parceladas, empréstimos, etc. |
| Média | Ollama no Docker Compose | Facilitar IA local em ambiente de desenvolvimento |
| Baixa | Modelos preditivos (ML) | Evoluir de regras fixas para modelos treinados com histórico do usuário |

---

## 📊 Monitoramento e Métricas

### Health Checks

- **Application Health**: `/actuator/health`
- **Database Health**: Verifica conexão com PostgreSQL
- **Component Health**: Status de cada componente

### Métricas Prometheus

- Requisições HTTP (contadores, timers)
- Erros HTTP
- Operações de banco de dados
- Uso de memória e CPU

### Grafana Dashboards

- Dashboard pré-configurado em `monitoring/grafana/dashboards/`
- Visualização de métricas em tempo real
- Alertas configuráveis

---

## 🧪 Testes

### Estratégia de Testes

- **Unitários**: Lógica de negócio, validators, calculators
- **Integração**: Controllers, repositórios
- **E2E**: Fluxos críticos (futuro)

### Cobertura Mínima

- **Backend**: 60% de cobertura (configurado no JaCoCo)
- **Services Críticos**: 90% de cobertura
- **Controllers**: Testes de integração obrigatórios

### Executar Testes

```bash
cd backend
mvn test
mvn jacoco:report  # Gerar relatório de cobertura
```

---

## 📝 Notas de Desenvolvimento

### Boas Práticas Implementadas

- ✅ **Clean Code**: Código limpo e legível
- ✅ **SOLID**: Princípios SOLID aplicados
- ✅ **DRY**: Don't Repeat Yourself
- ✅ **Separation of Concerns**: Separação clara de responsabilidades
- ✅ **Error Handling**: Tratamento robusto de erros
- ✅ **Logging**: Logs estruturados (JSON)
- ✅ **Versionamento**: Controle de versão com Git
- ✅ **Documentação**: Código documentado

### Padrões de Código

- **Naming**: Nomes descritivos e em inglês
- **Comments**: Comentários apenas quando necessário
- **Formatting**: Formatação consistente
- **Git**: Commits semânticos (feat, fix, docs, etc.)

### Versionamento

- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Git Flow**: main, develop, feature branches
- **Tags**: Tags para releases

---

## 🔮 Próximos Passos (Roadmap)

### Curto Prazo
- [X] Validação de força de senha
- [X] Recuperação de senha por email
- [X] Verificação de email
- [X] Testes E2E
- [X] Melhorias de performance

### Médio Prazo
- [x] Aplicativo mobile (React Native)
- [ ] Sincronização offline completa
- [x] Notificações push
- [x] Exportação para Excel/CSV
- [ ] Importação de extratos bancários

### Longo Prazo
- [X] Multi-moeda
- [X] Compartilhamento de contas
- [X] Integrações com bancos (Open Banking)
- [X] IA para categorização automática (heurística — ver [Inteligência Artificial](#inteligência-artificial))
- [~] Previsões e análises avançadas (alertas preditivos e projeções implementados; ML pendente)
- [X] PWA (Progressive Web App)
- [X] Dark mode
- [x] Internacionalização (i18n)

---

## 📄 Licença

[Especificar licença]

---

## 👥 Contribuidores

[Especificar contribuidores]

---

## 📞 Suporte

[Especificar informações de suporte]

---

**FinanceFlow** - Sistema de Gestão Financeira Pessoal Moderno e Seguro
