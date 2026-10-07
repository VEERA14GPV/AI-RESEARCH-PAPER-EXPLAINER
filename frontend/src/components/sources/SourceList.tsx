import { ChevronDown, FileStack } from 'lucide-react'
import { useState } from 'react'
import type { SearchResult } from '@/types/search'
import { cn } from '@/utils/cn'
import { formatSimilarity } from '@/utils/format'

/**
 * Renders exactly the metadata the backend returns in SearchResult - page
 * number, chunk id, similarity, content preview. Never shows an embedding
 * vector (SearchResult never carries one), and never invents a citation
 * that isn't backed by a real retrieved chunk.
 */
export function SourceList({ sources }: { sources: SearchResult[] }) {
  const [isOpen, setIsOpen] = useState(false)

  if (sources.length === 0) return null

  return (
    <div className="mt-2">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        className="flex items-center gap-1.5 rounded-md px-1.5 py-1 text-xs font-medium text-slate-500 hover:bg-slate-100 hover:text-slate-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-accent-600"
      >
        <FileStack className="size-3.5" aria-hidden />
        Sources ({sources.length})
        <ChevronDown className={cn('size-3.5 transition-transform', isOpen && 'rotate-180')} aria-hidden />
      </button>

      {isOpen && (
        <div className="mt-2 grid animate-slide-up gap-2 sm:grid-cols-2">
          {sources.map((source, index) => (
            <SourceCard key={source.chunkId} source={source} index={index} />
          ))}
        </div>
      )}
    </div>
  )
}

// Roughly the point where a 3-line clamp at typical sidebar/card width
// starts truncating visible content - below this, "Show more" would just
// be a dead button since nothing is actually hidden.
const EXCERPT_CLAMP_THRESHOLD = 180

function SourceCard({ source, index }: { source: SearchResult; index: number }) {
  const [isExpanded, setIsExpanded] = useState(false)
  const canExpand = source.content.length > EXCERPT_CLAMP_THRESHOLD

  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3 text-sm">
      <div className="mb-1 flex items-center justify-between gap-2">
        <span className="text-xs font-semibold text-accent-700">Source {index + 1}</span>
        <span
          className="text-xs font-medium text-slate-500"
          title="Cosine similarity to your question"
        >
          Relevance: {formatSimilarity(source.similarity)}
        </span>
      </div>

      <p className="mb-1.5 text-xs text-slate-500">
        {source.pageNumber != null ? `Page ${source.pageNumber}` : `Chunk #${source.chunkIndex}`}
      </p>

      <p className={cn('whitespace-pre-line text-slate-600', !isExpanded && 'line-clamp-3')}>
        &ldquo;{source.content}&rdquo;
      </p>

      {canExpand && (
        <button
          type="button"
          onClick={() => setIsExpanded((expanded) => !expanded)}
          className="mt-1 text-xs font-medium text-accent-600 hover:text-accent-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-accent-600"
        >
          {isExpanded ? 'Show less' : 'Show more'}
        </button>
      )}
    </div>
  )
}
