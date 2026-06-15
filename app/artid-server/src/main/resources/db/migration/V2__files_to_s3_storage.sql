-- Migrazione: storage file da bytea (blob in-table) a Supabase S3.
-- Lo schema è gestito a mano (spring.sql.init.mode=never): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- 1. file_size: la dimensione prima era derivata a runtime da OCTET_LENGTH(blob);
--    con i byte su S3 va persistita esplicitamente, popolata all'upload.
ALTER TABLE file ADD COLUMN IF NOT EXISTS file_size BIGINT;

-- 2. blob: i byte ora vivono su S3 (la object key è in file.file_path). La colonna
--    va rimossa: l'entity non la mappa più e, se era NOT NULL, ogni nuovo INSERT
--    fallirebbe. Operazione distruttiva — concordata (fresh start, nessun dato da migrare).
ALTER TABLE file DROP COLUMN IF EXISTS blob;

-- (Opzionale) Le risorse pre-esistenti puntavano ai blob ora rimossi e non sono più
-- scaricabili. Se erano solo dati di test, ripulisci:
-- DELETE FROM artid_resource;
-- DELETE FROM resource;
-- DELETE FROM file;
