# Tarefa para o Gemini

Duas coisas:

1. Reordenar o recibo impresso: a mensagem do serviço (ex: "Sala 07") deve
   sair logo abaixo do nome do serviço (ex: "WANDERSON BATISTA"), antes da
   data/hora — hoje sai depois do nome do cliente, no final.
2. Adicionar um controle "Mensagem do Serviço" na tela de Layout de
   Impressão do Admin (`PrintLayoutActivity`), com opção de mostrar/ocultar
   e tamanho de fonte, igual já existe pra Unidade/Prioridade/Serviço/Nome.

Siga exatamente as instruções abaixo, nesta ordem. Não altere mais nada
além disso.

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\util\SessionManager.java`

### Alteração 1 — novas chaves de configuração

Trecho ANTES:
```java
    private static final String KEY_PRINT_SIZE_SERVICE = "print_size_service";
```

Trecho DEPOIS:
```java
    private static final String KEY_PRINT_SIZE_SERVICE = "print_size_service";
    private static final String KEY_PRINT_SIZE_MENSAGEM = "print_size_mensagem";
    private static final String KEY_PRINT_SHOW_MENSAGEM = "print_show_mensagem";
```

### Alteração 2 — novos getters/setters

Trecho ANTES:
```java
    public int getPrintSizeService() { return pref.getInt(KEY_PRINT_SIZE_SERVICE, 0); }
    public void setPrintSizeService(int size) { editor.putInt(KEY_PRINT_SIZE_SERVICE, size).apply(); }
```

Trecho DEPOIS:
```java
    public int getPrintSizeService() { return pref.getInt(KEY_PRINT_SIZE_SERVICE, 0); }
    public void setPrintSizeService(int size) { editor.putInt(KEY_PRINT_SIZE_SERVICE, size).apply(); }

    public int getPrintSizeMensagem() { return pref.getInt(KEY_PRINT_SIZE_MENSAGEM, 0); }
    public void setPrintSizeMensagem(int size) { editor.putInt(KEY_PRINT_SIZE_MENSAGEM, size).apply(); }

    public boolean isPrintShowMensagem() { return pref.getBoolean(KEY_PRINT_SHOW_MENSAGEM, true); }
    public void setPrintShowMensagem(boolean show) { editor.putBoolean(KEY_PRINT_SHOW_MENSAGEM, show).apply(); }
```

## Arquivo: `app\src\main\res\layout\activity_print_layout.xml`

### Alteração 3 — nova linha de tamanho de fonte

Trecho ANTES:
```xml
                <TextView android:layout_width="120dp" android:layout_height="wrap_content" android:text="Serviço:"/>
                <Spinner android:id="@+id/spinnerSizeService" android:layout_width="match_parent" android:layout_height="wrap_content"/>
            </LinearLayout>
```

Trecho DEPOIS:
```xml
                <TextView android:layout_width="120dp" android:layout_height="wrap_content" android:text="Serviço:"/>
                <Spinner android:id="@+id/spinnerSizeService" android:layout_width="match_parent" android:layout_height="wrap_content"/>
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginTop="4dp">
                <TextView android:layout_width="120dp" android:layout_height="wrap_content" android:text="Mensagem Serviço:"/>
                <Spinner android:id="@+id/spinnerSizeMensagem" android:layout_width="match_parent" android:layout_height="wrap_content"/>
            </LinearLayout>
```

### Alteração 4 — nova checkbox de visibilidade

Trecho ANTES:
```xml
            <CheckBox android:id="@+id/cbShowService" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mostrar Nome do Serviço"/>
            <CheckBox android:id="@+id/cbShowDateTime" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mostrar Data e Hora"/>
```

Trecho DEPOIS:
```xml
            <CheckBox android:id="@+id/cbShowService" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mostrar Nome do Serviço"/>
            <CheckBox android:id="@+id/cbShowMensagem" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mostrar Mensagem do Serviço"/>
            <CheckBox android:id="@+id/cbShowDateTime" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mostrar Data e Hora"/>
```

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\PrintLayoutActivity.java`

### Alteração 5 — novos campos

Trecho ANTES:
```java
    private Spinner spinnerAlign, spinnerSizeUnit, spinnerSizePriority, spinnerSizeTicket, spinnerSizeService, spinnerSizeDateTime, spinnerSizeName, spinnerSizeFooter;
    private android.widget.EditText etFooterText;
    private CheckBox cbUnit, cbPriority, cbService, cbDateTime;
```

Trecho DEPOIS:
```java
    private Spinner spinnerAlign, spinnerSizeUnit, spinnerSizePriority, spinnerSizeTicket, spinnerSizeService, spinnerSizeMensagem, spinnerSizeDateTime, spinnerSizeName, spinnerSizeFooter;
    private android.widget.EditText etFooterText;
    private CheckBox cbUnit, cbPriority, cbService, cbMensagem, cbDateTime;
```

### Alteração 6 — vincular as novas views

Trecho ANTES:
```java
        spinnerSizeService = findViewById(R.id.spinnerSizeService);
        spinnerSizeDateTime = findViewById(R.id.spinnerSizeDateTime);
        spinnerSizeName = findViewById(R.id.spinnerSizeName);
        spinnerSizeFooter = findViewById(R.id.spinnerSizeFooter);
        etFooterText = findViewById(R.id.etFooterText);
        cbUnit = findViewById(R.id.cbShowUnit);
        cbPriority = findViewById(R.id.cbShowPriority);
        cbService = findViewById(R.id.cbShowService);
        cbDateTime = findViewById(R.id.cbShowDateTime);
```

Trecho DEPOIS:
```java
        spinnerSizeService = findViewById(R.id.spinnerSizeService);
        spinnerSizeMensagem = findViewById(R.id.spinnerSizeMensagem);
        spinnerSizeDateTime = findViewById(R.id.spinnerSizeDateTime);
        spinnerSizeName = findViewById(R.id.spinnerSizeName);
        spinnerSizeFooter = findViewById(R.id.spinnerSizeFooter);
        etFooterText = findViewById(R.id.etFooterText);
        cbUnit = findViewById(R.id.cbShowUnit);
        cbPriority = findViewById(R.id.cbShowPriority);
        cbService = findViewById(R.id.cbShowService);
        cbMensagem = findViewById(R.id.cbShowMensagem);
        cbDateTime = findViewById(R.id.cbShowDateTime);
```

### Alteração 7 — incluir no adapter de tamanhos

Trecho ANTES:
```java
        spinnerSizeUnit.setAdapter(sizeAdapter);
        spinnerSizePriority.setAdapter(sizeAdapter);
        spinnerSizeTicket.setAdapter(sizeAdapter);
        spinnerSizeService.setAdapter(sizeAdapter);
        spinnerSizeDateTime.setAdapter(sizeAdapter);
        spinnerSizeName.setAdapter(sizeAdapter);
        spinnerSizeFooter.setAdapter(sizeAdapter);
```

Trecho DEPOIS:
```java
        spinnerSizeUnit.setAdapter(sizeAdapter);
        spinnerSizePriority.setAdapter(sizeAdapter);
        spinnerSizeTicket.setAdapter(sizeAdapter);
        spinnerSizeService.setAdapter(sizeAdapter);
        spinnerSizeMensagem.setAdapter(sizeAdapter);
        spinnerSizeDateTime.setAdapter(sizeAdapter);
        spinnerSizeName.setAdapter(sizeAdapter);
        spinnerSizeFooter.setAdapter(sizeAdapter);
```

### Alteração 8 — carregar configurações salvas

Trecho ANTES:
```java
        spinnerSizeService.setSelection(sessionManager.getPrintSizeService());
        spinnerSizeDateTime.setSelection(sessionManager.getPrintSizeDateTime());
        spinnerSizeName.setSelection(sessionManager.getPrintSizeName());
        spinnerSizeFooter.setSelection(sessionManager.getPrintFooterSize());
        etFooterText.setText(sessionManager.getPrintFooterText());
        
        cbUnit.setChecked(sessionManager.isPrintShowUnit());
        cbPriority.setChecked(sessionManager.isPrintShowPriority());
        cbService.setChecked(sessionManager.isPrintShowService());
        cbDateTime.setChecked(sessionManager.isPrintShowDateTime());
```

Trecho DEPOIS:
```java
        spinnerSizeService.setSelection(sessionManager.getPrintSizeService());
        spinnerSizeMensagem.setSelection(sessionManager.getPrintSizeMensagem());
        spinnerSizeDateTime.setSelection(sessionManager.getPrintSizeDateTime());
        spinnerSizeName.setSelection(sessionManager.getPrintSizeName());
        spinnerSizeFooter.setSelection(sessionManager.getPrintFooterSize());
        etFooterText.setText(sessionManager.getPrintFooterText());
        
        cbUnit.setChecked(sessionManager.isPrintShowUnit());
        cbPriority.setChecked(sessionManager.isPrintShowPriority());
        cbService.setChecked(sessionManager.isPrintShowService());
        cbMensagem.setChecked(sessionManager.isPrintShowMensagem());
        cbDateTime.setChecked(sessionManager.isPrintShowDateTime());
```

### Alteração 9 — salvar configurações

Trecho ANTES:
```java
        sessionManager.setPrintSizeService(spinnerSizeService.getSelectedItemPosition());
        sessionManager.setPrintSizeDateTime(spinnerSizeDateTime.getSelectedItemPosition());
        sessionManager.setPrintSizeName(spinnerSizeName.getSelectedItemPosition());
        sessionManager.setPrintFooterSize(spinnerSizeFooter.getSelectedItemPosition());
        sessionManager.setPrintFooterText(etFooterText.getText().toString());
        
        sessionManager.setPrintShowUnit(cbUnit.isChecked());
        sessionManager.setPrintShowPriority(cbPriority.isChecked());
        sessionManager.setPrintShowService(cbService.isChecked());
        sessionManager.setPrintShowDateTime(cbDateTime.isChecked());
```

Trecho DEPOIS:
```java
        sessionManager.setPrintSizeService(spinnerSizeService.getSelectedItemPosition());
        sessionManager.setPrintSizeMensagem(spinnerSizeMensagem.getSelectedItemPosition());
        sessionManager.setPrintSizeDateTime(spinnerSizeDateTime.getSelectedItemPosition());
        sessionManager.setPrintSizeName(spinnerSizeName.getSelectedItemPosition());
        sessionManager.setPrintFooterSize(spinnerSizeFooter.getSelectedItemPosition());
        sessionManager.setPrintFooterText(etFooterText.getText().toString());
        
        sessionManager.setPrintShowUnit(cbUnit.isChecked());
        sessionManager.setPrintShowPriority(cbPriority.isChecked());
        sessionManager.setPrintShowService(cbService.isChecked());
        sessionManager.setPrintShowMensagem(cbMensagem.isChecked());
        sessionManager.setPrintShowDateTime(cbDateTime.isChecked());
```

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\SuccessFragment.java`

### Alteração 10 — reordenar e usar as novas configurações

Trecho ANTES:
```java
        // Nome do Serviço
        if (sessionManager.isPrintShowService() && !service.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\n" + service.toUpperCase() + "\n");
        }

        // Data e Hora
        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH'h'mm", java.util.Locale.getDefault());
            java.util.Date now = new java.util.Date();
            
            helper.printText("\n" + sdfDate.format(now) + "\n");
            helper.printText("Hora de chegada " + sdfTime.format(now) + "\n");
            helper.printText("( Horário local )\n");
        }

        boolean hasNome = getArguments() != null && getArguments().getBoolean("has_nome", false);
        String clienteNome = getArguments() != null ? getArguments().getString("cliente_nome", "") : "";
        if (hasNome && !clienteNome.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeName());
            helper.printText("\nNome: " + clienteNome + "\n");
        }

        String servicoMensagem = getArguments() != null ? getArguments().getString("servico_mensagem", "") : "";
        if (!servicoMensagem.isEmpty()) {
            helper.setFontSize(0);
            helper.printText("\n" + servicoMensagem + "\n");
        }

        boolean isFacial = getArguments() != null && getArguments().getBoolean("is_facial", false);
```

Trecho DEPOIS:
```java
        // Nome do Serviço
        if (sessionManager.isPrintShowService() && !service.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\n" + service.toUpperCase() + "\n");
        }

        // Mensagem do Serviço (ex: "Sala 07")
        String servicoMensagem = getArguments() != null ? getArguments().getString("servico_mensagem", "") : "";
        if (sessionManager.isPrintShowMensagem() && !servicoMensagem.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeMensagem());
            helper.printText("\n" + servicoMensagem + "\n");
        }

        // Data e Hora
        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH'h'mm", java.util.Locale.getDefault());
            java.util.Date now = new java.util.Date();
            
            helper.printText("\n" + sdfDate.format(now) + "\n");
            helper.printText("Hora de chegada " + sdfTime.format(now) + "\n");
            helper.printText("( Horário local )\n");
        }

        boolean hasNome = getArguments() != null && getArguments().getBoolean("has_nome", false);
        String clienteNome = getArguments() != null ? getArguments().getString("cliente_nome", "") : "";
        if (hasNome && !clienteNome.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeName());
            helper.printText("\nNome: " + clienteNome + "\n");
        }

        boolean isFacial = getArguments() != null && getArguments().getBoolean("is_facial", false);
```

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\ReprintFragment.java`

### Alteração 11 — reordenar e usar as novas configurações

Trecho ANTES:
```java
        if (sessionManager.isPrintShowService() && s.servico != null) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\n" + s.servico.nome.toUpperCase() + "\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            Date dataOriginal = parseDataChegada(s.dataChegada);
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH'h'mm", Locale.getDefault());
            helper.printText("\nREIMPRESSÃO\n");
            helper.printText(sdfDate.format(dataOriginal) + "\n");
            helper.printText("Hora de chegada " + sdfTime.format(dataOriginal) + "\n");
            helper.printText("( Horário local )\n");
        }

        if (s.cliente != null && s.cliente.nome != null) {
            helper.setFontSize(sessionManager.getPrintSizeName());
            helper.printText("\nNome: " + s.cliente.nome + "\n");
        }

        ServicoUnidade su = (s.servico != null) ? servicesMap.get(s.servico.id) : null;

        if (su != null && su.mensagem != null && !su.mensagem.isEmpty()) {
            helper.setFontSize(0);
            helper.printText("\n" + su.mensagem + "\n");
        }

        if (resolveFeatureImpressao(su, FeatureParser.FACIAL)) {
```

Trecho DEPOIS:
```java
        if (sessionManager.isPrintShowService() && s.servico != null) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\n" + s.servico.nome.toUpperCase() + "\n");
        }

        ServicoUnidade su = (s.servico != null) ? servicesMap.get(s.servico.id) : null;

        if (su != null && sessionManager.isPrintShowMensagem() && su.mensagem != null && !su.mensagem.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeMensagem());
            helper.printText("\n" + su.mensagem + "\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            Date dataOriginal = parseDataChegada(s.dataChegada);
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH'h'mm", Locale.getDefault());
            helper.printText("\nREIMPRESSÃO\n");
            helper.printText(sdfDate.format(dataOriginal) + "\n");
            helper.printText("Hora de chegada " + sdfTime.format(dataOriginal) + "\n");
            helper.printText("( Horário local )\n");
        }

        if (s.cliente != null && s.cliente.nome != null) {
            helper.setFontSize(sessionManager.getPrintSizeName());
            helper.printText("\nNome: " + s.cliente.nome + "\n");
        }

        if (resolveFeatureImpressao(su, FeatureParser.FACIAL)) {
```

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\PrintLayoutActivity.java`

### Alteração 12 — mesma reordenação/config no botão "IMPRIMIR TESTE" (método `testPrint()`)

Trecho ANTES:
```java
        if (sessionManager.isPrintShowService()) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\nATENDIMENTO T.I\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
```

Trecho DEPOIS:
```java
        if (sessionManager.isPrintShowService()) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\nATENDIMENTO T.I\n");
        }

        if (sessionManager.isPrintShowMensagem()) {
            helper.setFontSize(sessionManager.getPrintSizeMensagem());
            helper.printText("\nSala 07\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
```

## Depois de aplicar

Continue para o Passo 1 (Build) do `AGENT_PROTOCOL.md`.
