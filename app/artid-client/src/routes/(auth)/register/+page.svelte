<script lang="ts">
	import { enhance } from '$app/forms';
	import type { SubmitFunction } from '@sveltejs/kit';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';
	import ArtidSpidButton from '$lib/components/ui/artid-spid-button.svelte';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidOtpConfirm from '$lib/components/ui/artid-otp-confirm.svelte';
	import { RegisterRequestSchema, type RegisterRequest } from '$lib/models/schemas';
	import { toDateInputValue } from '$lib/utilities';
	import { loading } from '$lib/stores/loading.ts';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

	type RegisterFieldError =
		| 'name'
		| 'surname'
		| 'email'
		| 'password'
		| 'confirmPassword'
		| 'birthdate'
		| 'birthplace';

	// Messaggi per-campo, allineati a quelli di +page.server.ts: la validazione client mostra gli
	// stessi testi dei controlli server-side.
	const FIELD_MESSAGES: Record<RegisterFieldError, string> = {
		name: 'Il nome è obbligatorio.',
		surname: 'Il cognome è obbligatorio.',
		email: 'Inserisci un indirizzo email valido.',
		password: 'La password deve contenere almeno 8 caratteri.',
		confirmPassword: 'Le password non coincidono.',
		birthdate: 'Inserisci una data di nascita valida e antecedente a oggi.',
		birthplace: 'Luogo di nascita non valido.'
	};

	// Passo di conferma prima dell'invio OTP: "Registrati" valida i dati (client + server via
	// ?/validate) SENZA inviare il codice; l'OTP (e la creazione dell'account) partono solo all'"Ok".
	let confirmPhase = $state(false);
	// Errori di validazione client, mostrati sotto i campi (stessi controlli del server).
	let clientErrors = $state<Partial<Record<RegisterFieldError, string>>>({});

	// Validazione client (stessa di ?/validate: Zod + coincidenza password): blocca il round-trip su
	// dati palesemente invalidi. L'unicità dell'email resta server-side (?/validate). Ritorna true
	// se i dati passano.
	function validateRegistrationClient(): boolean {
		const errors: Partial<Record<RegisterFieldError, string>> = {};
		const parsed = RegisterRequestSchema.safeParse({
			name: userDTO.name.trim(),
			surname: userDTO.surname.trim(),
			email: userDTO.email.trim(),
			password: userDTO.password,
			birthdate: userDTO.birthdate || undefined,
			birthplace: userDTO.birthplace || undefined
		});
		if (!parsed.success) {
			for (const issue of parsed.error.issues) {
				const field = issue.path[0] as RegisterFieldError | undefined;
				if (field && !errors[field]) errors[field] = FIELD_MESSAGES[field] ?? issue.message;
			}
		}
		if (userDTO.password && userDTO.password !== confirmPassword) {
			errors.confirmPassword = FIELD_MESSAGES.confirmPassword;
		}
		clientErrors = errors;
		return Object.keys(errors).length === 0;
	}

	// Submit unificato del form dati. "Registrati" (?/validate) verifica dati ed email lato server e,
	// se validi, apre la conferma; "Ok" (?/requestOtp) invia davvero l'OTP. Overlay durante la submit;
	// reset:false preserva i campi (nascosti con d-none) così l'"Ok" li reinvia.
	const onSubmit: SubmitFunction = ({ action, cancel }) => {
		if (action.search === '?/validate' && !validateRegistrationClient()) {
			cancel();
			return;
		}
		$loading = true;
		return async ({ result, update }) => {
			$loading = false;
			await update({ reset: false });
			if (action.search === '?/validate') {
				if (result.type === 'success') confirmPhase = true;
			} else if (result.type === 'failure') {
				confirmPhase = false;
			}
		};
	};

	// Dopo la validazione dei dati il server risponde con step "otp": si passa alla schermata di
	// inserimento del codice inviato via email. L'account viene creato solo dopo la verifica.
	// Un reload riporta alla fase dati (form = null).
	let otpPhase = $derived(form?.step === 'otp');

	let userDTO: RegisterRequest = $state({
		name: form?.name ?? '',
		surname: form?.surname ?? '',
		email: form?.email ?? '',
		password: '',
		birthdate: form?.birthdate || undefined,
		birthplace: form?.birthplace || undefined
	});

	let confirmPassword = $state('');
	let code = $state('');

	// Limite nativo del date picker: ieri, così la selezione rispecchia la regola "antecedente a oggi"
	// (la validazione Zod resta la fonte di verità, lato client e server).
	const maxBirthdate = (() => {
		const yesterday = new Date();
		yesterday.setDate(yesterday.getDate() - 1);
		return toDateInputValue(yesterday);
	})();

	let passwordMismatch = $derived(
		confirmPassword.length > 0 && userDTO.password !== confirmPassword
	);

	let confirmPasswordError = $derived(
		passwordMismatch
			? 'Le password non coincidono'
			: (clientErrors.confirmPassword ?? form?.errors?.confirmPassword ?? undefined)
	);

	// Form di verifica: lo inviamo via JS appena il codice è completo, senza pulsante (come nel login).
	let verifyForm: HTMLFormElement | undefined = $state();

	// Forza il remount dell'input OTP: dopo un tentativo fallito svuota le caselle e riporta il focus
	// sulla prima, così l'utente reinserisce il codice da capo.
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

	// Pattern condiviso col login: attiva l'overlay di caricamento durante la submit.
	const withLoading = () => {
		$loading = true;
		return async ({ update }: { update: () => Promise<void> }) => {
			$loading = false;
			await update();
		};
	};
</script>

{#if otpPhase}
	<h2 class="fw-bold text-center mb-2">Verifica la tua email</h2>
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
		<a href={resolve('/register')} class="auth-link">Usa un altro indirizzo</a>
	</p>
{:else}
	{#if !confirmPhase}
		<h2 class="fw-bold text-center mb-4">Registrati</h2>
	{/if}

	<form method="POST" action="?/requestOtp" class="auth-form" use:enhance={onSubmit}>
		<!-- Fase dati: i campi restano nel DOM (nascosti con d-none) durante la conferma, così l'"Ok"
			li invia insieme alla richiesta OTP. -->
		<div class:d-none={confirmPhase}>
			<div class="row">
				<div class="col-12 col-md-6 p-1">
					<ArtidInput
						name="name"
						label="Nome"
						bind:value={userDTO.name}
						error={clientErrors.name ?? form?.errors?.name}
					/>
				</div>
				<div class="col-12 col-md-6 p-1">
					<ArtidInput
						name="surname"
						label="Cognome"
						bind:value={userDTO.surname}
						error={clientErrors.surname ?? form?.errors?.surname}
					/>
				</div>
			</div>
			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput
						type="email"
						name="email"
						label="Email"
						bind:value={userDTO.email}
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
						bind:value={userDTO.password}
						error={clientErrors.password ?? form?.errors?.password}
					/>
				</div>
			</div>
			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput
						type="password"
						name="confirmPassword"
						label="Ripeti password"
						bind:value={confirmPassword}
						error={confirmPasswordError}
					/>
				</div>
			</div>
			<div class="row">
				<div class="col-12 col-md-6 p-1">
					<ArtidInput
						type="date"
						name="birthdate"
						label="Data di nascita"
						max={maxBirthdate}
						bind:value={userDTO.birthdate}
						error={clientErrors.birthdate ?? form?.errors?.birthdate}
					/>
				</div>
				<div class="col-12 col-md-6 p-1">
					<ArtidInput
						name="birthplace"
						label="Luogo di nascita"
						bind:value={userDTO.birthplace}
						error={clientErrors.birthplace ?? form?.errors?.birthplace}
					/>
				</div>
			</div>
			{#if form?.formError}
				<div class="text-danger small text-center mt-2">{form.formError}</div>
			{/if}

			<div class="row p-1 mt-2">
				<ArtidButton label="Registrati" type="submit" formaction="?/validate" />
			</div>
		</div>

		{#if confirmPhase}
			<ArtidOtpConfirm email={userDTO.email} onback={() => (confirmPhase = false)} />
		{/if}
	</form>

	{#if !confirmPhase}
		<div class="d-flex justify-content-center align-items-center my-3">
			<hr class="w-25 position-absolute" />
			<span class="p-2 bg-artid-light z-2">Oppure</span>
		</div>

		<ArtidSpidButton label="Entra con SPID" />

		<p class="text-center mt-4 mb-0">
			Hai già un account? <a href={resolve('/login')} class="auth-link">Accedi</a>
		</p>
	{/if}
{/if}
