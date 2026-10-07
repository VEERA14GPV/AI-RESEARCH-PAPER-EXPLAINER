package com.paperexplainer.service;

import com.paperexplainer.client.OllamaChatClient;
import com.paperexplainer.dto.ChatRequest;
import com.paperexplainer.dto.ChatResponse;
import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.entity.ChatMessage;
import com.paperexplainer.entity.Paper;
import com.paperexplainer.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatService {

    private static final int DEFAULT_TOP_K = 5;

    private final ChatMessageRepository chatMessageRepository;
    private final PaperService paperService;
    private final RagService ragService;
    private final OllamaChatClient chatClient;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            PaperService paperService,
            RagService ragService,
            OllamaChatClient chatClient
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.paperService = paperService;
        this.ragService = ragService;
        this.chatClient = chatClient;
    }

    public ChatResponse chat(ChatRequest request) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException(
                    "Message is required."
            );
        }

        Paper paper = paperService.getPaperEntity(
                request.getPaperId()
        );

        ChatMessage userMessage = new ChatMessage();

        userMessage.setPaper(paper);
        userMessage.setRole("USER");
        userMessage.setContent(request.getMessage());

        chatMessageRepository.save(userMessage);

        /*
         * Retrieve the most relevant chunks for this question exactly once,
         * then ground the LLM's answer in them via RagService's prompt.
         * If retrieval or generation fails, the exception propagates as-is
         * (no fallback/fake answer) and this whole @Transactional method
         * rolls back, so a failed reply never leaves an orphaned user
         * message in chat history.
         */
        List<SearchResult> sources = ragService.retrieve(
                paper.getId(),
                request.getMessage(),
                DEFAULT_TOP_K
        );

        String prompt = ragService.buildPrompt(
                request.getMessage(),
                sources
        );

        String answer = chatClient.generateResponse(prompt);

        ChatMessage assistantMessage = new ChatMessage();

        assistantMessage.setPaper(paper);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(answer);

        chatMessageRepository.save(assistantMessage);

        return new ChatResponse(
                paper.getId(),
                answer,
                sources
        );
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(
            Long paperId
    ) {

        paperService.getPaperEntity(paperId);

        return chatMessageRepository
                .findByPaperIdOrderByCreatedAtAsc(paperId);
    }
}
