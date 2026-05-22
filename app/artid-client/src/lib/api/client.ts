import createClient from "openapi-fetch";
import { env } from "$env/dynamic/private";
import { beginRequest, endRequest } from "$lib/stores/spinner-loading";
import type { paths } from "./schema";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

export function createApiClient(token?: string | null) {
	const client = createClient<paths>({
		baseUrl: API_BASE,
		headers: token ? { Authorization: `Bearer ${token}` } : {},
	});

	// Auto-progress: ogni chiamata fatta tramite questo client incrementa
	// il counter di richieste attive. La progress bar si attiva di
	// conseguenza, senza che il consumer debba toccare nulla.
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
