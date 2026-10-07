import { AlertTriangle, Calendar, FileText, HardDrive, Search, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { StatusBadge } from './StatusBadge'
import { Button } from '@/components/ui/Button'
import { SearchPanel } from '@/components/search/SearchPanel'
import { useDeletePaper } from '@/hooks/useDeletePaper'
import type { Paper } from '@/types/paper'
import { getErrorMessage } from '@/utils/errors'
import { formatDate, formatFileSize } from '@/utils/format'
import { isChatEnabled } from '@/utils/status'

export function PaperInfoPanel({ paper }: { paper: Paper }) {
  const navigate = useNavigate()
  const deletePaper = useDeletePaper()
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const searchSectionRef = useRef<HTMLDivElement>(null)

  function handleConfirmDelete() {
    deletePaper.mutate(paper.id, {
      onSuccess: () => navigate('/'),
    })
  }

  function scrollToSearch() {
    searchSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const searchEnabled = isChatEnabled(paper.status)

  return (
    <div className="flex h-full flex-col gap-5 overflow-y-auto p-4 sm:p-5">
      <div>
        <div className="mb-2 flex items-start justify-between gap-2">
          <div className="flex size-10 items-center justify-center rounded-lg bg-accent-50 text-accent-600">
            <FileText className="size-5" aria-hidden />
          </div>
          <StatusBadge status={paper.status} />
        </div>
        <h1 className="text-lg font-semibold leading-snug text-slate-900">{paper.title}</h1>
      </div>

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center gap-2 text-slate-500">
          <Calendar className="size-4 shrink-0" aria-hidden />
          <dt className="sr-only">Uploaded</dt>
          <dd>Uploaded {formatDate(paper.createdAt)}</dd>
        </div>
        <div className="flex items-center gap-2 text-slate-500">
          <HardDrive className="size-4 shrink-0" aria-hidden />
          <dt className="sr-only">File size</dt>
          <dd>
            {paper.fileName} · {formatFileSize(paper.fileSize)}
          </dd>
        </div>
      </dl>

      {paper.status === 'FAILED' && (
        <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          <p>
            Processing failed for this paper. There is no automatic retry - remove it below and
            upload the PDF again.
          </p>
        </div>
      )}

      {(paper.status === 'UPLOADED' || paper.status === 'PROCESSING') && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-700">
          This paper is still being processed. Chat will be available once it finishes.
        </div>
      )}

      <div className="border-t border-slate-200 pt-4">
        <h2 className="mb-2 text-sm font-medium text-slate-700">Quick actions</h2>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            icon={<Search className="size-3.5" aria-hidden />}
            onClick={scrollToSearch}
            disabled={!searchEnabled}
            title={searchEnabled ? undefined : 'Available once this paper finishes processing.'}
          >
            Search
          </Button>

          {!isConfirmingDelete && (
            <Button
              variant="danger"
              size="sm"
              icon={<Trash2 className="size-3.5" aria-hidden />}
              onClick={() => setIsConfirmingDelete(true)}
            >
              Delete
            </Button>
          )}

          {isConfirmingDelete && (
            <div className="flex flex-wrap items-center gap-2 rounded-lg border border-red-200 bg-red-50 py-1 pl-2.5 pr-1.5">
              <span className="text-xs font-medium text-red-700">Delete this paper?</span>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsConfirmingDelete(false)}
                disabled={deletePaper.isPending}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmDelete}
                isLoading={deletePaper.isPending}
              >
                Confirm
              </Button>
            </div>
          )}
        </div>

        {deletePaper.isError && (
          <p className="mt-2 text-xs text-red-700">{getErrorMessage(deletePaper.error)}</p>
        )}
      </div>

      <div ref={searchSectionRef} className="border-t border-slate-200 pt-4">
        <h2 className="mb-2 text-sm font-medium text-slate-700">Search this paper</h2>
        <SearchPanel paperId={paper.id} disabled={!searchEnabled} />
      </div>
    </div>
  )
}
