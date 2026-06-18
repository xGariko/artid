import type { Handle } from '@sveltejs/kit';
import { redirect } from '@sveltejs/kit';
import { fetchCurrentUser, getToken, logout } from '$lib/auth.ts';
import { createApiClient } from '$lib/api/client';

// Pagine "auth-only": accessibili senza login, ma se sei loggato vieni rimandato in dashboard.
const PUBLIC_PATHS = ['/login', '/register', '/welcome', '/spid'];

// Pagine aperte a tutti, con o senza login (nessun redirect in nessuno dei due sensi).
const OPEN_PATHS = ['/explore'];

function isPublic(pathname: string): boolean {
	return PUBLIC_PATHS.some((p) => pathname.startsWith(p));
}

function isOpen(pathname: string): boolean {
	return OPEN_PATHS.some((p) => pathname.startsWith(p));
}

export const handle: Handle = async ({ event, resolve }) => {
	let token = getToken(event.cookies);
	event.locals.token = token;
	event.locals.user = null;
	event.locals.api = createApiClient(token);

	if (token) {
		const result = await fetchCurrentUser(event.locals.api, token);
		if (result.ok) {
			event.locals.user = result.user;
		} else if (result.status === 'unauthorized') {
			logout(event.cookies);
			event.locals.token = null;
			event.locals.api = createApiClient(null);
			token = null;
		}
	}

	const { pathname } = event.url;

	// Le rotte /api/* sono chiamate da fetch/XHR: senza token devono lasciar rispondere
	// 401 JSON al rispettivo +server.ts, NON redirigere a /login (l'HTML del login farebbe
	// fallire il parsing JSON lato client con "Unexpected token '<'").
	const isApiRoute = pathname.startsWith('/api/');

	if (pathname === '/') {
		redirect(303, token ? '/dashboard' : '/welcome');
	}

	if (!token && !isPublic(pathname) && !isOpen(pathname) && !isApiRoute) {
		redirect(303, '/login');
	}

	if (token && isPublic(pathname)) {
		redirect(303, '/dashboard');
	}

	return resolve(event);
};
