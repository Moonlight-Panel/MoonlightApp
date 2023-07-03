package link.endelon.moonlight.api.models;

import com.fasterxml.jackson.annotation.*;

public class StatusModel {
    private boolean status;

    @JsonProperty("status")
    public boolean getStatus() { return status; }
    @JsonProperty("status")
    public void setStatus(boolean value) { this.status = value; }
}
