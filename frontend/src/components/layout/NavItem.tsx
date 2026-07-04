/**
 * NavItem — um item do menu lateral (ícone + label).
 * Usado em WEB e MOBILE; no web recolhido mostra só ícone com Tooltip.
 */
import { Link } from 'react-router-dom'
import {
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Tooltip,
  alpha,
  useTheme,
} from '@mui/material'

export interface NavItemProps {
  to: string
  label: string
  icon: React.ReactNode
  isActive: boolean
  isCollapsed: boolean
  onClick?: () => void
}

const ITEM_HEIGHT = 48

export default function NavItem({
  to,
  label,
  icon,
  isActive,
  isCollapsed,
  onClick,
}: NavItemProps) {
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'

  const button = (
    <ListItemButton
      component={Link}
      to={to}
      selected={isActive}
      onClick={onClick}
      disableRipple={false}
      sx={{
        width: '100%',
        borderRadius: 1.5,
        borderLeft: '3px solid transparent',
        pl: 1.5,
        pr: 1.5,
        minHeight: ITEM_HEIGHT,
        py: 0.75,
        display: 'flex',
        alignItems: 'center',
        boxSizing: 'border-box',
        transition: theme.transitions.create(
          ['background-color', 'box-shadow', 'border-color', 'color'],
          { duration: theme.transitions.duration.short, easing: theme.transitions.easing.easeInOut }
        ),
        color: 'text.secondary',
        '& .MuiListItemIcon-root': {
          color: 'inherit',
          minWidth: isCollapsed ? 0 : 40,
          mr: isCollapsed ? 0 : 1.5,
          justifyContent: 'center',
        },
        '&:hover': {
          backgroundColor: alpha(
            theme.palette.primary.main,
            isDark ? 0.12 : 0.08
          ),
          color: 'text.primary',
          boxShadow: theme.shadows[1],
        },
        '&:focus-visible': {
          outline: `2px solid ${theme.palette.primary.main}`,
          outlineOffset: 2,
        },
        '&.Mui-selected': {
          backgroundColor: alpha(theme.palette.primary.main, isDark ? 0.2 : 0.12),
          borderLeftColor: theme.palette.primary.main,
          color: 'primary.main',
          boxShadow: theme.shadows[2],
          '& .MuiListItemIcon-root': {
            color: 'primary.main',
          },
          '&:hover': {
            backgroundColor: alpha(theme.palette.primary.main, isDark ? 0.24 : 0.16),
            boxShadow: theme.shadows[3],
          },
        },
      }}
    >
      <ListItemIcon sx={{ justifyContent: 'center' }}>{icon}</ListItemIcon>
      {!isCollapsed && (
        <ListItemText
          primary={label}
          primaryTypographyProps={{
            fontWeight: 500,
            variant: 'body2',
            sx: { whiteSpace: 'normal', wordBreak: 'break-word' },
          }}
        />
      )}
    </ListItemButton>
  )

  return (
    <ListItem disablePadding sx={{ mb: 0.75, px: 0.5 }}>
      {isCollapsed ? (
        <Tooltip title={label} placement="right" arrow>
          {button}
        </Tooltip>
      ) : (
        button
      )}
    </ListItem>
  )
}
