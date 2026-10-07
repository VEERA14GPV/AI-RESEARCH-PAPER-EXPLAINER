package com.paperexplainer.service;

import com.paperexplainer.client.OllamaChatClient;
import com.paperexplainer.dto.ChatRequest;
import com.paperexplainer.dto.ChatResponse;
import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.entity.ChatMessage;
import com.paperexplainer.entity.Paper;
import com.paperexplainer.exception.PaperNotFoundException;
import com.paperexplainer.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long PAPER_ID = 1L;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private PaperService paperService;

    @Mock
    private RagService ragService;

    @Mock
    private OllamaChatClient chatClient;

    private ChatService chatService;
    private Paper paper;

    @BeforeEach
    @SuppressWarnings("unused") // invoked reflectively by JUnit, not called directly
    void setUp() {

        chatService = new ChatService(
                chatMessageRepository,
                paperService,
                ragService,
                chatClient
        );

        paper = new Paper();
        paper.setFilePath("/storage/papers/1/paper.pdf");
        ReflectionTestUtils.setField(paper, "id", PAPER_ID);
    }

    @Test
    void chat_validQuestion_retrievesContextExactlyOnceAndCallsLlm() {

        when(paperService.getPaperEntity(PAPER_ID)).thenReturn(paper);

        List<SearchResult> sources = List.of(
                new SearchResult(10L, PAPER_ID, 0, 3, "attention is a mechanism", 0.9)
        );

        when(ragService.retrieve(PAPER_ID, "What is attention?", 5))
                .thenReturn(sources);
        when(ragService.buildPrompt("What is attention?", sources))
                .thenReturn("BUILT PROMPT");
        when(chatClient.generateResponse("BUILT PROMPT"))
                .thenReturn("Attention lets the model focus on relevant tokens.");

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage("What is attention?");

        ChatResponse response = chatService.chat(request);

        // Placeholder response is gone; the real LLM answer is returned.
        assertThat(response.getAnswer())
                .isEqualTo("Attention lets the model focus on relevant tokens.")
                .doesNotContain("AI generation is not connected yet");
        assertThat(response.getPaperId()).isEqualTo(PAPER_ID);
        assertThat(response.getSources()).isEqualTo(sources);

        verify(ragService, times(1)).retrieve(PAPER_ID, "What is attention?", 5);
        verify(chatClient, times(1)).generateResponse("BUILT PROMPT");

        ArgumentCaptor<ChatMessage> savedCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(savedCaptor.capture());

        List<ChatMessage> saved = savedCaptor.getAllValues();
        assertThat(saved.get(0).getRole()).isEqualTo("USER");
        assertThat(saved.get(0).getContent()).isEqualTo("What is attention?");
        assertThat(saved.get(0).getPaper()).isSameAs(paper);

        assertThat(saved.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(saved.get(1).getContent()).isEqualTo("Attention lets the model focus on relevant tokens.");
        assertThat(saved.get(1).getPaper()).isSameAs(paper);
    }

    @Test
    void chat_paperNotFound_propagatesAndNeverCallsRagOrLlm() {

        when(paperService.getPaperEntity(PAPER_ID))
                .thenThrow(new PaperNotFoundException(PAPER_ID));

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage("What is attention?");

        assertThatThrownBy(() -> chatService.chat(request))
                .isInstanceOf(PaperNotFoundException.class);

        verifyNoInteractions(ragService, chatClient, chatMessageRepository);
    }

    @Test
    void chat_blankQuestion_throwsWithoutTouchingCollaborators() {

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage("   ");

        assertThatThrownBy(() -> chatService.chat(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(paperService, ragService, chatClient, chatMessageRepository);
    }

    @Test
    void chat_nullQuestion_throws() {

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage(null);

        assertThatThrownBy(() -> chatService.chat(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chat_paperIdNotSetOnRequest_stillWorksBecauseControllerAlwaysSetsItFromThePath() {

        // Regression test: ChatRequest.paperId used to be @NotNull/@Positive,
        // so a body without it (the correct shape, since the URL already
        // carries the paper id) failed validation before this method ever
        // ran. ChatService itself never required it either - the field is
        // only ever populated by ChatController from the path variable
        // before calling this method, so this simulates that exact call
        // shape with paperId set programmatically rather than parsed from
        // a validated request body.
        when(paperService.getPaperEntity(PAPER_ID)).thenReturn(paper);

        List<SearchResult> sources = List.of();
        when(ragService.retrieve(PAPER_ID, "What is this paper about?", 5)).thenReturn(sources);
        when(ragService.buildPrompt("What is this paper about?", sources)).thenReturn("PROMPT");
        when(chatClient.generateResponse("PROMPT")).thenReturn("It's about attention.");

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID); // set the way ChatController does it, not via body binding
        request.setMessage("What is this paper about?");

        ChatResponse response = chatService.chat(request);

        assertThat(response.getAnswer()).isEqualTo("It's about attention.");
    }

    @Test
    void chat_retrievalFailure_propagatesAndNeverCallsLlm() {

        when(paperService.getPaperEntity(PAPER_ID)).thenReturn(paper);
        when(ragService.retrieve(eq(PAPER_ID), anyString(), eq(5)))
                .thenThrow(new RuntimeException("embedding service unavailable"));

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage("What is attention?");

        assertThatThrownBy(() -> chatService.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("embedding service unavailable");

        verifyNoInteractions(chatClient);
        // The user question was recorded before retrieval ran; the surrounding
        // @Transactional boundary (not exercised by this unit test) is what
        // rolls this back so no orphaned message survives a failed reply.
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void chat_llmFailure_propagatesWithoutSavingFakeAnswer() {

        when(paperService.getPaperEntity(PAPER_ID)).thenReturn(paper);

        List<SearchResult> sources = List.of();
        when(ragService.retrieve(eq(PAPER_ID), anyString(), eq(5))).thenReturn(sources);
        when(ragService.buildPrompt(anyString(), eq(sources))).thenReturn("BUILT PROMPT");
        when(chatClient.generateResponse("BUILT PROMPT"))
                .thenThrow(new RuntimeException("Ollama request failed"));

        ChatRequest request = new ChatRequest();
        request.setPaperId(PAPER_ID);
        request.setMessage("What is attention?");

        assertThatThrownBy(() -> chatService.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ollama request failed");

        // Only the user message was saved (before the LLM call) - no fake
        // assistant answer was ever constructed or persisted.
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }
}
