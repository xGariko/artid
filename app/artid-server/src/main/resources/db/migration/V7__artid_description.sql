-- Descrizione testuale di un ArtID (HTML rich-text di Quill, come la biografia del profilo):
-- mostrata in dettaglio/anteprima. Nullable e senza DEFAULT: gli ArtID già esistenti e quelli
-- creati senza descrizione restano a NULL (ArtidService.create non valorizza la colonna).
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- Idempotente: IF NOT EXISTS rende sicura la riesecuzione (la colonna è già stata applicata a mano).
ALTER TABLE artid ADD COLUMN IF NOT EXISTS description TEXT;
