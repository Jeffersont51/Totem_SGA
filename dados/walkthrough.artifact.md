# Walkthrough: Estabilização da Reimpressão e Ajuste de URL (V34)

Esta atualização corrige o problema de comunicação no totem físico ao ajustar o encodamento da URL e implementar um sistema de auto-recuperação de sessão mais inteligente e seguro.

## Alterações Realizadas

### 1. Ajuste de Encodamento de Rede (Fim do Duplo Escape)
- **[SessionManager.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/Totem_SGA_homologacao/app/src/main/java/br/com/jefferson/totemsga/util/SessionManager.java)**:
    - ✅ **Correção de URL**: O separador de IDs de serviço foi alterado de `%2C` para `,` no código. Isso permite que o Retrofit realize o encodamento correto para `%2C` na URL final, eliminando o erro de "duplo escape" (`%252C`) que causava a rejeição da requisição pelo servidor físico.

### 2. Auto-Recuperação de Sessão Blindada
- **[ClienteAuthManager.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/Totem_SGA_homologacao/app/src/main/java/br/com/jefferson/totemsga/util/ClienteAuthManager.java)**:
    - ✅ **Detecção de HTML Inteligente**: Se o servidor SGA retornar HTML (página de login) em vez de JSON de dados, o app agora identifica a falha de sessão instantaneamente.
    - ✅ **Recuperação Automática**: Ao detectar o redirecionamento indevido, o sistema limpa os cookies e realiza um novo login automático, repetindo a busca logo em seguida.
    - ✅ **Proteção contra Loops**: Adicionada uma trava de segurança que limita a recuperação automática a uma única tentativa, evitando loops infinitos em caso de credenciais realmente inválidas.

## Resultados da Verificação
- **Build**: Sucesso total. ✅
- **URL Gerada**: `ids=16%2C5%2C18...` (Confirmado no padrão oficial do NovoSGA). ✅

## Como Testar
1. Acesse a tela **REIMPRIMIR SENHA**.
2. Realize uma consulta.
3. ✅ A busca agora deve ser aceita pelo servidor físico sem redirecionar para a tela de login, trazendo a lista de senhas imediatamente.

> [!TIP]
> Esta correção resolve a discrepância entre o emulador (que era mais tolerante) e o totem físico (que exigia o padrão rigoroso de URL).
