import { Send } from 'lucide-react'
import { type KeyboardEvent, useState } from 'react'
import { Button } from '@/components/ui/Button'

interface ChatInputProps {
  onSend: (message: string) => void
  disabled?: boolean
  isSending?: boolean
  placeholder?: string
}

export function ChatInput({ onSend, disabled, isSending, placeholder }: ChatInputProps) {
  const [value, setValue] = useState('')

  function submit() {
    const trimmed = value.trim()
    if (!trimmed || disabled || isSending) return
    onSend(trimmed)
    setValue('')
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  return (
    <div className="flex items-end gap-2 border-t border-slate-200 bg-white p-3">
      <textarea
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={1}
        placeholder={placeholder ?? 'Ask a question about this paper…'}
        aria-label="Ask a question about this paper"
        className="max-h-40 min-h-10 flex-1 resize-none rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 focus:border-accent-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-accent-100 disabled:cursor-not-allowed disabled:opacity-60"
      />
      <Button
        onClick={submit}
        disabled={disabled || !value.trim()}
        isLoading={isSending}
        aria-label="Send message"
        icon={<Send className="size-4" aria-hidden />}
      >
        Send
      </Button>
    </div>
  )
}
