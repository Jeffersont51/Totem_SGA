package br.com.jefferson.totemsga;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.model.Departamento;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.util.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LayoutConfigActivity extends BaseActivity {

    private ViewFlipper viewFlipper;
    private TextView tvStepTitle;
    private Button btnPrev, btnNext;
    private int currentStep = 0;

    private SessionManager sessionManager;
    private final Gson gson = new Gson();

    // Step 1
    private TextInputEditText etLogoUrl, etLogoWidth, etLogoHeight, etButtonHeight, etButtonFontSize, etBackWidthPercent, etBackHeight, etBackFontSize,
            etTopGradientHeight, etBottomGradientHeight, etLogoMarginTop, etTitleMarginTop;
    private android.widget.Spinner spinnerBackPosition, spinnerBackAlignment, spinnerFlowDirection, spinnerFlowSpeed;
    private SwitchMaterial switchGroupByDept, switchSoundEnabled;
    private MaterialCheckBox cbSelectAllDepts, cbSelectAllServices;
    private String bgColor, bgTextColor, btnColor, btnTextColor;
    private View previewContainer;
    private ImageView ivPreviewLogo;
    private TextView tvPreviewText;

    // Data
    private List<Departamento> allDepts = new ArrayList<>();
    private List<ServicoUnidade> allServices = new ArrayList<>();
    private List<br.com.jefferson.totemsga.model.Prioridade> allPriorities = new ArrayList<>();
    
    private List<Integer> selectedDepts = new ArrayList<>();
    private List<Integer> selectedServices = new ArrayList<>();
    
    private Map<String, String> deptColors = new HashMap<>();
    private Map<String, String> serviceColors = new HashMap<>();
    private Map<String, String> priorityColors = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_config);

        sessionManager = new SessionManager(this);
        
        viewFlipper = findViewById(R.id.viewFlipper);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        findViewById(R.id.btnSaveQuick).setOnClickListener(v -> saveAndFinish());

        // Step 1 views
        etLogoUrl = findViewById(R.id.etLogoUrlStep1);
        etLogoWidth = findViewById(R.id.etLogoWidth);
        etLogoHeight = findViewById(R.id.etLogoHeight);
        etButtonHeight = findViewById(R.id.etButtonHeight);
        etButtonFontSize = findViewById(R.id.etButtonFontSize);
        etBackWidthPercent = findViewById(R.id.etBackWidthPercent);
        etBackHeight = findViewById(R.id.etBackHeight);
        etBackFontSize = findViewById(R.id.etBackFontSize);
        etTopGradientHeight = findViewById(R.id.etTopGradientHeight);
        etBottomGradientHeight = findViewById(R.id.etBottomGradientHeight);
        etLogoMarginTop = findViewById(R.id.etLogoMarginTop);
        etTitleMarginTop = findViewById(R.id.etTitleMarginTop);
        spinnerBackPosition = findViewById(R.id.spinnerBackPosition);
        spinnerBackAlignment = findViewById(R.id.spinnerBackAlignment);
        spinnerFlowDirection = findViewById(R.id.spinnerFlowDirection);
        spinnerFlowSpeed = findViewById(R.id.spinnerFlowSpeed);
        
        android.widget.ArrayAdapter<String> adapterPos = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Abaixo dos Botões", "Rodapé Fixo"});
        spinnerBackPosition.setAdapter(adapterPos);

        android.widget.ArrayAdapter<String> adapterAlign = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Esquerda", "Centro", "Direita"});
        spinnerBackAlignment.setAdapter(adapterAlign);

        android.widget.ArrayAdapter<String> adapterFlowDir = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Esquerda → Direita", "Direita → Esquerda", "Cima → Baixo", "Baixo → Cima"});
        spinnerFlowDirection.setAdapter(adapterFlowDir);

        android.widget.ArrayAdapter<String> adapterFlowSpeed = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Lenta (8s)", "Média (4s)", "Rápida (2s)"});
        spinnerFlowSpeed.setAdapter(adapterFlowSpeed);

        switchGroupByDept = findViewById(R.id.switchGroupByDeptStep1);
        switchSoundEnabled = findViewById(R.id.switchSoundEnabled);
        cbSelectAllDepts = findViewById(R.id.cbSelectAllDepts);
        cbSelectAllServices = findViewById(R.id.cbSelectAllServices);
        
        previewContainer = findViewById(R.id.previewContainer);
        ivPreviewLogo = findViewById(R.id.ivPreviewLogo);
        tvPreviewText = findViewById(R.id.tvPreviewText);
        
        findViewById(R.id.btnBackgroundColor).setOnClickListener(v -> showColorPicker("Cor de Fundo", bgColor, color -> {
            bgColor = color;
            updatePreview();
        }));

        findViewById(R.id.btnBackgroundTextColor).setOnClickListener(v -> showColorPicker("Cor do Texto", bgTextColor, color -> {
            bgTextColor = color;
            updatePreview();
        }));

        findViewById(R.id.btnGlobalButtonColor).setOnClickListener(v -> showColorPicker("Cor dos Botões", btnColor, color -> {
            btnColor = color;
            updatePreview();
        }));

        findViewById(R.id.btnGlobalButtonTextColor).setOnClickListener(v -> showColorPicker("Cor do Texto Botões", btnTextColor, color -> {
            btnTextColor = color;
            updatePreview();
        }));

        etLogoUrl.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { updatePreview(); }
        });

        android.text.TextWatcher previewWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { updatePreview(); }
        };
        etLogoWidth.addTextChangedListener(previewWatcher);
        etLogoHeight.addTextChangedListener(previewWatcher);

        loadInitialData();

        btnPrev.setOnClickListener(v -> prevStep());
        btnNext.setOnClickListener(v -> nextStep());
        
        updateStep();
    }

    private void loadInitialData() {
        etLogoUrl.setText(sessionManager.getLogoUrl());
        etLogoWidth.setText(String.valueOf(sessionManager.getLogoWidth()));
        etLogoHeight.setText(String.valueOf(sessionManager.getLogoHeight()));
        etButtonHeight.setText(String.valueOf(sessionManager.getButtonHeight()));
        etButtonFontSize.setText(String.valueOf(sessionManager.getButtonFontSize()));
        etBackWidthPercent.setText(String.valueOf(sessionManager.getBackButtonWidthPercent()));
        etBackHeight.setText(String.valueOf(sessionManager.getBackButtonHeight()));
        etBackFontSize.setText(String.valueOf(sessionManager.getBackButtonFontSize()));
        etTopGradientHeight.setText(String.valueOf(sessionManager.getTopGradientHeight()));
        etBottomGradientHeight.setText(String.valueOf(sessionManager.getBottomGradientHeight()));
        etLogoMarginTop.setText(String.valueOf(sessionManager.getLogoMarginTop()));
        etTitleMarginTop.setText(String.valueOf(sessionManager.getTitleMarginTop()));
        spinnerBackPosition.setSelection(sessionManager.getBackButtonPosition());
        spinnerBackAlignment.setSelection(sessionManager.getBackButtonAlignment());
        spinnerFlowDirection.setSelection(sessionManager.getFlowDirection());
        spinnerFlowSpeed.setSelection(sessionManager.getFlowSpeed());
        
        switchGroupByDept.setChecked(sessionManager.isGroupByDept());
        switchSoundEnabled.setChecked(sessionManager.isSoundEnabled());
        
        bgColor = sessionManager.getBackgroundColor();
        bgTextColor = sessionManager.getBackgroundTextColor();
        btnColor = sessionManager.getButtonColor();
        btnTextColor = sessionManager.getButtonTextColor();
        updatePreview();
        
        selectedDepts = gson.fromJson(sessionManager.getSelectedDepts(), new TypeToken<List<Integer>>(){}.getType());
        selectedServices = gson.fromJson(sessionManager.getSelectedServices(), new TypeToken<List<Integer>>(){}.getType());
        deptColors = gson.fromJson(sessionManager.getDeptColors(), new TypeToken<Map<String, String>>(){}.getType());
        serviceColors = gson.fromJson(sessionManager.getServiceColors(), new TypeToken<Map<String, String>>(){}.getType());
        priorityColors = gson.fromJson(sessionManager.getPriorityColors(), new TypeToken<Map<String, String>>(){}.getType());
        
        ((TextInputEditText)findViewById(R.id.etDeptGrid)).setText(String.valueOf(sessionManager.getDeptGrid()));
        ((TextInputEditText)findViewById(R.id.etServiceGrid)).setText(String.valueOf(sessionManager.getServiceGrid()));

        fetchApiData();
    }

    private void updatePreview() {
        try {
            previewContainer.setBackgroundColor(Color.parseColor(bgColor));
        } catch (Exception e) {
            previewContainer.setBackgroundColor(Color.WHITE);
        }

        try {
            tvPreviewText.setTextColor(Color.parseColor(bgTextColor));
        } catch (Exception e) {
            tvPreviewText.setTextColor(Color.BLACK);
        }

        try {
            Button btnNextPreview = findViewById(R.id.btnNext);
            int bColor = Color.parseColor(btnColor);
            int bTextColor = Color.parseColor(btnTextColor);
            btnNextPreview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bColor));
            btnNextPreview.setTextColor(bTextColor);
            
            Button btnPrevPreview = findViewById(R.id.btnPrev);
            btnPrevPreview.setTextColor(bColor);
        } catch (Exception e) {}
        
        String url = etLogoUrl.getText().toString();
        if (!url.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(url).into(ivPreviewLogo);
        } else {
            ivPreviewLogo.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        try {
            int w = Integer.parseInt(etLogoWidth.getText().toString());
            int h = Integer.parseInt(etLogoHeight.getText().toString());
            float density = getResources().getDisplayMetrics().density;
            ViewGroup.LayoutParams lp = ivPreviewLogo.getLayoutParams();
            lp.width = (int) (w * density);
            lp.height = (int) (h * density);
            ivPreviewLogo.setLayoutParams(lp);
        } catch (Exception e) {}
    }

    private void fetchApiData() {
        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;

        api.getDepartamentos().enqueue(new Callback<List<Departamento>>() {
            @Override
            public void onResponse(Call<List<Departamento>> call, Response<List<Departamento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allDepts = response.body().stream()
                            .filter(d -> d.ativo)
                            .collect(Collectors.toList());
                    
                    // Sempre sincroniza: tudo que está ativo no sistema fica selecionado no Totem
                    selectedDepts.clear();
                    for (Departamento d : allDepts) {
                        selectedDepts.add(d.id);
                    }
                    setupDeptsLists();
                }
            }
            @Override public void onFailure(Call<List<Departamento>> call, Throwable t) {}
        });

        api.getServicos(sessionManager.getUnidadeId()).enqueue(new Callback<List<ServicoUnidade>>() {
            @Override
            public void onResponse(Call<List<ServicoUnidade>> call, Response<List<ServicoUnidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allServices = response.body().stream()
                            .filter(s -> s.ativo && s.servico.ativo)
                            .collect(Collectors.toList());

                    // Sempre sincroniza: tudo que está ativo no sistema fica selecionado no Totem
                    selectedServices.clear();
                    for (ServicoUnidade s : allServices) {
                        selectedServices.add(s.servico.id);
                    }
                    setupServicesLists();
                }
            }
            @Override public void onFailure(Call<List<ServicoUnidade>> call, Throwable t) {}
        });

        api.getPrioridades().enqueue(new Callback<List<br.com.jefferson.totemsga.model.Prioridade>>() {
            @Override
            public void onResponse(Call<List<br.com.jefferson.totemsga.model.Prioridade>> call, Response<List<br.com.jefferson.totemsga.model.Prioridade>> response) {
                if (response.isSuccessful() && response.body() != null) {
//                    allPriorities = response.body();
                    allPriorities = response.body()
                            .stream()
                            .filter(prioridade -> prioridade.ativo)
                            .collect(Collectors.toList());

                    setupPrioritiesLayout();
                }
            }
            @Override public void onFailure(Call<List<br.com.jefferson.totemsga.model.Prioridade>> call, Throwable t) {}
        });
    }

    private void setupPrioritiesLayout() {
        RecyclerView rvLayout = findViewById(R.id.rvPrioritiesLayout);
        rvLayout.setLayoutManager(new GridLayoutManager(this, 2));
        
        Map<String, String> textColors = gson.fromJson(sessionManager.getPriorityTextColors(), new TypeToken<Map<String, String>>(){}.getType());
        List<Integer> allIds = new ArrayList<>();
        for(br.com.jefferson.totemsga.model.Prioridade p : allPriorities) allIds.add(p.id);

        rvLayout.setAdapter(new ColorAdapter(allPriorities, allIds, priorityColors, textColors, null, null, null, false, true));
    }

    private void setupDeptsLists() {
        RecyclerView rvFilter = findViewById(R.id.rvDeptsFilter);
        rvFilter.setLayoutManager(new LinearLayoutManager(this));
        rvFilter.setAdapter(new SelectionAdapter(allDepts, selectedDepts, true));

        cbSelectAllDepts.setOnCheckedChangeListener(null);
        cbSelectAllDepts.setChecked(selectedDepts.size() == allDepts.size() && !allDepts.isEmpty());
        cbSelectAllDepts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedDepts.clear();
            if (isChecked) {
                for (br.com.jefferson.totemsga.model.Departamento d : allDepts) selectedDepts.add(d.id);
            }
            rvFilter.getAdapter().notifyDataSetChanged();
            setupDeptsLayout();
        });

        TextInputEditText etGrid = findViewById(R.id.etDeptGrid);
        etGrid.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { setupDeptsLayout(); }
        });

        setupDeptsLayout();
    }

    private void setupDeptsLayout() {
        RecyclerView rvLayout = findViewById(R.id.rvDeptsLayout);
        String gridVal = ((TextInputEditText)findViewById(R.id.etDeptGrid)).getText().toString();
        int cols = gridVal.isEmpty() ? 2 : Integer.parseInt(gridVal);
        rvLayout.setLayoutManager(new GridLayoutManager(this, cols > 0 ? cols : 2));
        
        Map<String, String> textColors = gson.fromJson(sessionManager.getDeptTextColors(), new TypeToken<Map<String, String>>(){}.getType());
        Map<String, Boolean> screeningEnabled = gson.fromJson(sessionManager.getDeptScreeningEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
        Map<String, Boolean> screeningRequired = gson.fromJson(sessionManager.getDeptScreeningRequired(), new TypeToken<Map<String, Boolean>>(){}.getType());
        Map<String, Boolean> priorityEnabled = gson.fromJson(sessionManager.getDeptPriorityEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
        
        rvLayout.setAdapter(new ColorAdapter(allDepts, selectedDepts, deptColors, textColors, screeningEnabled, screeningRequired, priorityEnabled, true));
    }

    private void setupServicesLists() {
        RecyclerView rvFilter = findViewById(R.id.rvServicesFilter);
        rvFilter.setLayoutManager(new LinearLayoutManager(this));
        rvFilter.setAdapter(new SelectionAdapter(allServices, selectedServices, false));

        cbSelectAllServices.setOnCheckedChangeListener(null);
        cbSelectAllServices.setChecked(selectedServices.size() == allServices.size() && !allServices.isEmpty());
        cbSelectAllServices.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedServices.clear();
            if (isChecked) {
                for (br.com.jefferson.totemsga.model.ServicoUnidade s : allServices) selectedServices.add(s.servico.id);
            }
            rvFilter.getAdapter().notifyDataSetChanged();
            setupServicesLayout();
        });

        TextInputEditText etGrid = findViewById(R.id.etServiceGrid);
        etGrid.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { setupServicesLayout(); }
        });

        setupServicesLayout();
    }

    private void setupServicesLayout() {
        RecyclerView rvLayout = findViewById(R.id.rvServicesLayout);
        String gridVal = ((TextInputEditText)findViewById(R.id.etServiceGrid)).getText().toString();
        int cols = gridVal.isEmpty() ? 2 : Integer.parseInt(gridVal);
        rvLayout.setLayoutManager(new GridLayoutManager(this, cols > 0 ? cols : 2));
        
        Map<String, String> textColors = gson.fromJson(sessionManager.getServiceTextColors(), new TypeToken<Map<String, String>>(){}.getType());
        Map<String, Boolean> screeningEnabled = gson.fromJson(sessionManager.getServiceScreeningEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
        Map<String, Boolean> screeningRequired = gson.fromJson(sessionManager.getServiceScreeningRequired(), new TypeToken<Map<String, Boolean>>(){}.getType());
        Map<String, Boolean> priorityEnabled = gson.fromJson(sessionManager.getServicePriorityEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());

        rvLayout.setAdapter(new ColorAdapter(allServices, selectedServices, serviceColors, textColors, screeningEnabled, screeningRequired, priorityEnabled, false));
    }

    private void updateSelectAllStates() {
        cbSelectAllDepts.setOnCheckedChangeListener(null);
        cbSelectAllDepts.setChecked(selectedDepts.size() == allDepts.size() && !allDepts.isEmpty());
        cbSelectAllDepts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedDepts.clear();
            if (isChecked) {
                for (br.com.jefferson.totemsga.model.Departamento d : allDepts) selectedDepts.add(d.id);
            }
            RecyclerView rv = findViewById(R.id.rvDeptsFilter);
            if (rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
            setupDeptsLayout();
        });

        cbSelectAllServices.setOnCheckedChangeListener(null);
        cbSelectAllServices.setChecked(selectedServices.size() == allServices.size() && !allServices.isEmpty());
        cbSelectAllServices.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedServices.clear();
            if (isChecked) {
                for (br.com.jefferson.totemsga.model.ServicoUnidade s : allServices) selectedServices.add(s.servico.id);
            }
            RecyclerView rv = findViewById(R.id.rvServicesFilter);
            if (rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
            setupServicesLayout();
        });
    }

    private void nextStep() {
        if (currentStep == 0) {
            if (!switchGroupByDept.isChecked()) {
                currentStep = 3; // Skip dept steps
            } else {
                currentStep++;
            }
        } else if (currentStep == 5) {
            saveAndFinish();
            return;
        } else {
            currentStep++;
        }
        updateStep();
    }

    private void prevStep() {
        if (currentStep == 3 && !switchGroupByDept.isChecked()) {
            currentStep = 0;
        } else if (currentStep > 0) {
            currentStep--;
        }
        updateStep();
    }

    private void updateStep() {
        viewFlipper.setDisplayedChild(currentStep);
        btnPrev.setVisibility(currentStep == 0 ? View.GONE : View.VISIBLE);
        btnNext.setText(currentStep == 5 ? "Finalizar" : "Próximo");
        
        switch (currentStep) {
            case 0: tvStepTitle.setText("Passo 1: Geral"); break;
            case 1: tvStepTitle.setText("Passo 2: Departamentos"); updateSelectAllStates(); break;
            case 2: tvStepTitle.setText("Passo 3: Layout Departamentos"); setupDeptsLists(); break;
            case 3: tvStepTitle.setText("Passo 4: Serviços"); updateSelectAllStates(); break;
            case 4: tvStepTitle.setText("Passo 5: Layout Serviços"); setupServicesLists(); break;
            case 5: tvStepTitle.setText("Passo 6: Cores de Prioridade"); setupPrioritiesLayout(); break;
        }
    }

    private void saveAndFinish() {
        android.content.SharedPreferences.Editor editor = sessionManager.getEditor();
        
        editor.putString("logo_url", etLogoUrl.getText().toString());

        editor.putInt("logo_width", parseIntField(etLogoWidth, sessionManager.getLogoWidth()));
        editor.putInt("logo_height", parseIntField(etLogoHeight, sessionManager.getLogoHeight()));
        editor.putInt("button_height", parseIntField(etButtonHeight, sessionManager.getButtonHeight()));
        editor.putInt("button_font_size", parseIntField(etButtonFontSize, sessionManager.getButtonFontSize()));
        editor.putInt("back_button_width_percent", parseIntField(etBackWidthPercent, sessionManager.getBackButtonWidthPercent()));
        editor.putInt("back_button_height", parseIntField(etBackHeight, sessionManager.getBackButtonHeight()));
        editor.putInt("back_button_font_size", parseIntField(etBackFontSize, sessionManager.getBackButtonFontSize()));
        editor.putInt("back_button_position", spinnerBackPosition.getSelectedItemPosition());
        editor.putInt("back_button_alignment", spinnerBackAlignment.getSelectedItemPosition());
        editor.putInt("flow_direction", spinnerFlowDirection.getSelectedItemPosition());
        editor.putInt("flow_speed", spinnerFlowSpeed.getSelectedItemPosition());
        editor.putInt("top_gradient_height", parseIntField(etTopGradientHeight, sessionManager.getTopGradientHeight()));
        editor.putInt("bottom_gradient_height", parseIntField(etBottomGradientHeight, sessionManager.getBottomGradientHeight()));
        editor.putInt("logo_margin_top", parseIntField(etLogoMarginTop, sessionManager.getLogoMarginTop()));
        editor.putInt("title_margin_top", parseIntField(etTitleMarginTop, sessionManager.getTitleMarginTop()));

        editor.putBoolean("sound_enabled", switchSoundEnabled.isChecked());
        editor.putBoolean("group_by_dept", switchGroupByDept.isChecked());
        editor.putString("background_color", bgColor);
        editor.putString("background_text_color", bgTextColor);
        editor.putString("button_color", btnColor);
        editor.putString("primary_color", btnColor); // Sincroniza cor do tema com cor dos botões
        editor.putString("button_text_color", btnTextColor);
        editor.putString("selected_depts", gson.toJson(selectedDepts));
        editor.putString("selected_services", gson.toJson(selectedServices));
        editor.putString("dept_colors", gson.toJson(deptColors));
        editor.putString("service_colors", gson.toJson(serviceColors));
        
        RecyclerView rvDepts = findViewById(R.id.rvDeptsLayout);
        if (rvDepts.getAdapter() instanceof ColorAdapter) {
            ColorAdapter adapter = (ColorAdapter) rvDepts.getAdapter();
            editor.putString("dept_text_colors", gson.toJson(adapter.textColors));
            editor.putString("dept_screening_enabled", gson.toJson(adapter.screeningEnabled));
            editor.putString("dept_screening_required", gson.toJson(adapter.screeningRequired));
            editor.putString("dept_priority_enabled", gson.toJson(adapter.priorityEnabled));
        }
        
        RecyclerView rvServices = findViewById(R.id.rvServicesLayout);
        if (rvServices.getAdapter() instanceof ColorAdapter) {
            ColorAdapter adapter = (ColorAdapter) rvServices.getAdapter();
            editor.putString("service_text_colors", gson.toJson(adapter.textColors));
            editor.putString("service_screening_enabled", gson.toJson(adapter.screeningEnabled));
            editor.putString("service_screening_required", gson.toJson(adapter.screeningRequired));
            editor.putString("service_priority_enabled", gson.toJson(adapter.priorityEnabled));
        }
        
        String dGrid = ((TextInputEditText)findViewById(R.id.etDeptGrid)).getText().toString();
        editor.putInt("dept_grid", dGrid.isEmpty() ? 2 : Integer.parseInt(dGrid));
        
        String sGrid = ((TextInputEditText)findViewById(R.id.etServiceGrid)).getText().toString();
        editor.putInt("service_grid", sGrid.isEmpty() ? 2 : Integer.parseInt(sGrid));

        RecyclerView rvPriorities = findViewById(R.id.rvPrioritiesLayout);
        if (rvPriorities.getAdapter() instanceof ColorAdapter) {
            ColorAdapter adapter = (ColorAdapter) rvPriorities.getAdapter();
            editor.putString("priority_colors", gson.toJson(adapter.colors));
            editor.putString("priority_text_colors", gson.toJson(adapter.textColors));
        }

        editor.commit(); // Use commit instead of apply for immediate disk write
        Toast.makeText(this, "Configurações de layout salvas", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int parseIntField(TextInputEditText field, int fallback) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void showColorPicker(String title, String currentColor, ColorSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        EditText etColor = view.findViewById(R.id.etColorHex);
        etColor.setText(currentColor);

        SeekBar sbHue = view.findViewById(R.id.sbHue);
        SeekBar sbSat = view.findViewById(R.id.sbSaturation);
        SeekBar sbVal = view.findViewById(R.id.sbValue);
        View vPreview = view.findViewById(R.id.vColorPreview);

        float[] hsv = new float[3];
        try { Color.colorToHSV(Color.parseColor(currentColor), hsv); } catch (Exception e) { hsv[0] = 0; hsv[1] = 0; hsv[2] = 1; }
        
        sbHue.setProgress((int) hsv[0]);
        sbSat.setProgress((int) (hsv[1] * 100));
        sbVal.setProgress((int) (hsv[2] * 100));
        vPreview.setBackgroundColor(Color.HSVToColor(hsv));

        SeekBar.OnSeekBarChangeListener colorChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    hsv[0] = sbHue.getProgress();
                    hsv[1] = sbSat.getProgress() / 100f;
                    hsv[2] = sbVal.getProgress() / 100f;
                    int color = Color.HSVToColor(hsv);
                    vPreview.setBackgroundColor(color);
                    etColor.setText(String.format("#%06X", (0xFFFFFF & color)));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        sbHue.setOnSeekBarChangeListener(colorChangeListener);
        sbSat.setOnSeekBarChangeListener(colorChangeListener);
        sbVal.setOnSeekBarChangeListener(colorChangeListener);

        ViewGroup colorGrid = view.findViewById(R.id.colorGrid);
        for (int i = 0; i < colorGrid.getChildCount(); i++) {
            View colorView = colorGrid.getChildAt(i);
            colorView.setOnClickListener(v -> {
                String hex = v.getTag().toString();
                etColor.setText(hex);
                try {
                    int c = Color.parseColor(hex);
                    vPreview.setBackgroundColor(c);
                    Color.colorToHSV(c, hsv);
                    sbHue.setProgress((int) hsv[0]);
                    sbSat.setProgress((int) (hsv[1] * 100));
                    sbVal.setProgress((int) (hsv[2] * 100));
                } catch (Exception e) {}
            });
        }
        
        builder.setView(view);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String color = etColor.getText().toString();
            try {
                Color.parseColor(color);
                listener.onColorSelected(color);
            } catch (Exception e) {
                Toast.makeText(this, "Cor inválida", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    interface ColorSelectedListener {
        void onColorSelected(String color);
    }

    // Adapters
    class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {
        List<?> items;
        List<Integer> selected;
        boolean isDept;

        SelectionAdapter(List<?> items, List<Integer> selected, boolean isDept) {
            this.items = items;
            this.selected = selected;
            this.isDept = isDept;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object item = items.get(position);
            int id = isDept ? ((Departamento)item).id : ((ServicoUnidade)item).servico.id;
            String name = isDept ? ((Departamento)item).nome : ((ServicoUnidade)item).servico.nome;
            
            android.widget.CheckedTextView ctv = (android.widget.CheckedTextView) holder.itemView;
            ctv.setText(name);
            ctv.setChecked(selected.contains(id));
            
            holder.itemView.setOnClickListener(v -> {
                if (selected.contains(id)) selected.remove(Integer.valueOf(id));
                else selected.add(id);
                notifyItemChanged(position);
                updateSelectAllStates();
                if (isDept) setupDeptsLayout(); else setupServicesLayout();
            });
        }

        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }

    class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
        List<?> items;
        List<Integer> selected;
        Map<String, String> colors;
        Map<String, String> textColors;
        Map<String, Boolean> screeningEnabled;
        Map<String, Boolean> screeningRequired;
        Map<String, Boolean> priorityEnabled;
        boolean isDept;
        boolean isPriority = false;

        ColorAdapter(List<?> items, List<Integer> selected, Map<String, String> colors, Map<String, String> textColors, 
                     Map<String, Boolean> screeningEnabled, Map<String, Boolean> screeningRequired, 
                     Map<String, Boolean> priorityEnabled, boolean isDept) {
            this(items, selected, colors, textColors, screeningEnabled, screeningRequired, priorityEnabled, isDept, false);
        }

        ColorAdapter(List<?> items, List<Integer> selected, Map<String, String> colors, Map<String, String> textColors, 
                     Map<String, Boolean> screeningEnabled, Map<String, Boolean> screeningRequired, 
                     Map<String, Boolean> priorityEnabled, boolean isDept, boolean isPriority) {
            this.items = new ArrayList<>();
            this.isPriority = isPriority;
            for (Object o : items) {
                int id;
                if (isPriority) id = ((br.com.jefferson.totemsga.model.Prioridade)o).id;
                else id = isDept ? ((Departamento)o).id : ((ServicoUnidade)o).servico.id;
                
                if (selected.contains(id)) ((List<Object>)this.items).add(o);
            }
            this.selected = selected;
            this.colors = colors;
            this.textColors = textColors != null ? textColors : new HashMap<>();
            this.screeningEnabled = screeningEnabled != null ? screeningEnabled : new HashMap<>();
            this.screeningRequired = screeningRequired != null ? screeningRequired : new HashMap<>();
            this.priorityEnabled = priorityEnabled != null ? priorityEnabled : new HashMap<>();
            this.isDept = isDept;

            // Default behavior: if it's a new entry and dept has screening, enable it for service
            if (!isDept && !isPriority) {
                Map<String, Boolean> deptEnabled = gson.fromJson(sessionManager.getDeptScreeningEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
                Map<String, Boolean> deptPriority = gson.fromJson(sessionManager.getDeptPriorityEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
                for (Object o : this.items) {
                    ServicoUnidade s = (ServicoUnidade) o;
                    String sid = String.valueOf(s.servico.id);
                    String did = s.departamento != null ? String.valueOf(s.departamento.id) : null;
                    
                    if (!this.screeningEnabled.containsKey(sid)) {
                        if (did != null && Boolean.TRUE.equals(deptEnabled.get(did))) {
                            this.screeningEnabled.put(sid, true);
                            this.screeningRequired.put(sid, true);
                        }
                    }
                    if (!this.priorityEnabled.containsKey(sid)) {
                        if (did != null && Boolean.TRUE.equals(deptPriority.get(did))) {
                            this.priorityEnabled.put(sid, true);
                        } else if (did == null) {
                            this.priorityEnabled.put(sid, true); // Default to true for services
                        }
                    }
                }
            } else if (isDept) {
                for (Object o : this.items) {
                    String did = String.valueOf(((Departamento)o).id);
                    if (!this.priorityEnabled.containsKey(did)) {
                        this.priorityEnabled.put(did, true); // Default to true for depts
                    }
                }
            }
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_config_button, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final int pos = holder.getAdapterPosition();
            Object item = items.get(pos);
            int id;
            String name;
            
            if (isPriority) {
                id = ((br.com.jefferson.totemsga.model.Prioridade)item).id;
                name = ((br.com.jefferson.totemsga.model.Prioridade)item).nome;
            } else {
                id = isDept ? ((Departamento)item).id : ((ServicoUnidade)item).servico.id;
                name = isDept ? ((Departamento)item).nome : ((ServicoUnidade)item).servico.nome;
            }
            
            Button btn = (Button) holder.itemView;
            btn.setText(name);
            
            String color = colors.get(String.valueOf(id));
            if (color == null) color = sessionManager.getButtonColor();
            btn.setBackgroundColor(Color.parseColor(color));

            String textColor = textColors.get(String.valueOf(id));
            if (textColor == null) textColor = sessionManager.getButtonTextColor();
            btn.setTextColor(Color.parseColor(textColor));
            
            btn.setOnClickListener(v -> {
                String sid = String.valueOf(id);
                
                if (isPriority) {
                    showPriorityConfig(v.getContext(), name, sid, pos);
                    return;
                }

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_config, null);
                SwitchMaterial swEnabled = dialogView.findViewById(R.id.cbScreeningEnabled);
                SwitchMaterial swRequired = dialogView.findViewById(R.id.cbScreeningRequired);
                SwitchMaterial swPriority = dialogView.findViewById(R.id.cbPriorityEnabled);
                
                swEnabled.setChecked(Boolean.TRUE.equals(screeningEnabled.get(sid)));
                swRequired.setChecked(Boolean.TRUE.equals(screeningRequired.get(sid)));
                
                // Priority Inheritance Logic for UI
                boolean isPrioritySet = priorityEnabled.containsKey(sid);
                boolean currentPriorityVal = Boolean.TRUE.equals(priorityEnabled.get(sid));
                
                if (!isPrioritySet && !isDept) {
                    // Check inheritance
                    Object currentItem = items.get(pos);
                    if (currentItem instanceof ServicoUnidade && ((ServicoUnidade)currentItem).departamento != null) {
                        Map<String, Boolean> dPriority = gson.fromJson(sessionManager.getDeptPriorityEnabled(), new TypeToken<Map<String, Boolean>>(){}.getType());
                        String did = String.valueOf(((ServicoUnidade)currentItem).departamento.id);
                        if (dPriority != null && dPriority.containsKey(did)) {
                            currentPriorityVal = Boolean.TRUE.equals(dPriority.get(did));
                        } else {
                            currentPriorityVal = true; // Default
                        }
                    } else {
                        currentPriorityVal = true; // Default
                    }
                } else if (!isPrioritySet && isDept) {
                    currentPriorityVal = true; // Default for depts
                }
                
                swPriority.setChecked(currentPriorityVal);

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Configurar: " + name)
                        .setView(dialogView)
                        .setPositiveButton("Salvar", (dialog, which) -> {
                            screeningEnabled.put(sid, swEnabled.isChecked());
                            screeningRequired.put(sid, swRequired.isChecked());
                            priorityEnabled.put(sid, swPriority.isChecked());
                        })
                        .setNeutralButton("Cores", (dialog, which) -> {
                            new AlertDialog.Builder(v.getContext())
                                    .setTitle("Editar Cores: " + name)
                                    .setItems(new String[]{"Cor de Fundo", "Cor do Texto"}, (d, w) -> {
                                        if (w == 0) {
                                            showColorPicker("Fundo: " + name, colors.get(sid), newColor -> {
                                                colors.put(sid, newColor);
                                                notifyItemChanged(pos);
                                            });
                                        } else {
                                            showColorPicker("Texto: " + name, textColors.get(sid), newColor -> {
                                                textColors.put(sid, newColor);
                                                notifyItemChanged(pos);
                                            });
                                        }
                                    }).show();
                        })
                        .show();
            });
        }

        private void showPriorityConfig(Context context, String name, String sid, int position) {
            new AlertDialog.Builder(context)
                    .setTitle("Editar Cores: " + name)
                    .setItems(new String[]{"Cor de Fundo", "Cor do Texto"}, (d, w) -> {
                        if (w == 0) {
                            showColorPicker("Fundo: " + name, colors.get(sid), newColor -> {
                                colors.put(sid, newColor);
                                notifyItemChanged(position);
                            });
                        } else {
                            showColorPicker("Texto: " + name, textColors.get(sid), newColor -> {
                                textColors.put(sid, newColor);
                                notifyItemChanged(position);
                            });
                        }
                    }).show();
        }

        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }
}
