import type { LayoutServerLoad } from "./$types";

// Propic per l'avatar in navbar. Query dedicata (solo l'immagine), caricata UNA volta
// all'ingresso nell'app shell: SvelteKit non rifà questo load nelle navigazioni client
// tra pagine (app), solo su full-load o invalidateAll(). Sta qui e non negli hooks
// (che girano a ogni richiesta, hot path) né dentro /api/auth/me, coerente con la
// scelta di tenere la propic — pesante — fuori dal percorso autenticazione.
// Dopo il salvataggio profilo, profile-form chiama invalidateAll() → la navbar si aggiorna.
export const load: LayoutServerLoad = async ({ locals }) => {
	if (!locals.user) {
		return { propicUrl: null };
	}
	const { data } = await locals.api.GET("/api/profile/avatar");
	return { propicUrl: data?.url ?? null };
};
