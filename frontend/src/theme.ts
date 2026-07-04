import { createTheme, alpha } from '@mui/material/styles'

// Cores Material Design 3 - FinanceFlow
const palette = {
  primary: {
    main: '#6750A4',
    light: '#B8A9D4',
    dark: '#4F378B',
  },
  secondary: {
    main: '#625B71',
  },
  success: {
    main: '#4CAF50', // Receitas
  },
  error: {
    main: '#F44336', // Despesas
  },
  info: {
    main: '#2196F3', // Saldo/Neutro
  },
}

const typography = {
  fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  h4: { fontWeight: 500, fontSize: '2rem' },
  h5: { fontWeight: 600, fontSize: '1.5rem' },
  h6: { fontWeight: 500, fontSize: '1.25rem' },
  subtitle1: { fontWeight: 500, fontSize: '0.875rem' },
  body2: { fontSize: '0.875rem' },
  caption: { fontSize: '0.75rem' },
}

// 5. Espaçamento e Layout - Material breakpoints
const breakpoints = {
  values: {
    xs: 0,
    sm: 600,
    md: 900,
    lg: 1200,
    xl: 1536,
  },
}

// Padding: 24px desktop, 16px mobile | Gap: 24px
const spacing = 8 // 1 unit = 8px → 2=16px, 3=24px

export const lightTheme = createTheme({
  breakpoints,
  spacing,
  palette: {
    mode: 'light',
    primary: palette.primary,
    secondary: palette.secondary,
    success: palette.success,
    error: palette.error,
    info: palette.info,
    background: {
      default: '#FEFBFF',
      paper: '#FFFFFF',
    },
  },
  typography,
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
          transition: 'box-shadow 0.2s ease-in-out',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
          },
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRight: '1px solid',
          borderColor: 'divider',
        },
      },
    },
  },
})

export const darkTheme = createTheme({
  breakpoints,
  spacing,
  palette: {
    mode: 'dark',
    primary: palette.primary,
    secondary: palette.secondary,
    success: palette.success,
    error: palette.error,
    info: palette.info,
    background: {
      default: '#1E1E1E',
      paper: '#2D2D2D',
    },
  },
  typography,
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
          transition: 'box-shadow 0.2s ease-in-out',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          },
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRight: '1px solid',
          borderColor: alpha('#fff', 0.12),
        },
      },
    },
  },
})
