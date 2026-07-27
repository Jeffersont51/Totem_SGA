# Histórico de Implementações - TOTEM SGA

## Índice
1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Cronologia das Implementações](#2-cronologia-das-implementações)
3. [Versões e Entregas](#3-versões-e-entregas)
4. [Problemas Resolvidos](#4-problemas-resolvidos)
5. [Melhorias Aplicadas](#5-melhorias-aplicadas)
6. [Lições Aprendidas](#6-lições-aprendidas)
7. [Próximos Passos](#7-próximos-passos)

---

## 1. Visão Geral do Projeto
O projeto **TOTEM SGA** visa fornecer uma interface de autoatendimento (Kiosk Mode) para o sistema NovoSGA 2. O objetivo principal deste ciclo de desenvolvimento foi a implementação e estabilização da funcionalidade de **Confirmação de Agendamento**, permitindo que usuários que agendaram horário previamente possam realizar o "check-in" no Totem por meio do CPF, CNPJ ou Telefone, gerando automaticamente a senha de atendimento. Além disso, buscou-se a unificação visual e de comportamento entre os fluxos de Triagem e Agendamento para uma UX consistente.

---

## 2. Cronologia das Implementações

### 22/07/2026 - Início do Projeto (Ambiente de Homologação)
- **Estado Inicial**: Sistema estável para triagem manual, mas sem suporte a agendamentos.
- **Objetivo**: Criar uma tela de "Já tenho agendamento" integrada ao fluxo existente.
- **Desafio**: Sincronizar cookies de sessão PHP (NovoSGA) com a API REST do Android.

### 22/07/2026 - Evolução das Estratégias de Busca (V1 a V6)
- Definição da regra de negócio: Agendamentos apenas para o dia atual (Opção B).
- Tentativa inicial com o endpoint `/agendamentos`.
- Migração para `/ajax_update` buscando senhas em status "emitida".
- Identificação de que agendamentos do NovoSGA permanecem em status "agendado" até a confirmação manual.

### 23/07/2026 - Estabilização e Unificação (V7 a V17)
- Correção de crashes de mapeamento JSON via wrappers de resposta.
- Implementação de **Busca Paralela** em múltiplos serviços via `ExecutorService` e `CountDownLatch`.
- Unificação total da UI com a Triagem (Logo dinâmica, RadioButtons, alinhamento de componentes).
- Refinamento de UX: substituição de Diálogos Modais por textos informativos in-line no Agendamento.
- Correção de conflitos entre Máscaras de entrada e validadores de documento.

### 23/07/2026 - Padronização de Feedback (V18)
- Extensão do padrão de feedback visual in-line para o fluxo de Triagem.
- Remoção total de `AlertDialog` nas telas principais de interação com o usuário.
- Garantia de que a publicidade não seja interrompida por janelas de erro do sistema.

### 23/07/2026 - Travas de Segurança e Tolerância (V19)
- Implementação de validação de horário para agendamentos.
- Introdução do parâmetro dinâmico `tolerancia` via campo descrição do NovoSGA.
- Feedback visual com borda vermelha para itens vencidos e bloqueio de confirmação.

### 23/07/2026 - Promoção para Produção (V20)
- Sincronização oficial entre o ambiente de Homologação (V2) e Produção (V1).
- Backup de segurança da produção anterior (`BACKUP_V1_PRE_V19_PROMOTION`).
- Atualização integral do código-fonte e configurações de build no ambiente oficial.

### 24/07/2026 - Blindagem de Publicidade e Timeout (V21 a V24)
- Resolução do conflito entre o Timer Global de Publicidade e o Timeout de Tela.
- Implementação da **Suspensão Ativa** no AdManager (cancelamento físico de tarefas no Handler).
- Reordenação do Ciclo de Vida: suspensão iniciada no `onCreateView` para evitar race conditions.
- Padronização total entre Triagem e Agendamento.

### 24/07/2026 - Estabilização Visual e Promoção Final (V25)
- Restauração do cronômetro visual ("Tempo restante") no Agendamento.
- Promoção final da Baseline V25 para o ambiente de Produção.
- Sincronização integral de código e histórico.

### 24/07/2026 - Reimpressão de Senha (V26 e V27)
- Implementação da tela "REIMPRIMIR SENHA" (ReprintFragment).
- Integração com o monitor do NovoSGA (ajax_update) com filtragem local por documento.
- Replicação dos padrões de segurança: suspensão de ads e cronômetro de 50s.
- Ativação dinâmica via parâmetro "SEGUNDAVIA".

### 24/07/2026 - Busca Alfanumérica por Nº Senha (V28)
- Expansão do motor de busca para suportar o formato da senha (ex: AD001).
- Implementação de AllCaps e validação mínima de 3 caracteres.
- Readequação do layout com RadioButton para seleção de busca por senha.

### 24/07/2026 - Eliminação de Duplicatas na Reimpressão (V29)
- Implementação de controle de unicidade via `HashSet`.
- Correção da lógica de consolidação de resultados do módulo Monitor.

### 24/07/2026 - Diagnóstico e Estabilização de Rede (V30 a V34)
- Criação de motor de diagnóstico visual para capturar respostas HTML indevidas.
- Correção do encodamento de URL (fim do duplo escape `%252C`).
- Implementação de auto-recuperação de sessão no totem físico.
- Blindagem contra loops infinitos de login.

---

## 3. Versões e Entregas

### V1 - Planejamento Agendamento
- Mapeamento inicial de endpoints e fluxo de usuário.

### V2 - Primeira Implementação UI
- Criação do botão "JÁ TENHO AGENDAMENTO" e fragmento inicial.

### V3 - Backup e Segurança
- Criação de baselines de segurança (`BACKUP_V1_ANTES_AGENDAMENTO`).

### V4 - Modelagem de Dados
- Criação dos modelos `Agendamento.java` e `AgendamentoResponse.java`.

### V5 - Estratégia AJAX Update
- Mudança para o monitor de fila em tempo real (ajax_update).

### V6 - Cards de Seleção
- Implementação visual da lista de agendamentos com horários e serviços.

### V7 - Correção de Crash (Wrapper)
- Criação do `TicketTriageResponse` para resolver conflito de envelope JSON.

### V8 - Flag FACIAL
- Suporte à flag `is_facial` para exibição de mensagens de rodapé customizadas.

### V9 - Unificação de Cores
- Passagem de cor de tema dinâmica para o `SuccessFragment`.

### V10 - UI Unificada
- Implementação de RadioButtons para seleção de documento (CPF/CNPJ/Fone).

### V11 - Feedback Visual (Agendamento)
- Substituição de `AlertDialog` por `TextView` dinâmico para erros no Agendamento.

### V12 - Resiliência e Login
- Implementação de `ensureLoggedIn()` síncrono para evitar concorrência no login inicial.

### V13 - Logs de Diagnóstico
- Injeção de logs de depuração profunda (Raio-X de Agendamento).

### V14 - Layout Dinâmico
- Redimensionamento da logo baseado nas configurações do banco de dados no Agendamento.

### V15 - Integração ValidationUtils
- Validação rigorosa de dígitos verificadores de CPF/CNPJ no Agendamento.

### V16 - Busca por Botão
- Troca da busca automática pelo botão "CONSULTAR" para garantir validação prévia.

### V17 - Correção de Máscara (Final)
- Implementação da flag `isApplyingMask` para resolver conflito com `TextWatcher`.

### V18 - Feedback Visual (Triagem)
- Replicação do sistema de avisos in-line no `ScreeningFragment`.
- Remoção dos últimos pop-ups modais da interface principal.

### V19 - Trava de Horário (Tolerância)
- Implementação da lógica de agendamento vencido.
- Suporte a parâmetros complexos no `FeatureParser` (ex: `tolerancia30`).
- Bloqueio de botões e destaque visual para itens fora da margem de tempo.

### V20 - Sincronização Produção
- Promoção final do código validado para a pasta oficial de Produção.
- Padronização de baselines entre V1 e V2.

### V21-V24 - Estabilização de Publicidade
- Refatoração do `AdManager` para suporte a interrupção física de timers.
- Remoção de timers redundantes em fragmentos.
- Movimentação da lógica de suspensão para o início do ciclo de vida (`onCreateView`).
- Garantia de que o Timeout de 50s (Admin) tenha prioridade sobre os 10s (Publicidade).

### V25 - Cronômetro Visual e Promoção Final
- Restauração do loop de 1s para o TextView do timer no Agendamento.
- Consolidação final e promoção para a Baseline de Produção.

### V26-V27 - Reimpressão de Senha
- Criação do fluxo completo de busca e reimpressão física de tickets via módulo Monitor.

### V28 - Busca por Nº da Senha
- Inclusão da opção de localização de tickets através da sigla/número da senha.

### V29 - Eliminação de Duplicatas
- Ajuste técnico para garantir que cada ticket apareça apenas uma vez nos resultados de reimpressão.

### V30-V34 - Estabilização de Rede e Sessão
- Ajuste rigoroso da URL de busca para compatibilidade com totens físicos.
- Implementação de detecção de redirecionamento HTML e relogin automático.

---

## 4. Problemas Resolvidos

### Problema 1: Crash NullPointerException na Confirmação
- **Causa:** Tentativa de mapear objeto com envelope (`data`) para modelo direto.
- **Solução:** Criado Wrapper `TicketTriageResponse`.
- **Versão:** V7

### Problema 2: Agendamento ID 80 Não Localizado
- **Causa:** Divergência de formato de data (`yyyy-MM-dd` vs `dd/MM/yyyy`).
- **Solução:** Refatoração do método `isHoje()` para suportar múltiplos formatos.
- **Versão:** V12

### Problema 3: Máscara Desaparecendo
- **Causa:** TextWatcher limpando pontuações durante a aplicação da máscara.
- **Solução:** Flag de controle `isApplyingMask` para ignorar limpeza programática.
- **Versão:** V17

### Problema 4: Confirmação de Agendamento Atrasado
- **Causa:** Falta de validação entre a hora atual e a hora do agendamento.
- **Solução:** Implementada trava de horário com tolerância configurável via descrição.
- **Versão:** V19

### Problema 5: Publicidade Interrompendo Atendimento
- **Causa:** Timer global de 10s sobrepondo o timeout de 50s da tela devido a race condition no ciclo de vida.
- **Solução:** Implementada suspensão ativa e reordenação no `onCreateView`.
- **Versão:** V24

---

## 5. Melhorias Aplicadas

### Melhoria 1: Busca Paralela (Performance)
- **Descrição:** Consulta simultânea em todos os serviços ativos da unidade.
- **Benefício:** Redução drástica no tempo de espera do usuário.
- **Versão:** V10

### Melhoria 2: UI In-line (UX)
- **Descrição:** Remoção de pop-ups modais para mensagens de erro em todo o app (Triagem e Agendamento).
- **Benefício:** Experiência mais fluida e publicidade ininterrupta.
- **Versão:** V11 e V18

---

## 6. Lições Aprendidas
- **Paralelismo**: Em Android nativo, a orquestração via `CountDownLatch` é vital para consolidar resultados de APIs que não possuem busca global.
- **Segurança**: Garantir o login síncrono antes de disparar threads concorrentes evita falhas intermitentes de autenticação.
- **UX em Totens**: Menos é mais. Avisos que não bloqueiam a tela e controle manual de ações críticas (botão vs automático) provaram ser mais estáveis para o usuário final.

---

## 7. Próximos Passos
- Geração e distribuição do APK oficial de Produção.
- Monitoramento de logs em ambiente real.
- Estudo de integração com leitores de QR Code para agendamentos.

---

## Apêndice A: Arquivos Modificados
- `ApiService.java`
- `ClienteAuthManager.java`
- `ConfirmSchedulingFragment.java`
- `SelectionFragment.java`
- `SuccessFragment.java`
- `ScreeningFragment.java`
- `SessionManager.java`
- `GenericItemAdapter.java`
- `Agendamento.java`
- `TicketResponse.java`
- `TicketTriageResponse.java`

## Apêndice B: Endpoints Utilizados
- `GET novosga.triage/agendamentos/{servicoId}`
- `POST novosga.triage/distribui_agendamento/{id}`
- `GET api/unidades/{id}/servicos`
- `GET login` (CSRF Extraction)

---
**DATA DE INÍCIO:** 22/07/2026
**DATA DE CONCLUSÃO:** 24/07/2026
**TOTAL DE VERSÕES:** 34
