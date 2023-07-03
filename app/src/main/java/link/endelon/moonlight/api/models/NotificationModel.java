package link.endelon.moonlight.api.models;

import com.fasterxml.jackson.annotation.*;

public class NotificationModel {
    private String action;
    private Notification notification;

    @JsonProperty("action")
    public String getAction() { return action; }
    @JsonProperty("action")
    public void setAction(String value) { this.action = value; }

    @JsonProperty("notification")
    public Notification getNotification() { return notification; }
    @JsonProperty("notification")
    public void setNotification(Notification value) { this.notification = value; }
}
