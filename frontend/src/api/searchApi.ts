import { apiClient } from './axios'
import type { SearchRequestBody, SearchResult } from '@/types/search'

export async function searchPaper(
  paperId: number,
  query: string,
  topK?: number,
): Promise<SearchResult[]> {
  const body: SearchRequestBody = { query, ...(topK ? { topK } : {}) }
  const response = await apiClient.post<SearchResult[]>(
    `/api/papers/${paperId}/search`,
    body,
  )
  return response.data
}
