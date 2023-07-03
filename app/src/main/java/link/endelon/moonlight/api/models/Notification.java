package link.endelon.moonlight.api.models;

import com.fasterxml.jackson.annotation.*;

public class Notification {
    private Long id;
    private String channel;
    private String content;
    private String title;
    private String url = "";

    @JsonProperty("id")
    public Long getid() { return id; }
    @JsonProperty("id")
    public void setid(Long value) { this.id = value; }

    @JsonProperty("channel")
    public String getChannel() { return channel; }
    @JsonProperty("channel")
    public void setChannel(String value) { this.channel = value; }

    @JsonProperty("content")
    public String getContent() { return content; }
    @JsonProperty("content")
    public void setContent(String value) { this.content = value; }

    @JsonProperty("title")
    public String getTitle() { return title; }
    @JsonProperty("title")
    public void setTitle(String value) { this.title = value; }

    @JsonProperty("url")
    public String getUrl() { return url; }
    @JsonProperty("url")
    public void setUrl(String value) { this.url = value; }
}
