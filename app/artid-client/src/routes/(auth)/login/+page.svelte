<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import spidIcon from '$lib/assets/spid-ico-circle.svg';

	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { loading } from '$lib/stores/spinner-loading';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

	let credentials = $state({
		email: form?.email ?? '',
		password: ''
	});
</script>

<h2 class="fw-bold text-center mb-4">Accedi</h2>

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
	Non hai un account? <a href={resolve('/register')} class="auth-link">Registrati</a>
</p>


<style>
    .spid-icon {
        width: 2rem;
        padding-right: 0.5rem;
        border-right: 1px solid #127AE2;
    }
</style>
