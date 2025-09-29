-- Create PostgreSQL enum type for upload status
CREATE TYPE upload_status AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');

-- Create image_metadata table
CREATE TABLE image_metadata
(
    image_id    VARCHAR(36) PRIMARY KEY,
    title       VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    content_type VARCHAR(50)  NOT NULL,
    file_size    BIGINT        NOT NULL,
    width       INTEGER       NOT NULL,
    height      INTEGER       NOT NULL,
    s3_key       VARCHAR(500)  NOT NULL,
    uploaded_by  VARCHAR(100)  NOT NULL,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    status      upload_status NOT NULL DEFAULT 'PROCESSING'
);

-- Create image_tags table
CREATE TABLE image_tags
(
    image_id VARCHAR(255) NOT NULL,
    tag      VARCHAR(255) NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image_metadata (image_id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_title ON image_metadata (title);
CREATE INDEX idx_content_type ON image_metadata (content_type);
CREATE INDEX idx_created_at ON image_metadata (created_at);
CREATE INDEX idx_file_size ON image_metadata (file_size);
