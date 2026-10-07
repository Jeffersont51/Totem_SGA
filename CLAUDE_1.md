# CLAUDE.md - Contexto do Projeto TOTEM SGA

Este arquivo existe para que qualquer instância do Claude (Claude Code
ou outra) trabalhando neste repositório tenha contexto imediato do
projeto, sem precisar re-descobrir decisões e integrações já validadas.

## Visão Geral

**TOTEM SGA** é um app Android nativo (Java) de autoatendimento para o
**Grupo Alvorada**, integrado a uma instância do **NovoSGA v2.2.5**
(sistema de gerenciamento de filas open-source) rodando em servidor
local: `http://10.7.0.89`.

Desde 07/10/2026 o desenvolvimento é feito direto pelo Claude Code
(edita o código e gera o APK com `gradlew assembleDebug`). O fluxo
antigo com o Gemini (`AGENT_PROTOCOL.md` / `TASK_FOR_GEMINI.md` /
`BUILD_STATUS.md`) não é mais usado; esses arquivos ficaram só como
histórico.

O totem de homologação (Sunmi K2, IP 192.168.168.139) está com a porta
USB quebrada e o Android dele não tem depuração sem fio: o APK é
instalado por pendrive e a tela é acessada por RealVNC. Não há como ler
logcat remotamente.

A documentação oficial é o `DOCUMENTATION.md` (operação, parte técnica,
como compilar/publicar, pendências e histórico de versões). Ao mudar o
app, atualize-o no mesmo commit. Este arquivo guarda só o contexto de
investigação que não cabe lá (endpoints não documentados, lições).

Publicar versão nova: compilar, conferir a assinatura, copiar o APK
para `\\192.168.1.227\Programas TI\TOTEM_SGA` como
`TOTEM_SGA_v<versionName>.apk`; no totem, Admin > Atualizar aplicativo.
O passo a passo completo está na seção 15 do `DOCUMENTATION.md`.

Decisões do Jefferson em 07/10/2026 que não devem ser revertidas sem
perguntar:
- Impressora sem papel, com tampa aberta ou travada **bloqueia** a
  emissão de senha (em vez de emitir e só avisar).
- O atualizador **não guarda** usuário e senha de rede; o técnico de TI
  digita o próprio acesso a cada atualização.
- Todas as correções vão num APK só; os testes são feitos por etapas
  depois de instalar.

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
- **Segundo bug (07/10/2026, v1.1.0)**: quando o refresh era recusado
  (refresh vencido ou perdido numa queda de Wi-Fi), o app desistia e
  ficava em "Falha ao carregar departamentos (401)" até alguém salvar a
  configuração de novo. Agora cai para `grant_type=password` com as
  credenciais salvas (`RetrofitClient.renewToken`).

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
  no Singleton `ClienteAuthManager`. Toda chamada nova nessa superfície
  deve passar por `executeWithSession()`, que detecta a expiração
  (inclusive HTML de login no lugar do JSON) e refaz o login.
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

## Reimpressão de senha (implementada)

A reimpressão está funcionando desde o primeiro commit do repositório
(`ReprintFragment`, busca via `/novosga.monitor/ajax_update`). O texto
abaixo é o registro da investigação original, mantido como histórico.

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
