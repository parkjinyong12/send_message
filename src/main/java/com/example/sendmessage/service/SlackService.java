package com.example.sendmessage.service;

import com.example.sendmessage.config.SlackProperties;
import com.example.sendmessage.dto.MessageResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class SlackService {
    private final SlackProperties slackProperties;
    private final CircuitBreakerRegistry cbRegistry;
    private final RateLimiterRegistry rlRegistry;
    private RestClient restClient;

    public SlackService(SlackProperties slackProperties, CircuitBreakerRegistry cbRegistry, RateLimiterRegistry rlRegistry) {
        this.slackProperties = slackProperties;
        this.cbRegistry = cbRegistry;
        this.rlRegistry = rlRegistry;
        this.restClient = RestClient.builder()
                .baseUrl(slackProperties.getApiBaseUrl())
                .build();
    }

    private String getTokenOrThrow() {
        String token = slackProperties.getBotToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("SLACK_BOT_TOKEN 환경 변수가 설정되지 않았습니다. .env 또는 배포 환경 변수로 설정하세요.");
        }
        return token;
    }

    private boolean looksLikeChannelId(String value) {
        return (value.startsWith("C") || value.startsWith("G")) && value.length() >= 8;
    }

    public String resolveChannelId(String channelOrName) {
        String value = channelOrName == null ? "" : channelOrName.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("채널명이 비어있습니다.");
        }
        if (looksLikeChannelId(value)) {
            return value;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        String token = getTokenOrThrow();
        String cursor = null;
        while (true) {
            String uri = slackProperties.getApiBaseUrl() + "/conversations.list?limit=1000&types=public_channel,private_channel" +
                    (cursor != null && !cursor.isBlank() ? "&cursor=" + cursor : "");
            Map<String, Object> resp = restClient.get()
                    .uri(URI.create(uri))
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (resp == null || !(Boolean.TRUE.equals(resp.get("ok")))) {
                break;
            }
            List<Map<String, Object>> channels = (List<Map<String, Object>>) resp.getOrDefault("channels", List.of());
            for (Map<String, Object> ch : channels) {
                String name = Objects.toString(ch.get("name"), "");
                if (normalized.equals(name)) {
                    return Objects.toString(ch.get("id"), value);
                }
            }
            Map<String, Object> meta = (Map<String, Object>) resp.getOrDefault("response_metadata", Map.of());
            Object next = meta.get("next_cursor");
            cursor = next == null ? null : Objects.toString(next, null);
            if (cursor == null || cursor.isBlank()) {
                break;
            }
        }
        return value; // 못 찾으면 원본 값 사용
    }

    private static void sleep(Duration d) {
        try { Thread.sleep(d.toMillis()); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private MessageResponse executeWithResilience(String destKey, Supplier<MessageResponse> supplier) {
        String name = "slack-" + destKey;
        CircuitBreaker cb = cbRegistry.circuitBreaker(name);
        RateLimiter rl = rlRegistry.rateLimiter(name);
        try {
            Supplier<MessageResponse> withRl = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(rl, supplier);
            Supplier<MessageResponse> withCb = io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateSupplier(cb, withRl);
            return withCb.get();
        } catch (Exception e) {
            return MessageResponse.error("일시적 오류로 전송 보류: " + e.getClass().getSimpleName());
        }
    }

    public MessageResponse sendMessage(String channelOrName, String text, Map<String, Object> extra) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("메시지 내용이 비어있습니다.");
        }
        return executeWithResilience("bot", () -> sendViaBot(channelOrName, text, extra));
    }

    private MessageResponse sendViaBot(String channelOrName, String text, Map<String, Object> extra) {
        String token = getTokenOrThrow();
        String channel = resolveChannelId(channelOrName);
        Map<String, Object> body = new HashMap<>();
        body.put("channel", channel);
        body.put("text", text);
        if (extra != null) {
            if (extra.containsKey("blocks")) body.put("blocks", extra.get("blocks"));
            if (extra.containsKey("mrkdwn")) body.put("mrkdwn", extra.get("mrkdwn"));
        }
        Duration[] waits = new Duration[]{Duration.ofMillis(200), Duration.ofMillis(500), Duration.ofMillis(1000)};
        for (int attempt = 0; attempt < waits.length; attempt++) {
            try {
                Map<String, Object> resp = restClient.post()
                        .uri("/chat.postMessage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                if (resp != null && Boolean.TRUE.equals(resp.get("ok"))) {
                    String ch = Objects.toString(resp.get("channel"), channel);
                    String ts = Objects.toString(resp.get("ts"), "");
                    return MessageResponse.ok(ch, ts);
                }
            } catch (Exception ignored) {}
            sleep(waits[attempt]);
        }
        return MessageResponse.error("Slack API 전송 실패");
    }
} 