/**
 * NavDrawer — menu lateral (sidebar).
 *
 * WEB (sm+): Drawer permanente ao lado do conteúdo; pode ser recolhido (só ícones).
 * MOBILE (xs): Drawer temporário em overlay; abre pelo ícone do header e fecha ao navegar/backdrop.
 * @see frontend/src/components/layout/README.md
 */
import { Box, Drawer, List, Typography, useTheme } from '@mui/material'
import NavItem from './NavItem'
import { menuSections } from './menuItems'

const HEADER_HEIGHT = 64
/** Largura do drawer na web expandido; suficiente para labels longos (ex.: "Transferência entre Contas") */
const DRAWER_WIDTH = 280
const DRAWER_WIDTH_COLLAPSED = 64
/** Largura do drawer no mobile: ocupa a maior parte da tela (como o destaque na UI). */
const DRAWER_WIDTH_MOBILE = 'min(360px, 88vw)'

export { HEADER_HEIGHT, DRAWER_WIDTH, DRAWER_WIDTH_COLLAPSED }

export interface NavDrawerProps {
  sidebarCollapsed: boolean
  isMobile: boolean
  mobileOpen: boolean
  onMobileClose: () => void
  isActive: (path: string) => boolean
  onItemClick: () => void
}

export default function NavDrawer({
  sidebarCollapsed,
  isMobile,
  mobileOpen,
  onMobileClose,
  isActive,
  onItemClick,
}: NavDrawerProps) {
  const theme = useTheme()
  const drawerWidth = sidebarCollapsed && !isMobile ? DRAWER_WIDTH_COLLAPSED : DRAWER_WIDTH

  const drawerBg =
    theme.palette.mode === 'dark'
      ? theme.palette.background.paper
      : 'rgba(250, 250, 250, 1)'

  const drawerContent = (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'auto',
        py: 1,
        px: 1,
      }}
    >
      {menuSections.map((section) => (
        <Box key={section.title} sx={{ py: 2, px: 0.5 }}>
          {(!sidebarCollapsed || isMobile) && (
            <Typography
              variant="overline"
              sx={{
                display: 'block',
                px: 1.5,
                py: 0.75,
                fontWeight: 600,
                letterSpacing: '0.1em',
                color: 'text.secondary',
                lineHeight: 1.4,
                transition: theme.transitions.create('color', {
                  duration: theme.transitions.duration.short,
                }),
              }}
            >
              {section.title}
            </Typography>
          )}
          <List disablePadding sx={{ mt: 0.5 }}>
            {section.items.map((item) => (
              <NavItem
                key={item.path}
                to={item.path}
                label={item.label}
                icon={item.icon}
                isActive={isActive(item.path)}
                isCollapsed={sidebarCollapsed && !isMobile}
                onClick={onItemClick}
              />
            ))}
          </List>
        </Box>
      ))}
    </Box>
  )

  return (
    <>
      {/* WEB: drawer permanente (≥600px); oculto no mobile */}
      <Drawer
        variant="permanent"
        open={!sidebarCollapsed}
        sx={{
          display: { xs: 'none', sm: 'block' },
          width: drawerWidth,
          
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            top: HEADER_HEIGHT,
            height: `calc(100% - ${HEADER_HEIGHT}px)`,
            borderRight: '1px solid',
            borderColor: 'divider',
            bgcolor: drawerBg,
            boxShadow: theme.shadows[4],
            transition: theme.transitions.create(['width', 'box-shadow'], {
              easing: theme.transitions.easing.easeInOut,
              duration: theme.transitions.duration.standard,
            }),
            overflowX: 'hidden',
          },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* MOBILE: drawer temporário em overlay (<600px); abre pelo ícone do header */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{
          keepMounted: true,
          BackdropProps: {
            sx: {
              backgroundColor: 'rgba(0, 0, 0, 0.4)',
              transition: 'opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            },
          },
        }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH_MOBILE,
            boxSizing: 'border-box',
            top: HEADER_HEIGHT,
            height: `calc(100% - ${HEADER_HEIGHT}px)`,
            borderRight: '1px solid',
            borderColor: 'divider',
            bgcolor: drawerBg,
            boxShadow: theme.shadows[8],
            transition: theme.transitions.create('transform', {
              easing: theme.transitions.easing.easeInOut,
              duration: theme.transitions.duration.standard,
            }),
          },
        }}
      >
        {drawerContent}
      </Drawer>
    </>
  )
}
