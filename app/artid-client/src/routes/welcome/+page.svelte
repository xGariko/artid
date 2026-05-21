<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import logoExtended from '$lib/assets/logo_extended.svg';
	import welcomeIllustration from '$lib/assets/welcome_illustration.svg';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
</script>

<div class="row mw-100 h-100">
	<div class="col-5 d-flex flex-column align-items-center justify-content-center">
		<img
			src="{logoExtended}"
			alt="Welcome Illustration"
			class="mh-100 mw-100 object-fit-contain"
		>
		<div class="d-flex flex-column gap-3 mt-5 w-50">
			<h2 class="mt-5 mb-2 fw-bold text-center">Benvenuto</h2>
			<ArtidButton
				label="Accedi"
				onclick={() => goto(resolve('/login'))}
			/>
			<ArtidButton
				label="Registrati"
				outline={true}
				onclick={() => goto(resolve('/register'))}
			/>

			<div class="d-flex justify-content-center align-items-center my-3">
				<hr class="w-25 position-absolute">
				<span class="p-2 bg-artid-section z-2">Oppure</span>
			</div>

			<ArtidButton
				label="Esplora"
				btnStyle="secondary"
			/>

		</div>
	</div>
	<div class="col-7 d-none d-lg-flex flex-column align-items-end justify-content-end px-5 bottom-0">
		<img
			src="{welcomeIllustration}"
			alt="Welcome Illustration"
			class="mh-100 mw-100 object-fit-contain"
		>
	</div>
</div>