package com.paperexplainer.controller;

import com.paperexplainer.dto.ChatResponse;
import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.exception.PaperNotFoundException;
import com.paperexplainer.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chat_validRequest_returnsAnswerAndSources() throws Exception {

        List<SearchResult> sources = List.of(
                new SearchResult(10L, 1L, 0, 2, "attention excerpt", 0.9)
        );

        when(chatService.chat(any())).thenReturn(
                new ChatResponse(1L, "Attention lets the model focus on relevant tokens.", sources)
        );

        mockMvc.perform(post("/api/papers/1/chat")
                        .contentType("application/json")
                        .content("""
                                {"paperId": 1, "message": "What is attention?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperId").value(1))
                .andExpect(jsonPath("$.answer").value("Attention lets the model focus on relevant tokens."))
                .andExpect(jsonPath("$.sources[0].chunkId").value(10))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(2));

        // The URL's paperId is authoritative, overriding whatever the body carried (or omitted).
        verify(chatService).chat(argThat(request ->
                request.getPaperId().equals(1L) && request.getMessage().equals("What is attention?")
        ));
    }

    @Test
    void chat_blankMessage_returns400ValidationError() throws Exception {

        // paperId included so the failure is unambiguously about the blank
        // message, not a missing-paperId validation failure.
        mockMvc.perform(post("/api/papers/1/chat")
                        .contentType("application/json")
                        .content("""
                                {"paperId": 1, "message": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_missingMessage_returns400ValidationError() throws Exception {

        mockMvc.perform(post("/api/papers/1/chat")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_bodyWithoutPaperId_succeeds_urlAloneIsTheSourceOfTruth() throws Exception {

        // Regression test: ChatRequest.paperId previously had @NotNull, so
        // the correct request shape here - omitting paperId from the body
        // since the URL already carries it - used to fail validation with
        // "paperId: Paper ID is required" before the controller ever ran.
        when(chatService.chat(any())).thenReturn(
                new ChatResponse(1L, "answer", List.of())
        );

        mockMvc.perform(post("/api/papers/1/chat")
                        .contentType("application/json")
                        .content("""
                                {"message": "What is this research paper about?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("answer"));

        verify(chatService).chat(argThat(request ->
                request.getPaperId().equals(1L)
                        && request.getMessage().equals("What is this research paper about?")
        ));
    }

    @Test
    void chat_paperIdInBodyIsOverriddenByPathVariable() throws Exception {

        when(chatService.chat(any())).thenReturn(
                new ChatResponse(1L, "answer", List.of())
        );

        // Body claims paperId 999, but the URL says 1 - the URL must win.
        mockMvc.perform(post("/api/papers/1/chat")
                        .contentType("application/json")
                        .content("""
                                {"paperId": 999, "message": "What is attention?"}
                                """))
                .andExpect(status().isOk());

        verify(chatService).chat(argThat(request -> request.getPaperId().equals(1L)));
    }

    @Test
    void chat_paperNotFound_returns404() throws Exception {

        when(chatService.chat(any())).thenThrow(new PaperNotFoundException(999L));

        mockMvc.perform(post("/api/papers/999/chat")
                        .contentType("application/json")
                        .content("""
                                {"paperId": 999, "message": "What is attention?"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatHistory_validPaperId_returnsOk() throws Exception {

        when(chatService.getChatHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/papers/1/chat"))
                .andExpect(status().isOk());
    }
}
