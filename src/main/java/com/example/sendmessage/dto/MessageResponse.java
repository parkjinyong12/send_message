package com.example.sendmessage.dto;

public class MessageResponse {
    private boolean ok;
    private String channel;
    private String ts;
    private String error;

    public MessageResponse() {}

    public static MessageResponse ok(String channel, String ts) {
        MessageResponse r = new MessageResponse();
        r.ok = true;
        r.channel = channel;
        r.ts = ts;
        return r;
    }

    public static MessageResponse error(String error) {
        MessageResponse r = new MessageResponse();
        r.ok = false;
        r.error = error;
        return r;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTs() { return ts; }
    public void setTs(String ts) { this.ts = ts; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
} 