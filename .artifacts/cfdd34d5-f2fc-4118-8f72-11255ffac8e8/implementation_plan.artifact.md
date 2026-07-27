# Plano de Implementação - Sincronização Periódica e Correção de Transição

Este plano visa implementar uma sincronização periódica de dados (Departamentos/Serviços) e configurações, garantindo que o totem esteja sempre atualizado, mesmo quando a publicidade não estiver ativa ou demorar a entrar. Além disso, corrigiremos o problema relatado onde os botões não aparecem após a saída da publicidade.

## User Review Required

> [!IMPORTANT]
> A sincronização periódica forçará a atualização da lista de serviços a cada **15 minutos** (valor sugerido) caso o totem esteja na tela inicial. Se o usuário estiver interagindo, a atualização será adiada para não interromper o fluxo.

## Propostas de Mudanças

### Core / Utilidades

#### [NEW] [SyncManager.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/util/SyncManager.java)
* Criar uma classe Singleton para gerenciar um timer global de sincronização.
* Método `performSync()` que recarrega as configurações do `SessionManager` e notifica a UI.

### Componente: MainActivity

#### [MODIFY] [MainActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/MainActivity.java)
* Iniciar o `SyncManager` no `onCreate`.
* Implementar uma interface de callback do `SyncManager` para chamar `startFlow()` ou atualizar o fragmento atual quando uma sincronização for solicitada e o totem estiver ocioso.
* Melhorar o método `onInactivityDetected` para garantir que a transição para `AdFragment` seja limpa.

### Componente: Publicidade (AdFragment)

#### [MODIFY] [AdFragment.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/ads/AdFragment.java)
* Otimizar o método `dismissAds()` para garantir que a transição de volta para a `MainActivity` seja atômica.
* Adicionar um log de depuração para rastrear por que os botões podem não carregar (falha de API).

### Componente: Telas de Seleção

#### [MODIFY] [SelectionFragment.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/SelectionFragment.java)
* Adicionar tratamento de erro nas chamadas de API (`onFailure` e `response.isSuccessful() == false`).
* Implementar um mecanismo de "Retry" automático caso a lista venha vazia por erro de rede/token.
* Adicionar um método público `refreshData()` para ser chamado pelo `SyncManager`.

## Plano de Verificação

### Testes Manuais
1.  **Sincronização**: Deixar o totem na tela de seleção por 15 minutos e observar se a lista de serviços é atualizada (alterando um nome no admin, por exemplo).
2.  **Transição**: Entrar na publicidade, esperar 1 minuto, e tocar para sair. Verificar se os botões aparecem instantaneamente.
3.  **Rede Offline**: Simular queda de rede e verificar se o app exibe uma mensagem ou tenta reconectar ao voltar para a tela principal, em vez de ficar sem botões.

### Comandos de Verificação
* `gradle_build` para garantir que as alterações não quebraram o projeto.
