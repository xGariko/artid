<script lang="ts">
	import ArtidButton from '$lib/components/ui/artid-button.svelte';

	// Passo di conferma mostrato PRIMA dell'invio effettivo dell'OTP (uniforme su login,
	// registrazione e recupero password). Il pulsante "Ok" è un submit: fa partire il form che lo
	// contiene, ovvero l'azione che invia davvero il codice. `onback` torna alla schermata precedente.
	let { email, onback }: { email: string; onback?: () => void } = $props();
</script>

<h2 class="fw-bold text-center mb-2">Invio del codice</h2>
<p class="text-center text-secondary mb-4">
	Verrà inviato un codice a 6 cifre all'indirizzo<br />
	<strong>{email}</strong>
</p>

<div class="row p-1">
	<ArtidButton label="Ok" type="submit" />
</div>

{#if onback}
	<p class="text-center mt-3 mb-0">
		<button type="button" class="btn btn-link p-0 align-baseline auth-link" onclick={onback}>
			Indietro
		</button>
	</p>
{/if}
