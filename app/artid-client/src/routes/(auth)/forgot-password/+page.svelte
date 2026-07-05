<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidOtpConfirm from '$lib/components/ui/artid-otp-confirm.svelte';
	import { loading } from '$lib/stores/loading';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

	const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	// Passo di conferma prima dell'invio OTP: "Conferma" non invia più subito il codice ma mostra la
	// conferma; l'OTP parte solo all'"Ok".
	let confirmPhase = $state(false);
	let clientError = $state('');

	// "Conferma": validazione del formato email lato client (messaggi RAD) per avere un'email da
	// mostrare nella conferma. Il controllo "esiste un account" resta server-side e scatta all'Ok.
	function goToConfirm() {
		clientError = '';
		if (!email.trim()) {
			clientError = 'Errore: Bisogna compilare il campo dell\'email!';
			return;
		}
		if (!EMAIL_REGEX.test(email.trim())) {
			clientError = 'Errore: L\'email non è nel formato corretto! Usa nome@dominio.ext.';
			return;
		}
		confirmPhase = true;
	}

	// Invio OTP (all'Ok): overlay durante la submit; su errore torna alla fase email col messaggio.
	const onRequestReset = () => {
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

	// Dopo la validazione dell'email il server risponde con step "otp": si passa all'inserimento del
	// codice. Un reload riporta alla fase email (form = null).
	let otpPhase = $derived(form?.step === 'otp');

	let email = $state(form?.email ?? '');
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
		<h2 class="fw-bold text-center mb-2">Recupero password</h2>
		<p class="text-center text-secondary mb-4">
			Inserisci l'email del tuo account: ti invieremo un codice per reimpostare la password.
		</p>
	{/if}

	<form method="POST" action="?/requestReset" class="auth-form" use:enhance={onRequestReset}>
		<!-- Fase email: il campo resta nel DOM (nascosto con d-none) durante la conferma, così l'"Ok"
			lo invia insieme alla richiesta. -->
		<div class:d-none={confirmPhase}>
			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput
						type="email"
						name="email"
						label="Email"
						bind:value={email}
						error={form?.emailError}
					/>
				</div>
			</div>

			{#if clientError}
				<div class="text-danger small text-center mt-2">{clientError}</div>
			{/if}
			{#if form?.formError}
				<div class="text-danger small text-center mt-2">{form.formError}</div>
			{/if}

			<div class="row p-1 mt-2">
				<ArtidButton label="Conferma" type="button" onclick={goToConfirm} />
			</div>
		</div>

		{#if confirmPhase}
			<ArtidOtpConfirm email={email.trim()} onback={() => (confirmPhase = false)} />
		{/if}
	</form>

	{#if !confirmPhase}
		<p class="text-center mt-4 mb-0">
			Ti sei ricordato la password? <a href={resolve('/login')} class="auth-link">Accedi</a>
		</p>
	{/if}
{/if}
