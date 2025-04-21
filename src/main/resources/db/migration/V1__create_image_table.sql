CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS asset_tree
(
    id         UUID      NOT NULL PRIMARY KEY,
    path       ltree     NOT NULL,
    bucket     TEXT      NOT NULL,
    url        TEXT      NOT NULL,
    store_key  TEXT      NOT NULL,
    mime_type  TEXT      NOT NULL,
    alt        TEXT,
    created_at TIMESTAMP NOT NULL
);
