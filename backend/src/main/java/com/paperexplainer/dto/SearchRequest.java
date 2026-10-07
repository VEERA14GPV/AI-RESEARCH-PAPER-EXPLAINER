package com.paperexplainer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/*
 * paperId is deliberately NOT a field here: the search endpoint is
 * path-scoped (POST /api/papers/{paperId}/search), so the URL is already
 * the one source of truth for which paper is being searched. An earlier
 * version of this DTO also carried a paperId field, but SearchController
 * never read it (it used the path variable instead) - a client-supplied
 * body value could silently disagree with the URL and nothing would
 * notice. Removing it avoids that split-brain instead of papering over it.
 */
public class SearchRequest {

    @NotBlank(message = "Search query is required")
    private String query;

    @Positive(message = "Top K must be greater than zero")
    private Integer topK = 5;

    public SearchRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}