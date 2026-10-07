/**
 * Mirrors the error body shape built by
 * exception/GlobalExceptionHandler.java's buildResponse():
 *   { timestamp, status, error, message }
 * Returned for 400 (validation, invalid PDF), 404 (paper not found), and
 * 500 (file storage failure, unexpected errors) responses.
 */
export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
}
