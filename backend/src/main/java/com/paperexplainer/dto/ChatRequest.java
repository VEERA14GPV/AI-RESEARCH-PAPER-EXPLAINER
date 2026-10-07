package com.paperexplainer.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * paperId is deliberately unvalidated here (and not required in the
 * request body at all): the chat endpoint is path-scoped
 * (POST /api/papers/{paperId}/chat), so the URL is already the one source
 * of truth for which paper is being asked about - ChatController always
 * overwrites this field from the path variable before ChatService ever
 * sees it. An @NotNull/@Positive constraint here previously made the URL's
 * paperId insufficient on its own: a client that (correctly) omitted
 * paperId from the body, since the URL already carries it, got a 400
 * before the controller could even run. See SearchRequest, which omits a
 * paperId field entirely for the same reason.
 */
public class ChatRequest {

    private Long paperId;

    @NotBlank(message = "Message is required")
    private String message;

    public ChatRequest() {
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}