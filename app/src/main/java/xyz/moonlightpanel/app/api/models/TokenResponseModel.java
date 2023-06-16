package xyz.moonlightpanel.app.api.models;

import com.fasterxml.jackson.annotation.*;

public class TokenResponseModel {
    private String token;

    @JsonProperty("token")
    public String getToken() { return token; }
    @JsonProperty("token")
    public void setToken(String value) { this.token = value; }
}
