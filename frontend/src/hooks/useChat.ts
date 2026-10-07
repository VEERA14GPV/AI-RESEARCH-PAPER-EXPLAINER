import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getChatHistory, sendChatMessage } from '@/api/chatApi'
import { queryKeys } from '@/api/queryKeys'

export function useChat(paperId: number) {
  const queryClient = useQueryClient()

  const historyQuery = useQuery({
    queryKey: queryKeys.chatHistory(paperId),
    queryFn: () => getChatHistory(paperId),
  })

  const sendMutation = useMutation({
    mutationFn: (message: string) => sendChatMessage(paperId, message),
    onSuccess: () => {
      // Backend persists both the question and the answer - refetch so the
      // history stays the single source of truth (correct ids/timestamps).
      queryClient.invalidateQueries({ queryKey: queryKeys.chatHistory(paperId) })
    },
  })

  return {
    messages: historyQuery.data ?? [],
    isHistoryLoading: historyQuery.isLoading,
    historyError: historyQuery.error,
    refetchHistory: historyQuery.refetch,
    sendMessage: sendMutation.mutateAsync,
    isSending: sendMutation.isPending,
    sendError: sendMutation.error,
    resetSendError: sendMutation.reset,
  }
}
