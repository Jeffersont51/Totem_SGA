# Protocolo do Agent (Gemini) — TOTEM SGA

Este arquivo define o que você (Gemini, rodando como Agent no Android Studio)
deve fazer sempre que o usuário digitar o comando: **"aplica a tarefa"**

## Passo 0 — Aplicar a tarefa (TASK_FOR_GEMINI.md)

Antes de compilar, leia o arquivo `TASK_FOR_GEMINI.md` na raiz do projeto.

Regras importantes:
- Aplique **exatamente** as substituições de código descritas nesse arquivo —
  linha por linha, trecho "antes" trocado pelo trecho "depois". Não invente,
  não melhore, não refatore nada além do que está escrito ali.
- Se algum trecho "antes" não for encontrado exatamente no arquivo indicado
  (por ter sido alterado desde que a tarefa foi escrita), **não adivinhe** —
  pare e registre isso claramente no relatório final (próxima seção),
  explicando qual trecho não bateu.
- Depois de aplicar todas as mudanças da tarefa, continue para o Passo 1
  (build) abaixo.

## Passo 1 — Build

Compile e gere o APK usando seu fluxo normal de agente (a forma que for mais
rápida e natural pra você dentro do Android Studio). Não é necessário rodar
`clean` nem usar o terminal PowerShell manualmente — só faça isso se o build
falhar e você identificar que é necessário para resolver o erro.

Se o build falhar por causa do erro de `ANDROID_PREFS_ROOT` conflitando com
`ANDROID_USER_HOME` (erro do AGP na fase de configuração), tente remover a
variável `ANDROID_PREFS_ROOT` do ambiente antes de tentar de novo — mas só
recorra a isso se o erro realmente aparecer.

## Passo 2 — Relatório (BUILD_STATUS.md)

Ao final de tudo — **sucesso ou falha** — crie ou sobrescreva o arquivo
`BUILD_STATUS.md` na raiz do projeto com o seguinte formato:

```markdown
# Status do Build

- Data/Hora: [data e hora atual]

## Tarefa (TASK_FOR_GEMINI.md)
- [ ] Cada alteração da tarefa: OK (aplicada) / NÃO ENCONTRADA (descreva o motivo)

## Build
- Resultado: SUCESSO ou FALHA

### Se FALHA:
[log de erro completo do Gradle, sem resumir]

### Se SUCESSO:
- Caminho do APK: app\build\outputs\apk\debug\app-debug.apk
- Data/Hora de modificação do APK (LastWriteTime)
```

Regras importantes:
- Sempre **sobrescreva** o `BUILD_STATUS.md` — ele reflete sempre o status da
  última execução, não um histórico acumulado.
- Em caso de falha, cole o log de erro **completo**, sem resumir ou omitir
  partes — isso é usado por outra IA (Claude) para diagnosticar o problema.
- Se alguma alteração da tarefa não pôde ser aplicada, isso deve aparecer
  claramente no relatório mesmo que o build tenha sucesso (com o código
  antigo ainda em vigor).
- Não pule nenhum passo.

## Contexto do projeto

Este é um app Android nativo (Java) de autoatendimento (Totem) integrado ao
NovoSGA, rodando em `http://10.7.0.89`. As instruções de código são escritas
por outra IA (Claude, via Claude Code) no arquivo `TASK_FOR_GEMINI.md`. Seu
papel é aplicar exatamente o que está nesse arquivo, compilar e reportar —
não altere código-fonte por conta própria além do que está descrito na
tarefa.
