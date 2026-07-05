import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	// Endpoint pubblico: nessun login. La GET registra lato server la visualizzazione (contatore,
	// prima/ultima visione) e restituisce il dettaglio dell'ArtID collegato alla condivisione,
	// purché attiva e non scaduta. 410 = link scaduto/disattivato, 404 = token non valido.
	let result;
	try {
		result = await locals.api.GET('/api/shares/public/{token}', {
			params: { path: { token: params.token } }
		});
	} catch {
		// La fetch verso il backend è fallita a livello di rete (backend irraggiungibile/in avvio,
		// timeout): openapi-fetch NON cattura questi errori, quindi senza questo try l'eccezione
		// risalirebbe come 500 "grezzo" invece della pagina d'errore. È un problema di servizio,
		// non del link.
		throw error(503, 'Servizio temporaneamente non disponibile. Riprova tra poco.');
	}

	const { data, response, error: apiError } = result;

	if (!data) {
		// Backend raggiungibile ma in errore interno (5xx): anche qui non è il link a essere invalido.
		if (response.status >= 500) {
			throw error(503, 'Servizio temporaneamente non disponibile. Riprova tra poco.');
		}
		// I messaggi puntuali (scaduto, disattivato, ArtID rimosso…) arrivano dal backend nel campo
		// `message` del body d'errore (server.error.include-message=always). Fallback per status.
		const backendMessage = (apiError as { message?: string } | undefined)?.message;
		const fallback =
			response.status === 410
				? 'Questo link di condivisione non è più disponibile.'
				: 'Link di condivisione non valido.';
		throw error(response.status || 404, backendMessage || fallback);
	}

	return { artid: data };
};
