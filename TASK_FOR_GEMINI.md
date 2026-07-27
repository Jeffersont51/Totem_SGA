# Tarefa para o Gemini

Remover os botões fixos "REIMPRIMIR" (Reimpressão) e "CONFIRMAR PRESENÇA"
(Agendamento) que ficam abaixo da lista — agora redundantes, já que cada
card tem seu próprio botão. A forma mais segura de fazer isso é impedir que
esses botões fiquem visíveis (eles já começam ocultos no XML), sem remover
o resto da lógica que ainda depende deles internamente (contadores, estado
selecionado, etc.) — assim o risco de quebrar algo é mínimo.

Siga exatamente as instruções abaixo. Não altere mais nada além disso.

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\ReprintFragment.java`

### Alteração 1

Trecho ANTES:
```java
        if (filteredSenhas.size() == 1) {
            selectedSenha = filteredSenhas.get(0);
            btnConfirmReprint.setVisibility(View.VISIBLE);
            tvResultHeader.setText("Senha emitida hoje encontrada:");
```

Trecho DEPOIS:
```java
        if (filteredSenhas.size() == 1) {
            selectedSenha = filteredSenhas.get(0);
            btnConfirmReprint.setVisibility(View.GONE);
            tvResultHeader.setText("Senha emitida hoje encontrada:");
```

### Alteração 2

Trecho ANTES:
```java
        GenericItemAdapter adapter = new GenericItemAdapter(filteredSenhas, "#F47B20", null, null, GenericItemAdapter.STYLE_REPRINT, item -> {
            selectedSenha = (SenhaFila) item;
            btnConfirmReprint.setVisibility(View.VISIBLE);
            ((GenericItemAdapter)rvResults.getAdapter()).setSelectedId(selectedSenha.id);
            resetInactivityTimer();
```

Trecho DEPOIS:
```java
        GenericItemAdapter adapter = new GenericItemAdapter(filteredSenhas, "#F47B20", null, null, GenericItemAdapter.STYLE_REPRINT, item -> {
            selectedSenha = (SenhaFila) item;
            btnConfirmReprint.setVisibility(View.GONE);
            ((GenericItemAdapter)rvResults.getAdapter()).setSelectedId(selectedSenha.id);
            resetInactivityTimer();
```

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\ConfirmSchedulingFragment.java`

### Alteração 3

Trecho ANTES:
```java
        if (uniqueList.size() == 1) {
            selectedAgendamento = uniqueList.get(0);
            tvResultHeader.setText("Agendamento de hoje encontrado:");
            btnConfirm.setVisibility(View.VISIBLE);
        } else {
```

Trecho DEPOIS:
```java
        if (uniqueList.size() == 1) {
            selectedAgendamento = uniqueList.get(0);
            tvResultHeader.setText("Agendamento de hoje encontrado:");
            btnConfirm.setVisibility(View.GONE);
        } else {
```

### Alteração 4

Trecho ANTES:
```java
            clearError();
        }
        btnConfirm.setVisibility(View.VISIBLE);
    }
```

Trecho DEPOIS:
```java
            clearError();
        }
        btnConfirm.setVisibility(View.GONE);
    }
```

## Depois de aplicar

Continue para o Passo 1 (Build) do `AGENT_PROTOCOL.md`.
