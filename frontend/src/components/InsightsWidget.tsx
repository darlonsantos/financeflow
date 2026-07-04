import { Link } from 'react-router-dom'
import {
  alpha,
  Box,
  Card,
  CardContent,
  CardHeader,
  Typography,
  Button,
} from '@mui/material'
import LightbulbIcon from '@mui/icons-material/Lightbulb'
import InfoIcon from '@mui/icons-material/Info'
import WarningIcon from '@mui/icons-material/Warning'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import type { Insight } from '../hooks/useInsights'

interface InsightsWidgetProps {
  insights: Insight[]
}

const variantConfig = {
  info: {
    icon: InfoIcon,
    bg: (t: { palette: { info: { main: string } } }) => alpha(t.palette.info.main, 0.08),
    border: (t: { palette: { info: { main: string } } }) => alpha(t.palette.info.main, 0.3),
    color: 'info.main',
  },
  warning: {
    icon: WarningIcon,
    bg: (t: { palette: { warning: { main: string } } }) => alpha(t.palette.warning.main, 0.08),
    border: (t: { palette: { warning: { main: string } } }) => alpha(t.palette.warning.main, 0.3),
    color: 'warning.main',
  },
  success: {
    icon: CheckCircleIcon,
    bg: (t: { palette: { success: { main: string } } }) => alpha(t.palette.success.main, 0.08),
    border: (t: { palette: { success: { main: string } } }) => alpha(t.palette.success.main, 0.3),
    color: 'success.main',
  },
} as const

export default function InsightsWidget({ insights }: InsightsWidgetProps) {
  if (insights.length === 0) return null

  return (
    <Card
      elevation={1}
      sx={{
        borderRadius: 2,
        transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        '&:hover': { boxShadow: 2 },
      }}
    >
      <CardHeader
        avatar={
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              bgcolor: (t) => alpha(t.palette.primary.main, 0.12),
              color: 'primary.main',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <LightbulbIcon />
          </Box>
        }
        title="Insights automáticos"
        subheader="Contexto com base nos seus dados"
        titleTypographyProps={{ variant: 'h6' }}
        sx={{ px: 3, pt: 3, pb: 0 }}
      />
      <CardContent sx={{ pt: 2, px: 3, pb: 3 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          {insights.map((insight) => {
            const config = variantConfig[insight.variant]
            const Icon = config.icon
            return (
              <Box
                key={insight.id}
                component={insight.link ? Link : 'div'}
                to={insight.link}
                sx={{
                  p: 2,
                  borderRadius: 2,
                  bgcolor: config.bg,
                  border: '1px solid',
                  borderColor: config.border,
                  textDecoration: 'none',
                  color: 'text.primary',
                  transition: 'background-color 0.2s',
                  '&:hover': insight.link
                    ? {
                        bgcolor: (t: { palette: { action: { hover: string } } }) =>
                          t.palette.action.hover,
                      }
                    : undefined,
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
                  <Icon sx={{ color: config.color, fontSize: 22, mt: 0.25 }} />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle2" fontWeight={600} color={config.color}>
                      {insight.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
                      {insight.message}
                    </Typography>
                  </Box>
                </Box>
              </Box>
            )
          })}
        </Box>
        <Button
          component={Link}
          to="/transactions"
          size="small"
          sx={{ mt: 2, textTransform: 'none' }}
        >
          Ver transações
        </Button>
      </CardContent>
    </Card>
  )
}
