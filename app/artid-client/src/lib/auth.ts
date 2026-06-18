import type { Cookies } from "@sveltejs/kit";
import { dev } from "$app/environment";
import { env } from "$env/dynamic/private";
import { createHash } from "node:crypto";
import type { ApiClient } from "$lib/api/client";
import type { AuthUser } from "$lib/stores/auth";

const TOKEN_COOKIE = "token";
const ME_CACHE_TTL_MS = 60_000;

const TOKEN_MAX_AGE = Number.parseInt(env.AUTH_COOKIE_MAX_AGE_SECONDS ?? "86400", 10);

// Step 1 del login: credenziali valide, OTP inviato via email. Nessuna sessione ancora.
type RequestOtpResult =
	| { ok: true; email: string }
	| { ok: false; error: string };

// Come RequestOtpResult ma con un hint sul campo in errore (es. email già registrata → 409,
// così la pagina può mostrare l'errore sul campo email anziché come errore generale).
type RegistrationOtpResult =
	| { ok: true; email: string }
	| { ok: false; error: string; field?: "email" };

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

interface VerifyOtpPayload {
	email: string;
	code: string;
}

interface RegisterPayload {
	name: string;
	surname: string;
	email: string;
	password: string;
	birthdate?: string;
	birthplace?: string;
}

// Forma minima di AuthResponse: ciò che serve per aprire la sessione.
interface AuthResponseData {
	token?: string;
	id?: number;
	email?: string;
	name?: string;
	surname?: string;
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

// Persiste il JWT come cookie di sessione e mappa la AuthResponse in AuthUser.
// Condiviso tra verifica OTP (login) e registrazione (che riceve già un token).
export function establishSession(cookies: Cookies, data: AuthResponseData): AuthUser {
	cookies.set(TOKEN_COOKIE, data.token!, cookieOptions);
	return {
		id: data.id!,
		email: data.email!,
		name: data.name!,
		surname: data.surname!,
	};
}

// Step 1 del login: valida le credenziali; in caso positivo il server invia l'OTP via email.
export async function requestOtp(
	api: ApiClient,
	payload: LoginPayload,
): Promise<RequestOtpResult> {
	const { data, response } = await api.POST("/api/auth/login", { body: payload });

	if (!response.ok || !data?.otpRequired) {
		// 401 = credenziali errate; altro (es. 502) = invio email fallito.
		const error =
			response.status === 401
				? "Email o password non corretti."
				: "Impossibile inviare il codice OTP. Riprova tra poco.";
		return { ok: false, error };
	}

	return { ok: true, email: data.email ?? payload.email };
}

// Step 2 del login: verifica l'OTP e, se valido, apre la sessione.
export async function verifyOtp(
	api: ApiClient,
	cookies: Cookies,
	payload: VerifyOtpPayload,
): Promise<LoginResult> {
	const { data, response } = await api.POST("/api/auth/verify-otp", { body: payload });

	if (!response.ok || !data?.token) {
		return { ok: false, error: "Codice non valido o scaduto." };
	}

	return { ok: true, user: establishSession(cookies, data) };
}

// "Riprova": rigenera e rinvia l'OTP. Fire-and-forget: il server risponde 200 comunque.
export async function resendOtp(api: ApiClient, email: string): Promise<void> {
	await api.POST("/api/auth/resend-otp", { body: { email } });
}

// --- Registrazione con verifica email: stesso schema a due passi del login, ma l'account
// viene creato solo allo step 2 (dopo l'OTP). I dati pendenti vivono lato server (registration_otp). ---

// Step 1: se l'email è libera, il server salva i dati come pending e invia l'OTP di verifica.
export async function requestRegistration(
	api: ApiClient,
	payload: RegisterPayload,
): Promise<RegistrationOtpResult> {
	const { data, response } = await api.POST("/api/auth/register", { body: payload });

	if (!response.ok || !data?.otpRequired) {
		if (response.status === 409) {
			return { ok: false, error: "Esiste già un account con questa email.", field: "email" };
		}
		// 502 = email non inviabile; altro = errore generico.
		const error =
			response.status === 502
				? "Impossibile inviare il codice di verifica. Riprova tra poco."
				: "Errore durante la registrazione. Riprova.";
		return { ok: false, error };
	}

	return { ok: true, email: data.email ?? payload.email };
}

// Step 2: verifica l'OTP e, se valido, crea l'account e apre la sessione.
export async function verifyRegistration(
	api: ApiClient,
	cookies: Cookies,
	payload: VerifyOtpPayload,
): Promise<LoginResult> {
	const { data, response } = await api.POST("/api/auth/verify-registration", { body: payload });

	if (!response.ok || !data?.token) {
		return { ok: false, error: "Codice non valido o scaduto." };
	}

	return { ok: true, user: establishSession(cookies, data) };
}

// "Riprova": rigenera e rinvia l'OTP di registrazione. Fire-and-forget (200 comunque).
export async function resendRegistrationOtp(api: ApiClient, email: string): Promise<void> {
	await api.POST("/api/auth/resend-registration-otp", { body: { email } });
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
