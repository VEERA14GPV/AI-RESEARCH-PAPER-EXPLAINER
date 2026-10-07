import { apiClient } from './axios'
import type { ChatMessage, ChatRequestBody, ChatResponse } from '@/types/chat'

export async function getChatHistory(paperId: number): Promise<ChatMessage[]> {
  const response = await apiClient.get<ChatMessage[]>(`/api/papers/${paperId}/chat`)
  return response.data
}

export async function sendChatMessage(
  paperId: number,
  message: string,
): Promise<ChatResponse> {
  // paperId is included in the body (not just the URL) because
  // ChatRequest.paperId is validated server-side before the controller
  // overrides it from the path variable - see types/chat.ts.
  const body: ChatRequestBody = { paperId, message }
  const response = await apiClient.post<ChatResponse>(`/api/papers/${paperId}/chat`, body)
  return response.data
}
