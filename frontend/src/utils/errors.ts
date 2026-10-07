import { isAxiosError } from 'axios'
import type { ApiErrorBody } from '@/types/api'

/**
 * Turns any error thrown by an API call into a single human-readable
 * string, never a raw Java stack trace. Backend errors use the
 * { timestamp, status, error, message } shape from GlobalExceptionHandler.
 */
export function getErrorMessage(error: unknown): string {
  if (isAxiosError<ApiErrorBody>(error)) {
    if (!error.response) {
      return error.code === 'ECONNABORTED'
        ? 'The request took too long to respond. Please try again.'
        : 'Could not reach the server. Check that the backend is running and try again.'
    }

    const body = error.response.data
    if (body && typeof body.message === 'string' && body.message.length > 0) {
      return body.message
    }

    switch (error.response.status) {
      case 400:
        return 'The request was invalid. Please check your input and try again.'
      case 404:
        return 'The requested paper could not be found.'
      case 413:
        return 'That file is too large to upload.'
      case 500:
        return 'Something went wrong on the server. Please try again.'
      default:
        return `Request failed (${error.response.status}).`
    }
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'An unexpected error occurred.'
}

export function isNotFoundError(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 404
}
