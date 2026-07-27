package br.com.jefferson.totemsga.model;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("token_type")
    public String tokenType;
    @SerializedName("expires_in")
    public int expiresIn;
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("refresh_token")
    public String refreshToken;
}
