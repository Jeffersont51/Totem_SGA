package br.com.jefferson.totemsga;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReprintFragment extends BaseKioskFragment {

    private TextInputEditText etDocument;
    private TextInputLayout tilDocumento;
    private RadioGroup rgDocumentType;
    private Button btnSearch, btnConfirmReprint, btnBack;
    private ProgressBar progressSearch;
    private LinearLayout llResult;
    private RecyclerView rvResults;
    private TextView tvTitle, tvTimer, tvErrorReprint, tvResultHeader;

    private static final int TYPE_SENHA = 0;
    private static final int TYPE_CPF = 1;
    private static final int TYPE_CNPJ = 2;
    private static final int TYPE_PHONE = 3;
    private int selectedDocType = TYPE_SENHA;

    private List<SenhaFila> filteredSenhas = new ArrayList<>();
    private SenhaFila selectedSenha;
    private boolean isApplyingMask = false;
    private final Map<Integer, ServicoUnidade> servicesMap = new HashMap<>();

    private int secondsRemaining;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
                timerHandler.postDelayed(this, 1000);
            } else {
                onInactivityTimeout();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Suspender AdManager IMEDIATAMENTE
        AdManager.getInstance().setSuspended(true);

        View view = inflater.inflate(R.layout.fragment_reprint, container, false);

        setupViews(view);
        applyLayout(view);

        // Iniciar timer
        secondsRemaining = sessionManager.getScreeningTimeout();
        if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
        timerHandler.postDelayed(timerRunnable, 1000);

        applyGlobalSpacing(view);
        return view;
    }

    private void setupViews(View view) {
        etDocument = view.findViewById(R.id.etDocumento);
        tilDocumento = view.findViewById(R.id.tilDocumento);
        rgDocumentType = view.findViewById(R.id.rgDocumentType);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnConfirmReprint = view.findViewById(R.id.btnConfirmReprint);
        btnBack = view.findViewById(R.id.btnBack);
        progressSearch = view.findViewById(R.id.progressSearch);
        llResult = view.findViewById(R.id.llResult);
        rvResults = view.findViewById(R.id.rvReprintResults);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvErrorReprint = view.findViewById(R.id.tvErrorReprint);
        tvResultHeader = view.findViewById(R.id.tvResultHeader);

        // Auditoria de Sessão
        ClienteAuthManager auth = ClienteAuthManager.getInstance(sessionManager);
        Log.d("DEBUG_SGA", "🔑 Manager Hash: " + auth.hashCode());
        Log.d("DEBUG_SGA", "🏷️ Status Logado: " + auth.isLoggedIn());

        // Reset de Emergência na Logo
        ImageView ivLogo = view.findViewById(R.id.ivLogoReprint);
        if (ivLogo != null) {
            ivLogo.setOnLongClickListener(v -> {
                auth.logout();
                Toast.makeText(getContext(), "🔄 Sessão reiniciada!", Toast.LENGTH_SHORT).show();
                clearError();
                return true;
            });
        }

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initial state: Ticket Number
        selectedDocType = TYPE_SENHA;
        tilDocumento.setHint("Número da Senha");
        tilDocumento.setPlaceholderText("Ex: AD001");
        etDocument.setInputType(InputType.TYPE_CLASS_TEXT);
        etDocument.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        rgDocumentType.setOnCheckedChangeListener((group, checkedId) -> {
            etDocument.setText("");
            clearError();
            llResult.setVisibility(View.GONE);
            if (checkedId == R.id.rbSenha) {
                selectedDocType = TYPE_SENHA;
                tilDocumento.setHint("Número da Senha");
                tilDocumento.setPlaceholderText("Ex: AD001");
                etDocument.setInputType(InputType.TYPE_CLASS_TEXT);
                etDocument.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
            } else if (checkedId == R.id.rbCPF) {
                selectedDocType = TYPE_CPF;
                tilDocumento.setHint("CPF");
                tilDocumento.setPlaceholderText("000.000.000-00");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                etDocument.setFilters(new InputFilter[0]);
            } else if (checkedId == R.id.rbCNPJ) {
                selectedDocType = TYPE_CNPJ;
                tilDocumento.setHint("CNPJ");
                tilDocumento.setPlaceholderText("00.000.000/0000-00");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                etDocument.setFilters(new InputFilter[0]);
            } else if (checkedId == R.id.rbPhone) {
                selectedDocType = TYPE_PHONE;
                tilDocumento.setHint("Número Telefone");
                tilDocumento.setPlaceholderText("(00) 00000-0000");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                etDocument.setFilters(new InputFilter[0]);
            }
        });

        btnSearch.setOnClickListener(v -> {
            resetInactivityTimer();
            if (validateDocument(true)) {
                hideKeyboard();
                consultarSenhas();
            }
        });

        btnConfirmReprint.setOnClickListener(v -> {
            if (selectedSenha != null) {
                reimprimir(selectedSenha);
            }
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        etDocument.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetInactivityTimer();
                clearError();
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (isApplyingMask) return;
                String input = s.toString();
                String clean = input.replaceAll("[^a-zA-Z0-9]", "");
                if (!input.equals(clean)) {
                    etDocument.removeTextChangedListener(this);
                    etDocument.setText(clean);
                    etDocument.setSelection(clean.length());
                    etDocument.addTextChangedListener(this);
                }
            }
        });
    }

    private boolean validateDocument(boolean shouldFormat) {
        String input = etDocument.getText().toString().trim();
        String clean = input.replaceAll("[^a-zA-Z0-9]", "");
        if (clean.isEmpty()) {
            showError("Por favor, preencha o campo de busca.");
            return false;
        }

        boolean isValid = false;
        String msg = "";
        if (selectedDocType == TYPE_SENHA) {
            isValid = input.length() >= 3;
            msg = "Número da senha inválido (Mínimo 3 caracteres).";
        } else if (selectedDocType == TYPE_CPF) {
            isValid = ValidationUtils.isValidCPF(clean);
            msg = "CPF Inválido.";
        } else if (selectedDocType == TYPE_CNPJ) {
            isValid = ValidationUtils.isValidCNPJ(clean);
            msg = "CNPJ Inválido.";
        } else {
            isValid = ValidationUtils.isValidPhone(clean);
            msg = "Telefone Inválido.";
        }

        if (!isValid) {
            showError(msg);
            return false;
        }

        if (shouldFormat && selectedDocType != TYPE_SENHA) {
            isApplyingMask = true;
            String formatted = (selectedDocType == TYPE_CPF) ? ValidationUtils.formatCPF(clean) :
                             (selectedDocType == TYPE_CNPJ) ? ValidationUtils.formatCNPJ(clean) :
                             ValidationUtils.formatPhone(clean);
            etDocument.setText(formatted);
            isApplyingMask = false;
        }
        return true;
    }

    private void consultarSenhas() {
        String input = etDocument.getText().toString().trim();
        progressSearch.setVisibility(View.VISIBLE);
        llResult.setVisibility(View.GONE);
        clearError();
        
        Log.d("DEBUG_SGA", "🔍 [Busca] Iniciando consulta para: " + input);

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
                    @Override
                    public void onFound(MonitorResponse response) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            filtrarResultadosMonitor(response.data, input);
                        });
                    }

                    @Override
                    public void onNotFound() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            progressSearch.setVisibility(View.GONE);
                            showError(getString(R.string.reprint_not_found));
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            progressSearch.setVisibility(View.GONE);
                            Log.e("DEBUG_SGA", "❌ Detalhes da falha: " + message);
                            showError("DADOS RECEBIDOS:\n" + message);
                        });
                    }
                });
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressSearch.setVisibility(View.GONE);
                        showError(e.getMessage() != null ? e.getMessage() : "Falha na autenticação");
                    });
                }
            }
        }).start();
    }

    private void filtrarResultadosMonitor(List<MonitorResponse.MonitorData> monitorData, String inputBuscado) {
        progressSearch.setVisibility(View.GONE);
        filteredSenhas.clear();
        Set<Integer> processedIds = new HashSet<>();

        String hoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String docBuscado = inputBuscado.replaceAll("[^0-9]", "");

        if (monitorData != null) {
            for (MonitorResponse.MonitorData data : monitorData) {
                if (data.fila != null) {
                    for (SenhaFila s : data.fila) {
                        // 1. Evitar duplicatas (mesmo ID em múltiplos serviços)
                        if (processedIds.contains(s.id)) {
                            continue;
                        }

                        boolean isHoje = s.dataChegada != null && s.dataChegada.startsWith(hoje);
                        boolean isEmitida = "emitida".equals(s.status);

                        if (isHoje && isEmitida) {
                            boolean match = false;
                            if (selectedDocType == TYPE_SENHA) {
                                match = s.senha != null && s.senha.format != null && s.senha.format.equalsIgnoreCase(inputBuscado);
                            } else if (s.cliente != null && s.cliente.documento != null) {
                                String sDoc = s.cliente.documento.replaceAll("[^0-9]", "");
                                match = docBuscado.equals(sDoc);
                            }

                            if (match) {
                                processedIds.add(s.id);
                                filteredSenhas.add(s);
                            }
                        }
                    }
                }
            }
        }

        if (filteredSenhas.isEmpty()) {
            showError(getString(R.string.reprint_not_found));
        } else {
            exibirResultados();
        }
    }

    private void exibirResultados() {
        llResult.setVisibility(View.VISIBLE);
        if (filteredSenhas.size() == 1) {
            selectedSenha = filteredSenhas.get(0);
            btnConfirmReprint.setVisibility(View.GONE);
            tvResultHeader.setText("Senha emitida hoje encontrada:");
        } else {
            selectedSenha = null;
            btnConfirmReprint.setVisibility(View.GONE);
            tvResultHeader.setText(getString(R.string.reprint_found));
        }

        GenericItemAdapter adapter = new GenericItemAdapter(filteredSenhas, "#F47B20", null, null, GenericItemAdapter.STYLE_REPRINT, item -> {
            selectedSenha = (SenhaFila) item;
            btnConfirmReprint.setVisibility(View.GONE);
            ((GenericItemAdapter)rvResults.getAdapter()).setSelectedId(selectedSenha.id);
            resetInactivityTimer();
        });
        adapter.setPrintListener(item -> {
            resetInactivityTimer();
            reimprimir((SenhaFila) item);
        });
        rvResults.setAdapter(adapter);
        if (selectedSenha != null) adapter.setSelectedId(selectedSenha.id);
    }

    private void reimprimir(SenhaFila senha) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setInteractingWithSystem(true);
        }

        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;

        // Se não tiver hash, tentamos com string vazia (o NovoSGA pode permitir se a sessão estiver ativa)
        String hash = (senha.hash != null) ? senha.hash : "";

        api.getPrintContent(senha.id, hash).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String html = response.body().string();
                        doPrint(html, senha);
                    } catch (Exception e) {}
                } else {
                    Toast.makeText(getContext(), "Erro ao carregar conteúdo de impressão", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Erro de conexão ao imprimir", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
            }
        });
    }

    private void doPrint(String html, SenhaFila senha) {
        String printerType = sessionManager.getPrinterType();
        boolean isSunmiAvailable = SunmiPrinterHelper.getInstance().isConnected() && SunmiPrinterHelper.getInstance().getStatus() == 1;

        if ("SUNMI".equals(printerType) || ("AUTO".equals(printerType) && isSunmiAvailable)) {
            printSunmi(senha);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
            return;
        }

        if ("ALLPOS".equals(printerType) || "AUTO".equals(printerType)) {
            try {
                Intent intent = new Intent("in.allmark.allpos_print_service.PRINT");
                intent.setPackage("in.allmark.allpos_print_service");
                intent.putExtra("content", html);
                intent.putExtra("direct", "1");
                startActivity(intent);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
                }, 5000);
                return;
            } catch (Exception e) {}
        }

        doPrintStandard(html);
    }

    private void doPrintStandard(String html) {
        android.webkit.WebView webView = new android.webkit.WebView(requireContext());
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                try {
                    android.print.PrintManager pm = (android.print.PrintManager) requireActivity().getSystemService(android.content.Context.PRINT_SERVICE);
                    String job = "SGA_REPRINT_" + System.currentTimeMillis();
                    pm.print(job, webView.createPrintDocumentAdapter(job), new android.print.PrintAttributes.Builder().build());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
                    }, 8000);
                } catch (Exception e) {
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setInteractingWithSystem(false);
                }
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

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

    private void showError(String msg) {
        if (tvErrorReprint != null) {
            tvErrorReprint.setText(msg);
            tvErrorReprint.setVisibility(View.VISIBLE);
        }
    }

    private void clearError() {
        if (tvErrorReprint != null) tvErrorReprint.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        android.view.View v = getActivity().getCurrentFocus();
        if (v != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected boolean isTimerEnabled() { return true; }

    @Override
    public void resetInactivityTimer() {
        super.resetInactivityTimer();
        timerHandler.removeCallbacks(timerRunnable);
        secondsRemaining = sessionManager.getScreeningTimeout();
        if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        AdManager.getInstance().setSuspended(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
