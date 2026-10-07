import { FileSearch } from 'lucide-react'
import { Link } from 'react-router-dom'

export function Header() {
  return (
    <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur-sm">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link
          to="/"
          className="flex items-center gap-2 rounded-md font-semibold text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-600"
        >
          <span className="flex size-7 items-center justify-center rounded-lg bg-accent-600 text-white">
            <FileSearch className="size-4" aria-hidden />
          </span>
          <span className="tracking-tight">PaperExplainer</span>
        </Link>
      </div>
    </header>
  )
}
