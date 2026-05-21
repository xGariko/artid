import type { Cookies } from "@sveltejs/kit";
import { dev } from "$app/environment";
import { env } from "$env/dynamic/private";
import { createHash } from "node:crypto";
import type { AuthUser } from "$lib/stores/auth";

const TOKEN_COOKIE = "token";
const ME_CACHE_TTL_MS = 60_000;

const API_BASE = env.API_BASE ?? "http://localhost:8080";
const TOKEN_MAX_AGE = Number.parseInt(env.AUTH_COOKIE_MAX_AGE_SECONDS ?? "86400", 10);

type LoginResult =
	| { ok: true; user: AuthUser }
	| { ok: false; error: string };

type CurrentUserResult =
	| { ok: true; user: AuthUser }
	| { ok: false; status: "unauthorized" | "error" };

interface LoginPayload {
	email: string;
	password: string;
}

interface CacheEntry {
	user: AuthUser;
	expiresAt: number;
}

const meCache = new Map<string, CacheEntry>();

function hashToken(token: string): string {
	return createHash("sha256").update(token).digest("hex");
}

function pruneCache(now: number): void {
	for (const [k, entry] of meCache) {
		if (entry.expiresAt <= now) meCache.delete(k);
	}
}

function invalidateCacheFor(token: string): void {
	meCache.delete(hashToken(token));
}

const cookieOptions = {
	path: "/" as const,
	httpOnly: true as const,
	sameSite: "strict" as const,
	secure: !dev,
	maxAge: TOKEN_MAX_AGE,
};

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
	cookies.set(TOKEN_COOKIE, data.token, cookieOptions);

	return {
		ok: true,
		user: { id: data.id, email: data.email, name: data.name, surname: data.surname },
	};
}

export function logout(cookies: Cookies): void {
	const token = cookies.get(TOKEN_COOKIE);
	if (token) invalidateCacheFor(token);
	cookies.delete(TOKEN_COOKIE, { path: "/" });
}

export function getToken(cookies: Cookies): string | null {
	return cookies.get(TOKEN_COOKIE) ?? null;
}

export async function fetchCurrentUser(token: string): Promise<CurrentUserResult> {
	const key = hashToken(token);
	const now = Date.now();

	const cached = meCache.get(key);
	if (cached && cached.expiresAt > now) {
		return { ok: true, user: cached.user };
	}

	try {
		const res = await fetch(`${API_BASE}/api/auth/me`, {
			headers: { Authorization: `Bearer ${token}` },
		});
		if (res.status === 401 || res.status === 403) {
			meCache.delete(key);
			return { ok: false, status: "unauthorized" };
		}
		if (!res.ok) {
			return { ok: false, status: "error" };
		}
		const user = (await res.json()) as AuthUser;
		pruneCache(now);
		meCache.set(key, { user, expiresAt: now + ME_CACHE_TTL_MS });
		return { ok: true, user };
	} catch {
		return { ok: false, status: "error" };
	}
}
