package br.com.jefferson.totemsga.teste;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TesteApiService {
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
    Call<ResponseBody> buscarCliente(@Query("q") String documento);
}
