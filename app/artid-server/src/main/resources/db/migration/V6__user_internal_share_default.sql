-- internal_share_enabled è NOT NULL sulla tabella "user" ma era stata aggiunta a mano
-- senza DEFAULT: ogni INSERT che non valorizzava la colonna (es. la registrazione)
-- falliva il vincolo NOT NULL. Diamo un default lato DB come rete di sicurezza, oltre
-- a impostarlo esplicitamente nel codice (AuthController.register).
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata.
-- Entrambe le istruzioni sono idempotenti: rieseguirle non altera lo stato.
ALTER TABLE "user" ALTER COLUMN internal_share_enabled SET DEFAULT false;
UPDATE "user" SET internal_share_enabled = false WHERE internal_share_enabled IS NULL;
