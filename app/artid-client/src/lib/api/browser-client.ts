import createClient from "openapi-fetch";
import { beginRequest, endRequest } from "$lib/stores/loading";
import { toast } from "$lib/toast";
import type { paths } from "./schema";

// Retry delle chiamate al BE: dopo il primo tentativo la richiesta viene ripetuta fino a MAX_RETRIES
// volte, con RETRY_DELAY_MS di attesa fra un tentativo e l'altro.
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 2000;

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

// fetch con retry usato da openapi-fetch. Se una chiamata fallisce (risposta non-ok o errore di rete)
// la ripete fino a MAX_RETRIES volte, e segnala ogni RETRY fallito con un toast "Tentativo N: fallito"
// (il primo tentativo non è un retry, quindi non produce toast). Esauriti i retry propaga comunque
// l'ultimo esito — risposta non-ok o eccezione — così il chiamante mostra il suo toast d'errore come
// adesso. La Request va clonata a ogni tentativo perché fetch ne consuma il body.
async function fetchWithRetry(input: Request): Promise<Response> {
	let lastResponse: Response | undefined;
	let lastError: unknown;

	for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
		if (attempt > 0) {
			await wait(RETRY_DELAY_MS);
		}

		lastResponse = undefined;
		lastError = undefined;
		try {
			const response = await fetch(input.clone());
			if (response.ok) {
				return response;
			}
			lastResponse = response;
		} catch (error) {
			lastError = error;
		}

		// Tentativo fallito: i retry (attempt ≥ 1) vengono annunciati, numerati 1…MAX_RETRIES.
		if (attempt > 0) {
			toast.error(`Tentativo ${attempt}: fallito`);
		}
	}

	// Retry esauriti: propaga l'ultimo fallimento al chiamante.
	if (lastResponse) {
		return lastResponse;
	}
	throw lastError;
}

// Client openapi-fetch lato browser: baseUrl vuoto = chiamate relative,
// same-origin. Ogni path che usi qui deve avere un proxy +server.ts
// corrispondente che inoltra a Spring iniettando il JWT dal cookie httpOnly.
// Lo schema è lo stesso del server, quindi i tipi combaciano end-to-end.
export const api = createClient<paths>({ baseUrl: "", fetch: fetchWithRetry });

api.use({
	onRequest({ request }) {
		beginRequest();
		return request;
	},
	onResponse({ response }) {
		endRequest();
		return response;
	},
});
