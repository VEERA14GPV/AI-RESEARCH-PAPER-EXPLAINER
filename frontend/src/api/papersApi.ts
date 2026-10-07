import { apiClient } from './axios'
import type { CreatePaperRequest, Paper } from '@/types/paper'

function titleFromFileName(fileName: string): string {
  const withoutExtension = fileName.replace(/\.pdf$/i, '')
  return withoutExtension.trim().length > 0 ? withoutExtension.trim() : fileName
}

export async function getPapers(): Promise<Paper[]> {
  const response = await apiClient.get<Paper[]>('/api/papers')
  return response.data
}

export async function getPaper(paperId: number): Promise<Paper> {
  const response = await apiClient.get<Paper>(`/api/papers/${paperId}`)
  return response.data
}

/**
 * POST /api/papers expects multipart/form-data with exactly two parts:
 *  - "request": a JSON part matching CreatePaperRequest (title, fileName,
 *    contentType, fileSize) - PaperController binds it via @RequestPart,
 *    which requires the part's own Content-Type to be application/json.
 *  - "file": the raw PDF bytes.
 */
export async function uploadPaper(
  file: File,
  onUploadProgress?: (percent: number) => void,
): Promise<Paper> {
  const requestBody: CreatePaperRequest = {
    title: titleFromFileName(file.name),
    fileName: file.name,
    contentType: file.type || 'application/pdf',
    fileSize: file.size,
  }

  const formData = new FormData()
  formData.append(
    'request',
    new Blob([JSON.stringify(requestBody)], { type: 'application/json' }),
  )
  formData.append('file', file)

  const response = await apiClient.post<Paper>('/api/papers', formData, {
    onUploadProgress: (event) => {
      if (!onUploadProgress || !event.total) return
      onUploadProgress(Math.round((event.loaded / event.total) * 100))
    },
  })

  return response.data
}

export async function deletePaper(paperId: number): Promise<void> {
  await apiClient.delete(`/api/papers/${paperId}`)
}
