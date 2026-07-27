package br.com.jefferson.totemsga.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "TotemSGAPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_CLIENT_SECRET = "client_secret";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_UNIDADE_ID = "unidade_id";
    private static final String KEY_UNIDADE_NOME = "unidade_nome";
    private static final String KEY_GROUP_BY_DEPT = "group_by_dept";
    private static final String KEY_ENABLE_PRINT = "enable_print";
    private static final String KEY_ENABLE_SCREENING = "enable_screening";
    private static final String KEY_LOGO_URL = "logo_url";
    private static final String KEY_PRIMARY_COLOR = "primary_color";
    private static final String KEY_BUTTON_COLOR = "button_color";
    private static final String KEY_BUTTON_TEXT_COLOR = "button_text_color";
    private static final String KEY_ADMIN_PASS = "admin_pass";
    private static final String KEY_AUTOCOMPLETE_URL = "autocomplete_url";
    private static final String KEY_AUTOCOMPLETE_HEADERS = "autocomplete_headers";
    private static final String KEY_GRID_COLUMNS = "grid_columns";
    private static final String KEY_SCREENING_TIMEOUT = "screening_timeout";
    private static final String KEY_SELECTED_DEPTS = "selected_depts";
    private static final String KEY_SELECTED_SERVICES = "selected_services";
    private static final String KEY_DEPT_COLORS = "dept_colors";
    private static final String KEY_DEPT_TEXT_COLORS = "dept_text_colors";
    private static final String KEY_DEPT_SCREENING_ENABLED = "dept_screening_enabled";
    private static final String KEY_DEPT_SCREENING_REQUIRED = "dept_screening_required";
    private static final String KEY_SERVICE_COLORS = "service_colors";
    private static final String KEY_SERVICE_TEXT_COLORS = "service_text_colors";
    private static final String KEY_SERVICE_SCREENING_ENABLED = "service_screening_enabled";
    private static final String KEY_SERVICE_SCREENING_REQUIRED = "service_screening_required";
    private static final String KEY_DEPT_PRIORITY_ENABLED = "dept_priority_enabled";
    private static final String KEY_SERVICE_PRIORITY_ENABLED = "service_priority_enabled";
    private static final String KEY_DEPT_GRID = "dept_grid";
    private static final String KEY_SERVICE_GRID = "service_grid";
    private static final String KEY_PRIORITY_COLORS = "priority_colors";
    private static final String KEY_PRIORITY_TEXT_COLORS = "priority_text_colors";
    private static final String KEY_KIOSK_MODE = "kiosk_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_BACKGROUND_TEXT_COLOR = "background_text_color";
    private static final String KEY_ADS_ENABLED = "ads_enabled";
    private static final String KEY_ADS_PLAYLIST = "ads_playlist";
    private static final String KEY_ADS_SINGLE_URL = "ads_single_url";
    private static final String KEY_ADS_SINGLE_TYPE = "ads_single_type";
    private static final String KEY_ADS_SINGLE_BG_COLOR = "ads_single_bg_color";
    private static final String KEY_ADS_INACTIVITY_TIME = "ads_inactivity_time";
    private static final String KEY_PRINTER_TYPE = "printer_type";
    private static final String KEY_LETREIRO_CONFIG = "letreiro_config";

    private static final String KEY_PRINT_FOOTER_TEXT = "print_footer_text";
    private static final String KEY_PRINT_FOOTER_SIZE = "print_footer_size";

    private static final String KEY_PRINT_ALIGN = "print_align";
    private static final String KEY_PRINT_SIZE_UNIT = "print_size_unit";
    private static final String KEY_PRINT_SIZE_PRIORITY = "print_size_priority";
    private static final String KEY_PRINT_SIZE_TICKET = "print_size_ticket";
    private static final String KEY_PRINT_SIZE_SERVICE = "print_size_service";
    private static final String KEY_PRINT_SIZE_DATETIME = "print_size_datetime";
    private static final String KEY_PRINT_SIZE_NAME = "print_size_name";
    private static final String KEY_PRINT_SHOW_UNIT = "print_show_unit";
    private static final String KEY_PRINT_SHOW_PRIORITY = "print_show_priority";
    private static final String KEY_PRINT_SHOW_SERVICE = "print_show_service";
    private static final String KEY_PRINT_SHOW_DATETIME = "print_show_datetime";

    private static final String KEY_LOGO_WIDTH = "logo_width";
    private static final String KEY_LOGO_HEIGHT = "logo_height";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_BUTTON_HEIGHT = "button_height";
    private static final String KEY_BUTTON_FONT_SIZE = "button_font_size";
    private static final String KEY_BACK_BUTTON_WIDTH_PERCENT = "back_button_width_percent";
    private static final String KEY_BACK_BUTTON_HEIGHT = "back_button_height";
    private static final String KEY_BACK_BUTTON_FONT_SIZE = "back_button_font_size";
    private static final String KEY_BACK_BUTTON_POSITION = "back_button_position";
    private static final String KEY_BACK_BUTTON_ALIGNMENT = "back_button_alignment";
    private static final String KEY_FLOW_DIRECTION = "flow_direction";
    private static final String KEY_FLOW_SPEED = "flow_speed";
    private static final String KEY_TOP_GRADIENT_HEIGHT = "top_gradient_height";
    private static final String KEY_BOTTOM_GRADIENT_HEIGHT = "bottom_gradient_height";
    private static final String KEY_LOGO_MARGIN_TOP = "logo_margin_top";
    private static final String KEY_TITLE_MARGIN_TOP = "title_margin_top";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        pref = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveTokens(String access, String refresh) {
        editor.putString(KEY_ACCESS_TOKEN, access);
        editor.putString(KEY_REFRESH_TOKEN, refresh);
        editor.apply();
    }

    public void saveCredentials(String apiUrl, String clientId, String clientSecret, String user, String pass) {
        editor.putString(KEY_API_URL, apiUrl);
        editor.putString(KEY_CLIENT_ID, clientId);
        editor.putString(KEY_CLIENT_SECRET, clientSecret);
        editor.putString(KEY_USERNAME, user);
        editor.putString(KEY_PASSWORD, pass);
        editor.apply();
    }

    public void saveAdminSettings(int unidadeId, String unidadeNome, boolean groupByDept, boolean enablePrint, boolean enableScreening, 
                                 String logoUrl, String primaryColor, String autocompleteUrl, 
                                 String autocompleteHeaders, int gridColumns, int screeningTimeout,
                                 String selectedDepts, String selectedServices) {
        editor.putInt(KEY_UNIDADE_ID, unidadeId);
        editor.putString(KEY_UNIDADE_NOME, unidadeNome);
        editor.putBoolean(KEY_GROUP_BY_DEPT, groupByDept);
        editor.putBoolean(KEY_ENABLE_PRINT, enablePrint);
        editor.putBoolean(KEY_ENABLE_SCREENING, enableScreening);
        editor.putString(KEY_LOGO_URL, logoUrl);
        editor.putString(KEY_PRIMARY_COLOR, primaryColor);
        editor.putString(KEY_AUTOCOMPLETE_URL, autocompleteUrl);
        editor.putString(KEY_AUTOCOMPLETE_HEADERS, autocompleteHeaders);
        editor.putInt(KEY_GRID_COLUMNS, gridColumns);
        editor.putInt(KEY_SCREENING_TIMEOUT, screeningTimeout);
        editor.putString(KEY_SELECTED_DEPTS, selectedDepts);
        editor.putString(KEY_SELECTED_SERVICES, selectedServices);
        editor.apply();
    }

    public String getAccessToken() { return pref.getString(KEY_ACCESS_TOKEN, null); }
    public String getRefreshToken() { return pref.getString(KEY_REFRESH_TOKEN, null); }
    public String getApiUrl() { return pref.getString(KEY_API_URL, ""); }
    public String getClientId() { return pref.getString(KEY_CLIENT_ID, ""); }
    public String getClientSecret() { return pref.getString(KEY_CLIENT_SECRET, ""); }
    public String getUsername() { return pref.getString(KEY_USERNAME, ""); }
    public String getPassword() { return pref.getString(KEY_PASSWORD, ""); }

    public int getUnidadeId() { return pref.getInt(KEY_UNIDADE_ID, -1); }
    public String getUnidadeNome() { return pref.getString(KEY_UNIDADE_NOME, ""); }
    public boolean isGroupByDept() { return pref.getBoolean(KEY_GROUP_BY_DEPT, false); }
    public boolean isEnablePrint() { return pref.getBoolean(KEY_ENABLE_PRINT, true); }
    public boolean isEnableScreening() { return pref.getBoolean(KEY_ENABLE_SCREENING, false); }
    public String getLogoUrl() { return pref.getString(KEY_LOGO_URL, ""); }
    public String getPrimaryColor() { return pref.getString(KEY_PRIMARY_COLOR, "#6200EE"); }
    public String getButtonColor() { return pref.getString(KEY_BUTTON_COLOR, "#6200EE"); }
    public void setButtonColor(String color) { editor.putString(KEY_BUTTON_COLOR, color).apply(); }
    public String getButtonTextColor() { return pref.getString(KEY_BUTTON_TEXT_COLOR, "#FFFFFF"); }
    public void setButtonTextColor(String color) { editor.putString(KEY_BUTTON_TEXT_COLOR, color).apply(); }
    public String getAutocompleteUrl() { return pref.getString(KEY_AUTOCOMPLETE_URL, ""); }
    public String getAutocompleteHeaders() { return pref.getString(KEY_AUTOCOMPLETE_HEADERS, ""); }
    public int getGridColumns() { return pref.getInt(KEY_GRID_COLUMNS, 2); }
    public int getScreeningTimeout() { return pref.getInt(KEY_SCREENING_TIMEOUT, 30); }
    public String getSelectedDepts() { return pref.getString(KEY_SELECTED_DEPTS, "[]"); }
    public String getSelectedServices() { return pref.getString(KEY_SELECTED_SERVICES, "[]"); }

    public String getIdsServicosAtivos() {
        try {
            br.com.jefferson.totemsga.api.ApiService api = br.com.jefferson.totemsga.api.RetrofitClient.getInstance(this);
            if (api == null) return "";
            
            retrofit2.Response<java.util.List<br.com.jefferson.totemsga.model.ServicoUnidade>> response = api.getServicos(getUnidadeId()).execute();
            if (response.isSuccessful() && response.body() != null) {
                java.util.List<br.com.jefferson.totemsga.model.ServicoUnidade> servicos = response.body();
                StringBuilder ids = new StringBuilder();
                boolean first = true;
                for (br.com.jefferson.totemsga.model.ServicoUnidade su : servicos) {
                    if (su.ativo && su.servico != null && su.servico.ativo) {
                        if (!first) ids.append(",");
                        ids.append(su.servico.id);
                        first = false;
                    }
                }
                return ids.toString();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getDeptColors() { return pref.getString(KEY_DEPT_COLORS, "{}"); }
    public void setDeptColors(String colors) { editor.putString(KEY_DEPT_COLORS, colors).apply(); }

    public String getDeptTextColors() { return pref.getString(KEY_DEPT_TEXT_COLORS, "{}"); }
    public void setDeptTextColors(String colors) { editor.putString(KEY_DEPT_TEXT_COLORS, colors).apply(); }

    public String getDeptScreeningEnabled() { return pref.getString(KEY_DEPT_SCREENING_ENABLED, "{}"); }
    public void setDeptScreeningEnabled(String data) { editor.putString(KEY_DEPT_SCREENING_ENABLED, data).apply(); }

    public String getDeptScreeningRequired() { return pref.getString(KEY_DEPT_SCREENING_REQUIRED, "{}"); }
    public void setDeptScreeningRequired(String data) { editor.putString(KEY_DEPT_SCREENING_REQUIRED, data).apply(); }

    public String getServiceColors() { return pref.getString(KEY_SERVICE_COLORS, "{}"); }
    public void setServiceColors(String colors) { editor.putString(KEY_SERVICE_COLORS, colors).apply(); }

    public String getServiceTextColors() { return pref.getString(KEY_SERVICE_TEXT_COLORS, "{}"); }
    public void setServiceTextColors(String colors) { editor.putString(KEY_SERVICE_TEXT_COLORS, colors).apply(); }

    public String getServiceScreeningEnabled() { return pref.getString(KEY_SERVICE_SCREENING_ENABLED, "{}"); }
    public void setServiceScreeningEnabled(String data) { editor.putString(KEY_SERVICE_SCREENING_ENABLED, data).apply(); }

    public String getServiceScreeningRequired() { return pref.getString(KEY_SERVICE_SCREENING_REQUIRED, "{}"); }
    public void setServiceScreeningRequired(String data) { editor.putString(KEY_SERVICE_SCREENING_REQUIRED, data).apply(); }

    public String getDeptPriorityEnabled() { return pref.getString(KEY_DEPT_PRIORITY_ENABLED, "{}"); }
    public void setDeptPriorityEnabled(String data) { editor.putString(KEY_DEPT_PRIORITY_ENABLED, data).apply(); }

    public String getServicePriorityEnabled() { return pref.getString(KEY_SERVICE_PRIORITY_ENABLED, "{}"); }
    public void setServicePriorityEnabled(String data) { editor.putString(KEY_SERVICE_PRIORITY_ENABLED, data).apply(); }

    public int getDeptGrid() { return pref.getInt(KEY_DEPT_GRID, 2); }
    public void setDeptGrid(int grid) { editor.putInt(KEY_DEPT_GRID, grid).apply(); }

    public int getServiceGrid() { return pref.getInt(KEY_SERVICE_GRID, 2); }
    public void setServiceGrid(int grid) { editor.putInt(KEY_SERVICE_GRID, grid).apply(); }

    public SharedPreferences.Editor getEditor() {
        return pref.edit();
    }

    public void reload() {
        if (context != null) {
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = pref.edit();
        }
    }

    public String getPriorityColors() { return pref.getString(KEY_PRIORITY_COLORS, "{}"); }
    public void setPriorityColors(String colors) { editor.putString(KEY_PRIORITY_COLORS, colors).apply(); }

    public String getPriorityTextColors() { return pref.getString(KEY_PRIORITY_TEXT_COLORS, "{}"); }
    public void setPriorityTextColors(String colors) { editor.putString(KEY_PRIORITY_TEXT_COLORS, colors).apply(); }

    public boolean isKioskMode() { return pref.getBoolean(KEY_KIOSK_MODE, false); }
    public void setKioskMode(boolean enabled) { editor.putBoolean(KEY_KIOSK_MODE, enabled).apply(); }

    public String getBackgroundColor() { return pref.getString(KEY_BACKGROUND_COLOR, "#FFFFFF"); }
    public void setBackgroundColor(String color) { editor.putString(KEY_BACKGROUND_COLOR, color).apply(); }

    public String getBackgroundTextColor() { return pref.getString(KEY_BACKGROUND_TEXT_COLOR, "#000000"); }
    public void setBackgroundTextColor(String color) { editor.putString(KEY_BACKGROUND_TEXT_COLOR, color).apply(); }

    public String getAdminPass() { return pref.getString(KEY_ADMIN_PASS, "admin"); }
    public void setAdminPass(String pass) { editor.putString(KEY_ADMIN_PASS, pass).apply(); }

    public void setLogoUrl(String url) { editor.putString(KEY_LOGO_URL, url).apply(); }
    public void setGroupByDept(boolean group) { editor.putBoolean(KEY_GROUP_BY_DEPT, group).apply(); }
    public void setSelectedDepts(String depts) { editor.putString(KEY_SELECTED_DEPTS, depts).apply(); }
    public void setSelectedServices(String services) { editor.putString(KEY_SELECTED_SERVICES, services).apply(); }

    public boolean isAdsEnabled() { return pref.getBoolean(KEY_ADS_ENABLED, false); }
    public void setAdsEnabled(boolean enabled) { editor.putBoolean(KEY_ADS_ENABLED, enabled).apply(); }

    public String getAdsPlaylist() { return pref.getString(KEY_ADS_PLAYLIST, "[]"); }
    public void setAdsPlaylist(String playlistJson) { editor.putString(KEY_ADS_PLAYLIST, playlistJson).apply(); }

    public String getAdsSingleUrl() { return pref.getString(KEY_ADS_SINGLE_URL, ""); }
    public void setAdsSingleUrl(String url) { editor.putString(KEY_ADS_SINGLE_URL, url).apply(); }

    public String getAdsSingleType() { return pref.getString(KEY_ADS_SINGLE_TYPE, "IMAGE"); }
    public void setAdsSingleType(String type) { editor.putString(KEY_ADS_SINGLE_TYPE, type).apply(); }

    public String getAdsSingleBgColor() { return pref.getString(KEY_ADS_SINGLE_BG_COLOR, "#000000"); }
    public void setAdsSingleBgColor(String color) { editor.putString(KEY_ADS_SINGLE_BG_COLOR, color).apply(); }

    public int getAdsInactivityTime() { return pref.getInt(KEY_ADS_INACTIVITY_TIME, 30); }
    public void setAdsInactivityTime(int seconds) { editor.putInt(KEY_ADS_INACTIVITY_TIME, seconds).apply(); }

    public String getPrinterType() { return pref.getString(KEY_PRINTER_TYPE, "AUTO"); }
    public void setPrinterType(String type) { editor.putString(KEY_PRINTER_TYPE, type).apply(); }

    public String getLetreiroConfig() { return pref.getString(KEY_LETREIRO_CONFIG, "{}"); }
    public void setLetreiroConfig(String json) { editor.putString(KEY_LETREIRO_CONFIG, json).apply(); }

    public int getPrintAlign() { return pref.getInt(KEY_PRINT_ALIGN, 1); } // 1 = Center
    public void setPrintAlign(int align) { editor.putInt(KEY_PRINT_ALIGN, align).apply(); }

    public int getPrintSizeUnit() { return pref.getInt(KEY_PRINT_SIZE_UNIT, 0); } // 0=Normal, 1=Double
    public void setPrintSizeUnit(int size) { editor.putInt(KEY_PRINT_SIZE_UNIT, size).apply(); }

    public int getPrintSizePriority() { return pref.getInt(KEY_PRINT_SIZE_PRIORITY, 0); }
    public void setPrintSizePriority(int size) { editor.putInt(KEY_PRINT_SIZE_PRIORITY, size).apply(); }

    public int getPrintSizeTicket() { return pref.getInt(KEY_PRINT_SIZE_TICKET, 1); } // 1=Double
    public void setPrintSizeTicket(int size) { editor.putInt(KEY_PRINT_SIZE_TICKET, size).apply(); }

    public int getPrintSizeService() { return pref.getInt(KEY_PRINT_SIZE_SERVICE, 0); }
    public void setPrintSizeService(int size) { editor.putInt(KEY_PRINT_SIZE_SERVICE, size).apply(); }

    public int getPrintSizeDateTime() { return pref.getInt(KEY_PRINT_SIZE_DATETIME, 0); }
    public void setPrintSizeDateTime(int size) { editor.putInt(KEY_PRINT_SIZE_DATETIME, size).apply(); }

    public int getPrintSizeName() { return pref.getInt(KEY_PRINT_SIZE_NAME, 0); }
    public void setPrintSizeName(int size) { editor.putInt(KEY_PRINT_SIZE_NAME, size).apply(); }

    public boolean isPrintShowUnit() { return pref.getBoolean(KEY_PRINT_SHOW_UNIT, true); }
    public void setPrintShowUnit(boolean show) { editor.putBoolean(KEY_PRINT_SHOW_UNIT, show).apply(); }

    public boolean isPrintShowPriority() { return pref.getBoolean(KEY_PRINT_SHOW_PRIORITY, true); }
    public void setPrintShowPriority(boolean show) { editor.putBoolean(KEY_PRINT_SHOW_PRIORITY, show).apply(); }

    public boolean isPrintShowService() { return pref.getBoolean(KEY_PRINT_SHOW_SERVICE, true); }
    public void setPrintShowService(boolean show) { editor.putBoolean(KEY_PRINT_SHOW_SERVICE, show).apply(); }

    public boolean isPrintShowDateTime() { return pref.getBoolean(KEY_PRINT_SHOW_DATETIME, true); }
    public void setPrintShowDateTime(boolean show) { editor.putBoolean(KEY_PRINT_SHOW_DATETIME, show).apply(); }

    public int getLogoWidth() { return pref.getInt(KEY_LOGO_WIDTH, 200); }
    public void setLogoWidth(int width) { editor.putInt(KEY_LOGO_WIDTH, width).apply(); }

    public int getLogoHeight() { return pref.getInt(KEY_LOGO_HEIGHT, 100); }
    public void setLogoHeight(int height) { editor.putInt(KEY_LOGO_HEIGHT, height).apply(); }

    public boolean isSoundEnabled() { return pref.getBoolean(KEY_SOUND_ENABLED, true); }
    public void setSoundEnabled(boolean enabled) { editor.putBoolean(KEY_SOUND_ENABLED, enabled).apply(); }

    public int getButtonHeight() { return pref.getInt(KEY_BUTTON_HEIGHT, 140); }
    public void setButtonHeight(int height) { editor.putInt(KEY_BUTTON_HEIGHT, height).apply(); }

    public int getButtonFontSize() { return pref.getInt(KEY_BUTTON_FONT_SIZE, 24); }
    public void setButtonFontSize(int size) { editor.putInt(KEY_BUTTON_FONT_SIZE, size).apply(); }

    public int getBackButtonWidthPercent() { return pref.getInt(KEY_BACK_BUTTON_WIDTH_PERCENT, 100); }
    public void setBackButtonWidthPercent(int percent) { editor.putInt(KEY_BACK_BUTTON_WIDTH_PERCENT, percent).apply(); }

    public int getBackButtonHeight() { return pref.getInt(KEY_BACK_BUTTON_HEIGHT, 48); }
    public void setBackButtonHeight(int height) { editor.putInt(KEY_BACK_BUTTON_HEIGHT, height).apply(); }

    public int getBackButtonFontSize() { return pref.getInt(KEY_BACK_BUTTON_FONT_SIZE, 14); }
    public void setBackButtonFontSize(int size) { editor.putInt(KEY_BACK_BUTTON_FONT_SIZE, size).apply(); }

    public int getBackButtonPosition() { return pref.getInt(KEY_BACK_BUTTON_POSITION, 0); } // 0=Abaixo botões, 1=Rodapé Fixo
    public void setBackButtonPosition(int pos) { editor.putInt(KEY_BACK_BUTTON_POSITION, pos).apply(); }

    public int getBackButtonAlignment() { return pref.getInt(KEY_BACK_BUTTON_ALIGNMENT, 0); } // 0=Esq, 1=Centro, 2=Dir
    public void setBackButtonAlignment(int alignment) { editor.putInt(KEY_BACK_BUTTON_ALIGNMENT, alignment).apply(); }

    public int getFlowDirection() { return pref.getInt(KEY_FLOW_DIRECTION, 0); } // 0=Esq->Dir, 1=Dir->Esq, 2=Cima->Baixo, 3=Baixo->Cima
    public void setFlowDirection(int direction) { editor.putInt(KEY_FLOW_DIRECTION, direction).apply(); }

    public int getFlowSpeed() { return pref.getInt(KEY_FLOW_SPEED, 1); } // 0=Lenta (8s), 1=Média (4s), 2=Rápida (2s)
    public void setFlowSpeed(int speed) { editor.putInt(KEY_FLOW_SPEED, speed).apply(); }

    public int getTopGradientHeight() { return pref.getInt(KEY_TOP_GRADIENT_HEIGHT, 100); }
    public void setTopGradientHeight(int height) { editor.putInt(KEY_TOP_GRADIENT_HEIGHT, height).apply(); }

    public int getBottomGradientHeight() { return pref.getInt(KEY_BOTTOM_GRADIENT_HEIGHT, 80); }
    public void setBottomGradientHeight(int height) { editor.putInt(KEY_BOTTOM_GRADIENT_HEIGHT, height).apply(); }

    public int getLogoMarginTop() { return pref.getInt(KEY_LOGO_MARGIN_TOP, 2); }
    public void setLogoMarginTop(int margin) { editor.putInt(KEY_LOGO_MARGIN_TOP, margin).apply(); }

    public int getTitleMarginTop() { return pref.getInt(KEY_TITLE_MARGIN_TOP, 2); }
    public void setTitleMarginTop(int margin) { editor.putInt(KEY_TITLE_MARGIN_TOP, margin).apply(); }

    public String getPrintFooterText() { return pref.getString(KEY_PRINT_FOOTER_TEXT, ""); }
    public void setPrintFooterText(String text) { editor.putString(KEY_PRINT_FOOTER_TEXT, text).apply(); }

    public int getPrintFooterSize() { return pref.getInt(KEY_PRINT_FOOTER_SIZE, 0); }
    public void setPrintFooterSize(int size) { editor.putInt(KEY_PRINT_FOOTER_SIZE, size).apply(); }
}
