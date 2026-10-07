import { useQuery } from '@tanstack/react-query'
import { getPaper } from '@/api/papersApi'
import { queryKeys } from '@/api/queryKeys'
import type { Paper } from '@/types/paper'

/**
 * Ingestion normally runs synchronously inside the upload request, so a
 * freshly-created paper already comes back PROCESSED/FAILED. This polling
 * is a defensive safety net for papers that are (or get stuck) UPLOADED /
 * PROCESSING - e.g. older records from before the ingestion pipeline
 * existed - rather than something the happy path depends on.
 */
export function usePaper(paperId: number | undefined) {
  return useQuery({
    queryKey: paperId !== undefined ? queryKeys.paper(paperId) : ['papers', 'invalid'],
    queryFn: () => getPaper(paperId as number),
    enabled: paperId !== undefined && Number.isFinite(paperId),
    refetchInterval: (query) => {
      const data = query.state.data as Paper | undefined
      if (!data) return false
      return data.status === 'UPLOADED' || data.status === 'PROCESSING' ? 4000 : false
    },
  })
}
