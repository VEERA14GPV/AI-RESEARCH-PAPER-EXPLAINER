package com.paperexplainer.dto;

public class SearchResult {

    private Long chunkId;
    private Long paperId;
    private Integer chunkIndex;
    private Integer pageNumber;
    private String content;
    private Double similarity;

    public SearchResult() {
    }

    public SearchResult(
            Long chunkId,
            Long paperId,
            Integer chunkIndex,
            Integer pageNumber,
            String content,
            Double similarity
    ) {
        this.chunkId = chunkId;
        this.paperId = paperId;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.content = content;
        this.similarity = similarity;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public Long getPaperId() {
        return paperId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getContent() {
        return content;
    }

    public Double getSimilarity() {
        return similarity;
    }
}