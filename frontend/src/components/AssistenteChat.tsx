import { useState, useRef, useEffect } from 'react'
import { useAssistantChat } from '../hooks/useAssistant'
import { useErrorHandler } from '../hooks/useErrorHandler'
import {
  Box,
  Fab,
  Paper,
  Typography,
  TextField,
  IconButton,
  Collapse,
  useTheme,
} from '@mui/material'
import ChatIcon from '@mui/icons-material/Chat'
import SendIcon from '@mui/icons-material/Send'
import CloseIcon from '@mui/icons-material/Close'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import PersonIcon from '@mui/icons-material/Person'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

const SUGGESTIONS = [
  'Quanto gastei com mercado este mês?',
  'Quanto ganhei este mês?',
  'Qual foi o total de despesas?',
  'Qual a porcentagem da minha meta?',
  'Como estão os orçamentos?',
]

export default function AssistenteChat() {
  const theme = useTheme()
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const chatMutation = useAssistantChat()
  const { handleError } = useErrorHandler()

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    if (open && messages.length === 0) {
      setMessages([
        {
          id: 'welcome',
          role: 'assistant',
          content: 'Olá! Sou seu assistente financeiro. Pergunte sobre despesas, receitas, metas ou orçamentos. Ex.: "Quanto gastei com mercado este mês?", "Qual a porcentagem da minha meta?" ou "Como estão os orçamentos?"',
          timestamp: new Date(),
        },
      ])
    }
  }, [open])

  const handleSend = async () => {
    const text = input.trim()
    if (!text || chatMutation.isPending) return

    setInput('')
    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: new Date(),
    }
    setMessages((prev) => [...prev, userMsg])

    try {
      const response = await chatMutation.mutateAsync({ message: text })
      const assistantMsg: ChatMessage = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response.message,
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, assistantMsg])
    } catch (err) {
      handleError(err, 'Erro ao obter resposta do assistente')
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: 'Desculpe, ocorreu um erro. Tente novamente.',
          timestamp: new Date(),
        },
      ])
    }
  }

  const handleSuggestionClick = (suggestion: string) => {
    setInput(suggestion)
  }

  return (
    <Box sx={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1400 }}>
      <Collapse in={open}>
        <Paper
          elevation={8}
          sx={{
            position: 'absolute',
            bottom: 72,
            right: 0,
            width: 360,
            maxWidth: 'calc(100vw - 48px)',
            height: 480,
            maxHeight: 'calc(100vh - 160px)',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            borderRadius: 3,
          }}
        >
          <Box
            sx={{
              p: 2,
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <SmartToyIcon />
              <Typography variant="subtitle1" fontWeight={600}>
                Assistente Financeiro
              </Typography>
            </Box>
            <IconButton size="small" color="inherit" onClick={() => setOpen(false)} aria-label="fechar">
              <CloseIcon />
            </IconButton>
          </Box>

          <Box
            sx={{
              flex: 1,
              overflow: 'auto',
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              gap: 1.5,
              bgcolor: 'background.default',
            }}
          >
            {messages.map((msg) => (
              <Box
                key={msg.id}
                sx={{
                  display: 'flex',
                  gap: 1,
                  flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                  alignItems: 'flex-start',
                }}
              >
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    bgcolor: msg.role === 'user' ? 'primary.main' : 'action.hover',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  {msg.role === 'user' ? (
                    <PersonIcon sx={{ fontSize: 18, color: 'primary.contrastText' }} />
                  ) : (
                    <SmartToyIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                  )}
                </Box>
                <Paper
                  variant="outlined"
                  sx={{
                    p: 1.5,
                    maxWidth: '80%',
                    bgcolor: msg.role === 'user' ? 'primary.main' : 'background.paper',
                    color: msg.role === 'user' ? 'primary.contrastText' : 'text.primary',
                  }}
                >
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {msg.content}
                  </Typography>
                </Paper>
              </Box>
            ))}
            <div ref={messagesEndRef} />
          </Box>

          {messages.length === 1 && (
            <Box sx={{ px: 2, pb: 1 }}>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                Sugestões:
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {SUGGESTIONS.map((s) => (
                  <Typography
                    key={s}
                    variant="caption"
                    sx={{
                      cursor: 'pointer',
                      px: 1,
                      py: 0.5,
                      borderRadius: 1,
                      bgcolor: 'action.hover',
                      '&:hover': { bgcolor: 'action.selected' },
                    }}
                    onClick={() => handleSuggestionClick(s)}
                  >
                    {s}
                  </Typography>
                ))}
              </Box>
            </Box>
          )}

          <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField
                placeholder="Pergunte sobre suas finanças..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                size="small"
                fullWidth
                disabled={chatMutation.isPending}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
              />
              <IconButton
                color="primary"
                onClick={handleSend}
                disabled={!input.trim() || chatMutation.isPending}
                aria-label="enviar"
              >
                <SendIcon />
              </IconButton>
            </Box>
          </Box>
        </Paper>
      </Collapse>

      <Fab
        color="primary"
        aria-label="assistente financeiro"
        onClick={() => setOpen(!open)}
        sx={{
          boxShadow: theme.shadows[8],
          '&:hover': { boxShadow: theme.shadows[12] },
        }}
      >
        <ChatIcon />
      </Fab>
    </Box>
  )
}
