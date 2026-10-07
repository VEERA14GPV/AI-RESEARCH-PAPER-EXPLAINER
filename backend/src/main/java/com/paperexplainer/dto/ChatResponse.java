package com.paperexplainer.dto;

import java.util.List;

public class ChatResponse {

    private Long paperId;
    private String answer;
    private List<SearchResult> sources;

    public ChatResponse() {
    }

    public ChatResponse(
            Long paperId,
            String answer,
            List<SearchResult> sources
    ) {
        this.paperId = paperId;
        this.answer = answer;
        this.sources = sources;
    }

    public Long getPaperId() {
        return paperId;
    }

    public String getAnswer() {
        return answer;
    }

    public List<SearchResult> getSources() {
        return sources;
    }
}