import type { PaperStatus } from '@/types/paper'

interface StatusMeta {
  label: string
  description: string
  dotClassName: string
  badgeClassName: string
}

export const STATUS_META: Record<PaperStatus, StatusMeta> = {
  UPLOADED: {
    label: 'Uploaded',
    description: 'Waiting to be processed.',
    dotClassName: 'bg-slate-400',
    badgeClassName: 'bg-slate-100 text-slate-700 border-slate-200',
  },
  PROCESSING: {
    label: 'Processing',
    description: 'Extracting text and generating embeddings…',
    dotClassName: 'bg-amber-500',
    badgeClassName: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  PROCESSED: {
    label: 'Ready',
    description: 'Ready to chat.',
    dotClassName: 'bg-emerald-500',
    badgeClassName: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  },
  FAILED: {
    label: 'Failed',
    description: 'Processing failed.',
    dotClassName: 'bg-red-500',
    badgeClassName: 'bg-red-50 text-red-700 border-red-200',
  },
}

export function isChatEnabled(status: PaperStatus): boolean {
  return status === 'PROCESSED'
}
