package br.com.jefferson.totemsga;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import br.com.jefferson.totemsga.api.ApiService;
import br.com.jefferson.totemsga.api.RetrofitClient;
import br.com.jefferson.totemsga.util.Logger;
import br.com.jefferson.totemsga.util.SessionManager;
import br.com.jefferson.totemsga.util.SunmiPrinterHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuccessFragment extends BaseKioskFragment {

    private static final String ARG_TICKET = "ticket";
    private static final String ARG_ID = "ticket_id";
    private static final String ARG_HASH = "ticket_hash";
    private static final String ARG_PRIORITY = "priority";
    private static final String ARG_SERVICE = "service";
    private static final String ARG_THEME_COLOR = "theme_color";

    public static SuccessFragment newInstance(String ticket, int id, String hash, String priority, String service, String themeColor) {
        SuccessFragment fragment = new SuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TICKET, ticket);
        args.putInt(ARG_ID, id);
        args.putString(ARG_HASH, hash);
        args.putString(ARG_PRIORITY, priority);
        args.putString(ARG_SERVICE, service);
        args.putString(ARG_THEME_COLOR, themeColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isTimerEnabled() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_success, container, false);
        
        try {
            String bgColor = sessionManager.getBackgroundColor();
            if (bgColor != null && !bgColor.isEmpty()) {
                view.setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            }
        } catch (Exception e) {
            view.setBackgroundColor(android.graphics.Color.parseColor("#FFF5E1"));
        }

        TextView tvTicket = view.findViewById(R.id.tvTicketNumber);
        Button btnPrint = view.findViewById(R.id.btnPrint);
        
        try {
            ImageView ivLogo = view.findViewById(R.id.ivLogoSuccess);
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

            // "IMPRIMIR NOVAMENTE" é um botão de contorno/secundário - usa o
            // mesmo padrão de altura dos demais botões de contorno do app
            // (Ajustes do Botão VOLTAR), não a altura do botão principal.
            setupBackButton(btnPrint, null);

            Button btnBackToStart = view.findViewById(R.id.btnBackToStart);
            android.view.View contentContainer = view.findViewById(R.id.llHeader);
            android.view.ViewGroup.LayoutParams lpBackToStart = btnBackToStart.getLayoutParams();
            lpBackToStart.height = (int) (sessionManager.getButtonHeight() * getResources().getDisplayMetrics().density);
            btnBackToStart.setLayoutParams(lpBackToStart);

            int textColorVal = android.graphics.Color.parseColor(sessionManager.getBackgroundTextColor());
            TextView tvTitle = view.findViewById(R.id.tvSuccessTitle);
            TextView tvFooter = view.findViewById(R.id.tvSuccessFooter);
            if (tvTitle != null) tvTitle.setTextColor(textColorVal);
            if (tvFooter != null) tvFooter.setTextColor(textColorVal);
            tvTicket.setTextColor(android.graphics.Color.parseColor("#F47B20")); // Mantém cor tema para o número
        } catch (Exception e) {}

        if (getArguments() != null) {
            tvTicket.setText(getArguments().getString(ARG_TICKET));
            
            if (sessionManager.isEnablePrint()) {
                btnPrint.setVisibility(View.VISIBLE);
                btnPrint.setOnClickListener(v -> printTicket());
                
                // Opção: Auto-print ao carregar
                printTicket();
            }
        }

        view.findViewById(R.id.btnBackToStart).setOnClickListener(v -> goBack());

        applyGlobalSpacing(view);
        return view;
    }

    private void printTicket() {
        if (getArguments() == null) return;
        
        // Notifica Activity para suspender Kiosk temporariamente durante o diálogo de impressão
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setInteractingWithSystem(true);
        }
        
        int id = getArguments().getInt(ARG_ID);
        String hash = getArguments().getString(ARG_HASH);

        ApiService api = RetrofitClient.getInstance(sessionManager);
        if (api == null) return;
        api.getPrintContent(id, hash).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try { 
                        String html = response.body().string();
                        Logger.getInstance().setLastHtml(html);
                        doPrint(html); 
                    } catch (Exception e) {}
                }
            }
            @Override public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Erro ao imprimir", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doPrint(String html) {
        if (!isAdded()) return;

        String printerType = sessionManager.getPrinterType();

        // 1. Sunmi Nativa
        boolean isSunmiAvailable = SunmiPrinterHelper.getInstance().isConnected() && SunmiPrinterHelper.getInstance().getStatus() == 1;
        
        if ("SUNMI".equals(printerType) || ("AUTO".equals(printerType) && isSunmiAvailable)) {
            printSunmiTicket();
            // Para Sunmi nativa não há diálogo de sistema, podemos restaurar Kiosk rápido
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setInteractingWithSystem(false);
            }
            return;
        }

        // 2. AllPos Intent
        if ("ALLPOS".equals(printerType) || "AUTO".equals(printerType)) {
            try {
                Intent intent = new Intent("in.allmark.allpos_print_service.PRINT");
                intent.setPackage("in.allmark.allpos_print_service");
                intent.putExtra("content", html);
                intent.putExtra("direct", "1");
                
                startActivity(intent);
                
                // Intents externas tiram foco, o Kiosk será recuperado pelo onWindowFocusChanged
                // ou pelo timeout do setInteractingWithSystem(false) que faremos via Handler
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setInteractingWithSystem(false);
                    }
                }, 5000);
                
                return;
            } catch (Exception e) {
                Logger.getInstance().e("PRINT", "Falha ao chamar AllPos", e);
                if ("ALLPOS".equals(printerType)) {
                    Toast.makeText(getContext(), "Erro ao abrir AllPos", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // 3. Padrão (PrintManager)
        doPrintStandard(html);
    }

    private void doPrintStandard(String html) {
        if (!isAdded()) return;

        android.webkit.WebView webView = new android.webkit.WebView(requireContext());
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                try {
                    android.print.PrintManager printManager = (android.print.PrintManager) requireActivity().getSystemService(android.content.Context.PRINT_SERVICE);
                    String jobName = "SGA_" + System.currentTimeMillis();

                    android.print.PrintAttributes attributes = new android.print.PrintAttributes.Builder()
                        .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(android.print.PrintAttributes.COLOR_MODE_MONOCHROME)
                        .build();

                    printManager.print(jobName, webView.createPrintDocumentAdapter(jobName), attributes);
                    
                    // Diálogo de impressão padrão é demorado, damos um tempo generoso
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).setInteractingWithSystem(false);
                        }
                    }, 8000);

                } catch (Exception e) {
                    if (isAdded()) Toast.makeText(getContext(), "Erro no Spooler de Impressão", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setInteractingWithSystem(false);
                    }
                }
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void printSunmiTicket() {
        if (getArguments() == null) return;
        String ticket = getArguments().getString(ARG_TICKET);
        String hash = getArguments().getString(ARG_HASH);
        String priority = getArguments().getString(ARG_PRIORITY, "Normal");
        String service = getArguments().getString(ARG_SERVICE, "");
        String unitName = sessionManager.getUnidadeNome();

        SunmiPrinterHelper helper = SunmiPrinterHelper.getInstance();
        helper.printerInit();
        helper.setAlignment(sessionManager.getPrintAlign());
        
        // Cabeçalho: Nome da Unidade
        if (sessionManager.isPrintShowUnit() && !unitName.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintSizeUnit());
            helper.printText(unitName + "\n\n");
        }

        // Prioridade
        if (sessionManager.isPrintShowPriority()) {
            helper.setFontSize(sessionManager.getPrintSizePriority());
            helper.printText(priority + "\n");
        }
        
        // Senha (Grande)
        helper.setFontSize(sessionManager.getPrintSizeTicket());
        helper.setBold(true);
        helper.printText("\n" + ticket + "\n");
        helper.setBold(false);
        
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
        String footerText = sessionManager.getPrintFooterText();
        if (isFacial && !footerText.isEmpty()) {
            helper.setFontSize(sessionManager.getPrintFooterSize());
            helper.printText("\n" + footerText + "\n");
        }
        
        helper.setFontSize(0);
        helper.printText("\n==============================\n");
        
        if (hash != null && !hash.isEmpty()) {
            // helper.printQrCode(hash, 6, 2);
        }
        
        helper.lineWrap(4);
        helper.cutPaper();
    }

    private void goBack() {
        if (isAdded() && !getParentFragmentManager().isStateSaved()) {
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).startFlow();
            }
        }
    }
}
