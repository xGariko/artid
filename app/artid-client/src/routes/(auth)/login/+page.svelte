<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidOtpConfirm from '$lib/components/ui/artid-otp-confirm.svelte';
	import { loading } from '$lib/stores/loading';
	import type { ActionData } from './$types';
	import ArtidSpidButton from '$lib/components/ui/artid-spid-button.svelte';

	let { form }: { form: ActionData } = $props();

	// Messaggio di ritorno dal recupero password (redirect a /login?msg=...), mostrato solo nella fase
	// credenziali. reset-success / account-not-found → testi verbatim dal RAD (caso d'uso DIM PASS).
	let flashMessage = $derived.by(() => {
		const msg = page.url.searchParams.get('msg');
		if (msg === 'reset-success') {
			return {
				type: 'success',
				text: 'Password resettata con successo. Ti arriverà una email con la tua password temporanea.'
			};
		}
		if (msg === 'account-not-found') {
			return { type: 'danger', text: 'Non esiste un account con questa email.' };
		}
		return null;
	});

	// Dopo la validazione delle credenziali il server risponde con step "otp": si passa alla
	// schermata di inserimento del codice. Un reload riporta alla fase credenziali (form = null).
	let otpPhase = $derived(form?.step === 'otp');

	let credentials = $state({
		email: form?.email ?? '',
		password: ''
	});

	// Regex email allineata al controllo server-side (?/requestOtp).
	const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	// Passo di conferma prima dell'invio OTP: "Accedi" non invia più subito il codice ma mostra la
	// conferma; l'OTP parte solo all'"Ok" (che submitta il form verso ?/requestOtp).
	let confirmPhase = $state(false);
	// Errori di validazione client, mostrati sotto i campi (stessi controlli del server).
	let clientErrors = $state<{ email?: string; password?: string }>({});

	// "Accedi": valida i campi PRIMA della conferma (stessi controlli di ?/requestOtp), così l'"Ok"
	// appare solo con input valido. La verifica delle credenziali resta server-side e scatta all'Ok.
	function goToConfirm() {
		const errors: { email?: string; password?: string } = {};
		const email = credentials.email.trim();
		if (!email) {
			errors.email = "L'email è obbligatoria.";
		} else if (!EMAIL_REGEX.test(email)) {
			errors.email = 'Inserisci un indirizzo email valido.';
		}
		if (!credentials.password) {
			errors.password = 'La password è obbligatoria.';
		}
		clientErrors = errors;
		if (Object.keys(errors).length > 0) return;
		confirmPhase = true;
	}

	// Invio OTP (all'Ok): overlay durante la submit; su errore (credenziali errate) torna alla fase
	// credenziali mostrando il messaggio del server.
	const onRequestOtp = () => {
		$loading = true;
		return async ({
			result,
			update
		}: {
			result: { type: string };
			update: () => Promise<void>;
		}) => {
			$loading = false;
			await update();
			if (result.type === 'failure') confirmPhase = false;
		};
	};

	let code = $state('');

	// Form di verifica: lo inviamo via JS appena il codice è completo, senza pulsante.
	let verifyForm: HTMLFormElement | undefined = $state();

	// Forza il remount dell'input OTP: dopo un tentativo fallito svuota le caselle e riporta il
	// focus sulla prima, così l'utente reinserisce il codice da capo.
	let otpResetKey = $state(0);

	// Evita verifiche concorrenti: una sola submit in volo per volta.
	let verifying = false;

	// Cifre complete: invia il form di verifica (auto-submit senza pulsante).
	function onOtpComplete() {
		if (verifying) return;
		verifying = true;
		verifyForm?.requestSubmit();
	}

	// Verifica OTP: overlay durante la submit; se il codice è errato svuota le caselle e aspetta che
	// l'utente lo reinserisca — evita il re-invio in loop dello stesso codice già fallito.
	const onVerify = () => {
		$loading = true;
		return async ({
			result,
			update
		}: {
			result: { type: string };
			update: () => Promise<void>;
		}) => {
			$loading = false;
			await update();
			verifying = false;
			if (result.type === 'failure') {
				code = '';
				otpResetKey++;
			}
		};
	};

	// Pattern condiviso: attiva l'overlay di caricamento durante la submit.
	const withLoading = () => {
		$loading = true;
		return async ({ update }: { update: () => Promise<void> }) => {
			$loading = false;
			await update();
		};
	};
</script>

{#if otpPhase}
	<h2 class="fw-bold text-center mb-2">Verifica OTP</h2>
	<p class="text-center text-secondary mb-4">
		Ti abbiamo inviato un codice a 6 cifre all'indirizzo<br />
		<strong>{form?.email}</strong>
	</p>

	<form
		method="POST"
		action="?/verify"
		class="auth-form"
		use:enhance={onVerify}
		bind:this={verifyForm}
	>
		<input type="hidden" name="email" value={form?.email ?? ''} />

		<div class="p-1 mt-1">
			{#key otpResetKey}
				<ArtidOtpInput
					name="code"
					bind:value={code}
					error={form?.codeError}
					oncomplete={onOtpComplete}
					autofocus
				/>
			{/key}
		</div>

		{#if form?.codeError}
			<div class="text-danger small text-center mt-2">{form.codeError}</div>
		{/if}
		{#if form?.resent}
			<div class="text-success small text-center mt-2">Ti abbiamo inviato un nuovo codice.</div>
		{/if}
		{#if form?.formError}
			<div class="text-danger small text-center mt-2">{form.formError}</div>
		{/if}
	</form>

	<!-- Rinvio in un form separato: il form di verifica non ha pulsanti di submit, così l'invio
		automatico via requestSubmit() usa sempre l'azione ?/verify senza ambiguità. -->
	<form method="POST" action="?/resend" class="text-center mt-3" use:enhance={withLoading}>
		<input type="hidden" name="email" value={form?.email ?? ''} />
		<p class="mb-0">
			Non hai ricevuto il codice?
			<button type="submit" class="btn btn-link p-0 align-baseline auth-link">Invia di nuovo</button>
		</p>
	</form>

	<p class="text-center mt-2 mb-0">
		<a href={resolve('/login')} class="auth-link">Usa un altro account</a>
	</p>
{:else}
	{#if !confirmPhase}
		<h2 class="fw-bold text-center mb-4">Accedi</h2>

		{#if flashMessage}
			<div class="alert alert-{flashMessage.type} text-center py-2" role="alert">
				{flashMessage.text}
			</div>
		{/if}
	{/if}

	<form method="POST" action="?/requestOtp" class="auth-form" use:enhance={onRequestOtp}>
		<!-- Fase credenziali: i campi restano nel DOM (nascosti con d-none) anche durante la conferma,
			così l'"Ok" li invia insieme alla richiesta OTP. -->
		<div class:d-none={confirmPhase}>
			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput
						type="email"
						name="email"
						label="Email"
						bind:value={credentials.email}
						error={clientErrors.email ?? form?.errors?.email}
					/>
				</div>
			</div>
			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput
						type="password"
						name="password"
						label="Password"
						bind:value={credentials.password}
						error={clientErrors.password ?? form?.errors?.password}
					/>
				</div>
			</div>

			<div class="d-flex justify-content-end px-1">
				<a href={resolve('/forgot-password')} class="auth-link small">Password dimenticata?</a>
			</div>

			{#if form?.formError}
				<div class="text-danger small text-center mt-2">{form.formError}</div>
			{/if}

			<div class="row p-1 mt-2">
				<ArtidButton label="Accedi" type="button" onclick={goToConfirm} />
			</div>
		</div>

		{#if confirmPhase}
			<ArtidOtpConfirm email={credentials.email} onback={() => (confirmPhase = false)} />
		{/if}
	</form>

	{#if !confirmPhase}
		<div class="d-flex justify-content-center align-items-center my-3">
			<hr class="w-25 position-absolute" />
			<span class="p-2 bg-artid-light z-2">Oppure</span>
		</div>

		<ArtidSpidButton label="Entra con SPID" />

		<p class="text-center mt-4 mb-0">
			Non hai un account? <a href={resolve('/register')} class="auth-link">Registrati</a>
		</p>
	{/if}
{/if}
