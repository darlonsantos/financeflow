PROMPT -- Migração de IA Ollama → Google Gemini API

Atue como um arquiteto de software especialista em integração de IA.

Objetivo: Migrar a integração atual da IA local (Ollama) para a API do
Google Gemini, mantendo compatibilidade com o sistema existente e
garantindo performance, segurança e baixo custo.

Contexto atual: - A aplicação utiliza Ollama rodando localmente via
Docker. - As requisições são feitas via API REST. - O sistema é um
assistente financeiro web responsivo. - A IA é utilizada para: - análise
de gastos, - planejamento financeiro, - geração de relatórios, -
respostas inteligentes ao usuário.

Tarefas: 1. Criar a nova arquitetura de integração usando Google Gemini
API. 2. Definir o melhor modelo para produção (priorizando
custo-benefício). 3. Gerar exemplos de chamadas HTTP REST para o Gemini
API. 4. Definir o formato ideal de request e response. 5. Ajustar os
prompts para contexto financeiro. 6. Propor estratégias para: -
segurança da API Key, - controle de custos, - cache de respostas, -
limitação de tokens, - alta performance. 7. Sugerir boas práticas para
produção.

Restrições: - Não expor a API Key no frontend. - Priorizar baixa
latência. - Priorizar baixo custo. - Garantir escalabilidade.

Formato da resposta: - Arquitetura - Fluxo de requisição - Exemplo de
código - Prompt financeiro ideal - Estratégia de produção
