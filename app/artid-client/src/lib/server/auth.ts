import type { Cookies } from "@sveltejs/kit";
import { dev } from "$app/environment";

const TOKEN_COOKIE = "token";
const TOKEN_MAX_AGE = 60 * 60 * 24 * 7;
const API_BASE = "http://localhost:8080";

type LoginResult =
	| { ok: true; user: Record<string, unknown> }
	| { ok: false; error: string };

interface LoginPayload {
	email: string;
	password: string;
}

export async function login(cookies: Cookies, payload: LoginPayload): Promise<LoginResult> {
	const res = await fetch(`${API_BASE}/api/auth/login`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(payload),
	});

	if (!res.ok) {
		return { ok: false, error: "Credenziali non valide." };
	}

	const data = await res.json();

	cookies.set(TOKEN_COOKIE, data.token, {
		path: "/",
		httpOnly: true,
		sameSite: "strict",
		secure: !dev,
		maxAge: TOKEN_MAX_AGE,
	});

	return { ok: true, user: data };
}

export function logout(cookies: Cookies): void {
	cookies.delete(TOKEN_COOKIE, { path: "/" });
}

export function getToken(cookies: Cookies): string | null {
	return cookies.get(TOKEN_COOKIE) ?? null;
}
