import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	// Endpoint pubblico: nessun login. La GET registra lato server la visualizzazione (contatore,
	// prima/ultima visione) e restituisce il dettaglio dell'ArtID collegato alla condivisione,
	// purché attiva e non scaduta. 410 = link scaduto/disattivato, 404 = token non valido.
	const {
		data,
		response,
		error: apiError
	} = await locals.api.GET('/api/shares/public/{token}', {
		params: { path: { token: params.token } }
	});

	if (!data) {
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
