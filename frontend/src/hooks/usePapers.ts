import { useQuery } from '@tanstack/react-query'
import { getPapers } from '@/api/papersApi'
import { queryKeys } from '@/api/queryKeys'

export function usePapers() {
  return useQuery({
    queryKey: queryKeys.papers,
    queryFn: getPapers,
  })
}
