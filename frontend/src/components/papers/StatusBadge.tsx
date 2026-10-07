import { Loader2 } from 'lucide-react'
import type { PaperStatus } from '@/types/paper'
import { cn } from '@/utils/cn'
import { STATUS_META } from '@/utils/status'

export function StatusBadge({ status, className }: { status: PaperStatus; className?: string }) {
  const meta = STATUS_META[status]

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium',
        meta.badgeClassName,
        className,
      )}
    >
      {status === 'PROCESSING' ? (
        <Loader2 className="size-3 animate-spin" aria-hidden />
      ) : (
        <span className={cn('size-1.5 rounded-full', meta.dotClassName)} aria-hidden />
      )}
      {meta.label}
    </span>
  )
}
