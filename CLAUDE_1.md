# CLAUDE.md - Contexto do Projeto TOTEM SGA

Este arquivo existe para que qualquer instância do Claude (Claude Code
ou outra) trabalhando neste repositório tenha contexto imediato do
projeto, sem precisar re-descobrir decisões e integrações já validadas.

## Visão Geral

**TOTEM SGA** é um app Android nativo (Java) de autoatendimento para o
**Grupo Alvorada**, integrado a uma instância do **NovoSGA v2.2.5**
(sistema de gerenciamento de filas open-source) rodando em servidor
local: `http://10.7.0.89`.

O desenvolvimento tem sido feito com apoio de IA (Gemini no Android
Studio aplicando código; outra IA - Claude - atuando como camada de
investigação/especificação técnica antes de cada mudança). Este arquivo
resume o estado técnico consolidado até o momento.

## Stack Técnica

- Android nativo, Java.
- Retrofit + OkHttp para chamadas de rede.
- Glide para carregamento de imagem/GIF (logo animada configurável).
- SharedPreferences via `SessionManager` para toda configuração do
  Admin.
- Sem framework de UI declarativa - layouts XML tradicionais.

## IMPORTANTE: Duas autenticações diferentes coexistem

O NovoSGA expõe **duas superfícies de API distintas**, com autenticação
diferente cada uma. Não confundir:

### 1. API pública OAuth2 (`/api/*`)
- `POST /api/token` (`grant_type=password`, Client ID/Secret, usuário,
  senha) → retorna `access_token` (expira em 3600s/1h) + `refresh_token`.
- Usada para: `GET /api/unidades`, `GET /api/departamentos`,
  `POST /api/distribui` (emitir senha no fluxo normal de triagem).
- Autenticação via header `Authorization: Bearer {token}`.
- **Já teve um bug crítico**: o `Authenticator` do Retrofit reenviava o
  token expirado na própria tentativa de renovação, causando falha em
  cascata após ~1h de inatividade. Corrigido com cliente OkHttp "limpo"
  para o refresh + sincronização + keep-alive (ping a cada 30 min).

### 2. Sessão via cookie (`/novosga.triage/*`, `/novosga.monitor/*`)
- `GET /login` → extrair `_csrf_token` do HTML retornado (regex:
  `name=["']_csrf_token["']\s+value=["']([^"']+)["']`, com fallback
  para ordem inversa dos atributos).
- `POST /login` (`username`, `password`, `_csrf_token`) → resposta
  `302 Found` + `Set-Cookie: PHPSESSID=...`.
- Usada para: consulta de cliente, agendamentos, monitor de senhas
  (endpoints não documentados publicamente, descobertos via inspeção
  de rede do frontend real do NovoSGA).
- **Sessão deve ser reaproveitada**, não recriada a cada chamada (login
  só na primeira vez ou quando detectada expiração). Lógica encapsulada
  no Singleton `ClienteAuthManager`.
- Cookies devem ser **mesclados**, não sobrescritos, no `CookieJar`
  customizado (`SessionCookieJar`).

## Endpoints não documentados (descobertos via DevTools)

| Endpoint | Método | Retorno |
|---|---|---|
| `/novosga.triage/clientes?q={documento}` | GET | Dados do cliente por CPF/CNPJ/telefone |
| `/novosga.triage/agendamentos/{servicoId}` | GET | Lista de agendamentos de um serviço (contém `cliente.documento`, `dataConfirmacao`) |
| `/novosga.triage/distribui_agendamento/{agendamentoId}` | POST | Confirma presença e gera senha (mesmo formato do TicketResponse normal) |
| `/novosga.monitor/info_senha/{senhaId}` | GET | Detalhes completos de uma senha já emitida (inclui `cliente.documento`, `status`, `hash`) |
| `/novosga.monitor/ajax_update?ids={lista}` | GET | Status agregado da fila por serviço (contadores) - **ainda não confirmado se retorna IDs individuais de senha** |
| `/novosga.triage/ajax_update?ids={lista}` | GET | Equivalente ao acima, na superfície de triagem |

**Padrão importante**: nenhum desses endpoints faz busca direta por
documento. O padrão é sempre "buscar todos os itens de uma categoria
(serviço) e filtrar localmente pelo documento digitado" - confirmado
tanto para agendamentos quanto (provavelmente) para reimpressão de
senha, que está em investigação no momento deste registro.

## Funcionalidades já implementadas e validadas

1. **Autocomplete de cliente na triagem** (CPF/CNPJ/telefone) -
   `ClienteAuthManager` + `ScreeningFragment`. Gatilho de busca sensível
   ao tipo de documento (11 dígitos CPF/telefone, 14 CNPJ). Indicador
   visual "Encontrado no cadastro..." com possibilidade de edição
   manual. Reset do timer de inatividade ao receber resposta da API
   (evita timeout do totem durante a espera).
2. **Correção de expiração de token OAuth** (ver acima).
3. **Confirmação de presença via agendamento** (tela "Já Tenho
   Agendamento") - busca paralela (ExecutorService + CountDownLatch)
   em todos os serviços habilitados, filtro local por documento,
   tratamento de `dataConfirmacao` (já confirmado vs. pendente).
   **Um bug de crash ao confirmar presença foi relatado e o usuário
   afirma ter corrigido, mas a causa raiz não está documentada em
   texto neste momento.**
4. **Redesign visual completo**: paleta oficial Amarelo `#FFCC00`,
   Laranja `#F47B20`, Vermelho `#E31E24`; tipografia Poppins; ícones
   dinâmicos via `IconMapper` (mapeamento por palavra-chave, com
   fallback distinto para departamento/serviço/nome-de-pessoa); cards
   com fundo neutro + acento colorido (barra lateral + ícone) em vez de
   fundo sólido (testado e revertido após ficar visualmente pesado com
   15+ departamentos); botões padronizados em formato pill; efeito de
   "Onda Dupla em Camadas" no topo/rodapé (duas camadas de onda
   sobrepostas com offset, uma estática/translúcida atrás, uma animada
   na frente).

## Em investigação / pendente no momento deste registro

- **Tela de reimpressão de senha**: usuário digita documento, app
  busca se há senha emitida para aquele documento no dia, e permite
  reimprimir (útil para falha de impressora ou perda do papel). Decisão
  já tomada: implementar primeiro com **registro local no próprio app**
  (salvar cada `TicketResponse` gerado, com documento + data, em
  SQLite/storage local), já que hoje há apenas 1 totem. Investigação
  paralela em andamento para confirmar se dá para buscar via API
  (`/novosga.monitor/ajax_update` ou similar) para quando houver
  múltiplos totems no futuro - ainda não confirmado se esse endpoint
  retorna os IDs individuais de senha necessários para essa busca.

## Lições e padrões a seguir

- **Nunca aceitar "compilou com sucesso" como prova de que uma mudança
  funciona.** Várias vezes neste projeto uma implementação reportada
  como concluída não correspondia ao resultado real (autocomplete não
  disparando, onda dupla sem efeito visual, cor de card não aplicada).
  Sempre exigir validação real (print, log) antes de aceitar como
  pronto.
- **Investigar endpoints não documentados via DevTools do navegador**
  (aba Network, filtro Fetch/XHR) na interface web real do NovoSGA
  antes de assumir que um endpoint existe ou tem determinado formato.
- **Isolar mudanças arriscadas em telas de teste dedicadas** antes de
  integrar ao fluxo real de produção.
- **Reaproveitar sessão/autenticação já validada** em vez de recriar a
  cada operação - essencial dado o timeout curto de inatividade do
  totem.
- O projeto segue uma política de versionamento V1/V2 com backup físico
  de `java`, `res`, `AndroidManifest.xml` e arquivos de build antes de
  qualquer alteração arriscada (ver estrutura `BACKUP_V1_.../
  BACKUP_V2_.../` na raiz do projeto).

## Configurações do Admin (SharedPreferences via SessionManager)

URL da API, Client ID, Client Secret, Usuário, Senha (autenticação
unificada OAuth + sessão), Timeout de Triagem, Habilitar
Impressão/Triagem, Agrupar por Departamento, Ativar Sons, Dimensões do
Logotipo, Ajustes dos Botões de Seleção (altura/fonte), Ajustes do
Botão Voltar (largura %/altura/fonte/posição/alinhamento), Direção e
Velocidade do Gradiente Animado, Altura do Gradiente Topo/Rodapé,
Margem Logo/Título, número de colunas do grid (Departamentos e
Serviços, configuráveis separadamente via `getDeptGrid()` /
`getServiceGrid()`).
