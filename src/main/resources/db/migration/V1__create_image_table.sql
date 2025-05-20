CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS asset_tree
(
    id         UUID      NOT NULL PRIMARY KEY,
    entry_id INT NOT NULL,
    path       ltree     NOT NULL,
    bucket     TEXT      NOT NULL,
    url        TEXT      NOT NULL,
    store_key  TEXT      NOT NULL,
    mime_type  TEXT      NOT NULL,
    alt        TEXT,
    height INT NOT NULL,
    width  INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS asset_path_idx ON asset_tree USING gist (path);
