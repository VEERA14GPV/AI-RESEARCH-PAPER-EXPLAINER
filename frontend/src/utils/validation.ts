// Matches spring.servlet.multipart.max-file-size in application.yml.
export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024

export interface FileValidationResult {
  valid: boolean
  error?: string
}

export function validatePdfFile(file: File): FileValidationResult {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')

  if (!isPdf) {
    return { valid: false, error: 'Only PDF files are supported.' }
  }

  if (file.size === 0) {
    return { valid: false, error: 'This file is empty.' }
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    return { valid: false, error: 'File is larger than the 50 MB limit.' }
  }

  return { valid: true }
}
