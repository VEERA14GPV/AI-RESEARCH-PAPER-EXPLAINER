import { AlertCircle, Search as SearchIcon } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { useSearch } from '@/hooks/useSearch'
import { getErrorMessage } from '@/utils/errors'
import { formatSimilarity } from '@/utils/format'

/**
 * A direct semantic-search interface over the existing
 * POST /api/papers/:paperId/search endpoint (SearchController /
 * VectorSearchService) - separate from chat, since it returns raw ranked
 * chunks rather than a generated answer. No second search implementation:
 * this calls the same backend search API chat's RAG pipeline is built on.
 */
export function SearchPanel({ paperId, disabled }: { paperId: number; disabled?: boolean }) {
  const [query, setQuery] = useState('')
  const search = useSearch(paperId)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed || disabled) return
    search.mutate(trimmed)
  }

  return (
    <div className="space-y-3">
      <form onSubmit={handleSubmit} className="flex gap-2">
        <div className="relative flex-1">
          <SearchIcon
            className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-slate-400"
            aria-hidden
          />
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            disabled={disabled}
            placeholder="Search within this paper…"
            aria-label="Search within this paper"
            className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-8 pr-3 text-sm placeholder:text-slate-400 focus:border-accent-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-accent-100 disabled:cursor-not-allowed disabled:opacity-60"
          />
        </div>
        <Button type="submit" size="sm" variant="secondary" disabled={disabled || !query.trim()} isLoading={search.isPending}>
          Search
        </Button>
      </form>

      {search.isPending && (
        <div className="space-y-2">
          <Skeleton className="h-14 w-full rounded-lg" />
          <Skeleton className="h-14 w-full rounded-lg" />
        </div>
      )}

      {search.isError && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <AlertCircle className="size-4 shrink-0" aria-hidden />
          {getErrorMessage(search.error)}
        </div>
      )}

      {search.isSuccess && search.data.length === 0 && (
        <p className="text-sm text-slate-500">No matching passages found.</p>
      )}

      {search.isSuccess && search.data.length > 0 && (
        <ul className="scroll-thin max-h-72 space-y-2 overflow-y-auto">
          {search.data.map((result) => (
            <li key={result.chunkId} className="rounded-lg border border-slate-200 bg-slate-50/60 p-3 text-sm">
              <div className="mb-1 flex items-center justify-between text-xs text-slate-500">
                <span className="font-medium text-slate-600">
                  {result.pageNumber != null ? `Page ${result.pageNumber}` : `Chunk #${result.chunkIndex}`}
                </span>
                <span>Similarity: {formatSimilarity(result.similarity)}</span>
              </div>
              <p className="line-clamp-3 text-slate-600">&ldquo;{result.content}&rdquo;</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
