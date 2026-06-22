import { fail, redirect } from '@sveltejs/kit';
import type { Actions } from './$types';
import { spidLogin, spidVerifyOtp, resendOtp } from '$lib/auth.ts';

type SpidField = 'username' | 'password';
type FieldErrors = Partial<Record<SpidField, string>>;

// Forma unica di ritorno delle action (come login): la pagina legge i campi senza narrowing.
// `step: "otp"` indica il ramo "email già registrata" in attesa del codice.
type SpidActionData = {
	step?: 'otp';
	email?: string;
	username?: string;
	errors?: FieldErrors;
	formError?: string;
	codeError?: string;
	resent?: boolean;
};

const CODE_REGEX = /^\d{6}$/;

const SESSION_LOST = "Sessione scaduta, ricomincia l'accesso con SPID.";
// Stesso messaggio del login OTP (RAD, caso d'uso GENERA OTP).
const INVALID_CODE =
	'Errore: codice non valido, controlla nella mail che non sia scaduto. Se è scaduto clicca Riprova.';

export const actions: Actions = {
	// Step 1: credenziali della schermata del provider (mock). Apre la sessione (utente trovato o
	// creato) oppure entra in fase OTP se l'email è già di un account esistente senza spidCode.
	authenticate: async ({ request, cookies, locals }) => {
		const form = await request.formData();
		const providerId = (form.get('providerId') as string)?.trim() ?? '';
		const username = (form.get('username') as string)?.trim() ?? '';
		const password = (form.get('password') as string) ?? '';

		const errors: FieldErrors = {};
		if (!username) {
			errors.username = 'Il codice fiscale è obbligatorio.';
		}
		if (!password) {
			errors.password = 'La password è obbligatoria.';
		}
		if (Object.keys(errors).length > 0) {
			return fail(400, { errors, username } as SpidActionData);
		}

		const result = await spidLogin(locals.api, cookies, { providerId, username, password });
		if (result.status === 'authenticated') {
			redirect(303, '/dashboard');
		}
		if (result.status === 'otp') {
			return { step: 'otp', email: result.email, username } as SpidActionData;
		}
		return fail(401, { formError: result.error, username } as SpidActionData);
	},

	// Step 2 (ramo collisione email): verifica l'OTP e, se valido, apre la sessione.
	verifyOtp: async ({ request, cookies, locals }) => {
		const form = await request.formData();
		const username = (form.get('username') as string)?.trim() ?? '';
		const email = (form.get('email') as string)?.trim() ?? '';
		const code = (form.get('code') as string)?.trim() ?? '';

		if (!email || !username) {
			return fail(400, { formError: SESSION_LOST } as SpidActionData);
		}
		if (!CODE_REGEX.test(code)) {
			return fail(400, {
				step: 'otp',
				email,
				username,
				codeError: 'Inserisci il codice OTP a 6 cifre.'
			} as SpidActionData);
		}

		const result = await spidVerifyOtp(locals.api, cookies, { username, email, code });
		if (!result.ok) {
			return fail(401, {
				step: 'otp',
				email,
				username,
				codeError: INVALID_CODE
			} as SpidActionData);
		}

		redirect(303, '/dashboard');
	},

	// "Riprova": rinvia l'OTP riusando la challenge di login dell'account esistente.
	resend: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get('email') as string)?.trim() ?? '';
		const username = (form.get('username') as string)?.trim() ?? '';

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as SpidActionData);
		}

		await resendOtp(locals.api, email);
		return { step: 'otp', email, username, resent: true } as SpidActionData;
	}
};
