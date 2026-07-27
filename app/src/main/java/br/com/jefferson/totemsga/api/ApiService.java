package br.com.jefferson.totemsga.api;

import java.util.List;
import java.util.Map;

import br.com.jefferson.totemsga.model.Departamento;
import br.com.jefferson.totemsga.model.Prioridade;
import br.com.jefferson.totemsga.model.ServicoUnidade;
import br.com.jefferson.totemsga.model.TicketRequest;
import br.com.jefferson.totemsga.model.TicketResponse;
import br.com.jefferson.totemsga.model.TokenResponse;
import br.com.jefferson.totemsga.model.Unidade;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("api/token")
    Call<TokenResponse> getToken(@FieldMap Map<String, String> params);

    @GET("api/unidades")
    Call<List<Unidade>> getUnidades();

    @GET("api/departamentos")
    Call<List<Departamento>> getDepartamentos();

    @GET("api/unidades/{id}/servicos")
    Call<List<ServicoUnidade>> getServicos(@Path("id") int unidadeId);

    @GET("api/prioridades")
    Call<List<Prioridade>> getPrioridades();

    @POST("api/distribui")
    Call<TicketResponse> distribui(@Body TicketRequest request);

    @GET("api/print/{id}")
    Call<ResponseBody> getPrintContent(@Path("id") int ticketId, @Header("X-Hash") String hash);

    // --- Autocomplete NovoSGA ---
    @GET("login")
    Call<ResponseBody> getLoginPage();

    @FormUrlEncoded
    @POST("login")
    Call<ResponseBody> login(
            @Field("username") String username,
            @Field("password") String password,
            @Field("_csrf_token") String csrfToken
    );

    @GET("novosga.triage/clientes")
    Call<br.com.jefferson.totemsga.model.ClienteResponse> buscarCliente(@Query("q") String documento);

    @GET("novosga.triage/agendamentos/{servicoId}")
    Call<br.com.jefferson.totemsga.model.AgendamentoResponse> buscarAgendamentosPorServico(@Path("servicoId") int servicoId);

    @POST("novosga.triage/distribui_agendamento/{id}")
    Call<br.com.jefferson.totemsga.model.TicketTriageResponse> confirmarAgendamento(@Path("id") int agendamentoId);

    @GET("novosga.triage/ajax_update")
    Call<br.com.jefferson.totemsga.model.SenhasResponse> buscarSenhasFila(@Query("ids") String ids);

    @GET("novosga.monitor/ajax_update")
    Call<br.com.jefferson.totemsga.model.MonitorResponse> buscarSenhasMonitor(@Query("ids") String ids);

    @GET("novosga.monitor/ajax_update")
    Call<okhttp3.ResponseBody> buscarSenhasMonitorRaw(@Query("ids") String ids);
}
