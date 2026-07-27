package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.widget.ImageView;
import br.com.jefferson.totemsga.adapter.GenericItemAdapter;
import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Departamento;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.util.ConfigManager;
import br.com.jefferson.totemsga.util.FeatureParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectionFragment extends BaseKioskFragment {

    private static final String ARG_TYPE = "type";
    private static final String ARG_DEPT_ID = "dept_id";
    private static final String ARG_THEME_COLOR = "theme_color";

    private int type;
    private int deptId;
    private String themeColor;
    private RecyclerView recyclerView;
    private TextView tvTitle;
    private View llError, cardScheduling, cardReprint;
    private TextView tvErrorMessage;
    private Button btnRetry;
    private final Gson gson = new Gson();

    public static SelectionFragment newInstance(int type, int deptId) {
        return newInstance(type, deptId, null);
    }

    public static SelectionFragment newInstance(int type, int deptId, String themeColor) {
        SelectionFragment fragment = new SelectionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        args.putInt(ARG_DEPT_ID, deptId);
        args.putString(ARG_THEME_COLOR, themeColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
            deptId = getArguments().getInt(ARG_DEPT_ID);
            themeColor = getArguments().getString(ARG_THEME_COLOR);
        }
        if (themeColor == null || themeColor.isEmpty()) {
            themeColor = sessionManager.getButtonColor();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_selection, container, false);
        
        try {
            String bgColor = sessionManager.getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty()) {
                view.setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            }
        } catch (Exception e) {
            view.setBackgroundColor(android.graphics.Color.parseColor("#FFF5E1")); // Fallback to peach
        }

        recyclerView = view.findViewById(R.id.rvItems);
        tvTitle = view.findViewById(R.id.tvSelectionTitle);
        llError = view.findViewById(R.id.llError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
        cardScheduling = view.findViewById(R.id.cardScheduling);
        cardReprint = view.findViewById(R.id.cardReprint);

        if (cardScheduling != null) {
            // Only show scheduling button on the first level (Department selection or Service selection without parent)
            if (deptId == -1) {
                cardScheduling.setVisibility(View.VISIBLE);
                cardScheduling.setOnClickListener(v -> {
                    playSound();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.container, new ConfirmSchedulingFragment())
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                });
            } else {
                cardScheduling.setVisibility(View.GONE);
            }
        }

        if (cardReprint != null) {
            if (deptId == -1) {
                cardReprint.setOnClickListener(v -> {
                    playSound();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.container, new ReprintFragment())
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                });
                checkReprintFeature();
            } else {
                cardReprint.setVisibility(View.GONE);
            }
        }

        btnRetry.setOnClickListener(v -> {
            playSound();
            hideError();
            if (type == 1) fetchDepartments();
            else fetchServices();
        });
        
        applyLogoLayout(view);

        Button btnBackSecondary = view.findViewById(R.id.btnBackSelectionSecondary);
        View contentContainer = view.findViewById(R.id.rvItems);
        
        View.OnClickListener backListener = v -> {
            playSound();
            getParentFragmentManager().popBackStack();
        };

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            if (btnBackSecondary != null) {
                btnBackSecondary.setVisibility(View.VISIBLE);
                btnBackSecondary.setOnClickListener(backListener);
                setupBackButton(btnBackSecondary, contentContainer);
            }
        } else {
            if (btnBackSecondary != null) btnBackSecondary.setVisibility(View.GONE);
        }

        applyGlobalSpacing(view);
        return view;
    }

    private void applyLogoLayout(View view) {
        try {
            android.widget.ImageView ivLogo = view.findViewById(R.id.ivLogoSelection);
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
        } catch (Exception e) {
            android.util.Log.e("SelectionFragment", "Erro ao carregar logo: " + e.getMessage());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);

        try {
            int textColor = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            tvTitle.setTextColor(textColor);
            if (ivHeaderIcon != null) ivHeaderIcon.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));
        } catch (Exception e) {}

        if (type == 1) {
            tvTitle.setText("Escolha um\ndepartamento");
            if (ivHeaderIcon != null) ivHeaderIcon.setImageResource(R.drawable.ic_building);
            fetchDepartments();
        } else {
            // Se tiver deptId, pode ser "atendimento" ou "serviço" dependendo do contexto. 
            tvTitle.setText("Escolha um\natendimento");
            if (ivHeaderIcon != null) ivHeaderIcon.setImageResource(R.drawable.ic_people);
            fetchServices();
        }
    }

    private void fetchDepartments() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getDepartamentos().enqueue(new Callback<List<Departamento>>() {
            @Override
            public void onResponse(Call<List<Departamento>> call, Response<List<Departamento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hideError();
                    List<Departamento> filtered = new ArrayList<>();
                    for (Departamento d : response.body()) {
                        // Sincronização Automática: Mostra tudo o que estiver "ativo" no sistema local
                        if (d != null && d.nome != null && d.ativo) {
                            // Filtro de Visibilidade: Deve conter "VISIVEL" na descrição (Confirmação implícita)
                            Map<String, Boolean> features = FeatureParser.parse(d.descricao);
                            if (FeatureParser.isEnabled(features, FeatureParser.VISIVEL)) {
                                filtered.add(d);
                            }
                        }
                    }
                    Collections.sort(filtered, (o1, o2) -> o1.nome.compareToIgnoreCase(o2.nome));
                    setAdapter(filtered);
                } else {
                    showError("Falha ao carregar departamentos (" + response.code() + ")");
                }
            }
            @Override public void onFailure(Call<List<Departamento>> call, Throwable t) {
                showError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    private void fetchServices() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getServicos(sessionManager.getUnidadeId()).enqueue(new Callback<List<ServicoUnidade>>() {
            @Override
            public void onResponse(Call<List<ServicoUnidade>> call, Response<List<ServicoUnidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hideError();
                    List<ServicoUnidade> filtered = new ArrayList<>();
                    for (ServicoUnidade s : response.body()) {
                        // Sincronização Automática: Mostra tudo o que estiver "ativo" no sistema local
                        if (s != null && s.servico != null && s.servico.nome != null && s.ativo && s.servico.ativo) {
                            boolean deptMatch = (deptId == -1 || (s.departamento != null && s.departamento.id == deptId));
                            if (deptMatch) {
                                // Filtro de Visibilidade com Herança: Serviço > Departamento > Default (false)
                                if (resolveFeature(s, FeatureParser.VISIVEL)) {
                                    filtered.add(s);
                                }
                            }
                        }
                    }
                    Collections.sort(filtered, (o1, o2) -> o1.servico.nome.compareToIgnoreCase(o2.servico.nome));
                    setAdapter(filtered);
                } else {
                    showError("Falha ao carregar serviços (" + response.code() + ")");
                }
            }
            @Override public void onFailure(Call<List<ServicoUnidade>> call, Throwable t) {
                showError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        if (!isAdded()) return;
        recyclerView.setVisibility(View.GONE);
        llError.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);

        try {
            int bColor = android.graphics.Color.parseColor(sessionManager.getButtonColor());
            int tColor = android.graphics.Color.parseColor(sessionManager.getButtonTextColor());
            btnRetry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bColor));
            btnRetry.setTextColor(tColor);
            tvErrorMessage.setTextColor(android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor()));
        } catch (Exception e) {}
    }

    private void hideError() {
        if (!isAdded()) return;
        recyclerView.setVisibility(View.VISIBLE);
        llError.setVisibility(View.GONE);
    }

    private void setAdapter(List<?> items) {
        if (!isAdded()) return;
        
        int spanCount = calculateSpanCount();
        if (items.size() > 0 && items.size() < spanCount) {
            spanCount = items.size();
        }

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.STRETCH);
        recyclerView.setLayoutManager(layoutManager);
        
        // Implement Color Hierarchy
        Map<String, String> finalColors = new java.util.HashMap<>();
        Map<String, String> finalTextColors = new java.util.HashMap<>();
        
        Map<String, String> individualColors = gson.fromJson(
                (type == 1) ? sessionManager.getDeptColors() : sessionManager.getServiceColors(),
                new TypeToken<Map<String, String>>(){}.getType()
        );
        if (individualColors == null) individualColors = new java.util.HashMap<>();
        
        Map<String, String> individualTextColors = gson.fromJson(
                (type == 1) ? sessionManager.getDeptTextColors() : sessionManager.getServiceTextColors(),
                new TypeToken<Map<String, String>>(){}.getType()
        );
        if (individualTextColors == null) individualTextColors = new java.util.HashMap<>();

        Map<String, String> deptColors = gson.fromJson(sessionManager.getDeptColors(), new TypeToken<Map<String, String>>(){}.getType());
        if (deptColors == null) deptColors = new java.util.HashMap<>();

        Map<String, String> deptTextColors = gson.fromJson(sessionManager.getDeptTextColors(), new TypeToken<Map<String, String>>(){}.getType());
        if (deptTextColors == null) deptTextColors = new java.util.HashMap<>();

        for (Object item : items) {
            String itemId;
            String color = null;
            String textColor = null;

            if (item instanceof Departamento) {
                itemId = String.valueOf(((Departamento) item).id);
                color = individualColors.get(itemId);
                textColor = individualTextColors.get(itemId);
            } else if (item instanceof ServicoUnidade) {
                ServicoUnidade s = (ServicoUnidade) item;
                itemId = String.valueOf(s.servico.id);
                
                // 1. Individual Service Color
                String indColor = individualColors.get(itemId);
                String indTextColor = individualTextColors.get(itemId);
                
                if (indColor != null && !indColor.isEmpty()) color = indColor;
                if (indTextColor != null && !indTextColor.isEmpty()) textColor = indTextColor;
                
                // 2. Parent Department Color (Inheritance)
                // Use the themeColor passed to this fragment (which is the color of the selected Dept)
                if (color == null || color.isEmpty()) {
                    color = themeColor;
                }
                
                // Also try to inherit text color from the department if available
                if (textColor == null || textColor.isEmpty()) {
                    String effectiveDeptId = (s.departamento != null) ? String.valueOf(s.departamento.id) : String.valueOf(deptId);
                    if (!effectiveDeptId.equals("-1")) {
                        String dTextColor = deptTextColors.get(effectiveDeptId);
                        if (dTextColor != null && !dTextColor.isEmpty()) textColor = dTextColor;
                    }
                }
            } else {
                continue;
            }

            // 3. Global Color
            if (color == null || color.isEmpty()) color = themeColor;
            if (textColor == null || textColor.isEmpty()) textColor = sessionManager.getButtonTextColor();

            finalColors.put(itemId, color);
            finalTextColors.put(itemId, textColor);
        }

        int viewStyle = (type == 1) ? GenericItemAdapter.STYLE_HORIZONTAL : GenericItemAdapter.STYLE_VERTICAL;
        GenericItemAdapter adapter = new GenericItemAdapter(items, themeColor, finalColors, finalTextColors, viewStyle, item -> {
            playSound();
            if (item instanceof Departamento) {
                String dColor = finalColors.get(String.valueOf(((Departamento) item).id));
                if (dColor == null) dColor = themeColor;
                
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.container, SelectionFragment.newInstance(2, ((Departamento) item).id, dColor))
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            } else if (item instanceof ServicoUnidade) {
                handleServiceSelection((ServicoUnidade) item);
            }
        });
        adapter.setSpanCount(spanCount);
        recyclerView.setAdapter(adapter);
    }

    private int calculateSpanCount() {
        if (type == 1) {
            return Math.max(1, sessionManager.getDeptGrid());
        } else {
            return Math.max(1, sessionManager.getServiceGrid());
        }
    }

    private void handleServiceSelection(ServicoUnidade servico) {
        // Resolve features using inheritance: Service > Department > Default (false)
        boolean hasFacial = resolveFeature(servico, FeatureParser.FACIAL);
        boolean hasTriagem = resolveFeature(servico, FeatureParser.TRIAGEM);
        boolean isTriagemObrigatoria = resolveFeature(servico, FeatureParser.TRIAGEM_OBRIGATORIA);
        boolean hasPrioridade = resolveFeature(servico, FeatureParser.PRIORIDADE);
        boolean hasNome = resolveFeature(servico, FeatureParser.NOME);
        
        if (hasTriagem) {
            ScreeningFragment fragment = ScreeningFragment.newInstance(
                    servico.servico.id, 
                    isTriagemObrigatoria, 
                    hasPrioridade, 
                    servico.servico.nome, 
                    themeColor
            );
            Bundle args = fragment.getArguments();
            if (args != null) {
                args.putBoolean("is_facial", hasFacial);
                args.putBoolean("is_nome", hasNome);
                if (servico.mensagem != null && !servico.mensagem.isEmpty()) {
                    args.putString("servico_mensagem", servico.mensagem);
                }
            }
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } else {
            String msg = servico.mensagem;
            issueTicket(servico.servico.id, 1, servico.servico.nome, "Normal", hasFacial, hasNome, msg);
        }
    }

    private boolean resolveFeature(ServicoUnidade s, String feature) {
        // 1. Check Service
        Map<String, Boolean> sFeatures = FeatureParser.parse(s.servico.descricao);
        if (sFeatures.containsKey(feature)) {
            Boolean val = sFeatures.get(feature);
            return val != null && val;
        }

        // 2. Check Department
        if (s.departamento != null) {
            Map<String, Boolean> dFeatures = FeatureParser.parse(s.departamento.descricao);
            if (dFeatures.containsKey(feature)) {
                Boolean val = dFeatures.get(feature);
                return val != null && val;
            }
        }
        
        return false;
    }

    private void issueTicket(int servicoId, int prioridadeId, String serviceName, String priorityName, boolean isFacial, boolean hasNome, String message) {
        br.com.jefferson.totemsga.model.TicketRequest request = new br.com.jefferson.totemsga.model.TicketRequest();
        request.unidade = sessionManager.getUnidadeId();
        request.servico = servicoId;
        request.prioridade = prioridadeId;

        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.distribui(request).enqueue(new Callback<br.com.jefferson.totemsga.model.TicketResponse>() {
            @Override
            public void onResponse(Call<br.com.jefferson.totemsga.model.TicketResponse> call, Response<br.com.jefferson.totemsga.model.TicketResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SuccessFragment fragment = SuccessFragment.newInstance(
                            response.body().senha.format,
                            response.body().id,
                            response.body().hash,
                            priorityName,
                            serviceName,
                            themeColor
                    );
                    Bundle args = fragment.getArguments();
                    if (args != null) {
                        args.putBoolean("is_facial", isFacial);
                        args.putBoolean("has_nome", hasNome);
                        if (message != null && !message.isEmpty()) {
                            args.putString("servico_mensagem", message);
                        } else if (response.body().mensagem != null) {
                            args.putString("servico_mensagem", response.body().mensagem);
                        }
                    }

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment)
                            .commitAllowingStateLoss();
                }
            }
            @Override public void onFailure(Call<br.com.jefferson.totemsga.model.TicketResponse> call, Throwable t) {}
        });
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

    private void checkReprintFeature() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getServicos(sessionManager.getUnidadeId()).enqueue(new Callback<List<ServicoUnidade>>() {
            @Override
            public void onResponse(Call<List<ServicoUnidade>> call, Response<List<ServicoUnidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean enabled = false;
                    for (ServicoUnidade s : response.body()) {
                        if (resolveFeature(s, FeatureParser.SEGUNDAVIA)) {
                            enabled = true;
                            break;
                        }
                    }
                    if (isAdded()) {
                        final boolean finalEnabled = enabled;
                        requireActivity().runOnUiThread(() -> {
                            if (cardReprint != null) cardReprint.setVisibility(finalEnabled ? View.VISIBLE : View.GONE);
                        });
                    }
                }
            }
            @Override public void onFailure(Call<List<ServicoUnidade>> call, Throwable t) {}
        });
    }
}
