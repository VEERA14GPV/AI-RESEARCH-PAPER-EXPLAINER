import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { uploadPaper } from '@/api/papersApi'
import { queryKeys } from '@/api/queryKeys'

export function useUploadPaper() {
  const queryClient = useQueryClient()
  const [progress, setProgress] = useState(0)

  const mutation = useMutation({
    mutationFn: (file: File) => uploadPaper(file, setProgress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.papers })
    },
    onSettled: () => {
      setProgress(0)
    },
  })

  return { ...mutation, progress }
}
