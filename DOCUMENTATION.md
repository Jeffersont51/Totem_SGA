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
*Documentação gerada em 30 de Julho de 2026.*
