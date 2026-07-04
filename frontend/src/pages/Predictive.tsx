import Layout from '../components/Layout'
import { usePredictiveReport } from '../hooks/usePredictive'
import { ChartSkeleton } from '../components/Loading'
import {
  Box,
  Card,
  CardContent,
  Chip,
  Typography,
  Alert,
  AlertTitle,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
} from '@mui/material'
import PsychologyIcon from '@mui/icons-material/Psychology'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined'
import TimelineIcon from '@mui/icons-material/Timeline'

const severityConfig = {
  HIGH: { color: 'error' as const, icon: <WarningAmberIcon /> },
  MEDIUM: { color: 'warning' as const, icon: <InfoOutlinedIcon /> },
  LOW: { color: 'info' as const, icon: <InfoOutlinedIcon /> },
}

export default function Predictive() {
  const { data: report, isLoading, error } = usePredictiveReport()

  if (isLoading) {
    return (
      <Layout>
        <ChartSkeleton />
      </Layout>
    )
  }

  if (error || !report) {
    return (
      <Layout>
        <Alert severity="error" sx={{ mt: 2 }}>
          <AlertTitle>Erro ao carregar relatório</AlertTitle>
          Não foi possível carregar a análise preditiva. Tente novamente.
        </Alert>
      </Layout>
    )
  }

  const hasAlerts = report.alerts && report.alerts.length > 0

  return (
    <Layout>
      <Box sx={{ maxWidth: 900, mx: 'auto' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <PsychologyIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight={600}>
            Inteligência Preditiva
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Análise com base nos últimos {report.historicalMonths} meses e projeção para os próximos {report.projectionMonths} meses.
        </Typography>

        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Resumo
            </Typography>
            <Typography variant="body1">{report.summary}</Typography>
          </CardContent>
        </Card>

        {report.scenarioNextMonths && (
          <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: 'action.hover' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              <TimelineIcon fontSize="small" color="action" />
              <Typography variant="subtitle2" color="text.secondary">
                Cenário nos próximos meses
              </Typography>
            </Box>
            <Typography variant="body2">{report.scenarioNextMonths}</Typography>
          </Paper>
        )}

        {hasAlerts ? (
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Alertas preventivos ({report.alerts.length})
              </Typography>
              <List disablePadding>
                {report.alerts.map((alert, index) => {
                  const config = severityConfig[alert.severity] ?? severityConfig.LOW
                  return (
                    <ListItem
                      key={`${alert.riskType}-${index}`}
                      alignItems="flex-start"
                      sx={{
                        borderLeft: 3,
                        borderColor: config.color === 'error' ? 'error.main' : config.color === 'warning' ? 'warning.main' : 'info.main',
                        borderRadius: 1,
                        bgcolor: 'action.hover',
                        mb: 1,
                        py: 1.5,
                      }}
                    >
                      <ListItemIcon sx={{ minWidth: 40, mt: 0.5 }}>
                        {config.icon}
                      </ListItemIcon>
                      <ListItemText
                        primaryTypographyProps={{ component: 'div' }}
                        secondaryTypographyProps={{ component: 'div' }}
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mb: 0.5 }}>
                            <Typography variant="subtitle2" fontWeight={600} component="span">
                              {alert.title}
                            </Typography>
                            <Chip label={alert.severity} size="small" color={config.color} variant="outlined" />
                          </Box>
                        }
                        secondary={
                          <>
                            <Typography variant="body2" component="span" display="block" sx={{ mb: 0.5 }}>
                              {alert.message}
                            </Typography>
                            {alert.suggestion && (
                              <Box
                                sx={{
                                  display: 'flex',
                                  alignItems: 'flex-start',
                                  gap: 0.5,
                                  mt: 1,
                                  p: 1,
                                  borderRadius: 1,
                                  bgcolor: 'background.paper',
                                }}
                              >
                                <LightbulbOutlinedIcon fontSize="small" sx={{ color: 'warning.main', mt: 0.2 }} />
                                <Typography variant="body2" color="text.secondary" component="span">
                                  {alert.suggestion}
                                </Typography>
                              </Box>
                            )}
                          </>
                        }
                      />
                    </ListItem>
                  )
                })}
              </List>
            </CardContent>
          </Card>
        ) : (
          <Alert severity="success" icon={<CheckCircleOutlineIcon />} sx={{ mt: 2 }}>
            <AlertTitle>Nenhum alerta no momento</AlertTitle>
            Sua situação financeira está dentro do esperado. Continue acompanhando o relatório periodicamente.
          </Alert>
        )}
      </Box>
    </Layout>
  )
}
