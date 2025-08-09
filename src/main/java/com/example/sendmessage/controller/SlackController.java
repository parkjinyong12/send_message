package com.example.sendmessage.controller;

import com.example.sendmessage.dto.MessageRequest;
import com.example.sendmessage.dto.MessageResponse;
import com.example.sendmessage.service.SlackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class SlackController {

    private final SlackService slackService;

    public SlackController(SlackService slackService) {
        this.slackService = slackService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/api/v1/messages/send")
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody MessageRequest req) {
        try {
            Map<String, Object> extra = new HashMap<>();
            if (req.getBlocks() != null) extra.put("blocks", req.getBlocks());
            if (req.getMrkdwn() != null) extra.put("mrkdwn", req.getMrkdwn());
            MessageResponse result = slackService.sendMessage(req.getChannel(), req.getText(), extra);
            if (!result.isOk()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MessageResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MessageResponse.error("메시지 전송 중 오류"));
        }
    }
} 