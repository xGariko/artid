-- Email di contatto "aziendale" sull'utente: campo profilo opzionale, distinto da
-- "mail" (che è l'identità di login). Concorre alla percentuale di completamento
-- profilo (ProfileService.OPTIONAL_FIELDS), come phone e gli handle social.
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata.
-- La colonna era stata aggiunta per errore a "resource": la prima riga la rimuove di lì
-- (era vuota, nessun dato da migrare). IF EXISTS/IF NOT EXISTS rendono lo script idempotente.
ALTER TABLE resource DROP COLUMN IF EXISTS business_email;
ALTER TABLE "user"  ADD COLUMN IF NOT EXISTS business_email TEXT;
