# Documentação Técnica - Totem SGA

O **Totem SGA** é uma aplicação Android desenvolvida para terminais de autoatendimento (Kiosks). Ele permite a emissão de senhas de atendimento, confirmação de agendamentos e exibição de mídias publicitárias, com integração nativa com impressoras térmicas.

---

## 🚀 Funcionalidades Principais

1.  **Emissão de Senhas**: Seleção de departamentos e serviços para geração de senhas de fila.
2.  **Confirmação de Agendamento**: Integração com sistema de agendamento prévio via QR Code ou dados do cliente.
3.  **Triagem (Screening)**: Fluxo de perguntas configuráveis antes da emissão da senha.
4.  **Reimpressão**: Busca e reimpressão de senhas emitidas recentemente.
5.  **Modo Kiosk**: Bloqueio do dispositivo para uso exclusivo do aplicativo (Kiosk Mode).
6.  **Publicidade (Ads)**: Exibição de vídeos (YouTube/Local) e imagens durante períodos de inatividade.
7.  **Configuração Dinâmica**: Cores, logos e comportamentos são configurados remotamente via API.

---

## 🛠️ Stack Técnica

-   **Linguagem**: Java (Nível de Linguagem 11).
-   **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + OkHttp para comunicação REST.
-   **JSON Parsing**: GSON.
-   **Carregamento de Imagem**: [Glide](https://github.com/bumptech/glide).
-   **Interface**: [FlexboxLayout](https://github.com/google/flexbox-layout) para botões dinâmicos e adaptáveis.
-   **Vídeo**: ExoPlayer e YouTube Player Open Source.
-   **Impressão**: Biblioteca IT4R (TecToy/Sunmi) para comandos ESC/POS.

---

## 🏗️ Arquitetura e Fluxo

O aplicativo utiliza uma arquitetura baseada em uma **Activity Única (`MainActivity`)** com navegação via **Fragments**.

### Fluxo de Navegação (Nível Kiosk)
1.  `MainActivity` (Gerenciador do fluxo e timer de inatividade).
2.  `SelectionFragment` (Seleção de Departamento/Serviço).
3.  `ScreeningFragment` (Opcional - Perguntas de triagem).
4.  `ConfirmSchedulingFragment` (Opcional - Busca de agendamentos).
5.  `SuccessFragment` (Exibição da senha gerada e acionamento da impressão).

### Modo Kiosk (`BaseActivity` & `BaseKioskFragment`)
-   O app monitora a inatividade do usuário. Se não houver interação por um tempo determinado (`SessionManager.getTimeout()`), o fluxo retorna à tela inicial.
-   Implementa `onWindowFocusChanged` para impedir a saída do app (bloqueio da barra de status).

---

## ⚙️ Configuração Remota e Feature Flags

Uma característica central do projeto é o uso do campo `descricao` nos modelos de `Departamento` e `ServicoUnidade` para habilitar funcionalidades específicas através do `FeatureParser`.

**Exemplo de flags suportadas:**
-   `VISIVEL`: Define se o item deve aparecer no totem.
-   `TRIAGEM`: Habilita o fluxo de perguntas antes da senha.
-   `FACIAL`: Indica que o serviço requer reconhecimento facial.
-   `NOME`: Solicita o nome do cliente.
-   `PRIORIDADE`: Habilita a escolha entre atendimento normal ou prioritário.

---

## 🖨️ Integração com Impressora

A classe `SunmiPrinterHelper` centraliza a comunicação com a impressora térmica.
-   **Protocolo**: ESC/POS.
-   **Hardware Alvo**: Sunmi K2 (via IT4R TecToy).
-   **Customização**: O layout da impressão (tamanhos de fonte, alinhamento, rodapé) é configurado na tela `PrintLayoutActivity` e salvo no `SessionManager`.

---

## 🔐 Administração e Segurança

-   **AdminActivity**: Acesso protegido por senha para configurações de rede, URL da API e Unidade.
-   **BootReceiver**: Inicia o aplicativo automaticamente quando o Android liga.
-   **AdminReceiver**: Gerencia permissões de "Device Admin" para maior controle do terminal.
-   **DiagnosticActivity**: Ferramenta interna para testar conexão com API e status da impressora.

---

## 📁 Estrutura de Pastas Úteis

-   `br.com.jefferson.totemsga.api`: Definições da API REST.
-   `br.com.jefferson.totemsga.adapter`: Adaptadores de listas (RecyclerView).
-   `br.com.jefferson.totemsga.model`: Classes de dados (POJOs).
-   `br.com.jefferson.totemsga.util`: Classes auxiliares (Logger, Session, Printer).
-   `res/layout`: Arquivos de UI (XML).

---

## 🔁 Autenticação e Recuperação Automática

O app usa duas autenticações com o NovoSGA, e as duas se recuperam sozinhas:

-   **API OAuth (`/api/*`)**: quando o servidor responde 401, o `RetrofitClient` tenta renovar com o `refresh_token`. Se a renovação for recusada, refaz o login completo com o usuário e a senha salvos na configuração. O endpoint `/api/token` nunca recebe o header `Authorization`.
-   **Sessão por cookie (`/novosga.triage/*`, `/novosga.monitor/*`)**: `ClienteAuthManager.executeWithSession()` detecta sessão expirada (redirect, 401/403, `sessionStatus = expired` ou HTML da tela de login no lugar do JSON), refaz o login e repete a chamada uma vez.
-   **Tela de erro**: a tela de seleção tenta carregar de novo sozinha a cada 30 segundos enquanto o erro estiver visível.

---

## 📦 Histórico de Versões

### v1.1.6 (07/10/2026)

-   **Papel preso na impressora**: o estado `ERRO_GUILHOTINA` passa a bloquear a emissão, como sem papel e tampa aberta. Além disso, 3,5 segundos depois de mandar imprimir o app confere a impressora de novo e, se ela estiver em qualquer estado de erro, mostra aviso em vermelho na tela da senha (`SunmiPrinterHelper.getPostPrintProblem()`) e registra o nome do estado no Diagnóstico. **Não confirmado** qual estado a impressora informa quando o papel engasga; a biblioteca IT4R não tem um estado específico de "papel preso".

### v1.1.5 (07/10/2026)

-   **Atualizador não guarda mais usuário e senha de rede** (decisão do Jefferson): o técnico de TI digita o próprio acesso a cada atualização. Ao abrir o Admin, o app apaga as credenciais que as versões 1.1.3/1.1.4 tinham salvado. Só o caminho da pasta fica gravado.
-   Testado no totem em 07/10/2026 e aprovado: bloqueio de emissão sem papel/tampa aberta, toque duplo, agendamento, reimpressão e atualização pela pasta de rede. Pendente: confirmar a correção do erro 401 deixando o totem ligado de um dia para o outro.

### v1.1.3 e v1.1.4 (07/10/2026)

-   **Atualização pela pasta de rede**: botão "Atualizar aplicativo" no Admin. Lê uma pasta compartilhada do Windows (padrão `\\192.168.1.227\Programas TI\TOTEM_SGA`), escolhe o APK de maior versão **pelo nome do arquivo** (`TOTEM_SGA_v1.2.3.apk`), baixa, confere (mesmo pacote, mesma assinatura, versão não menor) e abre o instalador do Android. Usuário e senha de rede são digitados no próprio totem (a partir da v1.1.5 não ficam salvos). Código em `util/AppUpdater.java` (biblioteca `smbj`).
-   A v1.1.4 é idêntica à v1.1.3, só com o número de versão maior, publicada na pasta de rede para testar a atualização.
-   **Para publicar uma versão nova**: copiar o APK para a pasta de rede com a versão no nome. O `versionName` do `build.gradle.kts` e o nome do arquivo precisam ser iguais.

### v1.1.2 (07/10/2026)

-   **Impressora sem papel ou com a tampa aberta bloqueia a emissão** (decisão do Jefferson): com a impressão ligada, o app não gera senha nem confirma agendamento nesses dois estados, e mostra "Impressora indisponível, avise um atendente". Estado indefinido da impressora não bloqueia. Verificação em `BaseKioskFragment.printerBlockMessage()`.

### v1.1.1 (07/10/2026)

-   **Erro 404 ao carregar serviços**: salvar o Admin com a lista de unidades vazia (servidor fora ou 401) gravava a unidade como -1. Agora a unidade já salva é mantida, e a tela de serviços avisa "Unidade não configurada" em vez de mostrar 404.

### v1.1.0 (07/10/2026)

Ponto de partida anterior marcado no Git como `ponto-de-partida-2026-10-07` (APK guardado em `BACKUP_V4_PONTO_DE_PARTIDA_2026-10-07/`).

-   **Erro 401 ao carregar departamentos**: o app agora refaz o login sozinho quando a renovação do token é recusada.
-   **Senha duplicada**: toque duplo em um serviço, em "Gerar senha" ou em "Confirmar presença" não emite mais duas senhas.
-   **Impressão Sunmi**: imprime direto, sem depender de consulta ao servidor. Sem papel ou tampa aberta aparece aviso na tela da senha; no modo AUTO não abre mais o diálogo de impressão do Android nesses casos.
-   **Agendamento**: falha de consulta não é mais mostrada como "nenhum agendamento"; sessão expirada é recuperada automaticamente (também no preenchimento automático do nome).
-   **Fechamentos inesperados**: respostas de rede que chegam depois de o cliente sair da tela são descartadas (`runOnUi`, `safeToast`).
-   **Reimpressão**: removido o texto técnico "DADOS RECEBIDOS" da tela do cliente.
-   **Ping de 30 min**: registra falha quando o servidor recusa (antes registrava sucesso em qualquer resposta).
-   **Rede**: um único cliente HTTP reaproveitado, em vez de um novo a cada consulta.
-   **Segurança**: log de rede não grava mais senha e tokens; backup do app desligado (`allowBackup=false`); aviso no Admin quando a senha ainda é a padrão; versão exibida no botão de Diagnóstico.

**Pendências conhecidas**: a senha do NovoSGA continua salva sem criptografia nas preferências do app; as telas de teste continuam no APK de desenvolvimento.

---
*Documentação atualizada em 7 de Outubro de 2026.*
