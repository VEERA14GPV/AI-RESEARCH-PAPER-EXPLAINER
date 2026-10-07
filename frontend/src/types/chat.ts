import type { Paper } from './paper'
import type { SearchResult } from './search'

export type ChatRole = 'USER' | 'ASSISTANT'

/**
 * Mirrors entity/ChatMessage.java exactly, as returned by
 * GET /api/papers/:paperId/chat.
 *
 * Note: ChatMessage.paper has no @JsonIgnore on the backend, so the full
 * parent Paper is actually nested in every message the API returns. It's
 * included here for type accuracy but the UI never needs to render it.
 */
export interface ChatMessage {
  id: number
  paper: Paper
  role: ChatRole
  content: string
  createdAt: string
}

/**
 * Mirrors dto/ChatRequest.java - body of POST /api/papers/:paperId/chat.
 *
 * paperId IS required here even though the URL already carries it: the
 * backend validates ChatRequest.paperId (@NotNull @Positive) before the
 * controller ever gets a chance to override it from the path variable, so
 * omitting it causes a 400 before the question is even processed.
 */
export interface ChatRequestBody {
  paperId: number
  message: string
}

/**
 * Mirrors dto/ChatResponse.java exactly.
 */
export interface ChatResponse {
  paperId: number
  answer: string
  sources: SearchResult[]
}
