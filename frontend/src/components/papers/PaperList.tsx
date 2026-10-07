import { Inbox } from 'lucide-react'
import { PaperListItem } from './PaperListItem'
import { PaperListSkeleton } from './PaperListSkeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { usePapers } from '@/hooks/usePapers'
import { getErrorMessage } from '@/utils/errors'

export function PaperList() {
  const { data: papers, isLoading, isError, error, refetch } = usePapers()

  if (isLoading) {
    return <PaperListSkeleton />
  }

  if (isError) {
    return <ErrorState message={getErrorMessage(error)} onRetry={() => refetch()} />
  }

  if (!papers || papers.length === 0) {
    return (
      <EmptyState
        icon={Inbox}
        title="No papers yet"
        description="Upload a PDF above to get started."
      />
    )
  }

  return (
    <div className="space-y-3">
      {papers.map((paper) => (
        <PaperListItem key={paper.id} paper={paper} />
      ))}
    </div>
  )
}
