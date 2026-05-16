import type { Handle } from "@sveltejs/kit";
import { redirect } from "@sveltejs/kit";

const PUBLIC_PATHS = ["/login", "/register"];

function isPublic(pathname: string): boolean {
	return PUBLIC_PATHS.some((p) => pathname.startsWith(p));
}

export const handle: Handle = async ({ event, resolve }) => {
	const token = event.cookies.get("token") ?? null;
	event.locals.token = token;
	event.locals.user = null;

	if (token) {
		try {
			// TODO: chiamata a Spring GET /api/auth/me con Authorization header
			// const res = await fetch(`${API_BASE}/api/auth/me`, {
			//   headers: { Authorization: `Bearer ${token}` }
			// });
			// event.locals.user = await res.json();
		} catch {
			event.cookies.delete("token", { path: "/" });
			event.locals.token = null;
		}
	}

	const { pathname } = event.url;

	if (pathname === "/") {
		redirect(303, token ? "/dashboard" : "/login");
	}

	if (!token && !isPublic(pathname)) {
		redirect(303, "/login");
	}

	if (token && isPublic(pathname)) {
		redirect(303, "/dashboard");
	}

	return resolve(event);
};
