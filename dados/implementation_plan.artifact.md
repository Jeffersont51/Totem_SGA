# Plano de Implementação: Estabilização da Reimpressão e Ajuste de URL (V34)

Este plano detalha a correção técnica para garantir que a URL de consulta de senhas siga o padrão oficial do NovoSGA (usando `%2C` sem duplo escape) e torne a gestão de sessão mais resiliente no totem físico.

## User Review Required

> [!IMPORTANT]
> Alteraremos o separador de IDs de serviço de `%2C` para `,` no código Java. Isso permitirá que o Retrofit realize o encodamento correto para `%2C` na URL final, evitando o erro de duplo escape (`%252C`) que causa falhas no servidor físico.

## Proposed Changes

### [Utils]

#### [MODIFY] [SessionManager.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/Totem_SGA_homologacao/app/src/main/java/br/com/jefferson/totemsga/util/SessionManager.java)
- Alterar o método `getIdsServicosAtivos`:
    - Substituir `ids.append("%2C")` por `ids.append(",")`.
    - Isso garante que a string bruta enviada ao Retrofit seja amigável (ex: "1,2,3").

### [API / Auth]

#### [MODIFY] [ClienteAuthManager.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/Totem_SGA_homologacao/app/src/main/java/br/com/jefferson/totemsga/util/ClienteAuthManager.java)
- **Refatorar `buscarSenhasMonitor`**:
    - Manter a leitura RAW da resposta para segurança.
    - Se o corpo da resposta contiver `<!DOCTYPE html>` ou `<html`, o app agora:
        1. Marcará `isLoggedIn = false`.
        2. Limpará a sessão no `RetrofitClient`.
        3. Chamará `realizarLogin()`.
        4. Repetirá a busca automaticamente (máximo 1 tentativa) para garantir fluidez.

### [UI / Fragments]

#### [MODIFY] [ReprintFragment.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/Totem_SGA_homologacao/app/src/main/java/br/com/jefferson/totemsga/ReprintFragment.java)
- Nenhuma alteração visual necessária. A correção será 100% lógica no motor de dados.

## Verification Plan

### Manual Verification (No Totem Físico)
1. Abrir a tela de Reimpressão.
2. Realizar uma consulta.
3. ✅ Verificar no Logcat se a URL final contém apenas `%2C` (Ex: `ids=16%2C5%2C18...`).
4. ✅ Confirmar se os resultados aparecem na lista sem erro de JSON.
5. ✅ Validar se a Triagem e o Agendamento continuam funcionando (confirmando que a mudança do separador foi aceita globalmente).
