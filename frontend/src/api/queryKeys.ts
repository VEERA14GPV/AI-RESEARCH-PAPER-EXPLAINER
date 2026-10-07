export const queryKeys = {
  papers: ['papers'] as const,
  paper: (paperId: number) => ['papers', paperId] as const,
  chatHistory: (paperId: number) => ['papers', paperId, 'chat-history'] as const,
}
