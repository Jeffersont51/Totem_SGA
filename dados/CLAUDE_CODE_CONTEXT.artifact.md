# Contexto do Projeto: TOTEM SGA (Master Baseline V34)

Este documento fornece uma visão técnica completa do projeto **TOTEM SGA** para facilitar a atuação de agentes de IA (como Claude Code) na manutenção e evolução do sistema.

---

## 1. Visão Geral
- **Objetivo**: Interface de autoatendimento (Kiosk Mode) para o sistema NovoSGA 2.
- **Plataforma**: Android Nativo (Java).
- **Ambiente Atual**: Homologação (V2) sincronizada com Produção (V1) até a Baseline V34.

---

## 2. Arquitetura e Tecnologias
- **Rede**: Retrofit 2 + OkHttp + Gson.
- **Layout**: FlexboxLayout (para botões dinâmicos) e ConstraintLayout.
- **Estilização**: Temas dinâmicos baseados nas cores de Serviço/Departamento do NovoSGA.
- **Persistência**: `SessionManager` (SharedPreferences).
- **Publicidade**: `AdManager` (Gerencia vídeos/imagens/YouTube em ciclos de inatividade).

---

## 3. Fluxos Principais

### A. Triagem Manual (Screening)
1. `SelectionFragment`: Escolha de Departamento e Serviço.
2. `ScreeningFragment`: Coleta de Nome/Documento (se habilitado via feature flag).
3. `SuccessFragment`: Exibição da senha e impressão automática.

### B. Confirmação de Agendamento (Scheduling)
- **Tela**: `ConfirmSchedulingFragment`.
- **Lógica**: Busca paralela (threads concorrentes) em todos os serviços ativos da unidade para localizar agendamentos do dia por CPF/CNPJ/Telefone.
- **Regra**: Trava de horário vencido com parâmetro de `tolerancia` dinâmico.

### C. Reimpressão de Senha (Reprint)
- **Tela**: `ReprintFragment`.
- **Lógica**: Consulta ao módulo `/monitor` do NovoSGA para listar senhas emitidas hoje para o documento ou número da senha informado.
- **Resiliência**: Auto-recuperação de sessão caso o servidor responda com HTML indevido.

---

## 4. Autenticação e Sessão (NovoSGA)
O projeto lida com dois tipos de autenticação simultânea:
1. **API REST**: OAuth2 (Bearer Token) para listagem de serviços e unidades.
2. **Módulos Web (Triage/Monitor)**:
    - O `ClienteAuthManager` realiza um login manual via `GET /login` -> extração de `_csrf_token` via Regex -> `POST /login` -> Manutenção de cookies via `SessionCookieJar`.
    - **Ponto Crítico**: O servidor SGA pode retornar HTML de login com status `200 OK` se a sessão cair. O app detecta isso na Baseline V34 e reloga automaticamente.

---

## 5. Parâmetros Dinâmicos (Feature Flags)
O app lê o campo "Descrição" do serviço/departamento no NovoSGA para ativar funcionalidades:
- `VISIVEL`: Exibe o serviço no totem.
- `TRIAGEM`: Exibe tela de coleta de dados.
- `TRIAGEM_OBRIGATORIA`: Bloqueia emissão sem documento.
- `SEGUNDAVIA`: Ativa o botão de reimpressão na tela inicial.
- `tolerancia30`: Define 30 minutos de tolerância para atrasos no agendamento.
- `FACIAL`: Muda textos/rodapés para fluxos de biometria.

---

## 6. Padrões de UX e Estabilidade (V21 - V34)
- **Feedback In-line**: NUNCA usar `AlertDialog`. Usar `TextView` vermelho que some ao digitar.
- **Gestão de Timers**:
    - Inatividade de Tela: 50 segundos (configurável).
    - Inatividade de Ads: 10-30 segundos.
    - **Suspensão Ativa**: O `AdManager` é fisicamente interrompido no `onCreateView` de telas de atendimento para evitar sobreposição de anúncios.
- **Máscaras de Documento**: Protegidas pela flag `isApplyingMask` para evitar loops de formatação.

---

## 7. Histórico de Versões Recentes
- **V20**: Promoção para Produção.
- **V21-V24**: Estabilização de publicidade e suspensão ativa de timers.
- **V25**: Restauração do cronômetro visual regressivo.
- **V26-V29**: Implementação da Reimpressão e correção de duplicatas.
- **V30-V34**: Blindagem de rede, correção de encodamento de URL (`%2C`) e auto-recuperação de sessão no totem físico.

---
**Diretriz de Comunicação**: Manter todas as interações e documentações em **Português (PT-BR)**.
