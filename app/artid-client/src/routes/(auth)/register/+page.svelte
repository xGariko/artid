<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidSpidButton from '$lib/components/ui/artid-spid-button.svelte';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import type { RegisterRequest } from '$lib/models/schemas';
	import { loading } from '$lib/stores/loading.ts';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

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

	let passwordMismatch = $derived(
		confirmPassword.length > 0 && userDTO.password !== confirmPassword
	);

	let confirmPasswordError = $derived(
		passwordMismatch ? 'Le password non coincidono' : (form?.errors?.confirmPassword ?? undefined)
	);

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

	<form method="POST" action="?/verify" class="auth-form" use:enhance={withLoading}>
		<input type="hidden" name="email" value={form?.email ?? ''} />

		<div class="row">
			<div class="col-12 p-1">
				<ArtidInput
					name="code"
					label="Codice di verifica"
					bind:value={code}
					inputmode="numeric"
					maxlength={6}
					autocomplete="one-time-code"
					error={form?.codeError}
				/>
			</div>
		</div>

		{#if form?.resent}
			<div class="text-success small text-center mt-2">Ti abbiamo inviato un nuovo codice.</div>
		{/if}
		{#if form?.formError}
			<div class="text-danger small text-center mt-2">{form.formError}</div>
		{/if}

		<div class="row p-1 mt-2">
			<ArtidButton label="Verifica e crea account" type="submit" />
		</div>

		<p class="text-center mt-3 mb-0">
			Non hai ricevuto il codice?
			<button type="submit" formaction="?/resend" class="btn btn-link p-0 align-baseline auth-link">
				Invia di nuovo
			</button>
		</p>
	</form>

	<p class="text-center mt-2 mb-0">
		<a href={resolve('/register')} class="auth-link">Usa un altro indirizzo</a>
	</p>
{:else}
	<h2 class="fw-bold text-center mb-4">Registrati</h2>

	<form method="POST" action="?/requestOtp" class="auth-form" use:enhance={withLoading}>
		<div class="row">
			<div class="col-12 col-md-6 p-1">
				<ArtidInput name="name" label="Nome" bind:value={userDTO.name} error={form?.errors?.name} />
			</div>
			<div class="col-12 col-md-6 p-1">
				<ArtidInput
					name="surname"
					label="Cognome"
					bind:value={userDTO.surname}
					error={form?.errors?.surname}
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
					error={form?.errors?.email}
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
					error={form?.errors?.password}
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
					bind:value={userDTO.birthdate}
					error={form?.errors?.birthdate}
				/>
			</div>
			<div class="col-12 col-md-6 p-1">
				<ArtidInput
					name="birthplace"
					label="Luogo di nascita"
					bind:value={userDTO.birthplace}
					error={form?.errors?.birthplace}
				/>
			</div>
		</div>
		{#if form?.formError}
			<div class="text-danger small text-center mt-2">{form.formError}</div>
		{/if}

		<div class="row p-1 mt-2">
			<ArtidButton label="Registrati" type="submit" />
		</div>
	</form>

	<div class="d-flex justify-content-center align-items-center my-3">
		<hr class="w-25 position-absolute" />
		<span class="p-2 bg-artid-light z-2">Oppure</span>
	</div>

	<ArtidSpidButton label="Entra con SPID" />

	<p class="text-center mt-4 mb-0">
		Hai già un account? <a href={resolve('/login')} class="auth-link">Accedi</a>
	</p>
{/if}
