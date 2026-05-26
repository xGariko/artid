import type { Cookies } from "@sveltejs/kit";
import { dev } from "$app/environment";
import { env } from "$env/dynamic/private";
import { createHash } from "node:crypto";
import type { ApiClient } from "$lib/api/client";
import type { AuthUser } from "$lib/stores/auth";

const TOKEN_COOKIE = "token";
const ME_CACHE_TTL_MS = 60_000;

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

export async function login(
	api: ApiClient,
	cookies: Cookies,
	payload: LoginPayload,
): Promise<LoginResult> {
	const { data } = await api.POST("/api/auth/login", { body: payload });

	if (!data?.token) {
		return { ok: false, error: "Credenziali non valide." };
	}

	cookies.set(TOKEN_COOKIE, data.token, cookieOptions);

	return {
		ok: true,
		user: {
			id: data.id!,
			email: data.email!,
			name: data.name!,
			surname: data.surname!,
		},
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

export async function fetchCurrentUser(
	api: ApiClient,
	token: string,
): Promise<CurrentUserResult> {
	const key = hashToken(token);
	const now = Date.now();

	const cached = meCache.get(key);
	if (cached && cached.expiresAt > now) {
		return { ok: true, user: cached.user };
	}

	try {
		const { data, response } = await api.GET("/api/auth/me");
		if (response.status === 401 || response.status === 403) {
			meCache.delete(key);
			return { ok: false, status: "unauthorized" };
		}
		if (!data) {
			return { ok: false, status: "error" };
		}
		const user: AuthUser = {
			id: data.id!,
			email: data.email!,
			name: data.name!,
			surname: data.surname!,
		};
		pruneCache(now);
		meCache.set(key, { user, expiresAt: now + ME_CACHE_TTL_MS });
		return { ok: true, user };
	} catch {
		return { ok: false, status: "error" };
	}
}
