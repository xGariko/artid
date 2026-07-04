import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	// Endpoint pubblico: nessun login. La GET registra lato server la visualizzazione (contatore,
	// prima/ultima visione) e restituisce il dettaglio dell'ArtID collegato alla condivisione,
	// purché attiva e non scaduta. 410 = link scaduto/disattivato, 404 = token non valido.
	const { data, response } = await locals.api.GET('/api/shares/public/{token}', {
		params: { path: { token: params.token } }
	});

	if (!data) {
		if (response.status === 410) {
			throw error(410, 'Questo link di condivisione è scaduto o non è più disponibile.');
		}
		throw error(404, 'Link di condivisione non valido.');
	}

	return { artid: data };
};
