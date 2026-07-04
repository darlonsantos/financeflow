import DashboardIcon from '@mui/icons-material/Dashboard'
import ReceiptIcon from '@mui/icons-material/Receipt'
import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import FolderIcon from '@mui/icons-material/Folder'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import FlagIcon from '@mui/icons-material/Flag'
import RuleIcon from '@mui/icons-material/Rule'
import CurrencyExchangeIcon from '@mui/icons-material/CurrencyExchange'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import PsychologyIcon from '@mui/icons-material/Psychology'
import PersonIcon from '@mui/icons-material/Person'

export interface MenuItemConfig {
  path: string
  label: string
  icon: React.ReactNode
}

export interface MenuSection {
  title: string
  items: MenuItemConfig[]
}

export function getPageLabel(pathname: string): string | null {
  for (const section of menuSections) {
    const item = section.items.find((i) => i.path === pathname)
    if (item) return item.label
  }
  return null
}

export const menuSections: MenuSection[] = [
  {
    title: 'PRINCIPAL',
    items: [
      { path: '/dashboard', label: 'Dashboard', icon: <DashboardIcon /> },
    ],
  },
  {
    title: 'FINANCEIRO',
    items: [
      { path: '/transactions', label: 'Transações', icon: <ReceiptIcon /> },
      { path: '/accounts', label: 'Contas', icon: <AccountBalanceIcon /> },
      //{ path: '/open-finance', label: 'Open Finance', icon: <AccountBalanceIcon /> },
      { path: '/transfer', label: 'Transferência entre Contas', icon: <SwapHorizIcon /> },
      { path: '/categories', label: 'Categorias', icon: <FolderIcon /> },
      { path: '/budgets', label: 'Orçamentos', icon: <AccountBalanceWalletIcon /> },
      { path: '/goals', label: 'Metas', icon: <FlagIcon /> },
      { path: '/installments', label: 'Parcelamentos', icon: <CreditCardIcon /> },
    ],
  },
  {
    title: 'ANÁLISE E CONFIG',
    items: [
      { path: '/predictive', label: 'Inteligência Preditiva', icon: <PsychologyIcon /> },
      { path: '/behavioral-profile', label: 'Perfil Financeiro', icon: <PersonIcon /> },
      { path: '/automation-rules', label: 'Regras', icon: <RuleIcon /> },
      { path: '/currency-rates', label: 'Taxas de câmbio', icon: <CurrencyExchangeIcon /> },
    ],
  },
]
