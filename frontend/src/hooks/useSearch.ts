import { useMutation } from '@tanstack/react-query'
import { searchPaper } from '@/api/searchApi'

/**
 * A mutation (not a query) because search is an on-demand, user-triggered
 * action rather than passive server state that should auto-refetch.
 */
export function useSearch(paperId: number) {
  return useMutation({
    mutationFn: (query: string) => searchPaper(paperId, query),
  })
}
