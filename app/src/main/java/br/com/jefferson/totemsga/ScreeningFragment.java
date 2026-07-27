package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.jefferson.totemsga.adapter.GenericItemAdapter;
import br.com.jefferson.totemsga.ads.AdManager;
import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Prioridade;
import br.com.jefferson.totemsga.model.TicketRequest;
import br.com.jefferson.totemsga.model.TicketResponse;
import br.com.jefferson.totemsga.util.ClienteAuthManager;
import br.com.jefferson.totemsga.util.ValidationUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScreeningFragment extends BaseKioskFragment {

    private static final String ARG_SERVICO_ID = "servico_id";
    private static final String ARG_SERVICO_NOME = "servico_nome";
    private static final String ARG_REQUIRED = "required";
    private static final String ARG_SHOW_PRIORITY = "show_priority";
    private static final String ARG_THEME_COLOR = "theme_color";

    private int servicoId;
    private String servicoNome;
    private String themeColor;
    private boolean isRequired;
    private boolean showPrioritySelection = true;
    private TextInputEditText etNome, etDocumento;
    private TextView tvAutoFilledHint, tvErrorTriagem;
    private com.google.android.material.textfield.TextInputLayout tilDocumento;
    private RecyclerView rvPrioridades;
    private GenericItemAdapter prioridadeAdapter;
    private Button btnGerarSenha;
    private List<Prioridade> prioridadesList = new ArrayList<>();
    private int selectedPrioridadeId = 1;

    private static final int TYPE_CPF = 1;
    private static final int TYPE_CNPJ = 2;
    private static final int TYPE_PHONE = 3;
    private int selectedDocType = TYPE_CPF;

    private TextView tvTimer;
    private int secondsRemaining;
    private boolean isFacial;
    private boolean hasNome;
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable debounceRunnable;
    private android.os.Handler timerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
                timerHandler.postDelayed(this, 1000);
            } else {
                onInactivityTimeout();
            }
        }
    };

    public static ScreeningFragment newInstance(int servicoId, boolean required, boolean showPriority, String servicoNome, String themeColor) {
        ScreeningFragment fragment = new ScreeningFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SERVICO_ID, servicoId);
        args.putBoolean(ARG_REQUIRED, required);
        args.putBoolean(ARG_SHOW_PRIORITY, showPriority);
        args.putString(ARG_SERVICO_NOME, servicoNome);
        args.putString(ARG_THEME_COLOR, themeColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            servicoId = getArguments().getInt(ARG_SERVICO_ID);
            isRequired = getArguments().getBoolean(ARG_REQUIRED);
            showPrioritySelection = getArguments().getBoolean(ARG_SHOW_PRIORITY, true);
            servicoNome = getArguments().getString(ARG_SERVICO_NOME, "");
            themeColor = getArguments().getString(ARG_THEME_COLOR);
            isFacial = getArguments().getBoolean("is_facial", false);
            hasNome = getArguments().getBoolean("is_nome", false);
        }
        if (themeColor == null || themeColor.isEmpty()) {
            themeColor = sessionManager.getPrimaryColor();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Suspende o timer global de publicidade IMEDIATAMENTE
        AdManager.getInstance().setSuspended(true);
        
        View view = inflater.inflate(R.layout.fragment_screening, container, false);
        
        try {
            String bgColor = sessionManager.getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty()) {
                view.setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            }
        } catch (Exception e) {
            view.setBackgroundColor(android.graphics.Color.parseColor("#FFF5E1"));
        }

        etNome = view.findViewById(R.id.etNome);
        tvAutoFilledHint = view.findViewById(R.id.tvAutoFilledHint);
        tvErrorTriagem = view.findViewById(R.id.tvErrorTriagem);
        etDocumento = view.findViewById(R.id.etDocumento);
        tilDocumento = view.findViewById(R.id.tilDocumento);
        android.widget.RadioGroup rgDocumentType = view.findViewById(R.id.rgDocumentType);

        rgDocumentType.setOnCheckedChangeListener((group, checkedId) -> {
            etDocumento.setText("");
            clearError();
            if (checkedId == R.id.rbCPF) {
                selectedDocType = TYPE_CPF;
                tilDocumento.setHint("CPF");
                tilDocumento.setPlaceholderText("000.000.000-00");
                etDocumento.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            } else if (checkedId == R.id.rbCNPJ) {
                selectedDocType = TYPE_CNPJ;
                tilDocumento.setHint("CNPJ");
                tilDocumento.setPlaceholderText("00.000.000/0000-00");
                etDocumento.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            } else if (checkedId == R.id.rbPhone) {
                selectedDocType = TYPE_PHONE;
                tilDocumento.setHint("Número Telefone");
                tilDocumento.setPlaceholderText("(00) 00000-0000");
                etDocumento.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            }
        });

        // Set initial state for CPF
        tilDocumento.setPlaceholderText("000.000.000-00");
        
        // Apply mask ONLY when focus is lost to avoid keyboard flickering during typing
        etDocumento.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String input = etDocumento.getText().toString();
                String clean = input.replaceAll("[^a-zA-Z0-9]", "");
                if (clean.isEmpty()) return;

                String formatted;
                if (selectedDocType == TYPE_CPF) {
                    formatted = ValidationUtils.formatCPF(clean);
                } else if (selectedDocType == TYPE_CNPJ) {
                    formatted = ValidationUtils.formatCNPJ(clean);
                } else {
                    formatted = ValidationUtils.formatPhone(clean);
                }
                etDocumento.setText(formatted);
            }
        });

        rvPrioridades = view.findViewById(R.id.rvPrioridades);
        btnGerarSenha = view.findViewById(R.id.btnGerarSenha);
        tvTimer = view.findViewById(R.id.tvTimer);
        
        applyLayout(view);

        secondsRemaining = sessionManager.getScreeningTimeout();
        tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
        tvTimer.setVisibility(View.VISIBLE);
        resetInactivityTimer();
        timerHandler.postDelayed(timerRunnable, 1000);

        setupAutocomplete();
        fetchPrioridades();

        View.OnClickListener backListener = v -> {
            playSound();
            getParentFragmentManager().popBackStack();
        };

        view.findViewById(R.id.btnBackScreening).setOnClickListener(backListener);
        Button btnBackSecondary = view.findViewById(R.id.btnBackScreeningSecondary);
        if (btnBackSecondary != null) {
            btnBackSecondary.setOnClickListener(backListener);
        }

        btnGerarSenha.setOnClickListener(v -> gerarSenha());

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
        // Retoma o timer global de publicidade ao sair da triagem
        AdManager.getInstance().setSuspended(false);
    }

    private void applyLayout(View view) {
        try {
            ImageView ivLogo = view.findViewById(R.id.ivLogoScreening);
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

            // btnGerarSenha styling handled by style XML to ensure solid orange #F47B20
            // but we can apply theme color if specifically set in Admin (optional)
            // For now, following user request to standardize to #F47B20
            
            android.view.ViewGroup.LayoutParams lpGerar = btnGerarSenha.getLayoutParams();
            lpGerar.height = (int) (sessionManager.getButtonHeight() * getResources().getDisplayMetrics().density);
            btnGerarSenha.setLayoutParams(lpGerar);
            
            Button btnBackSecondary = view.findViewById(R.id.btnBackScreeningSecondary);
            android.view.View scrollView = view.findViewById(R.id.svScreening);
            
            if (btnBackSecondary != null) {
                setupBackButton(btnBackSecondary, scrollView);
            }

            int bgColorText = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            TextView tvTitle = view.findViewById(R.id.tvScreeningTitle);
            if (tvTitle != null) tvTitle.setTextColor(bgColorText);
            
            ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
            if (ivHeaderIcon != null) ivHeaderIcon.setImageTintList(android.content.res.ColorStateList.valueOf(bgColorText));

            TextView tvLabel = view.findViewById(R.id.tvPrioridadeLabel);
            if (tvLabel != null) tvLabel.setTextColor(bgColorText);
        } catch (Exception e) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void setupAutocomplete() {
        etDocumento.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                resetInactivityTimer();
                clearError();
            }
            @Override
            public void afterTextChanged(Editable s) {
                String clean = s.toString().replaceAll("[^a-zA-Z0-9]", "");

                debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    int requiredLength = 11;
                    if (selectedDocType == TYPE_CNPJ) {
                        requiredLength = 14;
                    }
                    
                    if (clean.length() >= requiredLength && !sessionManager.getApiUrl().isEmpty()) {
                        fetchAutocompleteData(clean);
                    }
                };
                debounceHandler.postDelayed(debounceRunnable, 500);
            }
        });
        
        etNome.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                resetInactivityTimer();
                clearError();
                // Oculta o indicador de preenchimento automático se o usuário editar manualmente
                if (tvAutoFilledHint != null && tvAutoFilledHint.getVisibility() == View.VISIBLE) {
                    tvAutoFilledHint.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected boolean isTimerEnabled() {
        return true;
    }

    @Override
    public void resetInactivityTimer() {
        super.resetInactivityTimer();
        secondsRemaining = sessionManager.getScreeningTimeout();
        if (tvTimer != null) tvTimer.setText("Tempo restante: " + secondsRemaining + "s");
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    private void fetchAutocompleteData(String doc) {
        ClienteAuthManager.getInstance(sessionManager).buscarCliente(doc, new ClienteAuthManager.AuthCallback() {
            @Override
            public void onFound(br.com.jefferson.totemsga.model.Cliente cliente) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        etNome.setText(cliente.nome);
                        if (tvAutoFilledHint != null) {
                            tvAutoFilledHint.setVisibility(View.VISIBLE);
                        }
                        resetInactivityTimer();
                    });
                }
            }

            @Override 
            public void onNotFound() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> resetInactivityTimer());
                }
            }
            
            @Override 
            public void onError(String message) {
                android.util.Log.w("ScreeningFragment", "Autocomplete falhou: " + message);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> resetInactivityTimer());
                }
            }
        });
    }

    private void fetchPrioridades() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getPrioridades().enqueue(new Callback<List<Prioridade>>() {
            @Override
            public void onResponse(Call<List<Prioridade>> call, Response<List<Prioridade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    prioridadesList = response.body()
                            .stream()
                            .filter(prioridade -> prioridade.ativo)
                            .collect(Collectors.toList());
//                    prioridadesList = response.body();
                    renderPrioridadeButtons();
                }
            }
            @Override public void onFailure(Call<List<Prioridade>> call, Throwable t) {}
        });
    }

    private void renderPrioridadeButtons() {
        if (!isAdded()) return;

        if (!showPrioritySelection || prioridadesList.size() <= 1) {
            rvPrioridades.setVisibility(View.GONE);
            View label = getView() != null ? getView().findViewById(R.id.tvPrioridadeLabel) : null;
            if (label != null) label.setVisibility(View.GONE);
            if (!prioridadesList.isEmpty()) {
                // Find Normal priority (ID 1) if available, else first one
                selectedPrioridadeId = prioridadesList.get(0).id;
                for (Prioridade p : prioridadesList) {
                    if (p.id == 1) {
                        selectedPrioridadeId = 1;
                        break;
                    }
                }
            }
            return;
        }

        rvPrioridades.setVisibility(View.VISIBLE);
        View label = getView() != null ? getView().findViewById(R.id.tvPrioridadeLabel) : null;
        if (label != null) label.setVisibility(View.VISIBLE);
        
        int spanCount = calculateSpanCount();

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.STRETCH);
        rvPrioridades.setLayoutManager(layoutManager);
        
        Map<String, String> colors = new Gson().fromJson(sessionManager.getPriorityColors(), new TypeToken<Map<String, String>>(){}.getType());
        Map<String, String> textColors = new Gson().fromJson(sessionManager.getPriorityTextColors(), new TypeToken<Map<String, String>>(){}.getType());

        prioridadeAdapter = new GenericItemAdapter(
                prioridadesList, 
                themeColor, 
                colors, 
                textColors,
                GenericItemAdapter.STYLE_VERTICAL,
                item -> {
                    playSound();
                    resetInactivityTimer();
                    selectedPrioridadeId = ((Prioridade) item).id;
                    prioridadeAdapter.setSelectedId(selectedPrioridadeId);
                }
        );
        prioridadeAdapter.setSpanCount(spanCount);
        prioridadeAdapter.setSelectedId(selectedPrioridadeId);
        rvPrioridades.setAdapter(prioridadeAdapter);
    }

    private int calculateSpanCount() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp >= 900) return 4;
        if (screenWidthDp >= 600) return 3;
        return 2;
    }

    private void gerarSenha() {
        String nome = etNome.getText().toString().trim();
        String documento = etDocumento.getText().toString().trim();
        String cleanDoc = documento.replaceAll("[^a-zA-Z0-9]", "");

        // Apply final mask to the UI before sending/validating
        if (!cleanDoc.isEmpty()) {
            String finalMasked;
            if (selectedDocType == TYPE_CPF) {
                finalMasked = ValidationUtils.formatCPF(cleanDoc);
            } else if (selectedDocType == TYPE_CNPJ) {
                finalMasked = ValidationUtils.formatCNPJ(cleanDoc);
            } else {
                finalMasked = ValidationUtils.formatPhone(cleanDoc);
            }
            etDocumento.setText(finalMasked);
        }

        if (isRequired && (nome.isEmpty() || documento.isEmpty())) {
            showError("Preencha o campo de identificação");
            return;
        }
        
        if (!documento.isEmpty()) {
            boolean valid = false;
            String errorMessage = "";

            if (selectedDocType == TYPE_CPF) {
                valid = ValidationUtils.isValidCPF(cleanDoc);
                errorMessage = "CPF Inválido. Digite um CPF válido com 11 dígitos";
            } else if (selectedDocType == TYPE_CNPJ) {
                valid = ValidationUtils.isValidCNPJ(cleanDoc);
                errorMessage = "CNPJ Inválido. Digite um CNPJ válido";
            } else if (selectedDocType == TYPE_PHONE) {
                valid = ValidationUtils.isValidPhone(cleanDoc);
                errorMessage = "Telefone Inválido. Digite um número de telefone válido";
            }
            
            if (!valid) {
                showError(errorMessage);
                return;
            }
        }

        resetInactivityTimer();
        TicketRequest request = new TicketRequest();
        request.unidade = sessionManager.getUnidadeId();
        request.servico = servicoId;
        request.prioridade = selectedPrioridadeId;
        if (!nome.isEmpty() || !documento.isEmpty()) {
            request.cliente = new TicketRequest.Cliente(nome, cleanDoc);
        }

        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.distribui(request).enqueue(new Callback<TicketResponse>() {
            @Override
            public void onResponse(Call<TicketResponse> call, Response<TicketResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String pName = "Normal";
                    for (Prioridade p : prioridadesList) {
                        if (p.id == selectedPrioridadeId) {
                            pName = p.nome;
                            break;
                        }
                    }
                    SuccessFragment fragment = SuccessFragment.newInstance(
                            response.body().senha.format,
                            response.body().id,
                            response.body().hash,
                            pName,
                            servicoNome,
                            themeColor
                    );
                    Bundle args = fragment.getArguments();
                    if (args != null) {
                        args.putBoolean("is_facial", isFacial);
                        args.putBoolean("has_nome", hasNome);
                        args.putString("cliente_nome", nome);
                        if (response.body().servico != null && response.body().servico.mensagem != null) {
                            args.putString("servico_mensagem", response.body().servico.mensagem);
                        }
                    }

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment)
                            .commitAllowingStateLoss();
                } else { 
                    showError("Não foi possível processar sua solicitação. Tente novamente.");
                }
            }
            @Override public void onFailure(Call<TicketResponse> call, Throwable t) {
                showError("Verifique sua internet e tente novamente.");
            }
        });
    }

    private void showError(String message) {
        if (tvErrorTriagem != null) {
            tvErrorTriagem.setText(message);
            tvErrorTriagem.setVisibility(View.VISIBLE);
        }
    }

    private void clearError() {
        if (tvErrorTriagem != null) {
            tvErrorTriagem.setVisibility(View.GONE);
        }
    }

    private void playSound() {
        if (!sessionManager.isSoundEnabled()) return;
        try {
            android.view.View view = getView();
            if (view != null) {
                view.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }
        } catch (Exception e) {}
    }
}
