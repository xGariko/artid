import createClient from "openapi-fetch";
import { beginRequest, endRequest } from "$lib/stores/loading";
import type { paths } from "./schema";

// Client openapi-fetch lato browser: baseUrl vuoto = chiamate relative,
// same-origin. Ogni path che usi qui deve avere un proxy +server.ts
// corrispondente che inoltra a Spring iniettando il JWT dal cookie httpOnly.
// Lo schema è lo stesso del server, quindi i tipi combaciano end-to-end.
export const api = createClient<paths>({ baseUrl: "" });

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
