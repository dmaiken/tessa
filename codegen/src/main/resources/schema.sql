--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13 (Debian 15.13-1.pgdg120+1)
-- Dumped by pg_dump version 16.9 (Ubuntu 16.9-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: ltree; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS ltree WITH SCHEMA public;


--
-- Name: EXTENSION ltree; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION ltree IS 'data type for hierarchical tree-like structures';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: asset_tree; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_tree
(
    id         uuid                        NOT NULL,
    entry_id   bigint                      NOT NULL,
    path       public.ltree                NOT NULL,
    alt        text,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: asset_variant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_variant
(
    id                  uuid                        NOT NULL,
    asset_id            uuid                        NOT NULL,
    object_store_bucket text                        NOT NULL,
    object_store_key    text                        NOT NULL,
    attributes          jsonb                       NOT NULL,
    original_variant    boolean                     NOT NULL,
    created_at          timestamp without time zone NOT NULL
);


--
-- Name: migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.migrations
(
    id          bigint NOT NULL,
    description text
);


--
-- Name: asset_tree asset_tree_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_tree
    ADD CONSTRAINT asset_tree_pkey PRIMARY KEY (id);


--
-- Name: asset_variant asset_variant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_variant
    ADD CONSTRAINT asset_variant_pkey PRIMARY KEY (id);


--
-- Name: migrations migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.migrations
    ADD CONSTRAINT migrations_pkey PRIMARY KEY (id);


--
-- Name: asset_path_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX asset_path_idx ON public.asset_tree USING gist (path);


--
-- Name: asset_variant_asset_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX asset_variant_asset_id_idx ON public.asset_variant USING btree (asset_id);


--
-- Name: asset_variant_asset_id_original_variant_uq; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX asset_variant_asset_id_original_variant_uq ON public.asset_variant USING btree (asset_id, original_variant);


--
-- PostgreSQL database dump complete
--

