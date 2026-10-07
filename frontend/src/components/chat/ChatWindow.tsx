import { Bot, MessageCircle } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { ChatInput } from './ChatInput'
import { MessageBubble } from './MessageBubble'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { Skeleton } from '@/components/ui/Skeleton'
import { useChat } from '@/hooks/useChat'
import type { SearchResult } from '@/types/search'
import { getErrorMessage } from '@/utils/errors'

interface ChatWindowProps {
  paperId: number
  chatEnabled: boolean
  disabledReason?: string
}

export function ChatWindow({ paperId, chatEnabled, disabledReason }: ChatWindowProps) {
  const {
    messages,
    isHistoryLoading,
    historyError,
    refetchHistory,
    sendMessage,
    isSending,
    sendError,
    resetSendError,
  } = useChat(paperId)

  const scrollRef = useRef<HTMLDivElement>(null)
  const [pendingQuestion, setPendingQuestion] = useState<string | null>(null)

  // The backend only persists role/content/createdAt for chat history - it
  // never stores per-message sources. Sources are only available on the
  // ChatResponse returned by the send call itself, so they're kept here
  // (correlated to the matching answer by content) rather than invented for
  // older, reloaded history.
  const [lastAnswer, setLastAnswer] = useState<{ content: string; sources: SearchResult[] } | null>(
    null,
  )

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, isSending])

  async function handleSend(message: string) {
    setPendingQuestion(message)
    resetSendError()
    try {
      const response = await sendMessage(message)
      setLastAnswer({ content: response.answer, sources: response.sources })
    } catch {
      // Surfaced via sendError below; pendingQuestion stays so the user sees
      // what failed to send.
    } finally {
      setPendingQuestion(null)
    }
  }

  function handleRetry() {
    if (pendingQuestion) {
      void handleSend(pendingQuestion)
    }
  }

  if (isHistoryLoading) {
    return (
      <div className="flex flex-1 flex-col gap-4 p-4" aria-busy>
        <Skeleton className="h-16 w-3/4 rounded-2xl" />
        <Skeleton className="ml-auto h-10 w-1/2 rounded-2xl" />
        <Skeleton className="h-20 w-2/3 rounded-2xl" />
      </div>
    )
  }

  if (historyError) {
    return (
      <div className="flex flex-1 items-center justify-center p-4">
        <ErrorState message={getErrorMessage(historyError)} onRetry={() => refetchHistory()} />
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div ref={scrollRef} className="scroll-thin flex-1 space-y-5 overflow-y-auto p-4">
        {messages.length === 0 && !pendingQuestion && (
          <EmptyState
            icon={MessageCircle}
            title="Ask your first question"
            description="Try “What is this paper about?” or “What are the key findings?”"
          />
        )}

        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            role={message.role}
            content={message.content}
            createdAt={message.createdAt}
            sources={
              message.role === 'ASSISTANT' && lastAnswer?.content === message.content
                ? lastAnswer.sources
                : undefined
            }
          />
        ))}

        {pendingQuestion && (
          <MessageBubble role="USER" content={pendingQuestion} createdAt={new Date().toISOString()} />
        )}

        {isSending && (
          <div className="flex gap-3" aria-live="polite" aria-label="Waiting for an answer">
            <div
              className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full bg-accent-100 text-accent-700"
              aria-hidden
            >
              <Bot className="size-3.5" />
            </div>
            <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-500">
              <span className="flex gap-1">
                <span className="size-1.5 animate-bounce rounded-full bg-slate-300 [animation-delay:-0.3s]" />
                <span className="size-1.5 animate-bounce rounded-full bg-slate-300 [animation-delay:-0.15s]" />
                <span className="size-1.5 animate-bounce rounded-full bg-slate-300" />
              </span>
              Thinking…
            </div>
          </div>
        )}

        {sendError && (
          <ErrorState message={getErrorMessage(sendError)} onRetry={handleRetry} compact />
        )}
      </div>

      <ChatInput
        onSend={handleSend}
        isSending={isSending}
        disabled={!chatEnabled}
        placeholder={
          chatEnabled ? undefined : disabledReason ?? 'Chat is unavailable for this paper.'
        }
      />
    </div>
  )
}
