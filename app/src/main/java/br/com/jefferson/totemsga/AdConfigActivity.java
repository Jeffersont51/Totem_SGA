package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import br.com.jefferson.totemsga.model.LetreiroConfig;
import br.com.jefferson.totemsga.util.SessionManager;

public class AdConfigActivity extends BaseActivity {

    private SwitchMaterial switchAdsEnabled;
    private TextInputEditText etAdInactivity;
    private TextInputEditText etAdUrlSingle;
    private TextInputEditText etAdBgColorSingle;
    private View vAdColorPreviewSingle;
    private RadioGroup rgAdType;
    private Button btnSaveAds, btnApplyAds;

    // Global Letreiro Views
    private CheckBox cbLetreiroHabilitadoGlobal;
    private EditText etLetreiroMsgGlobal, etLetreiroCorFonteGlobal, etLetreiroCorFundoGlobal, etLetreiroTamanhoGlobal, etLetreiroVelocidadeGlobal;
    private RadioGroup rgLetreiroPosicaoGlobal;
    private Spinner spinnerLetreiroEstiloGlobal, spinnerLetreiroEfeitoGlobal, spinnerLetreiroDirecaoGlobal;
    
    private SessionManager sessionManager;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_config);

        sessionManager = new SessionManager(this);
        
        switchAdsEnabled = findViewById(R.id.switchAdsEnabled);
        etAdInactivity = findViewById(R.id.etAdInactivity);
        etAdUrlSingle = findViewById(R.id.etAdUrlSingle);
        etAdBgColorSingle = findViewById(R.id.etAdBgColorSingle);
        vAdColorPreviewSingle = findViewById(R.id.vAdColorPreviewSingle);
        rgAdType = findViewById(R.id.rgAdType);
        btnSaveAds = findViewById(R.id.btnSaveAds);
        btnApplyAds = findViewById(R.id.btnApplyAds);

        // Global Letreiro Init
        cbLetreiroHabilitadoGlobal = findViewById(R.id.cbLetreiroHabilitadoGlobal);
        etLetreiroMsgGlobal = findViewById(R.id.etLetreiroMsgGlobal);
        etLetreiroCorFonteGlobal = findViewById(R.id.etLetreiroCorFonteGlobal);
        etLetreiroCorFundoGlobal = findViewById(R.id.etLetreiroCorFundoGlobal);
        etLetreiroTamanhoGlobal = findViewById(R.id.etLetreiroTamanhoGlobal);
        etLetreiroVelocidadeGlobal = findViewById(R.id.etLetreiroVelocidadeGlobal);
        rgLetreiroPosicaoGlobal = findViewById(R.id.rgLetreiroPosicaoGlobal);
        spinnerLetreiroEstiloGlobal = findViewById(R.id.spinnerLetreiroEstiloGlobal);
        spinnerLetreiroEfeitoGlobal = findViewById(R.id.spinnerLetreiroEfeitoGlobal);
        spinnerLetreiroDirecaoGlobal = findViewById(R.id.spinnerLetreiroDirecaoGlobal);

        String[] estilos = {"NORMAL", "NEGRITO", "ITALICO"};
        ArrayAdapter<String> estiloAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, estilos);
        estiloAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLetreiroEstiloGlobal.setAdapter(estiloAdapter);

        String[] efeitos = {"DESLIZAR", "PISCAR", "ESTATICO"};
        ArrayAdapter<String> efeitoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, efeitos);
        efeitoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLetreiroEfeitoGlobal.setAdapter(efeitoAdapter);

        String[] direcoes = {"DIREITA_ESQUERDA", "ESQUERDA_DIREITA", "BAIXO_CIMA", "CIMA_BAIXO"};
        ArrayAdapter<String> direcaoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, direcoes);
        direcaoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLetreiroDirecaoGlobal.setAdapter(direcaoAdapter);

        findViewById(R.id.btnPickColorFonte).setOnClickListener(v -> showColorPicker("Cor da Fonte", etLetreiroCorFonteGlobal.getText().toString(), color -> etLetreiroCorFonteGlobal.setText(color)));
        findViewById(R.id.btnPickColorFundo).setOnClickListener(v -> showColorPicker("Cor do Fundo", etLetreiroCorFundoGlobal.getText().toString(), color -> etLetreiroCorFundoGlobal.setText(color)));
        
        vAdColorPreviewSingle.setOnClickListener(v -> showColorPicker("Cor de Fundo da Publicidade", etAdBgColorSingle.getText().toString(), color -> {
            etAdBgColorSingle.setText(color);
            try { vAdColorPreviewSingle.setBackgroundColor(android.graphics.Color.parseColor(color)); } catch (Exception e) {}
        }));

        etAdBgColorSingle.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                try { vAdColorPreviewSingle.setBackgroundColor(android.graphics.Color.parseColor(s.toString())); } catch (Exception e) {}
            }
        });

        loadSettings();

        btnSaveAds.setOnClickListener(v -> saveSettings());
        styleButtons(btnSaveAds);
        btnApplyAds.setOnClickListener(v -> {
            saveSettingsSilently();
            br.com.jefferson.totemsga.ads.AdManager.getInstance().reloadConfig(sessionManager);
            Toast.makeText(this, "Configurações aplicadas imediatamente!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        switchAdsEnabled.setChecked(sessionManager.isAdsEnabled());
        etAdInactivity.setText(String.valueOf(sessionManager.getAdsInactivityTime()));
        etAdUrlSingle.setText(sessionManager.getAdsSingleUrl());
        etAdBgColorSingle.setText(sessionManager.getAdsSingleBgColor());
        try { vAdColorPreviewSingle.setBackgroundColor(android.graphics.Color.parseColor(sessionManager.getAdsSingleBgColor())); } catch (Exception e) {}
        
        String type = sessionManager.getAdsSingleType();
        if ("VIDEO".equals(type)) rgAdType.check(R.id.rbVideo);
        else if ("GIF".equals(type)) rgAdType.check(R.id.rbGif);
        else rgAdType.check(R.id.rbImage);

        // Load Global Letreiro
        LetreiroConfig lc = gson.fromJson(sessionManager.getLetreiroConfig(), LetreiroConfig.class);
        if (lc == null) lc = new LetreiroConfig();
        
        cbLetreiroHabilitadoGlobal.setChecked(lc.habilitado);
        etLetreiroMsgGlobal.setText(lc.mensagem);
        etLetreiroCorFonteGlobal.setText(lc.corFonte);
        etLetreiroCorFundoGlobal.setText(lc.corFundo);
        etLetreiroTamanhoGlobal.setText(String.valueOf(lc.tamanhoFonte));
        etLetreiroVelocidadeGlobal.setText(String.valueOf(lc.velocidadeSegundos));
        
        if ("TOPO".equals(lc.posicao)) ((RadioButton)findViewById(R.id.rbTopoGlobal)).setChecked(true);
        else if ("CENTRO".equals(lc.posicao)) ((RadioButton)findViewById(R.id.rbCentroGlobal)).setChecked(true);
        else ((RadioButton)findViewById(R.id.rbRodapeGlobal)).setChecked(true);

        String[] estilos = {"NORMAL", "NEGRITO", "ITALICO"};
        for(int i=0; i<estilos.length; i++) if(estilos[i].equals(lc.estilo)) spinnerLetreiroEstiloGlobal.setSelection(i);

        String[] efeitos = {"DESLIZAR", "PISCAR", "ESTATICO"};
        for(int i=0; i<efeitos.length; i++) if(efeitos[i].equals(lc.efeito)) spinnerLetreiroEfeitoGlobal.setSelection(i);

        String[] direcoes = {"DIREITA_ESQUERDA", "ESQUERDA_DIREITA", "BAIXO_CIMA", "CIMA_BAIXO"};
        for(int i=0; i<direcoes.length; i++) if(direcoes[i].equals(lc.direcao)) spinnerLetreiroDirecaoGlobal.setSelection(i);
    }

    private void saveSettings() {
        saveSettingsSilently();
        Toast.makeText(this, "Configurações de Publicidade salvas", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveSettingsSilently() {
        sessionManager.setAdsEnabled(switchAdsEnabled.isChecked());
        String inactivity = etAdInactivity.getText().toString();
        sessionManager.setAdsInactivityTime(inactivity.isEmpty() ? 30 : Integer.parseInt(inactivity));
        sessionManager.setAdsSingleUrl(etAdUrlSingle.getText().toString());
        sessionManager.setAdsSingleBgColor(etAdBgColorSingle.getText().toString());

        int typeId = rgAdType.getCheckedRadioButtonId();
        if (typeId == R.id.rbVideo) sessionManager.setAdsSingleType("VIDEO");
        else if (typeId == R.id.rbGif) sessionManager.setAdsSingleType("GIF");
        else sessionManager.setAdsSingleType("IMAGE");

        // Save Global Letreiro
        LetreiroConfig lc = new LetreiroConfig();
        lc.habilitado = cbLetreiroHabilitadoGlobal.isChecked();
        lc.mensagem = etLetreiroMsgGlobal.getText().toString();
        lc.corFonte = etLetreiroCorFonteGlobal.getText().toString();
        lc.corFundo = etLetreiroCorFundoGlobal.getText().toString();
        String tam = etLetreiroTamanhoGlobal.getText().toString();
        lc.tamanhoFonte = tam.isEmpty() ? 24 : Integer.parseInt(tam);
        String vel = etLetreiroVelocidadeGlobal.getText().toString();
        lc.velocidadeSegundos = vel.isEmpty() ? 5 : Integer.parseInt(vel);
        
        lc.estilo = spinnerLetreiroEstiloGlobal.getSelectedItem().toString();
        lc.efeito = spinnerLetreiroEfeitoGlobal.getSelectedItem().toString();
        lc.direcao = spinnerLetreiroDirecaoGlobal.getSelectedItem().toString();

        int rbId = rgLetreiroPosicaoGlobal.getCheckedRadioButtonId();
        if (rbId == R.id.rbTopoGlobal) lc.posicao = "TOPO";
        else if (rbId == R.id.rbCentroGlobal) lc.posicao = "CENTRO";
        else lc.posicao = "RODAPE";

        sessionManager.setLetreiroConfig(gson.toJson(lc));
        
        br.com.jefferson.totemsga.ads.AdManager.getInstance().reloadConfig(sessionManager);
    }


    private void showColorPicker(String title, String currentColor, ColorSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        EditText etColor = view.findViewById(R.id.etColorHex);
        etColor.setText(currentColor);

        android.widget.SeekBar sbHue = view.findViewById(R.id.sbHue);
        android.widget.SeekBar sbSat = view.findViewById(R.id.sbSaturation);
        android.widget.SeekBar sbVal = view.findViewById(R.id.sbValue);
        View vPreview = view.findViewById(R.id.vColorPreview);

        float[] hsv = new float[3];
        try { android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(currentColor), hsv); } catch (Exception e) { hsv[0] = 0; hsv[1] = 0; hsv[2] = 1; }
        
        sbHue.setProgress((int) hsv[0]);
        sbSat.setProgress((int) (hsv[1] * 100));
        sbVal.setProgress((int) (hsv[2] * 100));
        vPreview.setBackgroundColor(android.graphics.Color.HSVToColor(hsv));

        android.widget.SeekBar.OnSeekBarChangeListener colorChangeListener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    hsv[0] = sbHue.getProgress();
                    hsv[1] = sbSat.getProgress() / 100f;
                    hsv[2] = sbVal.getProgress() / 100f;
                    int color = android.graphics.Color.HSVToColor(hsv);
                    vPreview.setBackgroundColor(color);
                    etColor.setText(String.format("#%06X", (0xFFFFFF & color)));
                }
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        };

        sbHue.setOnSeekBarChangeListener(colorChangeListener);
        sbSat.setOnSeekBarChangeListener(colorChangeListener);
        sbVal.setOnSeekBarChangeListener(colorChangeListener);

        builder.setView(view);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String color = etColor.getText().toString();
            try {
                android.graphics.Color.parseColor(color);
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
}
