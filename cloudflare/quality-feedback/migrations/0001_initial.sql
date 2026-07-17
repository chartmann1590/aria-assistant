CREATE TABLE IF NOT EXISTS quality_feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT NOT NULL,
    app_version TEXT NOT NULL,
    language TEXT NOT NULL,
    response_length INTEGER NOT NULL,
    shared_prompt TEXT,
    shared_response TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_quality_feedback_created_at
    ON quality_feedback(created_at);

CREATE INDEX IF NOT EXISTS idx_quality_feedback_category
    ON quality_feedback(category);
