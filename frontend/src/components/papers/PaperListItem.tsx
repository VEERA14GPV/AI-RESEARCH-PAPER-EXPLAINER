import { ArrowRight, FileText } from 'lucide-react'
import { Link } from 'react-router-dom'
import { StatusBadge } from './StatusBadge'
import type { Paper } from '@/types/paper'
import { formatDate, formatFileSize } from '@/utils/format'

export function PaperListItem({ paper }: { paper: Paper }) {
  return (
    <Link
      to={`/papers/${paper.id}`}
      className="group flex items-center gap-4 rounded-xl border border-slate-200 bg-white px-4 py-3.5 transition-colors hover:border-accent-200 hover:bg-accent-50/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-600"
    >
      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500 group-hover:bg-accent-100 group-hover:text-accent-600">
        <FileText className="size-5" aria-hidden />
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate font-medium text-slate-800">{paper.title}</p>
        <p className="mt-0.5 text-xs text-slate-500">
          {formatDate(paper.createdAt)} · {formatFileSize(paper.fileSize)}
        </p>
      </div>

      <StatusBadge status={paper.status} className="shrink-0" />

      <ArrowRight
        className="size-4 shrink-0 text-slate-300 transition-transform group-hover:translate-x-0.5 group-hover:text-accent-600"
        aria-hidden
      />
    </Link>
  )
}
