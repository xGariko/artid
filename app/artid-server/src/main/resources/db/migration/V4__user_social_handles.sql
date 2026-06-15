-- Nuovi handle social sulla tabella "user", affiancati a linkedin_id.
-- Stessa natura di linkedin_id: campi profilo opzionali (text, nullable) che
-- concorrono alla percentuale di completamento profilo (ProfileService.OPTIONAL_FIELDS).
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata.
-- IF NOT EXISTS: idempotente — le colonne sono già state aggiunte a mano sul DB,
-- questo file serve a tracciare la modifica nella storia dello schema.
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS facebook_id  TEXT;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS instagram_id TEXT;
