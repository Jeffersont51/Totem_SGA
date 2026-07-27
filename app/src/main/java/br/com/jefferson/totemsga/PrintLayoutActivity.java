package br.com.jefferson.totemsga;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;

public class PrintLayoutActivity extends BaseActivity {

    private SessionManager sessionManager;
    private Spinner spinnerAlign, spinnerSizeUnit, spinnerSizePriority, spinnerSizeTicket, spinnerSizeService, spinnerSizeDateTime, spinnerSizeName, spinnerSizeFooter;
    private android.widget.EditText etFooterText;
    private CheckBox cbUnit, cbPriority, cbService, cbDateTime;
    private Button btnSave, btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_layout);

        sessionManager = new SessionManager(this);

        spinnerAlign = findViewById(R.id.spinnerPrintAlign);
        spinnerSizeUnit = findViewById(R.id.spinnerSizeUnit);
        spinnerSizePriority = findViewById(R.id.spinnerSizePriority);
        spinnerSizeTicket = findViewById(R.id.spinnerSizeTicket);
        spinnerSizeService = findViewById(R.id.spinnerSizeService);
        spinnerSizeDateTime = findViewById(R.id.spinnerSizeDateTime);
        spinnerSizeName = findViewById(R.id.spinnerSizeName);
        spinnerSizeFooter = findViewById(R.id.spinnerSizeFooter);
        etFooterText = findViewById(R.id.etFooterText);
        cbUnit = findViewById(R.id.cbShowUnit);
        cbPriority = findViewById(R.id.cbShowPriority);
        cbService = findViewById(R.id.cbShowService);
        cbDateTime = findViewById(R.id.cbShowDateTime);
        btnSave = findViewById(R.id.btnSavePrintLayout);
        btnTest = findViewById(R.id.btnTestPrint);

        setupSpinners();
        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        styleButtons(btnSave);
        btnTest.setOnClickListener(v -> testPrint());
    }

    private void setupSpinners() {
        String[] aligns = {"Esquerda", "Centro", "Direita"};
        spinnerAlign.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, aligns));

        String[] sizes = {"Normal", "Grande (Dobro)", "Extra Grande (Triplo)"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sizes);
        
        spinnerSizeUnit.setAdapter(sizeAdapter);
        spinnerSizePriority.setAdapter(sizeAdapter);
        spinnerSizeTicket.setAdapter(sizeAdapter);
        spinnerSizeService.setAdapter(sizeAdapter);
        spinnerSizeDateTime.setAdapter(sizeAdapter);
        spinnerSizeName.setAdapter(sizeAdapter);
        spinnerSizeFooter.setAdapter(sizeAdapter);
    }

    private void loadSettings() {
        spinnerAlign.setSelection(sessionManager.getPrintAlign());
        spinnerSizeUnit.setSelection(sessionManager.getPrintSizeUnit());
        spinnerSizePriority.setSelection(sessionManager.getPrintSizePriority());
        spinnerSizeTicket.setSelection(sessionManager.getPrintSizeTicket());
        spinnerSizeService.setSelection(sessionManager.getPrintSizeService());
        spinnerSizeDateTime.setSelection(sessionManager.getPrintSizeDateTime());
        spinnerSizeName.setSelection(sessionManager.getPrintSizeName());
        spinnerSizeFooter.setSelection(sessionManager.getPrintFooterSize());
        etFooterText.setText(sessionManager.getPrintFooterText());
        
        cbUnit.setChecked(sessionManager.isPrintShowUnit());
        cbPriority.setChecked(sessionManager.isPrintShowPriority());
        cbService.setChecked(sessionManager.isPrintShowService());
        cbDateTime.setChecked(sessionManager.isPrintShowDateTime());
    }

    private void saveSettings() {
        applySettings();
        Toast.makeText(this, "Configurações de impressão salvas!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applySettings() {
        sessionManager.setPrintAlign(spinnerAlign.getSelectedItemPosition());
        sessionManager.setPrintSizeUnit(spinnerSizeUnit.getSelectedItemPosition());
        sessionManager.setPrintSizePriority(spinnerSizePriority.getSelectedItemPosition());
        sessionManager.setPrintSizeTicket(spinnerSizeTicket.getSelectedItemPosition());
        sessionManager.setPrintSizeService(spinnerSizeService.getSelectedItemPosition());
        sessionManager.setPrintSizeDateTime(spinnerSizeDateTime.getSelectedItemPosition());
        sessionManager.setPrintSizeName(spinnerSizeName.getSelectedItemPosition());
        sessionManager.setPrintFooterSize(spinnerSizeFooter.getSelectedItemPosition());
        sessionManager.setPrintFooterText(etFooterText.getText().toString());
        
        sessionManager.setPrintShowUnit(cbUnit.isChecked());
        sessionManager.setPrintShowPriority(cbPriority.isChecked());
        sessionManager.setPrintShowService(cbService.isChecked());
        sessionManager.setPrintShowDateTime(cbDateTime.isChecked());
    }

    private void testPrint() {
        applySettings();
        SunmiPrinterHelper helper = SunmiPrinterHelper.getInstance();
        if (!helper.isConnected()) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show();
            return;
        }

        helper.printerInit();
        helper.setAlignment(sessionManager.getPrintAlign());

        if (sessionManager.isPrintShowUnit()) {
            helper.setFontSize(sessionManager.getPrintSizeUnit());
            helper.printText(sessionManager.getUnidadeNome() + "\n\n");
        }

        if (sessionManager.isPrintShowPriority()) {
            helper.setFontSize(sessionManager.getPrintSizePriority());
            helper.printText("Normal\n");
        }

        helper.setFontSize(sessionManager.getPrintSizeTicket());
        helper.setBold(true);
        helper.printText("\nTI020\n");
        helper.setBold(false);

        if (sessionManager.isPrintShowService()) {
            helper.setFontSize(sessionManager.getPrintSizeService());
            helper.printText("\nATENDIMENTO T.I\n");
        }

        if (sessionManager.isPrintShowDateTime()) {
            helper.setFontSize(sessionManager.getPrintSizeDateTime());
            helper.printText("\n13/07/2026\n");
            helper.printText("Hora de chegada 15h55\n");
            helper.printText("( Horário local )\n");
        }

        String footerText = sessionManager.getPrintFooterText();
        if (!footerText.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintFooterSize());
            helper.printText("\n" + footerText + "\n");
        }

        helper.setFontSize(0);
        helper.printText("\n==============================\n");
        helper.lineWrap(4);
        helper.cutPaper();
    }

}
