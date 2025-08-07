package com.example.demo.controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/rooms/{roomId}/messages")
    public CompletableFuture<ResponseEntity<String>> sendMessage(
            @PathVariable String roomId,
            @RequestBody ChatMessage message) {

        message.setRoomId(roomId);
        return chatService.sendMessage(message)
                .thenApply(messageId -> ResponseEntity.ok(messageId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public CompletableFuture<ResponseEntity<List<ChatMessage>>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {

        return chatService.getMessages(roomId, limit)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/projects/{projectId}/membership")
    public CompletableFuture<ResponseEntity<Boolean>> checkProjectMembership(
            @PathVariable String projectId,
            Authentication authentication
    ) {
        String userId = authentication.getName(); // 현재 로그인된 사용자 ID
        return chatService.isProjectMember(projectId, userId)
                .thenApply(isMember -> ResponseEntity.ok(isMember));
    }
}