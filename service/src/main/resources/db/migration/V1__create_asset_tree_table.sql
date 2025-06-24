CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS asset_tree
(
    id         UUID      NOT NULL PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    path       ltree     NOT NULL,
    alt        TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS asset_path_idx ON asset_tree USING gist (path);

CREATE TABLE IF NOT EXISTS asset_variant
(
    id                  UUID                        NOT NULL PRIMARY KEY,
    asset_id            UUID                        NOT NULL,
    object_store_bucket TEXT                        NOT NULL,
    object_store_key    TEXT                        NOT NULL,
    attributes          JSONB                       NOT NULL,
    original_variant    BOOLEAN                     NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS asset_variant_asset_id_idx ON asset_variant (asset_id);
CREATE UNIQUE INDEX IF NOT EXISTS asset_variant_asset_id_original_variant_uq ON asset_variant (asset_id, original_variant);
