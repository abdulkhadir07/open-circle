-- Banter is one shared board again, so paging no longer filters by campus.
CREATE INDEX idx_banters_created ON banters (created_at DESC, id DESC);
