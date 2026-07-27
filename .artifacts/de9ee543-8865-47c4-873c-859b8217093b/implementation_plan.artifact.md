# Plano de Implementação - Limpeza das Telas de Teste na Admin

Este plano descreve as etapas para remover os botões de teste temporários da `AdminActivity` e mover a funcionalidade "Debug: Teste Cliente NovoSGA" para a `DiagnosticActivity`.

## Revisão do Usuário Necessária

> [!IMPORTANT]
> A `TestAutocompleteActivity` será desconectada da interface do usuário, mas seus arquivos permanecerão no projeto por enquanto, conforme solicitado. A `TesteClienteActivity` será movida para a tela de Diagnóstico.

## Mudanças Propostas

### Tela de Admin

#### [MODIFY] [activity_admin.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/layout/activity_admin.xml)
- Remover o `Button` com id `btnTestAutocomplete`.
- Remover o `Button` com id `btnTesteClienteDebug`.

#### [MODIFY] [AdminActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdminActivity.java)
- Remover as variáveis de membro `btnTestAutocomplete` e `btnTesteClienteDebug`.
- Remover as chamadas `findViewById` e os registros de `setOnClickListener` para esses botões.

---

### Tela de Diagnóstico

#### [MODIFY] [activity_diagnostic.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/layout/activity_diagnostic.xml)
- Adicionar um novo `Button` com id `btnTesteCliente` e texto "Teste Cliente NovoSGA" dentro do layout "Linha 4: Sistema".
- Garantir que o botão corresponda ao estilo dos outros botões naquela linha.

#### [MODIFY] [DiagnosticActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/DiagnosticActivity.java)
- Adicionar um `setOnClickListener` para `btnTesteCliente` que inicia a `TesteClienteActivity`.

## Plano de Verificação

### Testes Automatizados
- Compilar o projeto para garantir que não haja erros de compilação após a remoção das referências.

### Verificação Manual
1. Abrir a tela de Admin e verificar se os botões "Teste Autocomplete (NovoSGA)" e "Debug: Teste Cliente NovoSGA" sumiram.
2. Clicar em "DIAGNÓSTICO".
3. Verificar se o botão "Teste Cliente NovoSGA" está presente na tela de Diagnóstico.
4. Clicar no botão e verificar se ele abre a `TesteClienteActivity`.
