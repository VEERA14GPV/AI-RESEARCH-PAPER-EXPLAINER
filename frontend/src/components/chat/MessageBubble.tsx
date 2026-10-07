import { Bot, User } from 'lucide-react'
import { MarkdownAnswer } from './MarkdownAnswer'
import { SourceList } from '@/components/sources/SourceList'
import type { SearchResult } from '@/types/search'
import { cn } from '@/utils/cn'
import { formatTime } from '@/utils/format'

interface MessageBubbleProps {
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
  sources?: SearchResult[]
}

export function MessageBubble({ role, content, createdAt, sources }: MessageBubbleProps) {
  const isUser = role === 'USER'

  return (
    <div className={cn('flex gap-3', isUser && 'flex-row-reverse')}>
      <div
        className={cn(
          'mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full',
          isUser ? 'bg-slate-200 text-slate-600' : 'bg-accent-100 text-accent-700',
        )}
        aria-hidden
      >
        {isUser ? <User className="size-3.5" /> : <Bot className="size-3.5" />}
      </div>

      <div className={cn('flex max-w-[85%] flex-col', isUser && 'items-end')}>
        <div
          className={cn(
            'rounded-2xl px-4 py-2.5',
            isUser
              ? 'bg-accent-600 text-white'
              : 'border border-slate-200 bg-white text-slate-800',
          )}
        >
          {isUser ? (
            <p className="whitespace-pre-wrap text-sm">{content}</p>
          ) : (
            <MarkdownAnswer content={content} />
          )}
        </div>

        <span className="mt-1 px-1 text-[11px] text-slate-400">{formatTime(createdAt)}</span>

        {!isUser && sources && sources.length > 0 && <SourceList sources={sources} />}
      </div>
    </div>
  )
}
