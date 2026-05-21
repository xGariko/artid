import type { Handle } from "@sveltejs/kit";
import { redirect } from "@sveltejs/kit";
import { fetchCurrentUser, getToken, logout } from "$lib/server/auth";

const PUBLIC_PATHS = ["/login", "/register", "/welcome"];

function isPublic(pathname: string): boolean {
	return PUBLIC_PATHS.some((p) => pathname.startsWith(p));
}

export const handle: Handle = async ({ event, resolve }) => {
	let token = getToken(event.cookies);
	event.locals.token = token;
	event.locals.user = null;

	if (token) {
		const result = await fetchCurrentUser(token);
		if (result.ok) {
			event.locals.user = result.user;
		} else if (result.status === "unauthorized") {
			logout(event.cookies);
			event.locals.token = null;
			token = null;
		}
	}

	const { pathname } = event.url;

	if (pathname === "/") {
		redirect(303, token ? "/dashboard" : "/welcome");
	}

	if (!token && !isPublic(pathname)) {
		redirect(303, "/login");
	}

	if (token && isPublic(pathname)) {
		redirect(303, "/dashboard");
	}

	return resolve(event);
};
