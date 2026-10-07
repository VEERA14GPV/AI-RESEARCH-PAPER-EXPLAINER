import { PageContainer } from '@/components/layout/PageContainer'
import { PaperList } from '@/components/papers/PaperList'
import { Card } from '@/components/ui/Card'
import { UploadDropzone } from '@/components/upload/UploadDropzone'

export function HomePage() {
  return (
    <PageContainer className="py-10">
      <div className="mb-8 max-w-2xl">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Upload a paper, ask it anything
        </h1>
        <p className="mt-1.5 text-slate-500">
          Upload a research paper and get grounded, source-cited answers about its content.
        </p>
      </div>

      <Card className="p-5 sm:p-6">
        <UploadDropzone />
      </Card>

      <section className="mt-10">
        <h2 className="mb-3 text-sm font-medium uppercase tracking-wide text-slate-500">
          Your papers
        </h2>
        <PaperList />
      </section>
    </PageContainer>
  )
}
