import Layout from '../components/Layout'
import { useBehavioralProfile } from '../hooks/useBehavioralProfile'
import { ChartSkeleton } from '../components/Loading'
import {
  Box,
  Button,
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
import RefreshIcon from '@mui/icons-material/Refresh'
import PersonIcon from '@mui/icons-material/Person'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined'
import PsychologyIcon from '@mui/icons-material/Psychology'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'

const riskColors: Record<string, 'success' | 'warning' | 'error'> = {
  Baixo: 'success',
  Médio: 'warning',
  Alto: 'error',
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

export default function BehavioralProfile() {
  const { data: profile, isLoading, error, refetch, isFetching } = useBehavioralProfile()
  const riskColor = profile ? riskColors[profile.riskLevel] ?? 'warning' : 'warning'

  if (isLoading && !profile) {
    return (
      <Layout>
        <ChartSkeleton />
      </Layout>
    )
  }

  if (error || !profile) {
    return (
      <Layout>
        <Alert
          severity="error"
          sx={{ mt: 2 }}
          action={
            <Button color="inherit" size="small" onClick={() => refetch()} disabled={isFetching}>
              Tentar novamente
            </Button>
          }
        >
          <AlertTitle>Erro ao carregar perfil</AlertTitle>
          Não foi possível gerar o perfil comportamental. Tente novamente.
        </Alert>
      </Layout>
    )
  }

  const hasLists =
    (profile.patterns?.length ?? 0) > 0 ||
    (profile.criticalPoints?.length ?? 0) > 0 ||
    (profile.suggestions?.length ?? 0) > 0

  return (
    <Layout>
      <Box sx={{ maxWidth: 900, mx: 'auto' }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonIcon color="primary" sx={{ fontSize: 32 }} />
            <Box>
              <Typography variant="h5" fontWeight={600}>
                Perfil Financeiro Comportamental
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Classificação com base em gastos, renda, metas e parcelamentos (últimos 6 meses).
              </Typography>
            </Box>
          </Box>
          <Button
            variant="outlined"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? 'Atualizando...' : 'Atualizar perfil'}
          </Button>
        </Box>

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
          <Chip
            icon={<PsychologyIcon />}
            label={profile.profileType}
            color="primary"
            variant="filled"
            sx={{ fontWeight: 600 }}
          />
          <Chip
            label={`Risco: ${profile.riskLevel}`}
            color={riskColor}
            variant="outlined"
          />
          {profile.fromAi && (
            <Chip label="Análise por IA" size="small" variant="outlined" />
          )}
          <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center', ml: 1 }}>
            Gerado em {formatDate(profile.generatedAt)}
          </Typography>
        </Box>

        {hasLists && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {profile.patterns?.length > 0 && (
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <TrendingUpIcon color="action" />
                    <Typography variant="subtitle1" fontWeight={600}>
                      Principais padrões comportamentais
                    </Typography>
                  </Box>
                  <List dense disablePadding>
                    {profile.patterns.map((item, i) => (
                      <ListItem key={i} disablePadding sx={{ py: 0.25 }}>
                        <ListItemIcon sx={{ minWidth: 28 }}>
                          <CheckCircleOutlineIcon fontSize="small" color="success" />
                        </ListItemIcon>
                        <ListItemText primary={item} primaryTypographyProps={{ variant: 'body2' }} />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>
              </Card>
            )}

            {profile.criticalPoints?.length > 0 && (
              <Card sx={{ borderLeft: 3, borderColor: 'warning.main' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <WarningAmberIcon color="warning" />
                    <Typography variant="subtitle1" fontWeight={600}>
                      Pontos críticos
                    </Typography>
                  </Box>
                  <List dense disablePadding>
                    {profile.criticalPoints.map((item, i) => (
                      <ListItem key={i} disablePadding sx={{ py: 0.25 }}>
                        <ListItemIcon sx={{ minWidth: 28 }}>
                          <WarningAmberIcon fontSize="small" color="warning" />
                        </ListItemIcon>
                        <ListItemText primary={item} primaryTypographyProps={{ variant: 'body2' }} />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>
              </Card>
            )}

            {profile.suggestions?.length > 0 && (
              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <LightbulbOutlinedIcon sx={{ color: 'warning.main' }} />
                  <Typography variant="subtitle1" fontWeight={600}>
                    Sugestões de melhoria
                  </Typography>
                </Box>
                <List dense disablePadding>
                  {profile.suggestions.map((item, i) => (
                    <ListItem key={i} disablePadding sx={{ py: 0.25 }}>
                      <ListItemIcon sx={{ minWidth: 28 }}>
                        <LightbulbOutlinedIcon fontSize="small" sx={{ color: 'warning.main' }} />
                      </ListItemIcon>
                      <ListItemText primary={item} primaryTypographyProps={{ variant: 'body2' }} />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            )}
          </Box>
        )}

        {!hasLists && (
          <Alert severity="info" sx={{ mt: 2 }}>
            O perfil foi classificado como <strong>{profile.profileType}</strong> com risco{' '}
            <strong>{profile.riskLevel}</strong>. Adicione mais transações e metas para obter padrões e sugestões detalhados.
          </Alert>
        )}
      </Box>
    </Layout>
  )
}
