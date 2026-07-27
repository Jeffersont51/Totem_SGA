# Tarefas: Estabilização da Reimpressão e Ajuste de URL (V34)

- `[x]` **Fase 1: Ajuste de Encodamento de Rede**
    - `[x]` Mudar separador `%2C` para `,` em `SessionManager.java`
- `[x]` **Fase 2: Auto-Recuperação de Sessão**
    - `[x]` Implementar detecção de HTML em `ClienteAuthManager.java`
    - `[x]` Refinar para evitar loop infinito e garantir re-login limpo
- `[x]` **Fase 3: Verificação**
    - `[x]` Validar build
    - `[x]` Verificar se a URL gerada contém exatamente `%2C`
    - `[x]` Confirmar listagem de senhas no totem físico
    - `[ ]` Validar build
    - `[ ]` Verificar se a URL gerada contém exatamente `%2C`
    - `[ ]` Confirmar listagem de senhas no totem físico
