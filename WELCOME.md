<div align="center">

<img src="resources/logo.svg" alt="ArtID Logo" height="70" style="margin-top: 20pt"/>

# Developer Welcome Kit

[![SvelteKit](https://img.shields.io/badge/SvelteKit-2.x-FF3E00?logo=svelte&logoColor=white)](https://kit.svelte.dev)
[![Svelte](https://img.shields.io/badge/Svelte-5%20Runes-FF3E00?logo=svelte&logoColor=white)](https://svelte.dev)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.x-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![Vite](https://img.shields.io/badge/Vite-8.x-646CFF?logo=vite&logoColor=white)](https://vite.dev)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)

</div>

---

## Indice

- [Setup](#setup)
- [Architettura](#architettura)
- [Struttura del progetto](#struttura-del-progetto)
- [Convenzioni](#convenzioni)
- [Moduli](#moduli)
- [Git workflow](#git-workflow)
- [Il team](#il-team)

---

## Setup

### Frontend

**Prerequisiti:** Node.js >= 20, npm >= 10

```bash
cd app/artid-client
npm install
npm run dev        # http://localhost:5173
```

| Comando               | Descrizione                            |
|-----------------------|----------------------------------------|
| `npm run dev`         | Dev server con HMR                     |
| `npm run api:generate`| Aggiorna schema OpenAPI                |
| `npm run build`       | Build di produzione                    |
| `npm run preview`     | Preview della build locale             |
| `npm run check`       | Type-check con `svelte-check`          |
| `npm run check:watch` | Type-check in watch mode               |
| `npm run lint`        | Lint + verifica formattazione          |
| `npm run format`      | Formatta tutto con Prettier            |

### Backend

**Prerequisiti:** JDK 21, Maven (o usa il wrapper incluso)

```bash
cd app/artid-server
./mvnw spring-boot:run     # http://localhost:8080
```

| Comando                    | Descrizione                        |
|----------------------------|------------------------------------|
| `./mvnw spring-boot:run`   | Avvia il server in dev             |
| `./mvnw clean package`     | Build del JAR                      |
| `./mvnw test`              | Esegue i test                      |

> è consigliabile usare IntelliJ avviando `ArtidServerApplication.java` dall'ide

---

## Architettura

```
┌──────────────────────────────────────────┐
│              artid-client                │
│          SvelteKit (SSR + CSR)           │
│                                          │
│  routes/   → pagine (file-based)         │
│  lib/      → componenti, store, api      │
└──────────────────┬───────────────────────┘
                   │ REST (JSON)
┌──────────────────▼───────────────────────┐
│              artid-server                │
│         Spring Boot 4 + Java 21          │
│                                          │
│  controller/ → endpoint REST             │
│  service/    → logica di business        │
│  dao/        → accesso al database       │
│  model/      → entità e DTO              │
└──────────────────┬───────────────────────┘
                   │ Spring Data JDBC
┌──────────────────▼───────────────────────┐
│               Database (TBD)             │
└──────────────────────────────────────────┘
```

> **Svelte 5 Runes** attivo su tutto il client (forzato in `svelte.config.js`).
> Usa `$state`, `$derived`, `$effect` — non mescolare con la vecchia API reattiva.

---

## Struttura del progetto

```
artid/
├── app/
│   ├── artid-client/                        # Frontend SvelteKit
│   │   ├── src/
│   │   │   ├── routes/                      # Pagine e layout (file-based routing)
│   │   │   └── lib/
│   │   │       ├── components/              # Componenti UI riusabili
│   │   │       ├── stores/                  # Stato globale
│   │   │       ├── api/                     # Client HTTP verso il backend
│   │   │       └── types/                   # Tipi TypeScript condivisi
│   │   └── static/
│   │
│   └── artid-server/                        # Backend Spring Boot
│       └── src/main/java/afam/artidserver/
│           ├── controller/                  # REST controllers
│           ├── service/                     # Business logic
│           ├── dao/                         # Data access (Spring Data JDBC)
│           └── model/                       # Entità e DTO
│
└── resources/                               # Logo e asset di progetto
```

---

## Convenzioni

Variabili e nomi sempre in **inglese**.

| Contesto              | Stile             | Esempio                          |
|-----------------------|-------------------|----------------------------------|
| Classi (Java/TS)      | `PascalCase`      | `UserProfile`, `FileController`  |
| Variabili / metodi    | `camelCase`       | `getSharedLink`, `userId`        |
| Costanti / enum       | `CAPS_SNAKE_CASE` | `MAX_FILE_SIZE_MB`, `Role.ADMIN` |
| File `.svelte`        | `PascalCase`      | `FileCard.svelte`                |
| File `.ts`            | `camelCase`       | `fileUtils.ts`                   |
| Route SvelteKit       | `kebab-case/`     | `routes/share-link/+page.svelte` |
| Package Java          | `lowercase`       | `afam.artidserver.controller`    |

### Backend (Java)
- Architettura a strati: `controller` → `service` → `dao` — non saltare livelli
- Lombok per ridurre il boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- Nessuna logica di business nei controller; nessuna query SQL nei service

### Frontend (Svelte)
- Logica di business in file `.ts` sotto `$lib`, non inline nel `<script>` del componente
- Stato globale via stores; evitare prop-drilling oltre 2 livelli
- Runes only: `$state`, `$derived`, `$effect`
- Niente `any` in TypeScript — usa `unknown` e restringi il tipo

---

## Moduli

| Modulo           | Responsabilità                                                                |
|------------------|-------------------------------------------------------------------------------|
| **Auth**         | Login, OTP via email, recupero credenziali, mock SPID                         |
| **Profile**      | Dati anagrafici, formazione, tag competenze                                   |
| **File Manager** | CRUD file multimediali, tagging, container dinamici                           |
| **Sharing**      | Token di condivisione opachi con scadenza, visibilità granulare, tracciamento |
| **Guest Page**   | Apertura link senza account, ricerca per nome o tag                           |

---

## Git workflow

```
main
  └── feature/<nome>    # nuova funzionalità
  └── fix/<nome>        # bugfix
  └── chore/<nome>      # dipendenze, config, refactor
```

- Nessun commit diretto su `main` — PR con almeno una review
- Squash merge per tenere la history pulita
- Formato commit: `feat: descrizione` · `fix: descrizione` · `chore: descrizione`

---

## Il team

<div style="display: flex; gap:25pt; justify-content: space-around; align-items: center; padding: 10pt 0 10pt 0;">
    <div align="center">
      <img src="https://avatars.githubusercontent.com/u/67762382?v=4" width="64" height="64" style="border-radius:50%"/><br/>
      <strong>Davide Alaimo</strong><br/>
      <sub>Developer</sub><br/>
      <a href="https://github.com/Tynamo9/">@Tynamo9</a>
    </div>
    <div align="center">
      <img src="https://avatars.githubusercontent.com/u/48565568?v=4" width="64" height="64" style="border-radius:50%"/><br/>
      <strong>Andrea Sciortino</strong><br/>
      <sub>Developer</sub><br/>
      <a href="https://github.com/astroxd/">@astroxd</a>
    </div>
    <div align="center">
      <img src="https://avatars.githubusercontent.com/u/60857450?v=4" width="64" height="64" style="border-radius:50%"/><br/>
      <strong>Gabriele Iovino</strong><br/>
      <sub>Developer</sub><br/>
      <a href="https://github.com/xGariko/">@xGariko</a>
    </div>
    <div align="center">
      <img src="https://avatars.githubusercontent.com/u/185094450?v=4" width="64" height="64" style="border-radius:50%"/><br/>
      <strong>Giovanni Luca Cusano</strong><br/>
      <sub>Developer</sub><br/>
      <a href="https://github.com/0ftt/">@0ftt</a>
    </div>
</div>
