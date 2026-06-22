-- Cancellazione fisica dell'utente (GDPR art. 17, diritto all'oblio): il DELETE su "user"
-- deve propagarsi a tutte le righe che lo referenziano, direttamente o a catena. Senza una
-- regola ON DELETE Postgres rifiuta il delete (es. artid_id_user_fkey su artid).
--
-- Schema gestito a mano (spring.sql.init.mode=never, niente Flyway): esegui questo script
-- nello SQL Editor di Supabase, sul progetto a cui punta l'app.
--
-- Regole (verificate sullo schema reale via pg_constraint):
--   * CASCADE  → la riga figlia sparisce con l'utente/artid/resource padre. Usato per i dati
--                personali dell'utente: artid, resource, certifications, internal/external_share,
--                artid_resource, notification. login_otp ha già CASCADE (V8).
--   * SET NULL → audit_log: la traccia di audit resta (action/entity/timestamp) ma si stacca
--                dall'utente. NB: ip_address/user_agent/old_value/new_value sono dati personali e
--                vanno azzerati a parte (lo fa UserService.deleteById prima del delete).
--
-- NB: il cascade DB rimuove solo le RIGHE relazionali. I byte su Supabase Storage e le righe
-- `file` (PADRE di resource/certifications/artid.thumbnail, non figlie) NON vengono toccati:
-- la loro pulizia è in UserService.deleteById lato applicazione.

-- --------------------------------------------------------------------------------------------
-- Diagnostica (eseguibile a parte): elenca le FK e la regola ON DELETE delle tabelle coinvolte.
-- confdeltype: 'a'=NO ACTION, 'r'=RESTRICT, 'c'=CASCADE, 'n'=SET NULL.
-- --------------------------------------------------------------------------------------------
-- SELECT con.conname, child.relname AS child_table, parent.relname AS parent_table, con.confdeltype
-- FROM pg_constraint con
-- JOIN pg_class child  ON child.oid  = con.conrelid
-- JOIN pg_class parent ON parent.oid = con.confrelid
-- WHERE con.contype = 'f' AND parent.relname IN ('user', 'artid', 'resource');

-- Riscrive ogni FK dell'albero con la regola voluta. Idempotente e indipendente dal nome del
-- vincolo: lo ritrova dal catalogo per (tabella, colonna). Salta in sicurezza tabelle/colonne assenti.
DO $$
DECLARE
    -- (tabella_figlia, colonna_fk, tabella_padre, colonna_padre, azione_on_delete)
    targets text[] := ARRAY[
        ['artid',          'id_user',     'user',     'id', 'CASCADE'],
        ['resource',       'id_user',     'user',     'id', 'CASCADE'],
        ['certifications', 'id_user',     'user',     'id', 'CASCADE'],
        ['internal_share', 'id_user',     'user',     'id', 'CASCADE'],
        ['notification',   'id_user',     'user',     'id', 'CASCADE'],
        ['audit_log',      'id_user',     'user',     'id', 'SET NULL'],
        ['external_share', 'id_artid',    'artid',    'id', 'CASCADE'],
        ['internal_share', 'id',          'artid',    'id', 'CASCADE'],
        ['artid_resource', 'id',          'artid',    'id', 'CASCADE'],
        ['artid_resource', 'id_resource', 'resource', 'id', 'CASCADE']
    ];
    t text[];
    child_table   text;
    child_col     text;
    parent_table  text;
    parent_col    text;
    delete_action text;
    existing_name text;
BEGIN
    FOREACH t SLICE 1 IN ARRAY targets LOOP
        child_table   := t[1];
        child_col     := t[2];
        parent_table  := t[3];
        parent_col    := t[4];
        delete_action := t[5];

        IF to_regclass('public.' || quote_ident(child_table)) IS NULL THEN
            RAISE NOTICE 'Salto %: tabella assente', child_table;
            CONTINUE;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = child_table AND column_name = child_col
        ) THEN
            RAISE NOTICE 'Salto %.%: colonna assente', child_table, child_col;
            CONTINUE;
        END IF;

        -- FK a colonna singola esistente su (child_table.child_col)?
        SELECT con.conname INTO existing_name
        FROM pg_constraint con
        WHERE con.contype = 'f'
          AND con.conrelid = ('public.' || quote_ident(child_table))::regclass
          AND array_length(con.conkey, 1) = 1
          AND (SELECT attname FROM pg_attribute
                WHERE attrelid = con.conrelid AND attnum = con.conkey[1]) = child_col
        LIMIT 1;

        IF existing_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I', child_table, existing_name);
        END IF;

        EXECUTE format(
            'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I(%I) ON DELETE %s',
            child_table, child_table || '_' || child_col || '_fkey', child_col, parent_table, parent_col, delete_action
        );
        RAISE NOTICE 'FK %.% -> %.% ora ON DELETE %', child_table, child_col, parent_table, parent_col, delete_action;
    END LOOP;
END $$;
