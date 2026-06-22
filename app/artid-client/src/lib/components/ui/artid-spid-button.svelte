<script lang="ts">
	import { resolve } from '$app/paths';
	import spidIcon from '$lib/assets/spid-ico-circle.svg';

	// Senza onclick è un link alla schermata SPID (/login, /register). Con onclick diventa un
	// bottone d'azione (es. apertura modale "Collega SPID" in Gestione Profilo).
	let {
		label,
		onclick,
		disabled = false,
		fullWidth = true
	}: { label: string; onclick?: () => void; disabled?: boolean; fullWidth?: boolean } = $props();
</script>

{#snippet inner()}
	<span class="spid-btn__mark"><img src={spidIcon} alt="" /></span>
	<span class="spid-btn__label">{label}</span>
{/snippet}

{#if onclick}
	<button class="spid-btn" class:spid-btn--block={fullWidth} type="button" {onclick} {disabled}>
		{@render inner()}
	</button>
{:else}
	<a class="spid-btn" class:spid-btn--block={fullWidth} href={resolve('/spid')}>
		{@render inner()}
	</a>
{/if}

<style>
	/* Bottone ufficiale "Entra con SPID" rivisitato: campo blu SPID (= brand ArtID #06c) con
	   lockup [marchio | etichetta] separato da un filo. Profondità sobria, stati completi. */
	.spid-btn {
		display: inline-flex;
		align-items: stretch;
		width: auto;
		border: none;
		border-radius: 0.6rem;
		background: var(--artid-primary);
		color: #fff;
		font-weight: 600;
		letter-spacing: 0.01em;
		text-decoration: none;
		cursor: pointer;
		overflow: hidden;
		transition: background-color 0.16s ease;
	}

	.spid-btn--block {
		display: flex;
		width: 100%;
	}

	.spid-btn__mark {
		display: flex;
		align-items: center;
		padding: 0.6rem 0.7rem;
		border-right: 1px solid rgba(255, 255, 255, 0.32);
	}
	.spid-btn__mark img {
		width: 1.5rem;
		height: 1.5rem;
		display: block;
	}

	.spid-btn__label {
		display: flex;
		align-items: center;
		justify-content: center;
		flex: 1 1 auto;
		padding: 0.6rem 1rem;
	}

	.spid-btn:hover {
		background: color-mix(in oklab, var(--artid-primary), black 8%);
	}
	.spid-btn:active {
		background: color-mix(in oklab, var(--artid-primary), black 15%);
	}
	.spid-btn:focus-visible {
		outline: 3px solid color-mix(in oklab, var(--artid-primary), white 45%);
		outline-offset: 2px;
	}

	/* Stato "collegato" (profilo): calmo e non interattivo. */
	.spid-btn:disabled {
		cursor: not-allowed;
		filter: grayscale(0.3);
		opacity: 0.55;
	}
</style>
