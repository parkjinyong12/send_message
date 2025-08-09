package com.example.sendmessage.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class MessageRequest {
    @NotBlank
    private String channel;

    @NotBlank
    private String text;

    // Optional: Slack Block Kit blocks
    private List<Map<String, Object>> blocks;

    // Optional: whether to enable Markdown in text (default true in Slack)
    private Boolean mrkdwn;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Map<String, Object>> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Map<String, Object>> blocks) {
        this.blocks = blocks;
    }

    public Boolean getMrkdwn() {
        return mrkdwn;
    }

    public void setMrkdwn(Boolean mrkdwn) {
        this.mrkdwn = mrkdwn;
    }
} 