package xyz.moonlightpanel.app.api.models;

import com.fasterxml.jackson.annotation.*;

public class ActionModel {
    private String action;

    @JsonProperty("action")
    public String getAction() { return action; }
    @JsonProperty("action")
    public void setAction(String value) { this.action = value; }
}
