-- Flag "l'account ha una password reale scelta dall'utente". Distingue, in fase di eliminazione
-- account, chi verifica con la PASSWORD (registrazione classica, o Membro SPID che ha poi impostato
-- una password) da chi — nato da SPID — non ha mai avuto una password vera e va verificato con le
-- CREDENZIALI SPID (codice fiscale + password del provider).
--
-- Perché un flag e non una derivazione: un account nato da SPID (SpidAuthService.provisionUser)
-- riceve comunque un password_hash casuale/inutilizzabile (colonna NOT NULL), indistinguibile da un
-- hash reale. Senza questo flag un Membro solo-SPID non potrebbe mai eliminare l'account (la sua
-- password non combacia con nulla).
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script nello
-- SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata. Idempotente.

-- DEFAULT true ("ha una password") come rete di sicurezza per eventuali INSERT che non valorizzano
-- la colonna, sullo stesso pattern di internal_share_enabled (V6); i percorsi di creazione la
-- impostano comunque esplicitamente (registrazione = true, provisioning SPID = false).
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS password_set BOOLEAN NOT NULL DEFAULT true;

-- Backfill delle righe preesistenti. Non è possibile sapere con certezza quali account SPID siano
-- "nati da SPID" (senza password) e quali abbiano invece una password classica poi collegata a SPID:
-- l'hash casuale è indistinguibile da uno reale. Approssimazione scelta: ogni account con spid_code
-- collegato viene verificato con le credenziali SPID (che possiede comunque), gli account senza SPID
-- restano su password. Se sai che un account SPID ha in realtà una password classica, riportalo a
-- true a mano:  UPDATE "user" SET password_set = true WHERE id = <id>;
-- Le nuove creazioni impostano il flag in modo preciso, quindi questo tocca solo i dati esistenti.
UPDATE "user" SET password_set = false WHERE spid_code IS NOT NULL;
