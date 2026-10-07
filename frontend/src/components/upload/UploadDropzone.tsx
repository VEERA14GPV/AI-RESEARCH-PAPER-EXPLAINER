import { AlertCircle, FileText, UploadCloud, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { type ChangeEvent, type DragEvent, useRef, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useUploadPaper } from '@/hooks/useUploadPaper'
import { formatFileSize } from '@/utils/format'
import { getErrorMessage } from '@/utils/errors'
import { validatePdfFile } from '@/utils/validation'

export function UploadDropzone() {
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)

  const upload = useUploadPaper()

  function handleFiles(files: FileList | null) {
    const file = files?.[0]
    if (!file) return

    const result = validatePdfFile(file)
    if (!result.valid) {
      setValidationError(result.error ?? 'Invalid file.')
      setSelectedFile(null)
      return
    }

    setValidationError(null)
    setSelectedFile(file)
    upload.reset()
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragging(false)
    handleFiles(event.dataTransfer.files)
  }

  function handleInputChange(event: ChangeEvent<HTMLInputElement>) {
    handleFiles(event.target.files)
    event.target.value = ''
  }

  function clearSelection() {
    setSelectedFile(null)
    setValidationError(null)
    upload.reset()
  }

  async function handleUpload() {
    if (!selectedFile) return
    try {
      const paper = await upload.mutateAsync(selectedFile)
      setSelectedFile(null)
      navigate(`/papers/${paper.id}`)
    } catch {
      // Surfaced below via upload.isError / getErrorMessage.
    }
  }

  return (
    <div className="space-y-3">
      <div
        onDragOver={(event) => {
          event.preventDefault()
          setIsDragging(true)
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors ${
          isDragging ? 'border-accent-500 bg-accent-50' : 'border-slate-300 bg-slate-50/50'
        }`}
      >
        <div className="flex size-12 items-center justify-center rounded-full bg-white shadow-sm ring-1 ring-slate-200">
          <UploadCloud className="size-5 text-slate-500" aria-hidden />
        </div>
        <div>
          <p className="font-medium text-slate-700">Drag and drop a PDF here</p>
          <p className="text-sm text-slate-500">or select a file from your computer (max 50 MB)</p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => inputRef.current?.click()}>
          Browse files
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          className="hidden"
          onChange={handleInputChange}
          aria-label="Select a PDF file to upload"
        />
      </div>

      {validationError && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <AlertCircle className="size-4 shrink-0" aria-hidden />
          {validationError}
        </div>
      )}

      {selectedFile && (
        <div className="flex items-center gap-3 rounded-lg border border-slate-200 bg-white px-4 py-3">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-accent-50">
            <FileText className="size-4 text-accent-600" aria-hidden />
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-slate-800">{selectedFile.name}</p>
            <p className="text-xs text-slate-500">{formatFileSize(selectedFile.size)}</p>
          </div>
          {upload.isPending ? (
            <div className="w-28">
              <div className="h-1.5 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-accent-600 transition-all"
                  style={{ width: `${upload.progress}%` }}
                />
              </div>
              <p className="mt-1 text-right text-xs text-slate-500">
                {upload.progress < 100 ? `${upload.progress}%` : 'Processing…'}
              </p>
            </div>
          ) : (
            <div className="flex shrink-0 items-center gap-2">
              <button
                type="button"
                onClick={clearSelection}
                aria-label="Remove selected file"
                className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-accent-600"
              >
                <X className="size-4" aria-hidden />
              </button>
              <Button size="sm" onClick={handleUpload}>
                Upload
              </Button>
            </div>
          )}
        </div>
      )}

      {upload.isError && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <AlertCircle className="size-4 shrink-0" aria-hidden />
          {getErrorMessage(upload.error)}
        </div>
      )}
    </div>
  )
}
