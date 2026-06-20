<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { loading } from '$lib/stores/loading';
	import type { ActionData } from './$types';
	import ArtidSpidButton from '$lib/components/ui/artid-spid-button.svelte';

	let { form }: { form: ActionData } = $props();

	// Dopo la validazione delle credenziali il server risponde con step "otp": si passa alla
	// schermata di inserimento del codice. Un reload riporta alla fase credenziali (form = null).
	let otpPhase = $derived(form?.step === 'otp');

	let credentials = $state({
		email: form?.email ?? '',
		password: ''
	});

	let code = $state('');

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

	<form method="POST" action="?/verify" class="auth-form" use:enhance={withLoading}>
		<input type="hidden" name="email" value={form?.email ?? ''} />

		<div class="p-1 mt-1">
			<ArtidOtpInput name="code" bind:value={code} error={form?.codeError} autofocus />
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

		<div class="row p-1 mt-2">
			<ArtidButton label="Verifica e accedi" type="submit" />
		</div>

		<p class="text-center mt-3 mb-0">
			Non hai ricevuto il codice?
			<button type="submit" formaction="?/resend" class="btn btn-link p-0 align-baseline auth-link">
				Invia di nuovo
			</button>
		</p>
	</form>

	<p class="text-center mt-2 mb-0">
		<a href={resolve('/login')} class="auth-link">Usa un altro account</a>
	</p>
{:else}
	<h2 class="fw-bold text-center mb-4">Accedi</h2>

	<form method="POST" action="?/requestOtp" class="auth-form" use:enhance={withLoading}>
		<div class="row">
			<div class="col-12 p-1">
				<ArtidInput
					type="email"
					name="email"
					label="Email"
					bind:value={credentials.email}
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
					bind:value={credentials.password}
					error={form?.errors?.password}
				/>
			</div>
		</div>

		{#if form?.formError}
			<div class="text-danger small text-center mt-2">{form.formError}</div>
		{/if}

		<div class="row p-1 mt-2">
			<ArtidButton label="Accedi" type="submit" />
		</div>
	</form>

	<div class="d-flex justify-content-center align-items-center my-3">
		<hr class="w-25 position-absolute" />
		<span class="p-2 bg-artid-light z-2">Oppure</span>
	</div>

	<ArtidSpidButton label="Entra con SPID" />

	<p class="text-center mt-4 mb-0">
		Non hai un account? <a href={resolve('/register')} class="auth-link">Registrati</a>
	</p>
{/if}
