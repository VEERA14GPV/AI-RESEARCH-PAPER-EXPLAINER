import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/**
 * Renders AI answer text as Markdown (headings, lists, bold, code, tables)
 * via react-markdown, which parses to a React element tree - no
 * dangerouslySetInnerHTML anywhere. Equation/LaTeX rendering is
 * intentionally not implemented: the backend doesn't produce or mark up
 * equations specially, so adding a math renderer here would just be
 * guessing at content the API never promises.
 */
export function MarkdownAnswer({ content }: { content: string }) {
  return (
    <div className="prose-answer text-sm">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  )
}
