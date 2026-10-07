import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deletePaper } from '@/api/papersApi'
import { queryKeys } from '@/api/queryKeys'

export function useDeletePaper() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (paperId: number) => deletePaper(paperId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.papers })
    },
  })
}
