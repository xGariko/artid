# Piano operativo — Collection di task ArtID

> Documento self-contained, pensato per essere ripreso da qualunque istanza di Claude Code
> senza il contesto della conversazione originale. Ogni task riporta file, righe, stato
> attuale, modifiche puntuali e verifica. I numeri di riga sono al `2026-07-05`: usare
> anche gli ancoraggi stabili (nomi funzione/landmark di markup) perché possono slittare.

## 0. Contesto del progetto

- **Monorepo** `C:\progetti\artid`:
  - Frontend: `app/artid-client` — **SvelteKit 5** (rune `$state`/`$derived`/`$props`/`$effect`/`$bindable`), **Bootstrap 5**, **Bootstrap Icons** (`bi bi-*`), **Zod**. Niente tailwind/styled-components.
  - Backend: `app/artid-server` — **Spring Boot 4**, Java 17, **Spring Data JDBC** (non JPA), Lombok, AWS SDK v2 (S3), JWT. Package base `afam.artidserver`.
- **Proxy API**: il browser chiama SvelteKit su `/api/...`; la route catch-all `src/routes/api/[...path]/+server.ts` inoltra a Spring iniettando il `Bearer` dal cookie httpOnly. I loader server-side usano invece `locals.api` (openapi-fetch) direttamente verso Spring. **Non creare `+server.ts` dedicati** salvo necessità: passa tutto dal catch-all.
- **Tipi API**: `src/lib/api/schema.d.ts` è **generato** da OpenAPI. Dopo ogni modifica ai DTO/endpoint backend eseguire `npm run api:generate` (in `app/artid-client`). **Mai** patchare `schema.d.ts` a mano. I tipi comodi stanno in `src/lib/api/types.ts`.
- **Convenzioni frontend**: utilities condivise in `src/lib/utilities.ts`; nomi variabili completi; commenti minimali "why-only"; SvelteKit-first.
- **Design flat**: niente ombre né gradienti; profondità via colore/bordi; focus con outline. (NB: alcune card hanno ancora `box-shadow` legacy, es. `artid-shared-card.svelte:91-95` — non introdurne di nuove.)
- **Sanitizzazione HTML**: `src/lib/sanitize.ts` esporta `sanitizeHtml()`. Le descrizioni (ArtID, risorse) sono prodotte da editor Quill → contengono HTML. Oggi `sanitizeHtml`/`{@html}` **non sono ancora usati** in nessun punto.
- **DB**: schema gestito a mano su Supabase (`spring.sql.init.mode=never`), migration nel repo incomplete. Per FK/colonne introspezionare il DB live (MCP Supabase) se serve.

### Come far girare / verificare
- Frontend dev: da `app/artid-client` → `npm run dev` (Vite). Preferire i tool `preview_*` per verifica in-browser.
- Type-check FE: `npm run check` (svelte-check). Rigenerazione tipi: `npm run api:generate`.
- Backend: build Maven da `app/artid-server` (`./mvnw ...` o wrapper presente); config in `src/main/resources/application.properties` (+ `application-local.properties` per i segreti).

## 1. Decisioni prese (dall'utente)

- **Task 3**: dalla pagina `/shares` il click sul nome apre **sempre la preview classica** (tutti gli ArtID lì sono dell'utente). Il caso "ArtID di altri → explore" si applica invece alle **card "Condivisi con me" della pagina `/artid`** (`artid-shared-card.svelte`), non alla pagina `/shares`.
- **Task 2**: l'email di notifica va inviata **solo ai destinatari registrati che accettano condivisioni** (`internalShareEnabled = true`, ramo in cui `isAccepted` viene messo a `true`). Chi ha disattivato non riceve nulla.

### Note tecniche cross-cutting emerse in analisi
- **Nessun `@ControllerAdvice`/`@ExceptionHandler`** in tutto il backend: la mappatura errore→HTTP dipende dai default Boot + `ResponseStatusException` lanciate nei service.
- `server.error.include-message` **non è impostato** → default `never` → il `reason` delle `ResponseStatusException` **non finisce nel body JSON**. Rilevante per il Task 4.

### Ordine di implementazione consigliato
1. **Backend "puri"** (basso rischio, nessuna dipendenza FE): Task 5 (ORDER BY), Task 4-BE (`include-message`), Task 2 (email).
2. **Task 7-BE** (guardia unlink) → poi **Task 7-FE**.
3. **FE indipendenti**: Task 9, Task 6, Task 8, Task 3, Task 4-FE.
4. **Task 1** per ultimo (richiede modifica BE + `npm run api:generate` + refactor sidebar/pagina).

I task che richiedono **rigenerazione tipi** (`npm run api:generate`): **Task 1** (se si estende `ArtidResponse`). Gli altri backend (2/4/5/7) non cambiano DTO usati dal FE.

---

## Task 1 — Filtro tag in AND con la categoria (non in sostituzione)

**Obiettivo**: nella lista ArtID, selezionare un tag deve **restringere** la categoria attiva (es. `I miei` + `tagx` = solo i miei ArtID con `tagx`), non sostituirla.

### Stato attuale (perché oggi sostituisce)
- Il filtro è **interamente client-side**. Loader `src/routes/(app)/artid/+page.server.ts` carica tutto (`/api/artids`, `/api/tags`, `/api/shares/internal/to-me`) senza parametri di filtro.
- Esiste **un solo stato** condiviso: `src/routes/(app)/artid/+page.svelte:19` → `activeFilterValue: ArtIdFilterType` (`'all'|'mine'|'sharedWithMe'|'recent'|'favourite'`), passato alla sidebar con `bind:activeButton`.
- In `src/lib/components/layout/artid-sidebar.svelte`:
  - Bottoni **categoria** (riga 39): `onclick={() => (activeButton = button.value)}`.
  - Bottoni **tag** (riga 61): `onclick={() => (activeButton = tag.title)}` → **sovrascrivono** lo stesso `activeButton` con il *titolo* del tag.
- Conseguenza: `visibleArtids` (`+page.svelte:128-141`) è uno `switch` sui 4 casi categoria; con valore = titolo tag cade nel `default` → ritorna `[]`. Il tag oggi **svuota la lista** invece di filtrare.
- **Manca l'associazione artid→tag a livello pagina**: i tag di ciascuna card sono fetchati dentro `artid-card.svelte:16-31` via `GET /api/artids/{id}/tags` (uno per card). La pagina non ha una mappa su cui fare AND.

### Modifiche

**A) Backend (consigliato) — esporre i tag nella lista ArtID**
Estendere `ArtidResponse` (`app/artid-server/.../model/dto/ArtidResponse.java`) con `List<Long> tagIds` (o `List<TagResponse> tags`) popolato con un join su `artid_tag`. Questo dà alla pagina il dato per il filtro AND **e** elimina le N chiamate per-card (`getArtidTags`).
- Aggiornare la query/DAO che alimenta `GET /api/artids` per includere i tag (batch, niente N+1).
- Poi in `app/artid-client`: `npm run api:generate`.

> Alternativa senza backend (fallback): nel loader `+page.server.ts` fare N `GET /api/artids/{id}/tags` e costruire una mappa `artidId → tagIds`. Sconsigliata (N+1, più codice), ma non tocca il backend.

**B) Sidebar — separare stato tag da stato categoria** (`artid-sidebar.svelte`)
- Aggiungere una seconda prop bindable **opzionale** (per non rompere la pagina `/shares` che usa la stessa sidebar senza tag):
  ```svelte
  let { buttonsGroups, sidebarActions, activeButton = $bindable(), activeTag = $bindable(null) }:
    { buttonsGroups: SidebarButtonGroup[]; sidebarActions: SidebarAction[]; activeButton: string; activeTag?: number | null } = $props();
  ```
- Bottoni tag (righe 56-68): usare `tag.id` (non `tag.title`) e fare **toggle**:
  ```svelte
  class="... {activeTag === tag.id ? 'tag-active' : ''} tag"
  onclick={() => (activeTag = activeTag === tag.id ? null : tag.id)}
  ```
  (il toggle dà anche il "reset" del tag, oggi assente).

**C) Pagina lista — comporre il filtro in AND** (`+page.svelte`)
- Nuovo stato: `let activeTagId = $state<number | null>(null);`
- Passare alla sidebar: `<ArtidSidebar ... bind:activeButton={activeFilterValue} bind:activeTag={activeTagId} />`.
- In `visibleArtids` applicare prima la categoria, poi (se `activeTagId != null`) il tag:
  ```ts
  const visibleArtids = $derived.by(() => {
      let list;
      switch (activeFilterValue) {
          case 'mine': list = data.artids; break;
          case 'recent': list = data.artids.filter((a) => isRecent(a.lastModified)); break;
          case 'sharedWithMe': list = data.sharedArtids; break; // niente tag qui (vedi nota)
          case 'favourite': list = data.artids.filter((a) => a.favourite); break;
          default: list = [];
      }
      if (activeTagId != null && activeFilterValue !== 'sharedWithMe') {
          list = list.filter((a) => a.tagIds?.includes(activeTagId));
      }
      return list;
  });
  ```
- **Nota `sharedWithMe`**: sono ArtID di altri (`InternalShareArtIDExtendedResponse`), senza tag lato UI → il tag non si applica. Scelte: (a) ignorarlo come sopra, oppure (b) nascondere il gruppo tag quando la categoria è `sharedWithMe`. Default consigliato: (a).

**D) Cleanup opzionale** (`artid-card.svelte`): se i tag arrivano dalla lista (opzione A), rimuovere il fetch per-card `getArtidTags` (righe 16-31) e leggere `artid.tags`/`tagIds` dalla prop.

### Verifica
- `I miei` + nessun tag → tutti i miei. `I miei` + `tagx` → solo i miei con `tagx`. Cambio categoria mantiene il tag (o si decide di resettarlo — default: mantenere). Ri-click sullo stesso tag → deseleziona.
- `npm run check` senza errori di tipo (dopo `api:generate`).

---

## Task 2 — Email dopo condivisione interna (solo backend)

**Obiettivo**: alla creazione di una condivisione interna verso un utente **registrato che accetta condivisioni**, inviargli una mail: `[Utente X] ha condiviso con te l'ArtID "[nome_artid]"`.

### Stato attuale
- Endpoint: `POST /api/artids/{id}/share/internal`, body = **stringa email grezza** (una POST per destinatario; il FE fa fan-out con `Promise.all`). Controller: `app/artid-server/.../controller/ArtidController.java` → `addInternalShare` (righe ~224-240) che **silenzia ogni errore e risponde sempre 200** (scelta voluta per privacy).
- Service: `app/artid-server/.../service/ShareService.java` → `addInternalShare(Long artidId, String email, Long userId)` (righe ~283-374, `@Transactional`):
  - Normalizza email; ownership ArtID (`artidDAO.findByIdAndIdUserAndDeletedAtIsNull`), altrimenti 404.
  - `User targetUser = userDAO.findByMail(trimmedEmail).orElse(null);`
    - **registrato** (`targetUser != null`, righe ~318-344): crea/aggiorna `InternalShare` con `idUserTo`, `recipientMail`, `isAccepted = targetUser.getInternalShareEnabled()`.
    - non registrato (`else`): `idUserTo = null`, `isAccepted = true`.
  - `internalShareDAO.save(share);`
  - **Riga ~372**: `// TODO se accetta condivisioni invia mail` ← punto di aggancio.
- **Infrastruttura email già completa e in uso** (OTP/registrazione):
  - `pom.xml`: `spring-boot-starter-mail`. `application.properties` righe ~60-76: SMTP Hostinger, `otp.from-address=noreply@artid.space`.
  - `@EnableAsync` attivo (`ArtidServerApplication.java`).
  - `service/EmailService.java`: `sendText(to, subject, body)` e `sendHtml(to, subject, html)`, entrambi `@Async` **fire-and-forget** (loggano l'errore, non propagano). **Già iniettato** in `ShareService` (`private final EmailService emailService;`).
  - Pattern da imitare: `ShareService.notifyOwnerFirstOpen(...)` (righe ~250-265) usa `sendText` con null-guard sull'indirizzo.

### Modifiche (`ShareService.addInternalShare`)
Alla riga del TODO, **solo nel ramo registrato** e **solo se accetta condivisioni**, dopo `internalShareDAO.save(share)`:
```java
// Notifica solo i destinatari registrati che accettano condivisioni interne.
if (Boolean.TRUE.equals(targetUser.getInternalShareEnabled())
        && targetUser.getMail() != null && !targetUser.getMail().isBlank()) {
    User fromUser = userDAO.findById(userId).orElse(null);
    String sharerName = displayName(fromUser); // "Nome Cognome" con fallback
    String subject = sharerName + " ha condiviso un ArtID con te";
    String body = sharerName + " ha condiviso con te l'ArtID \"" + artid.getTitle() + "\".";
    emailService.sendText(targetUser.getMail(), subject, body);
}
```
Helper (o inline) per il nome mittente:
```java
private static String displayName(User u) {
    if (u == null) return "Un utente";
    String full = ((u.getName() != null ? u.getName() : "") + " "
                 + (u.getSurname() != null ? u.getSurname() : "")).trim();
    if (!full.isBlank()) return full;
    return u.getMail() != null ? u.getMail() : "Un utente";
}
```
Note:
- `artid` è già in scope nel metodo (variabile locale caricata per l'ownership). Il campo è `title` (non "nome").
- L'invio è `@Async`: non blocca la richiesta né rischia di far fallire la transazione.
- `sendText` (plain) per coerenza con `notifyOwnerFirstOpen`. Se si vuole HTML brandizzato, esiste `service/OtpEmailTemplate.java` come riferimento (andrebbe generalizzato/duplicato — opzionale, fuori scope minimo).

### Verifica
- Condivisione verso utente registrato con `internalShareEnabled=true` → arriva mail col testo corretto. Verso registrato con flag `false` → **nessuna** mail. Verso email non registrata → nessuna mail (ramo else). Il controller risponde comunque 200 in tutti i casi.

---

## Task 3 — Navigazione dal nome ArtID (preview vs explore)

**Obiettivo**: (a) da `/shares`, click sul nome → **preview classica** (tutti gli ArtID lì sono dell'utente); (b) dalle card "Condivisi con me" di `/artid` (ArtID di altri) → **vista explore** dell'ArtID altrui.

### Stato attuale
- `/shares` → `shares-list.svelte`: il nome è già un `<a>` (righe ~433-447) ma l'`onclick` chiama `openDetails(share.idArtid)` → va alla pagina di **modifica** `/(app)/artid/details/[id]`. Esiste già `openPreview(artidId)` (righe ~265-271) che fa `goto(resolve('/(app)/artid/details/[id]/preview', {...}))`, oggi collegata solo al bottone "Apri anteprima" (solo esterni).
- `/artid` → card ricevute `artid-shared-card.svelte`: il link (riga 37) è
  `href={resolve('/(app)/artid/details/[id]/preview', { id: String(sharedArtid.id) })}` →
  **usa `sharedArtid.id` (id della *condivisione*, non dell'ArtID): bug**. Punta a una preview sbagliata e comunque non alla vista explore.
- DTO `InternalShareArtIDExtendedResponse` (prop `sharedArtid`) espone: `id` (share), `idArtid`, `idUserFrom` (proprietario), `title`, `name`, `isAccepted`, `filePath`.
- Route explore per singolo ArtID altrui: `/explore/[id]/artid/[artidId]` con `id` = userId proprietario, `artidId` = id ArtID (loader `explore/[id]/artid/[artidId]/+page.server.ts` → `GET /api/users/{userId}/public/artids/{artidId}`).

### Modifiche

**A) `/shares` — titolo → dettaglio, pulsante azione → preview** (`shares-list.svelte`)
- Il click sul **titolo** apre la pagina di **dettaglio/modifica**: `openDetails(share.idArtid)` → `resolve('/(app)/artid/details/[id]', ...)` (href e onclick coerenti; mantenere `preventDefault`/`stopPropagation`). Idem il fallback "Non trovato".
- Il **pulsante azione "Apri anteprima"** resta su `openPreview(extShare.idArtid)` → route `/preview`.
- Entrambe le funzioni (`openDetails`, `openPreview`) restano in uso.

**B) `/artid` "Condivisi con me" — card → explore** (`artid-shared-card.svelte:37`)
- Sostituire l'href (corregge anche il bug `id` → dati corretti):
  ```svelte
  href={resolve('/explore/[id]/artid/[artidId]', {
      id: String(sharedArtid.idUserFrom),
      artidId: String(sharedArtid.idArtid)
  })}
  ```
- Verificare in `src/lib/api/types.ts`/`schema.d.ts` il nome esatto del campo proprietario (`idUserFrom`) su `InternalShareArtIDExtendedResponse`. Se `idUserFrom`/`idArtid` potessero mancare, aggiungere una guardia (fallback o disabilitazione del link).

### Verifica
- Da `/shares` (Esterne/Interne/Scadute) il click sul nome apre `/artid/details/{idArtid}/preview`.
- Da `/artid` una card "Condivisi con me" apre `/explore/{idUserFrom}/artid/{idArtid}` e mostra l'ArtID come in esplora. Il bottone cestino (rimuovi condivisione) continua a funzionare (ha già `stopPropagation`).

---

## Task 4 — Il frontend rispetta i messaggi di `openSharedArtid`

**Obiettivo**: mostrare all'utente i messaggi d'errore definiti nel backend all'apertura di un link condiviso, invece di testi generici hardcoded.

### Stato attuale
- Metodo backend: `ShareService.openSharedArtid(String token)` (righe ~199-248) — **nome con `d` minuscola**. Endpoint pubblico `GET /api/shares/public/{token}` (`ShareController` righe ~114-117), passthrough senza try/catch.
- Messaggi (tutti `ResponseStatusException`):

  | # | Condizione | Status | Messaggio |
  |---|---|---|---|
  | 1 | token non decifrabile | 404 | `Link non valido.` |
  | 2 | share inesistente | 404 | `Link non valido.` |
  | 3 | scaduto | 410 | `Il link non è più valido.` |
  | 4 | disattivato | 410 | `Questo link è stato momentaneamente disattivato. Contatta l'autore per sapere quando tornerà attivo.` (apostrofo tipografico `’`) |
  | 5 | ArtID inesistente/soft-deleted | 410 | `L'ArtID desiderato non esiste più.` |
  | 6 | anteprima non costruibile | 410 | `Non è possibile visualizzare questo ArtID` |

- **Problema**: `server.error.include-message` non impostato → default `never` → il `message` **non è nel body JSON** (solo status + reason-phrase). Oggi il FE non può ricevere questi testi.
- FE: loader `src/routes/s/[token]/+page.server.ts` (righe ~8-19) usa `locals.api.GET('/api/shares/public/{token}')`, **ignora il body** e usa 2 messaggi hardcoded distinguendo solo 410 vs altro. `s/[token]/+error.svelte:15` mostra `page.error?.message`.

### Modifiche

**A) Backend — esporre il messaggio** (`application.properties`)
- Aggiungere: `server.error.include-message=always`.
- Tradeoff: espone il `reason` di **tutte** le `ResponseStatusException` dell'app. Qui sono testi user-facing in italiano scritti apposta → accettabile. Alternativa più controllata (opzionale): un `@RestControllerAdvice` che gestisce `ResponseStatusException` e serializza `getReason()` in un campo `message` stabile (RFC7807/ProblemDetail).

**B) Frontend — leggere e propagare il messaggio** (`s/[token]/+page.server.ts`)
- openapi-fetch ritorna `{ data, response, error }`; con `include-message=always` il body d'errore contiene `message`. Propagare quel messaggio (con fallback per robustezza):
  ```ts
  const { data, response, error: apiError } = await locals.api.GET('/api/shares/public/{token}', {
      params: { path: { token: params.token } }
  });
  if (!data) {
      const backendMessage = (apiError as { message?: string } | undefined)?.message;
      const fallback = response.status === 410
          ? 'Questo link di condivisione non è più disponibile.'
          : 'Link di condivisione non valido.';
      throw error(response.status || 404, backendMessage || fallback);
  }
  return { artid: data };
  ```
- `+error.svelte` mostra già `page.error?.message` → nessuna modifica lì.

### Verifica
- Token corrotto → 404 "Link non valido.". Link scaduto → 410 "Il link non è più valido.". Link disattivato → 410 col messaggio dedicato (non più collassato nello stesso testo dello scaduto). Casi 5/6 mostrano i rispettivi messaggi.
- Confermare che con `include-message=always` il campo `message` compaia effettivamente nel JSON (test manuale su un token scaduto).

---

## Task 5 — Materiali ordinati per `rank` (preview + explore) (solo backend)

**Obiettivo**: quando si mostra un ArtID in anteprima o in explore, i materiali sono ordinati per `rank`.

### Stato attuale
- `rank` **non è sull'entity `Resource`**: vive nella tabella di join `artid_resource` (colonna `rank`, 1-based; in quella tabella `id` = id dell'ArtID, `id_resource` = id risorsa). Scrittura del rank già gestita in `ArtidService` (link/reorder/remove).
- Vista **proprietario in modifica** (`GET /api/artids/{id}/resources` → `ResourceDAO.findByArtidForUser`): **già** `ORDER BY ar.rank ASC, r.id ASC`. OK.
- Viste **preview / explore / link condiviso** passano tutte per `UserService.buildArtidDetail` → `findPublicArtidMaterials` → `PUBLIC_ARTID_MATERIALS_SQL` (`app/artid-server/.../service/UserService.java`, righe ~140-147), che ordina **`ORDER BY r.id`** (NON per rank). ← causa radice.
- DTO al FE (`PublicMaterialResponse`) non espone `rank`; il FE (`public-artid-detail-view.svelte:12,134-140`) renderizza l'array **così com'è** → basta ordinarlo lato query.

### Modifica (`UserService.PUBLIC_ARTID_MATERIALS_SQL`, riga ~146)
```diff
-     ORDER BY r.id
+     ORDER BY ar.rank ASC, r.id ASC
```
Copre in un colpo solo **anteprima, explore e link condiviso** (stesso tie-break della vista proprietario). **Nessuna modifica frontend** né ai DTO.

### Verifica
- Riordinare i materiali dal dettaglio (drag&drop, già scrive il `rank`), poi aprire preview/explore/link pubblico: l'ordine coincide con quello del dettaglio.

---

## Task 6 — Layout stepper creazione link (`artid-create-link-modal.svelte`)

**Obiettivo**: migliorare estetica/UX dello stepper a 3 step del modale di creazione link esterni.

### Stato attuale
- File `src/lib/components/pages/artid/artid-create-link-modal.svelte`. 3 step statici (`STEPS`, righe ~18-22): 1 "Scadenza" (input data), 2 "Descrizione" (textarea), 3 "Fatto" (link generato + copia + apri anteprima). Stato `step` (1→3), reset su apertura (`$effect`), `createShare()` porta allo step 3.
- Indicatore: pallini numerati + connettori (markup righe ~114-134; stili `.step-circle`/`.step-connector` righe ~237-272). Contenuto/footer righe ~135-231. Contenitore in `ArtidEditorModal` con `customHeight="50" customWidth="42"`.
- **Problemi individuati**:
  1. Le label ("Scadenza/Descrizione/Fatto") compaiono solo nell'header (`currentStepLabel`), non sotto i pallini → i numeri nudi non comunicano il contenuto.
  2. Altezza modale fissa (`customHeight="50"`) con step di altezze molto diverse → spazio vuoto (step 1) e "salto" dei footer tra step.
  3. Allineamento footer incoerente: step 1 `justify-content-end`, step 2/3 `justify-content-between` → i bottoni si spostano orizzontalmente cambiando step.
  4. Textarea (step 2) e input link (step 3) sono grezzi (`form-control`), non allineati allo stile di `ArtidInput` (`bg-artid-section`, rounded).
  5. Nessun feedback di caricamento su "Conferma" durante `createShare` (`ArtidButton` non ha prop `loading`).
  6. Gerarchia visiva invertita: lo stato **corrente** ha sfondo trasparente, il **done** è pieno → il passo attivo risalta meno di quelli completati.

### Modifiche proposte (mantenere flat design: niente ombre/gradienti)
- **(1) Label sotto i pallini**: rendere ogni item dello stepper una colonna (pallino sopra, `label` sotto, in `small text-artid-text-muted`; label del corrente in `text-artid-primary`). Potrebbe servire un filo di `customWidth` in più per non andare a capo.
- **(3) Footer uniforme**: usare lo stesso schema in tutti gli step — secondario a sinistra, primario a destra (`justify-content-between`), così i bottoni non "ballano". (Step 1: "Chiudi" a sinistra, "Avanti" a destra.)
- **(6) Corrente più evidente**: invertire la gerarchia — `current` = riempimento `--artid-primary` + numero bianco; `done` = check con contorno primary (o pieno più tenue). Bilanciare col flat design.
- **(4) Coerenza input**: dare a textarea (step 2) e input link (step 3) le classi coerenti (`bg-artid-section`, `rounded-3`) o incapsularli nello stile di `ArtidInput`.
- **(2) Altezza**: valutare `customHeight` auto/ridotta per step compatti, oppure mantenere l'altezza fissa ma aggiungere spaziatura verticale attorno alla barra step (oggi solo `gap-3`).
- **(5, opzionale)** Spinner su "Conferma": richiede aggiungere una prop `loading` a `ui/artid-button.svelte` (componente condiviso — impatta altri usi, valutare).
- **(opzionale)** Estrarre in `ui/artid-stepper.svelte` se si prevede riuso (oggi è l'unico stepper del progetto).

### Verifica
- Percorso 1→2→3 fluido, bottoni in posizione stabile, passo corrente evidente, label leggibili, input coerenti col tema, nessuna regressione su `createShare`/copia/apri anteprima. Verifica responsive con `preview_resize`.

---

## Task 7 — resource-editor: in modifica nascondere select ArtID e input file

**Obiettivo**: aprendo l'editor su una risorsa esistente (modifica), mostrare **solo** Titolo e Descrizione; nascondere la select "Aggiungi a un ArtID" e la dropzone/input file. In creazione resta tutto.

### Stato attuale
- File `src/lib/components/pages/resources/resource-editor.svelte`. Flag già presente: `const isEditMode = $derived(resource != null);` (riga ~22) — `resource` è la prop opzionale.
- Markup: `div.row.g-3` con Titolo (`col-md-6`, sempre) + **select ArtID** (`col-12 col-md-6`, righe ~178-197); **dropzone + input file** (righe ~209-233). La validazione del bottone Salva già gestisce l'assenza di file in edit (`disabled={... || (!selectedFile && !isEditMode)}`).
- **⚠ Rischio backend (data loss)**: `ResourceService.update()` (`app/artid-server/.../service/ResourceService.java`, righe ~167-174) fa **sempre** `DELETE FROM artid_resource WHERE id_resource = ?` e ri-collega solo se `request.artidId() != null`. Nascondendo la select, `selectedArtidId` resta `''` → il FE non invia `artidId` → **ogni modifica di titolo/descrizione scollega il materiale da tutti gli ArtID**. (È già un bug latente anche oggi se si modifica senza riselezionare l'ArtID.)

### Modifiche

**A) Backend (necessario, prima o insieme al FE)** — `ResourceService.update()` righe ~171-174
Non toccare i collegamenti quando non arriva un `artidId`:
```java
// Preserva i collegamenti esistenti se l'update non specifica un ArtID
// (la modifica dai "Materiali" cambia solo metadati/file). Se un artidId è
// presente, resta valido il design "un materiale → un ArtID" (wipe+insert).
if (request.artidId() != null) {
    jdbcTemplate.update("DELETE FROM artid_resource WHERE id_resource = ?", saved.getId());
    artidService.linkArtidResource(request.artidId(), saved.getId(), userId);
}
```
(Rimuove il `DELETE` incondizionato; corregge anche il bug latente.)

**B) Frontend** — `resource-editor.svelte`
- Avvolgere la **colonna select** (righe ~178-197) e il **blocco dropzone+input file** (righe ~209-233) in `{#if !isEditMode} ... {/if}`.
- Layout: nascondendo la select, il Titolo (`col-md-6`) resta a metà. In edit portarlo a piena larghezza, es. `class={isEditMode ? 'col-12' : 'col-12 col-md-6'}` sulla colonna Titolo.
- Nessun'altra modifica alla logica submit: `selectedArtidId`/`selectedFile` partono vuoti e vengono appesi solo se valorizzati → in edit non si inviano (coerente col fix backend).

### Verifica
- Aprendo l'editor in **modifica**: niente select ArtID né dropzone; solo Titolo (full width) + Descrizione; Salva funziona. Dopo il salvataggio il materiale **resta collegato** al suo ArtID (verifica su DB/preview). In **creazione**: tutto invariato (select + file presenti, link creato).

---

## Task 8 — Colonna descrizione con icona + popup in hover (`resources-list.svelte`)

**Obiettivo**: aggiungere una piccola colonna con `<i class="bi bi-card-text"></i>`; in hover mostra un popup con la descrizione del file **renderizzata come HTML** (sanificata); esce dal mouse → sparisce. Senza rovinare l'estetica della tabella.

### Stato attuale
- File `src/lib/components/pages/resources/resources-list.svelte`. **Non** è una `<table>`: è una griglia Bootstrap (`div.row`/`div.col-*`). Le colonne sommano a **12**:
  `col-1` (checkbox/badge) + `col-3` (Nome) + `col-2` (Dimensioni) + `col-2` (Creato) + `col-2` (Modificato) + `col-1` (Collegato) + `col-1` (Preferito).
  Header righe ~148-162; righe ~165-214. Pattern icona-in-colonna da imitare (Preferito, righe ~206-212).
- Descrizione nel dato: `resource.description` (`ResourceResponse.description`, opzionale, **HTML** da Quill). Già presente nell'array renderizzato — nessuna fetch aggiuntiva.
- **Nessun componente tooltip/popover riutilizzabile**; Bootstrap JS è caricato ma i Tooltip/Popover **non sono inizializzati** (solo `data-bs-toggle="dropdown"` è usato). `title=` nativo non supporta HTML formattato.

### Modifiche
- **Ribilanciare a 12**: ridurre "Nome" da `col-3` a `col-2` e aggiungere una colonna `col-1` "Descrizione" (header con icona o testo breve). Aggiornare header **e** righe in modo coerente.
- **Icona + popup CSS-only** (coerente col flat design, niente JS Bootstrap): wrapper `position-relative` con l'icona; pannello `position-absolute` nascosto, mostrato su `:hover`, con dentro `{@html sanitizeHtml(resource.description)}`.
  ```svelte
  <script>
    import { sanitizeHtml } from '$lib/sanitize';
  </script>

  <div class="col-1 text-center desc-cell">
    {#if resource.description}
      <i class="bi bi-card-text fs-5 text-artid-text-muted" aria-label="Descrizione"></i>
      <div class="desc-popup border border-artid-border bg-artid-surface rounded-3 p-2 text-start">
        {@html sanitizeHtml(resource.description)}
      </div>
    {:else}
      <i class="bi bi-card-text fs-5 text-artid-border" aria-hidden="true"></i>
    {/if}
  </div>
  ```
  ```scss
  .desc-cell { position: relative; }
  .desc-cell .desc-popup {
      position: absolute; z-index: 5; right: 0; top: 100%;
      width: 18rem; max-height: 14rem; overflow-y: auto;
      display: none;                 // nascosto di default
      font-size: 0.85rem;
  }
  .desc-cell:hover .desc-popup { display: block; }  // compare in hover, sparisce all'uscita
  ```
  Note: `{@html}` + `sanitizeHtml` è il **primo uso** nel progetto — `sanitize.ts` è esattamente la difesa XSS prevista per questo caso. Se `description` è vuota, icona muted senza popup. (Alternativa Bootstrap Popover JS `html:true` sconsigliata: nessun init esistente, meno idiomatica.)

### Verifica
- Riga con descrizione: hover sull'icona → popup con HTML formattato; mouse-out → sparisce. Riga senza descrizione: icona spenta, nessun popup. Layout tabella invariato (12 colonne), nessuno scroll orizzontale (`preview_inspect`/`preview_screenshot`).

---

## Task 9 — Stellina preferiti non si aggiorna (`artid-card.svelte`)

**Obiettivo**: aggiungendo/rimuovendo un ArtID dai preferiti nella lista, la stella si aggiorna subito (piena/vuota) senza "rollback" ottico.

### Stato attuale (perché non si aggiorna)
- File `src/lib/components/pages/artid/artid-card.svelte`. Stato locale `isFavourite` (riga ~36) + `$effect` (righe ~37-39) che **riallinea** `isFavourite` a `artid.favourite` ad ogni cambio prop.
- `toggleFavourite` (righe ~44-74): `PUT /api/artids/{id}/favourite` (body booleano nudo), poi `isFavourite = newFavouriteState` **e** `await invalidateAll()`.
- **Race**: `invalidateAll()` ricarica il `load` → nuovo oggetto `artid` dal server → l'`$effect` riscrive `isFavourite = artid.favourite`. Il `PUT` **non restituisce** l'artid aggiornato (`updateFavourite` → 200 senza body) e `favourite` è opzionale nel GET: se il GET non riflette il nuovo valore, l'effect **annulla** l'update ottimistico → la stella torna vuota.
- Il dettaglio invece funziona (`artid-details-top-actions.svelte:26-52`): muta direttamente `artid.favourite` e lega l'icona a `artid.favourite`, senza stato locale né `invalidateAll`.

### Modifiche (adottare il pattern del dettaglio)
- Rimuovere lo stato locale `isFavourite` (riga ~36) e l'`$effect` di riallineamento (righe ~37-39).
- Legare l'icona direttamente a `artid.favourite` (righe ~120-123): `bi bi-star{artid.favourite ? '-fill text-warning' : ''}`.
- In `toggleFavourite`, su successo mutare il prop reattivo e **rimuovere `invalidateAll()`**:
  ```ts
  const newFavouriteState = !artid.favourite;
  const response = await api.PUT('/api/artids/{id}/favourite', {
      params: { path: { id: artid.id! } },
      body: newFavouriteState
  });
  if (!response.error) {
      artid.favourite = newFavouriteState;   // aggiorna subito la stella + i counts derivati
      toast.success(newFavouriteState ? 'ArtID aggiunto ai preferiti' : 'ArtID rimosso dai preferiti');
  } else { /* toast.error ... */ }
  ```
  `data.artids` è un array `$state`: mutare `artid.favourite` aggiorna la card **e** `filterCounts`/`visibleArtids` (derivati). Nel filtro "Preferiti", rimuovere un preferito fa uscire la card dal derivato senza refresh.
- **Prerequisito da confermare**: il backend **persiste** `favourite` (così un reload manuale mostra lo stato giusto). Se così, niente `invalidateAll`. Se per qualche motivo servisse il refresh dei counts dal server, tenerlo **senza** ripristinare lo stato locale/effect (che è la causa del rollback).

### Verifica
- Click sulla stella in "I miei": si riempie e resta piena; ri-click: si svuota. In "Preferiti": rimuovendo, la card sparisce. Ricaricando la pagina lo stato è coerente col DB.

---

## Appendice — Riepilogo file per task

| Task | Frontend | Backend | api:generate |
|---|---|---|---|
| 1 Filtro tag AND | `routes/(app)/artid/+page.svelte`, `components/layout/artid-sidebar.svelte`, `components/pages/artid/artid-card.svelte` | `ArtidResponse` + query lista (tagIds) *(consigliato)* | **Sì** (se opzione BE) |
| 2 Email interna | — | `service/ShareService.java` (`addInternalShare`, TODO) | No |
| 3 Nav preview/explore | `components/pages/shares/shares-list.svelte`, `components/pages/artid/artid-shared-card.svelte` | — | No |
| 4 Messaggi errore | `routes/s/[token]/+page.server.ts` | `application.properties` (`include-message`) | No |
| 5 Ordine rank | — (nessuna) | `service/UserService.java` (`PUBLIC_ARTID_MATERIALS_SQL`) | No |
| 6 Stepper | `components/pages/artid/artid-create-link-modal.svelte` (+ opz. `ui/artid-button.svelte`) | — | No |
| 7 Editor modifica | `components/pages/resources/resource-editor.svelte` | `service/ResourceService.java` (`update`) | No |
| 8 Colonna descrizione | `components/pages/resources/resources-list.svelte`, `lib/sanitize.ts` | — | No |
| 9 Stellina preferiti | `components/pages/artid/artid-card.svelte` | — | No |

**Percorsi backend** relativi a `app/artid-server/src/main/java/afam/artidserver/`. **Percorsi frontend** relativi a `app/artid-client/src/`.
