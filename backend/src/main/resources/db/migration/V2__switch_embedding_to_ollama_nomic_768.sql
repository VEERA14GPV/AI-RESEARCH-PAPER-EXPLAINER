-- Switches paper_chunks.embedding from OpenAI's text-embedding-3-small
-- (1536 dimensions) to Ollama's local nomic-embed-text model
-- (768 dimensions, confirmed via the installed model's own metadata).
--
-- Embeddings from different models are not comparable to each other, so
-- any existing rows are cleared rather than silently mixing 1536-d
-- OpenAI vectors with 768-d Ollama vectors in the same column. At the
-- time this migration was written, paper_chunks had no successfully
-- processed rows in the dev database anyway (every prior ingestion had
-- failed before reaching the storage step), so this is not a lossy
-- change in practice - it exists to make that guarantee explicit and to
-- keep this safe to re-run against any environment.
DELETE FROM paper_chunks;

ALTER TABLE paper_chunks
    ALTER COLUMN embedding TYPE vector(768) USING embedding::vector(768);
