package com.paperexplainer.controller;

import com.paperexplainer.dto.ChatRequest;
import com.paperexplainer.dto.ChatResponse;
import com.paperexplainer.entity.ChatMessage;
import com.paperexplainer.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Ask a question about a research paper.
     */
    @PostMapping("/{paperId}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable Long paperId,
            @Valid @RequestBody ChatRequest request
    ) {

        /*
         * Keep the URL paper ID authoritative.
         * This prevents accidentally asking questions
         * against a different paper ID in the request body.
         */
        request.setPaperId(paperId);

        ChatResponse response =
                chatService.chat(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get previous chat messages for a paper.
     */
    @GetMapping("/{paperId}/chat")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Long paperId
    ) {

        return ResponseEntity.ok(
                chatService.getChatHistory(paperId)
        );
    }
}