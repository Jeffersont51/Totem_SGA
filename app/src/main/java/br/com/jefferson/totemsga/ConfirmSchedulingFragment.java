package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import br.com.jefferson.totemsga.adapter.GenericItemAdapter;
import br.com.jefferson.totemsga.ads.AdManager;
import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Agendamento;
import br.com.jefferson.totemsga.model.AgendamentoResponse;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.model.TicketRequest;
import br.com.jefferson.totemsga.model.TicketResponse;
import br.com.jefferson.totemsga.util.ClienteAuthManager;
import br.com.jefferson.totemsga.util.ValidationUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConfirmSchedulingFragment extends BaseKioskFragment {

    private TextInputEditText etDocument;
    private TextInputLayout tilDocumento;
    private RadioGroup rgDocumentType;
    private Button btnConfirm, btnBack, btnSearch;
    private ProgressBar progressSearch;
    private LinearLayout llResult;
    private RecyclerView rvSchedulings;
    private TextView tvResultHeader, tvTimer, tvErrorAgendamento;

    private static final int TYPE_CPF = 1;
    private static final int TYPE_CNPJ = 2;
    private static final int TYPE_PHONE = 3;
    private int selectedDocType = TYPE_CPF;

    private List<Agendamento> filteredList = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, ServicoUnidade> servicesMap = new HashMap<>();
    private Agendamento selectedAgendamento;
    private final Gson gson = new Gson();
    private boolean isApplyingMask = false;

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
        // 1. PRIMEIRO: Suspender AdManager (antes de qualquer timer)
        AdManager.getInstance().setSuspended(true);
        
        View view = inflater.inflate(R.layout.fragment_confirm_scheduling, container, false);
        
        setupViews(view);
        applyLayout(view);

        // Iniciar timer com valor do Admin
        secondsRemaining = sessionManager.getScreeningTimeout();
        if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
        timerHandler.postDelayed(timerRunnable, 1000);

        applyGlobalSpacing(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Retoma o timer global de publicidade ao sair do agendamento
        AdManager.getInstance().setSuspended(false);
    }

    @Override
    protected boolean isTimerEnabled() {
        return true;
    }

    private void setupViews(View view) {
        etDocument = view.findViewById(R.id.etDocumento);
        tilDocumento = view.findViewById(R.id.tilDocumento);
        rgDocumentType = view.findViewById(R.id.rgDocumentType);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnBack = view.findViewById(R.id.btnBack);
        btnSearch = view.findViewById(R.id.btnSearch);
        progressSearch = view.findViewById(R.id.progressSearch);
        llResult = view.findViewById(R.id.llResult);
        rvSchedulings = view.findViewById(R.id.rvSchedulings);
        tvResultHeader = view.findViewById(R.id.tvResultHeader);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvErrorAgendamento = view.findViewById(R.id.tvErrorAgendamento);

        rvSchedulings.setLayoutManager(new LinearLayoutManager(getContext()));

        rgDocumentType.setOnCheckedChangeListener((group, checkedId) -> {
            etDocument.setText("");
            clearError();
            clearResults();
            if (checkedId == R.id.rbCPF) {
                selectedDocType = TYPE_CPF;
                tilDocumento.setHint("CPF");
                tilDocumento.setPlaceholderText("000.000.000-00");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            } else if (checkedId == R.id.rbCNPJ) {
                selectedDocType = TYPE_CNPJ;
                tilDocumento.setHint("CNPJ");
                tilDocumento.setPlaceholderText("00.000.000/0000-00");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            } else if (checkedId == R.id.rbPhone) {
                selectedDocType = TYPE_PHONE;
                tilDocumento.setHint("Número Telefone");
                tilDocumento.setPlaceholderText("(00) 00000-0000");
                etDocument.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            }
        });

        btnConfirm.setOnClickListener(v -> confirmAgendamento());
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnSearch.setOnClickListener(v -> {
            String input = etDocument.getText().toString();
            String clean = input.replaceAll("[^a-zA-Z0-9]", "");

            if (clean.isEmpty()) {
                showError("Identificação", "Por favor, digite seu documento.");
                return;
            }

            if (validateDocument(true)) {
                hideKeyboard();
                searchAgendamento(clean);
            }
        });

        etDocument.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                resetInactivityTimer();
                clearError();
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (isApplyingMask) return;
                
                // Remove punctuation while typing to keep it numeric-only for the user
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

        etDocument.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateDocument();
            }
        });
    }

    private void validateDocument() {
        validateDocument(false);
    }

    private boolean validateDocument(boolean shouldFormat) {
        String input = etDocument.getText().toString();
        String clean = input.replaceAll("[^a-zA-Z0-9]", "");
        
        if (clean.isEmpty()) {
            clearError();
            return false;
        }
        
        boolean isValid = false;
        String errorMsg = "";
        
        switch (selectedDocType) {
            case TYPE_CPF:
                isValid = ValidationUtils.isValidCPF(clean);
                errorMsg = "CPF Inválido. Digite um CPF válido com 11 dígitos.";
                break;
            case TYPE_CNPJ:
                isValid = ValidationUtils.isValidCNPJ(clean);
                errorMsg = "CNPJ Inválido. Digite um CNPJ válido com 14 dígitos.";
                break;
            case TYPE_PHONE:
                isValid = ValidationUtils.isValidPhone(clean);
                errorMsg = "Telefone Inválido. Digite um telefone válido.";
                break;
        }
        
        if (!isValid) {
            showError("Validação", errorMsg);
            clearResults();
            return false;
        } else {
            clearError();
            if (shouldFormat) {
                isApplyingMask = true;
                etDocument.setText(formatDocument(clean));
                isApplyingMask = false;
            }
            return true;
        }
    }

    private String formatDocument(String clean) {
        if (selectedDocType == TYPE_CPF) return ValidationUtils.formatCPF(clean);
        if (selectedDocType == TYPE_CNPJ) return ValidationUtils.formatCNPJ(clean);
        return ValidationUtils.formatPhone(clean);
    }

    private void clearResults() {
        filteredList.clear();
        llResult.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.GONE);
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
            ImageView ivLogo = view.findViewById(R.id.ivLogoScheduling);
            if (ivLogo != null) {
                String logoUrl = sessionManager.getLogoUrl();
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    ivLogo.setVisibility(View.VISIBLE);
                    
                    int lw = sessionManager.getLogoWidth();
                    int lh = sessionManager.getLogoHeight();
                    float density = getResources().getDisplayMetrics().density;
                    
                    android.view.ViewGroup.LayoutParams lpLogo = ivLogo.getLayoutParams();
                    if (lw > 0) lpLogo.width = (int) (lw * density);
                    if (lh > 0) lpLogo.height = (int) (lh * density);
                    else lpLogo.height = (int) (100 * density);
                    
                    ivLogo.setLayoutParams(lpLogo);
                    
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(logoUrl)
                            .priority(com.bumptech.glide.Priority.IMMEDIATE)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                            .into(ivLogo);
                } else {
                    ivLogo.setVisibility(View.GONE);
                }
            }

            int textColor = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            TextView tvTitle = view.findViewById(R.id.tvTitle);
            if (tvTitle != null) tvTitle.setTextColor(textColor);
            
            ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
            if (ivHeaderIcon != null) ivHeaderIcon.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));

            Button btnSearchButton = view.findViewById(R.id.btnSearch);
            if (btnSearchButton != null) {
                android.view.ViewGroup.LayoutParams lpSearch = btnSearchButton.getLayoutParams();
                lpSearch.height = (int) (sessionManager.getButtonHeight() * getResources().getDisplayMetrics().density);
                btnSearchButton.setLayoutParams(lpSearch);
            }

            // Dynamic back button positioning
            Button btnBack = view.findViewById(R.id.btnBack);
            View scrollView = view.findViewById(R.id.svConfirmScheduling);
            if (btnBack != null) {
                setupBackButton(btnBack, scrollView);
            }

        } catch (Exception e) {}
    }

    private void searchAgendamento(String doc) {
        Log.d("AGENDAMENTO", "========================================");
        Log.d("AGENDAMENTO", "📝 Iniciando busca automática (" + getDocumentTypeName() + "): " + doc);
        Log.d("AGENDAMENTO", "========================================");

        clearError();
        progressSearch.setVisibility(View.VISIBLE);
        llResult.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // 1. GARANTIR LOGIN ANTES DE TUDO (Síncrono)
                Log.d("AGENDAMENTO", "🔐 [Auth] Iniciando ensureLoggedIn()...");
                ClienteAuthManager.getInstance(sessionManager).ensureLoggedIn();
                Log.d("AGENDAMENTO", "✅ [Auth] Sessão garantida com sucesso.");

                ApiService api = RetrofitClient.getInstance(sessionManager);
                if (api == null) {
                    Log.e("AGENDAMENTO", "❌ [Config] ApiService é nulo.");
                    requireActivity().runOnUiThread(() -> showError("Erro", "API não configurada"));
                    return;
                }

                Log.d("AGENDAMENTO", "🕐 [Contexto] Data/Hora Dispositivo: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                Response<List<ServicoUnidade>> svcResponse = api.getServicos(sessionManager.getUnidadeId()).execute();
                if (!svcResponse.isSuccessful() || svcResponse.body() == null) {
                    Log.e("AGENDAMENTO", "❌ [API] Falha ao carregar serviços. Code: " + svcResponse.code());
                    requireActivity().runOnUiThread(() -> showError("Erro", "Falha ao carregar serviços da unidade"));
                    return;
                }

                List<ServicoUnidade> ativos = svcResponse.body().stream()
                        .filter(s -> s.ativo && s.servico != null && s.servico.ativo)
                        .collect(Collectors.toList());
                
                Log.d("AGENDAMENTO", "📋 [Serviços] Total de serviços ativos encontrados: " + ativos.size());
                servicesMap.clear();
                for (ServicoUnidade su : svcResponse.body()) {
                    if (su.servico != null) {
                        servicesMap.put(su.servico.id, su);
                        if (su.ativo && su.servico.ativo) {
                            Log.d("AGENDAMENTO", "   - ID: " + su.servico.id + " | Nome: " + su.servico.nome);
                        }
                    }
                }

                if (ativos.isEmpty()) {
                    Log.w("AGENDAMENTO", "⚠️ [Aviso] Nenhum serviço ativo para esta unidade.");
                    requireActivity().runOnUiThread(() -> showError("Aviso", "Nenhum serviço ativo encontrado"));
                    return;
                }

                filteredList.clear();
                CountDownLatch latch = new CountDownLatch(ativos.size());
                ExecutorService executor = Executors.newFixedThreadPool(Math.min(ativos.size(), 5));

                for (ServicoUnidade su : ativos) {
                    executor.execute(() -> {
                        try {
                            Log.d("AGENDAMENTO", "🛰️ [Busca] Consultando serviço: " + su.servico.nome + " (ID: " + su.servico.id + ")");
                            ClienteAuthManager.getInstance(sessionManager).buscarAgendamentosPorServico(su.servico.id, new ClienteAuthManager.AgendamentoCallback() {
                                @Override
                                public void onFound(List<Agendamento> agendamentos) {
                                    Log.d("AGENDAMENTO", "📥 [Resposta] " + su.servico.nome + " retornou " + agendamentos.size() + " agendamentos.");
                                    for (Agendamento a : agendamentos) {
                                        String aDoc = (a.cliente != null && a.cliente.documento != null) ? a.cliente.documento.replaceAll("[^0-9]", "") : "";
                                        boolean isHoje = a.isHoje();
                                        boolean isConfirmado = a.isConfirmado();
                                        
                                        Log.d("AGENDAMENTO", "   📅 Analisando Agendamento ID: " + a.id);
                                        Log.d("AGENDAMENTO", "      Cliente: " + (a.cliente != null ? a.cliente.nome : "N/A") + " | Doc: " + aDoc);
                                        Log.d("AGENDAMENTO", "      Data RAW: '" + a.data + "' | Hora: " + a.hora);
                                        Log.d("AGENDAMENTO", "      Filtros -> CPF Match: " + doc.equals(aDoc) + " | isHoje: " + isHoje + " | isConfirmado: " + isConfirmado);

                                        if (doc.equals(aDoc) && isHoje && !isConfirmado) {
                                            Log.d("AGENDAMENTO", "      ✅ AGENDAMENTO ACEITO PELO FILTRO");
                                            filteredList.add(a);
                                        }
                                    }
                                    latch.countDown();
                                }

                                @Override
                                public void onNotFound() {
                                    Log.d("AGENDAMENTO", "📥 [Resposta] " + su.servico.nome + " não possui agendamentos.");
                                    latch.countDown();
                                }

                                @Override
                                public void onError(String message) {
                                    Log.e("AGENDAMENTO", "❌ [Erro] Serviço " + su.servico.nome + ": " + message);
                                    latch.countDown();
                                }
                            });
                        } catch (Exception e) {
                            Log.e("AGENDAMENTO", "❌ [Crash] Thread do serviço " + su.servico.nome, e);
                            latch.countDown();
                        }
                    });
                }

                // Aguarda todos ou timeout de 20s (Aumentado de 10s para resiliência)
                boolean finished = latch.await(20, TimeUnit.SECONDS);
                executor.shutdown();

                requireActivity().runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    Log.d("AGENDAMENTO", "✅ Busca finalizada. Encontrados: " + filteredList.size() + " | Completo: " + finished);

                    if (filteredList.isEmpty()) {
                        String msg = finished ? 
                            "Nenhum agendamento pendente localizado para hoje com este " + getDocumentTypeName() + "." :
                            "Tempo de busca excedido. Tente novamente.";
                        showError("Busca", msg);
                    } else {
                        showResults();
                    }
                });

            } catch (Exception e) {
                Log.e("AGENDAMENTO", "Erro no processo de busca", e);
                requireActivity().runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    showError("Erro", "Falha: " + e.getMessage());
                });
            }
        }).start();
    }

    private String getDocumentTypeName() {
        if (selectedDocType == TYPE_CPF) return "CPF";
        if (selectedDocType == TYPE_CNPJ) return "CNPJ";
        return "Telefone";
    }

    private void showResults() {
        llResult.setVisibility(View.VISIBLE);
        
        // Remove duplicados se houver e ordena por hora
        List<Agendamento> uniqueList;
        synchronized (filteredList) {
            uniqueList = new ArrayList<>(new HashSet<>(filteredList));
            Collections.sort(uniqueList, (o1, o2) -> o1.hora.compareTo(o2.hora));
        }

        if (uniqueList.size() == 1) {
            selectedAgendamento = uniqueList.get(0);
            tvResultHeader.setText("Agendamento de hoje encontrado:");
            btnConfirm.setVisibility(View.GONE);
        } else {
            selectedAgendamento = null;
            tvResultHeader.setText("Selecione o agendamento para confirmar:");
            btnConfirm.setVisibility(View.GONE);
        }

        GenericItemAdapter adapter = new GenericItemAdapter(uniqueList, "#F47B20", null, null, GenericItemAdapter.STYLE_SCHEDULING, item -> {
            selectedAgendamento = (Agendamento) item;
            updateConfirmationButton(selectedAgendamento);
            
            ((GenericItemAdapter)rvSchedulings.getAdapter()).setSelectedId(selectedAgendamento.id);
        });
        adapter.setConfirmListener(item -> {
            selectedAgendamento = (Agendamento) item;
            updateConfirmationButton(selectedAgendamento);
            ((GenericItemAdapter) rvSchedulings.getAdapter()).setSelectedId(selectedAgendamento.id);
            confirmAgendamento();
        });
        
        // Mark expired items in the adapter
        Map<Integer, Boolean> expiredMap = new HashMap<>();
        for (Agendamento a : uniqueList) {
            expiredMap.put(a.id, isAgendamentoVencido(a));
        }
        adapter.setExpiredItems(expiredMap);
        
        rvSchedulings.setAdapter(adapter);
        
        if (selectedAgendamento != null) {
            adapter.setSelectedId(selectedAgendamento.id);
            updateConfirmationButton(selectedAgendamento);
        }
    }

    private void updateConfirmationButton(Agendamento a) {
        if (isAgendamentoVencido(a)) {
            btnConfirm.setEnabled(false);
            btnConfirm.setText("AGENDAMENTO VENCIDO");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            showError("Vencido", "Agendamento das " + a.hora + " já passou. Procure o balcão de atendimento.");
        } else {
            btnConfirm.setEnabled(true);
            btnConfirm.setText("CONFIRMAR PRESENÇA");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2ECC71")));
            clearError();
        }
        btnConfirm.setVisibility(View.GONE);
    }

    private void confirmAgendamento() {
        if (selectedAgendamento == null) return;

        btnConfirm.setEnabled(false);
        progressSearch.setVisibility(View.VISIBLE);
        
        ClienteAuthManager.getInstance(sessionManager).confirmarAgendamento(selectedAgendamento.id, new ClienteAuthManager.TicketCallback() {
            @Override
            public void onSuccess(TicketResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    
                    String pName = (response.prioridade != null && response.prioridade.nome != null) 
                            ? response.prioridade.nome : "Normal";
                    
                    boolean isFacial = resolveFeature(selectedAgendamento.servico.id, br.com.jefferson.totemsga.util.FeatureParser.FACIAL);
                    String color = resolveColor(selectedAgendamento.servico.id);
                    
                    SuccessFragment fragment = SuccessFragment.newInstance(
                            response.senha.format,
                            response.id,
                            response.hash,
                            pName,
                            selectedAgendamento.servico.nome,
                            color
                    );
                    Bundle args = fragment.getArguments();
                    if (args != null) {
                        args.putString("cliente_nome", selectedAgendamento.cliente.nome);
                        args.putBoolean("has_nome", true);
                        args.putBoolean("is_facial", isFacial);
                        if (response.servico != null && response.servico.mensagem != null) {
                            args.putString("servico_mensagem", response.servico.mensagem);
                        }
                    }
                    
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment)
                            .commitAllowingStateLoss();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                    progressSearch.setVisibility(View.GONE);
                    showError("Erro na Confirmação", message);
                });
            }
        });
    }

    private void showError(String title, String msg) {
        if (tvErrorAgendamento != null) {
            tvErrorAgendamento.setText(msg);
            tvErrorAgendamento.setVisibility(View.VISIBLE);
        }
    }

    private void clearError() {
        if (tvErrorAgendamento != null) {
            tvErrorAgendamento.setVisibility(View.GONE);
        }
    }

    private boolean resolveFeature(int servicoId, String feature) {
        ServicoUnidade su = servicesMap.get(servicoId);
        if (su == null) return false;

        // 1. Check Service
        Map<String, Boolean> sFeatures = br.com.jefferson.totemsga.util.FeatureParser.parse(su.servico.descricao);
        if (sFeatures.containsKey(feature)) {
            Boolean val = sFeatures.get(feature);
            return val != null && val;
        }

        // 2. Check Department
        if (su.departamento != null) {
            Map<String, Boolean> dFeatures = br.com.jefferson.totemsga.util.FeatureParser.parse(su.departamento.descricao);
            if (dFeatures.containsKey(feature)) {
                Boolean val = dFeatures.get(feature);
                return val != null && val;
            }
        }
        return false;
    }

    private int getToleranciaMinutos(int servicoId) {
        ServicoUnidade su = servicesMap.get(servicoId);
        if (su == null) return 15;

        Map<String, String> params = br.com.jefferson.totemsga.util.FeatureParser.parseParams(su.servico.descricao);
        if (params.containsKey(br.com.jefferson.totemsga.util.FeatureParser.TOLERANCIA)) {
            String val = params.get(br.com.jefferson.totemsga.util.FeatureParser.TOLERANCIA);
            if ("false".equals(val) || "0".equals(val)) return -1;
            try { return Integer.parseInt(val); } catch (Exception e) {}
        }

        if (su.departamento != null) {
            params = br.com.jefferson.totemsga.util.FeatureParser.parseParams(su.departamento.descricao);
            if (params.containsKey(br.com.jefferson.totemsga.util.FeatureParser.TOLERANCIA)) {
                String val = params.get(br.com.jefferson.totemsga.util.FeatureParser.TOLERANCIA);
                if ("false".equals(val) || "0".equals(val)) return -1;
                try { return Integer.parseInt(val); } catch (Exception e) {}
            }
        }
        return 15; // Padrão
    }

    private boolean isAgendamentoVencido(Agendamento a) {
        if (a == null || a.hora == null) return false;
        
        int tolerance = getToleranciaMinutos(a.servico.id);
        if (tolerance < 0) return false;

        try {
            String[] parts = a.hora.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            Calendar calAgendamento = Calendar.getInstance();
            calAgendamento.set(Calendar.HOUR_OF_DAY, h);
            calAgendamento.set(Calendar.MINUTE, m);
            calAgendamento.set(Calendar.SECOND, 0);
            calAgendamento.set(Calendar.MILLISECOND, 0);

            calAgendamento.add(Calendar.MINUTE, tolerance);

            Calendar calAtual = Calendar.getInstance();
            return calAtual.after(calAgendamento);
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveColor(int servicoId) {
        ServicoUnidade su = servicesMap.get(servicoId);
        if (su == null) return "#F47B20";

        // 1. Service Color
        Map<String, String> svcColors = gson.fromJson(sessionManager.getServiceColors(), new TypeToken<Map<String, String>>(){}.getType());
        if (svcColors != null && svcColors.containsKey(String.valueOf(su.servico.id))) {
            String c = svcColors.get(String.valueOf(su.servico.id));
            if (c != null && !c.isEmpty()) return c;
        }

        // 2. Dept Color
        if (su.departamento != null) {
            Map<String, String> deptColors = gson.fromJson(sessionManager.getDeptColors(), new TypeToken<Map<String, String>>(){}.getType());
            if (deptColors != null && deptColors.containsKey(String.valueOf(su.departamento.id))) {
                String c = deptColors.get(String.valueOf(su.departamento.id));
                if (c != null && !c.isEmpty()) return c;
            }
        }

        return sessionManager.getButtonColor();
    }

    private void hideKeyboard() {
        android.view.View view = getActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void resetInactivityTimer() {
        super.resetInactivityTimer();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
            secondsRemaining = sessionManager.getScreeningTimeout();
            if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
