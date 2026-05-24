<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import spidIcon from '$lib/assets/spid-ico-circle.svg';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import type { RegisterRequest } from '$lib/models/schemas';
	import { loading } from '$lib/stores/loading.ts';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

	let userDTO: RegisterRequest = $state({
		name: form?.name ?? '',
		surname: form?.surname ?? '',
		email: form?.email ?? '',
		password: '',
		birthdate: form?.birthdate || undefined,
		birthplace: form?.birthplace || undefined,
		taxId: form?.taxId ?? ''
	});

	let confirmPassword = $state('');

	let passwordMismatch = $derived(
		confirmPassword.length > 0 && userDTO.password !== confirmPassword
	);

	let confirmPasswordError = $derived(
		passwordMismatch
			? 'Le password non coincidono'
			: (form?.errors?.confirmPassword ?? undefined)
	);
</script>

<h2 class="fw-bold text-center mb-4">Registrati</h2>

<form
	method="POST"
	class="auth-form"
	use:enhance={() => {
		$loading = true;
		return async ({ update }) => {
			$loading = false;
			await update();
		};
	}}
>
	<div class="row">
		<div class="col-12 col-md-6 p-1">
			<ArtidInput
				name="name"
				label="Nome"
				bind:value={userDTO.name}
				error={form?.errors?.name}
			/>
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
	<div class="row">
		<div class="col-12 p-1">
			<ArtidInput
				type="text"
				name="taxId"
				label="Codice fiscale"
				bind:value={userDTO.taxId}
				error={form?.errors?.taxId}
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
	<hr class="w-25 position-absolute">
	<span class="p-2 bg-artid-light z-2">Oppure</span>
</div>

<!-- SPID BUTTON -->
<div class="d-flex justify-content-center">
	<button class="btn btn-primary rounded-0 d-flex w-auto align-items-center justify-content-center gap-2 py-2">
		<img src="{spidIcon}" class="spid-icon" alt="SPID">
		<span class="border-left">Entra con spid</span>
	</button>
</div>

<p class="text-center mt-4 mb-0">
	Hai già un account? <a href={resolve('/login')} class="auth-link">Accedi</a>
</p>


<style>
    .spid-icon {
        width: 2rem;
        padding-right: 0.5rem;
        border-right: 1px solid #127AE2;
    }
</style>
