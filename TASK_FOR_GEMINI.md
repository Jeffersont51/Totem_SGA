# Tarefa para o Gemini

Duas coisas pendentes no arquivo `app\src\main\java\br\com\jefferson\totemsga\ReprintFragment.java`:

1. Padronizar o `applyLayout()` (mesmo padrão de Triagem/Agendamento) — tarefa
   anterior que ainda não foi aplicada.
2. Corrigir o recibo impresso da Reimpressão, que hoje sai incompleto
   comparado ao recibo da Triagem: falta a data/hora formatada corretamente
   (hoje sai o texto bruto tipo "2026-07-27T08:47:28" em vez de
   "27/07/2026" + "Hora de chegada 08h47"), falta a mensagem própria do
   serviço (ex: "Sala 07"), e falta o aviso de biometria facial quando o
   serviço tem a flag FACIAL habilitada.

Siga exatamente as instruções abaixo, nesta ordem. Não altere mais nada
além disso.

## Arquivo: `app\src\main\java\br\com\jefferson\totemsga\ReprintFragment.java`

### Alteração 1 — imports

Trecho ANTES:
```java
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.jefferson.totemsga.adapter.GenericItemAdapter;
import br.com.jefferson.totemsga.ads.AdManager;
import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.MonitorResponse;
import br.com.jefferson.totemsga.model.SenhaFila;
import br.com.jefferson.totemsga.util.ClienteAuthManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;
import br.com.jefferson.totemsga.util.ValidationUtils;
```

Trecho DEPOIS:
```java
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import br.com.jefferson.totemsga.adapter.GenericItemAdapter;
import br.com.jefferson.totemsga.ads.AdManager;
import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.MonitorResponse;
import br.com.jefferson.totemsga.model.SenhaFila;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.util.ClienteAuthManager;
import br.com.jefferson.totemsga.util.FeatureParser;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;
import br.com.jefferson.totemsga.util.ValidationUtils;
```

### Alteração 2 — novo campo

Trecho ANTES:
```java
    private List<SenhaFila> filteredSenhas = new ArrayList<>();
    private SenhaFila selectedSenha;
    private boolean isApplyingMask = false;
```

Trecho DEPOIS:
```java
    private List<SenhaFila> filteredSenhas = new ArrayList<>();
    private SenhaFila selectedSenha;
    private boolean isApplyingMask = false;
    private final Map<Integer, ServicoUnidade> servicesMap = new HashMap<>();
```

### Alteração 3 — `consultarSenhas()` passa a montar o `servicesMap`

Trecho ANTES:
```java
        ClienteAuthManager auth = ClienteAuthManager.getInstance(sessionManager);
        new Thread(() -> {
            try {
                auth.ensureLoggedIn();
                // Busca em todos os serviços ativos via módulo MONITOR
                String ids = sessionManager.getIdsServicosAtivos();
                auth.buscarSenhasMonitor(ids, new ClienteAuthManager.MonitorCallback() {
```

Trecho DEPOIS:
```java
        ClienteAuthManager auth = ClienteAuthManager.getInstance(sessionManager);
        new Thread(() -> {
            try {
                auth.ensureLoggedIn();

                // Busca a lista completa de serviços (para IDs ativos + dados
                // de mensagem/feature-flags usados na impressão do recibo)
                String ids = "";
                try {
                    ApiService apiServicos = RetrofitClient.getInstance(sessionManager);
                    retrofit2.Response<List<ServicoUnidade>> svcResponse = apiServicos.getServicos(sessionManager.getUnidadeId()).execute();
                    if (svcResponse.isSuccessful() && svcResponse.body() != null) {
                        StringBuilder idsBuilder = new StringBuilder();
                        boolean first = true;
                        servicesMap.clear();
                        for (ServicoUnidade su : svcResponse.body()) {
                            if (su.servico != null) {
                                servicesMap.put(su.servico.id, su);
                                if (su.ativo && su.servico.ativo) {
                                    if (!first) idsBuilder.append(",");
                                    idsBuilder.append(su.servico.id);
                                    first = false;
                                }
                            }
                        }
                        ids = idsBuilder.toString();
                    }
                } catch (Exception e) {
                    Log.e("DEBUG_SGA", "Erro ao carregar lista de serviços para reimpressão", e);
                }

                auth.buscarSenhasMonitor(ids, new ClienteAuthManager.MonitorCallback() {
```

### Alteração 4 — `printSunmi()` com data formatada, mensagem do serviço e aviso facial

Trecho ANTES:
```java
    private void printSunmi(SenhaFila s) {
        SunmiPrinterHelper helper = SunmiPrinterHelper.getInstance();
        helper.printerInit();
        helper.setAlignment(sessionManager.getPrintAlign());

        if (sessionManager.isPrintShowUnit()) {
            helper.setFontSize(sessionManager.getPrintSizeUnit());
            helper.printText(sessionManager.getUnidadeNome() + "\n\n");
        }

        if (sessionManager.isPrintShowPriority() && s.prioridade != null) {
            helper.setFontSize(sessionManager.getPrintSizePriority());
            helper.printText(s.prioridade.nome + "\n");
        }

        helper.setFontSize(sessionManager.getPrintSizeTicket());
        helper.setBold(true);
        helper.printText("\n" + (s.senha != null ? s.senha.format : "") + "\n");
        helper.setBold(false);

        if (sessionManager.isPrintShowService() && s.servico != null) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\n" + s.servico.nome.toUpperCase() + "\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            helper.printText("\nREIMPRESSÃO\n" + s.dataChegada + "\n");
        }

        if (s.cliente != null && s.cliente.nome != null) {
            helper.setFontSize(sessionManager.getPrintSizeName());
            helper.printText("\nNome: " + s.cliente.nome + "\n");
        }

        helper.printText("\n==============================\n");
        helper.lineWrap(4);
        helper.cutPaper();
    }
```

Trecho DEPOIS:
```java
    private void printSunmi(SenhaFila s) {
        SunmiPrinterHelper helper = SunmiPrinterHelper.getInstance();
        helper.printerInit();
        helper.setAlignment(sessionManager.getPrintAlign());

        if (sessionManager.isPrintShowUnit()) {
            helper.setFontSize(sessionManager.getPrintSizeUnit());
            helper.printText(sessionManager.getUnidadeNome() + "\n\n");
        }

        if (sessionManager.isPrintShowPriority() && s.prioridade != null) {
            helper.setFontSize(sessionManager.getPrintSizePriority());
            helper.printText(s.prioridade.nome + "\n");
        }

        helper.setFontSize(sessionManager.getPrintSizeTicket());
        helper.setBold(true);
        helper.printText("\n" + (s.senha != null ? s.senha.format : "") + "\n");
        helper.setBold(false);

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

        if (su != null && su.servico != null && su.servico.mensagem != null && !su.servico.mensagem.isEmpty()) {
            helper.setFontSize(0);
            helper.printText("\n" + su.servico.mensagem + "\n");
        }

        if (resolveFeatureImpressao(su, FeatureParser.FACIAL)) {
            String footerText = sessionManager.getPrintFooterText();
            if (footerText != null && !footerText.isEmpty()) {
                helper.setFontSize(sessionManager.getPrintFooterSize());
                helper.printText("\n" + footerText + "\n");
            }
        }

        helper.setFontSize(0);
        helper.printText("\n==============================\n");
        helper.lineWrap(4);
        helper.cutPaper();
    }

    private boolean resolveFeatureImpressao(ServicoUnidade su, String feature) {
        if (su == null || su.servico == null) return false;

        Map<String, Boolean> sFeatures = FeatureParser.parse(su.servico.descricao);
        if (sFeatures.containsKey(feature)) {
            Boolean val = sFeatures.get(feature);
            return val != null && val;
        }

        if (su.departamento != null) {
            Map<String, Boolean> dFeatures = FeatureParser.parse(su.departamento.descricao);
            if (dFeatures.containsKey(feature)) {
                Boolean val = dFeatures.get(feature);
                return val != null && val;
            }
        }
        return false;
    }

    private Date parseDataChegada(String raw) {
        if (raw != null) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                return isoFormat.parse(raw);
            } catch (Exception e) {}
        }
        return new Date();
    }
```

### Alteração 5 — padronizar `applyLayout()` (tarefa anterior, ainda pendente)

Trecho ANTES:
```java
    private void applyLayout(View view) {
        try {
            String bgColor = sessionManager.getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty()) view.setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            
            ImageView ivLogo = view.findViewById(R.id.ivLogoReprint);
            if (ivLogo != null) {
                String url = sessionManager.getLogoUrl();
                if (url != null && !url.isEmpty()) {
                    float density = getResources().getDisplayMetrics().density;
                    android.view.ViewGroup.LayoutParams lp = ivLogo.getLayoutParams();
                    lp.width = (int) (sessionManager.getLogoWidth() * density);
                    lp.height = (int) (sessionManager.getLogoHeight() * density);
                    ivLogo.setLayoutParams(lp);
                    com.bumptech.glide.Glide.with(requireContext()).load(url).into(ivLogo);
                } else ivLogo.setVisibility(View.GONE);
            }

            int textColor = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            tvTitle.setTextColor(textColor);
            ImageView ivHeader = view.findViewById(R.id.ivHeaderIcon);
            if (ivHeader != null) ivHeader.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));

            setupBackButton(btnBack, view.findViewById(R.id.svReprint));
        } catch (Exception e) {}
    }
```

Trecho DEPOIS:
```java
    private void applyLayout(View view) {
        try {
            String bgColor = sessionManager.getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty()) {
                view.setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            }
        } catch (Exception e) {
            view.setBackgroundColor(android.graphics.Color.parseColor("#FFF5E1"));
        }

        try {
            ImageView ivLogo = view.findViewById(R.id.ivLogoReprint);
            if (ivLogo != null) {
                String url = sessionManager.getLogoUrl();
                if (url != null && !url.isEmpty()) {
                    ivLogo.setVisibility(View.VISIBLE);

                    int lw = sessionManager.getLogoWidth();
                    int lh = sessionManager.getLogoHeight();
                    float density = getResources().getDisplayMetrics().density;

                    android.view.ViewGroup.LayoutParams lp = ivLogo.getLayoutParams();
                    if (lw > 0) lp.width = (int) (lw * density);
                    if (lh > 0) lp.height = (int) (lh * density);
                    else lp.height = (int) (100 * density);

                    ivLogo.setLayoutParams(lp);

                    com.bumptech.glide.Glide.with(requireContext())
                            .load(url)
                            .priority(com.bumptech.glide.Priority.IMMEDIATE)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                            .into(ivLogo);
                } else {
                    ivLogo.setVisibility(View.GONE);
                }
            }

            int textColor = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            tvTitle.setTextColor(textColor);
            ImageView ivHeader = view.findViewById(R.id.ivHeaderIcon);
            if (ivHeader != null) ivHeader.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));

            android.view.ViewGroup.LayoutParams lpSearch = btnSearch.getLayoutParams();
            lpSearch.height = (int) (sessionManager.getButtonHeight() * getResources().getDisplayMetrics().density);
            btnSearch.setLayoutParams(lpSearch);

            setupBackButton(btnBack, view.findViewById(R.id.svReprint));
        } catch (Exception e) {}
    }
```

## Depois de aplicar

Continue para o Passo 1 (Build) do `AGENT_PROTOCOL.md`.
