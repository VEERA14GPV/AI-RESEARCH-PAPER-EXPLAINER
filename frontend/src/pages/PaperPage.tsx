import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { ChatWindow } from '@/components/chat/ChatWindow'
import { PaperInfoPanel } from '@/components/papers/PaperInfoPanel'
import { Card } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { Skeleton } from '@/components/ui/Skeleton'
import { usePaper } from '@/hooks/usePaper'
import { STATUS_META, isChatEnabled } from '@/utils/status'
import { getErrorMessage, isNotFoundError } from '@/utils/errors'

export function PaperPage() {
  const { paperId } = useParams<{ paperId: string }>()
  const numericPaperId = paperId ? Number(paperId) : undefined
  const isValidId = numericPaperId !== undefined && Number.isFinite(numericPaperId)

  const { data: paper, isLoading, isError, error, refetch } = usePaper(
    isValidId ? numericPaperId : undefined,
  )

  if (!isValidId) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <ErrorState message="That paper link doesn't look valid." />
        <BackLink />
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="mx-auto grid max-w-6xl gap-4 px-4 py-6 sm:px-6 lg:grid-cols-[380px_1fr] lg:h-[calc(100vh-56px)]">
        <Skeleton className="h-64 rounded-xl lg:h-full" />
        <Skeleton className="h-96 rounded-xl lg:h-full" />
      </div>
    )
  }

  if (isError || !paper) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <ErrorState
          message={
            isNotFoundError(error)
              ? 'This paper could not be found. It may have been removed.'
              : getErrorMessage(error)
          }
          onRetry={isNotFoundError(error) ? undefined : () => refetch()}
        />
        <BackLink />
      </div>
    )
  }

  const chatEnabled = isChatEnabled(paper.status)

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-4 sm:px-6 lg:h-[calc(100vh-56px)] lg:grid lg:grid-cols-[380px_1fr] lg:py-6">
      <Card className="overflow-hidden lg:h-full">
        <PaperInfoPanel paper={paper} />
      </Card>

      <Card className="flex h-[70vh] flex-col overflow-hidden lg:h-full">
        <ChatWindow
          paperId={paper.id}
          chatEnabled={chatEnabled}
          disabledReason={
            chatEnabled ? undefined : STATUS_META[paper.status].description
          }
        />
      </Card>
    </div>
  )
}

function BackLink() {
  return (
    <Link
      to="/"
      className="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-accent-600 hover:text-accent-700"
    >
      <ArrowLeft className="size-4" aria-hidden />
      Back to papers
    </Link>
  )
}
