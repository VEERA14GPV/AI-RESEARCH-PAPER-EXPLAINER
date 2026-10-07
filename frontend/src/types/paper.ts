/**
 * Mirrors entity/Paper.java exactly, as returned by:
 *   POST   /api/papers            (201)
 *   GET    /api/papers/:paperId   (200)
 *   GET    /api/papers            (200, array)
 *
 * `chunks` is annotated @JsonIgnore on the backend and never appears in the
 * JSON, so it is intentionally not part of this type.
 */
export type PaperStatus = 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'FAILED'

export interface Paper {
  id: number
  title: string
  fileName: string
  contentType: string
  fileSize: number
  filePath: string | null
  status: PaperStatus
  createdAt: string
  updatedAt: string | null
}

/**
 * Mirrors dto/CreatePaperRequest.java - the JSON "request" part of the
 * multipart upload body (POST /api/papers).
 */
export interface CreatePaperRequest {
  title: string
  fileName: string
  contentType: string
  fileSize: number
}
