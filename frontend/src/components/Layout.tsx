import { useState, useEffect, useCallback, useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { Box, useTheme, useMediaQuery } from '@mui/material'
import AppBar from './layout/AppBar'
import NavDrawer, {
  HEADER_HEIGHT,
  DRAWER_WIDTH,
  DRAWER_WIDTH_COLLAPSED,
} from './layout/NavDrawer'
import { getPageLabel } from './layout/menuItems'
import AssistenteChat from './AssistenteChat'

const STORAGE_KEY_DRAWER = 'financeflow-drawer-collapsed'

interface LayoutProps {
  children: React.ReactNode
}

function getInitialDrawerCollapsed(): boolean {
  if (typeof window === 'undefined') return false
  try {
    const stored = localStorage.getItem(STORAGE_KEY_DRAWER)
    return stored === 'true'
  } catch {
    return false
  }
}

/**
 * Layout principal: AppBar + NavDrawer (menu) + conteúdo.
 * WEB (sm+): menu lateral fixo, pode recolher. MOBILE (xs): menu em overlay.
 * @see ./layout/README.md
 */
export default function Layout({ children }: LayoutProps) {
  const location = useLocation()
  const theme = useTheme()
  /** true em <600px (mobile); controla drawer permanente vs temporário no NavDrawer */
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))

  const [sidebarCollapsed, setSidebarCollapsed] = useState(getInitialDrawerCollapsed)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY_DRAWER, String(sidebarCollapsed))
    } catch {
      // ignore
    }
  }, [sidebarCollapsed])

  const handleDrawerToggle = useCallback(() => {
    if (isMobile) {
      setMobileOpen((prev) => !prev)
    } else {
      setSidebarCollapsed((prev) => !prev)
    }
  }, [isMobile])

  const handleDrawerClose = useCallback(() => {
    if (isMobile) setMobileOpen(false)
  }, [isMobile])

  const isActive = useCallback(
    (path: string) => location.pathname === path,
    [location.pathname]
  )

  const currentPageLabel = useMemo(
    () => getPageLabel(location.pathname),
    [location.pathname]
  )

  const drawerWidth =
    sidebarCollapsed && !isMobile ? DRAWER_WIDTH_COLLAPSED : DRAWER_WIDTH

  const contentBg =
    theme.palette.mode === 'dark'
      ? theme.palette.background.default
      : 'rgba(245, 246, 250, 1)'

  return (
    <Box sx={{ display: 'flex', minHeight: '1vh', bgcolor: 'background.default' }}>
      <AppBar
        onDrawerToggle={handleDrawerToggle}
        sidebarCollapsed={sidebarCollapsed}
        isMobile={isMobile}
        currentPageLabel={currentPageLabel ?? undefined}
      />

      <NavDrawer
        sidebarCollapsed={sidebarCollapsed}
        isMobile={isMobile}
        mobileOpen={mobileOpen}
        onMobileClose={handleDrawerClose}
        isActive={isActive}
        onItemClick={handleDrawerClose}
      />

      <Box
        component="main"
        sx={{
          flexGrow: '100%',
          width: { sm: `calc(100% - ${drawerWidth}px)` },
        //  ml: { sm: `${drawerWidth}px` },
          mt: `${HEADER_HEIGHT}px`,
          minWidth: 0,
          boxSizing: 'border-box',
          bgcolor: contentBg,
          overflowX: 'hidden',
          overflowY: 'auto',
          minHeight: `calc(100vh - ${HEADER_HEIGHT}px)`,
        }}
      >
        <Box
          sx={{
            maxWidth: '1280px',
            mx: 'auto',
            pl: 1.5,
            pr: 3,
            py: 3,
            boxSizing: 'border-box',
            minWidth: 0,
            overflow: 'visible',
          }}
        >
          {children}
        </Box>
      </Box>

      <AssistenteChat />
    </Box>
  )
}
