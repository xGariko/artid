-- Un'identità SPID (spid_code = codice fiscale restituito dal provider) può appartenere a UN SOLO
-- account ArtID. Senza vincolo, "Associa SPID" (COL_SPID) poteva agganciare lo stesso codice a più
-- utenti: al successivo login SPID, findBySpidCode trovava 2 righe e sollevava
-- IncorrectResultSizeDataAccessException ("expected 1, actual 2").
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script nello
-- SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- NB: "user" è parola riservata in Postgres, va quotata.

-- 1) Bonifica dei duplicati preesistenti: mantiene lo spid_code sull'account con id più basso (il
--    più vecchio) e lo azzera sugli altri. Tie-break arbitrario: i duplicati esistono solo per via
--    del bug pre-fix, non c'è un proprietario "giusto" deducibile dallo schema.
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY spid_code ORDER BY id) AS rn
    FROM "user"
    WHERE spid_code IS NOT NULL
)
UPDATE "user" u
   SET spid_code = NULL
  FROM ranked
 WHERE u.id = ranked.id AND ranked.rn > 1;

-- 2) Vincolo definitivo: unicità tra i soli valori non nulli. Gli account senza SPID restano molti
--    con spid_code NULL, liberamente ammessi (indice parziale).
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_spid_code ON "user" (spid_code) WHERE spid_code IS NOT NULL;
