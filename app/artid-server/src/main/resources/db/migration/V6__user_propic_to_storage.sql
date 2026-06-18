-- Foto profilo: da BYTEA in-table a Supabase Storage (bucket privato "propics").
-- Stesso spostamento fatto per i file delle risorse in V2: i byte vivono su Storage,
-- il DB tiene solo la object key. La foto si serve via presigned GET URL.
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata.
-- 1. propic: i byte (BYTEA) non servono più in tabella. Operazione DISTRUTTIVA — concordata
--    (fresh start, nessun dato da migrare). IF EXISTS rende lo script idempotente.
ALTER TABLE "user" DROP COLUMN IF EXISTS propic;

-- 2. propic_path: object key dell'immagine nel bucket "propics" (NULL = nessuna foto).
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS propic_path TEXT;
