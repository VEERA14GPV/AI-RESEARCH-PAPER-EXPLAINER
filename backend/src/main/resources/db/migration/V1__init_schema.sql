-- Baseline schema for the AI Research Paper Explainer backend.
-- Table shapes mirror the current JPA entities (Paper, PaperChunk, ChatMessage)
-- so that spring.jpa.hibernate.ddl-auto=validate does not report a mismatch.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE papers (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    file_path       VARCHAR(500),
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP
);

CREATE TABLE paper_chunks (
    id              BIGSERIAL PRIMARY KEY,
    paper_id        BIGINT NOT NULL REFERENCES papers (id),
    chunk_index     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    page_number     INTEGER,
    token_count     INTEGER,
    created_at      TIMESTAMP NOT NULL,
    -- text-embedding-3-small (configured in application.yml) outputs 1536 dimensions.
    embedding       vector(1536)
);

CREATE INDEX idx_paper_chunks_paper_id ON paper_chunks (paper_id);
CREATE INDEX idx_paper_chunks_chunk_index ON paper_chunks (chunk_index);

-- No ANN index (ivfflat/hnsw) on paper_chunks.embedding yet:
-- vector search is not wired up yet (VectorSearchService is still a stub),
-- and per-paper chunk counts are small enough that an exact sequential-scan
-- distance search is fast and simpler. ivfflat in particular needs a
-- representative amount of data at CREATE INDEX time to build useful
-- clusters, which an empty/near-empty table doesn't provide. Revisit once
-- similarity search is implemented and real chunk volumes are known.

CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    paper_id        BIGINT NOT NULL REFERENCES papers (id),
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_chat_messages_paper_id ON chat_messages (paper_id);
