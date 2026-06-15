import createClient from "openapi-fetch";
import { env } from "$env/dynamic/private";
import { beginRequest, endRequest } from "$lib/stores/loading";
import type { paths } from "./schema";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

export type ApiClient = ReturnType<typeof createClient<paths>>;

// Factory usato solo da hooks.server.ts per costruire il client per-request
// e attaccarlo a event.locals.api. Nel resto dell'app usare `locals.api`.
export function createApiClient(token?: string | null): ApiClient {
	const client = createClient<paths>({
		baseUrl: API_BASE,
		headers: token ? { Authorization: `Bearer ${token}` } : {},
	});

	// Auto-progress: ogni chiamata incrementa il counter di richieste attive.
	client.use({
		onRequest({ request }) {
			beginRequest();
			return request;
		},
		onResponse({ response }) {
			endRequest();
			return response;
		},
	});

	return client;
}
