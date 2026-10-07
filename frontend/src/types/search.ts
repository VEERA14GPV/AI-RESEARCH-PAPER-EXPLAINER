/**
 * Mirrors dto/SearchResult.java exactly. `similarity` is already converted
 * to a cosine-similarity score by the backend (VectorSearchService) -
 * higher is more relevant. No embedding vector is ever included.
 */
export interface SearchResult {
  chunkId: number
  paperId: number
  chunkIndex: number
  pageNumber: number | null
  content: string
  similarity: number
}

/**
 * Mirrors dto/SearchRequest.java - body of POST /api/papers/:paperId/search.
 * paperId is deliberately not a field here (see backend comment); the URL
 * path segment is the sole source of truth for which paper is searched.
 */
export interface SearchRequestBody {
  query: string
  topK?: number
}
