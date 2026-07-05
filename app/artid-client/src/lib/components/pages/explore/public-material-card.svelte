<script lang="ts">
	import type { PublicMaterial } from '$lib/api/types';
	import { resourceTypeFromMime, iconForResourceType } from '$lib/utilities';
	import { sanitizeHtml } from '$lib/sanitize';

	let { material }: { material: PublicMaterial } = $props();

	const kind = $derived(resourceTypeFromMime(material.mimeType));
	const icon = $derived(iconForResourceType(kind));

	// --- Audio: player con waveform. preload="none" → i byte si scaricano solo al primo play. ---
	let audioEl: HTMLAudioElement | undefined = $state();
	let paused = $state(true);
	let currentTime = $state(0);
	let duration = $state(0);

	const progress = $derived(duration > 0 ? currentTime / duration : 0);

	// Waveform deterministica dall'id (niente dato reale di ampiezza): altezze stabili tra i render.
	const WAVEFORM_BARS = 36;
	const bars = $derived.by(() => {
		let seed = (material.id ?? 1) * 97 + 13;
		const heights: number[] = [];
		for (let i = 0; i < WAVEFORM_BARS; i++) {
			seed = (seed * 1103515245 + 12345) & 0x7fffffff;
			heights.push(0.25 + ((seed % 1000) / 1000) * 0.75);
		}
		return heights;
	});

	function toggleAudio() {
		if (!audioEl) return;
		if (audioEl.paused) audioEl.play().catch(() => {});
		else audioEl.pause();
	}

	// --- Video: poster finché non si clicca; al click monta il player (download + autoplay). ---
	let videoLoaded = $state(false);
</script>

<div class="border border-artid-border rounded-3 bg-artid-section p-3 h-100 d-flex flex-column gap-2">
	<div class="d-flex align-items-center gap-2 text-artid-text">
		<i class="bi bi-{icon} text-primary"></i>
		<span class="fw-semibold text-truncate flex-grow-1" title={material.title}>{material.title}</span>
		{#if material.url && kind === 'audio'}
			<button
				type="button"
				class="material-card__play rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
				onclick={toggleAudio}
				aria-label={paused ? `Riproduci ${material.title}` : `Metti in pausa ${material.title}`}
			>
				<i class="bi {paused ? 'bi-play-fill' : 'bi-pause-fill'}"></i>
			</button>
		{/if}
	</div>

	{#if !material.url}
		<div class="text-artid-text-muted small">File non disponibile</div>
	{:else if kind === 'audio'}
		<div class="material-card__waveform d-flex align-items-center" aria-hidden="true">
			{#each bars as height, i (i)}
				<span
					class="material-card__bar"
					class:is-played={i / bars.length <= progress}
					style:height="{Math.round(height * 100)}%"
				></span>
			{/each}
		</div>
		<audio
			bind:this={audioEl}
			bind:paused
			bind:currentTime
			bind:duration
			preload="none"
			src={material.url}
		></audio>
		{#if material.description}
			<div class="text-artid-text-muted small mb-0 mt-auto pt-1 material-card__description">
			{@html sanitizeHtml(material.description)}
		</div>
		{/if}
	{:else if kind === 'video'}
		{#if videoLoaded}
			<!-- svelte-ignore a11y_media_has_caption -->
			<video
				class="w-100 rounded-2 material-card__video"
				controls
				autoplay
				preload="metadata"
				src={material.url}
			></video>
			{#if material.description}
				<div class="text-artid-text-muted small mb-0 pt-1 material-card__description">
					{@html sanitizeHtml(material.description)}
				</div>
			{/if}
		{:else}
			<button
				type="button"
				class="material-card__poster bg-artid-dark rounded-2 w-100 border-0 d-flex align-items-center justify-content-center"
				onclick={() => (videoLoaded = true)}
				aria-label={`Riproduci ${material.title}`}
			>
				<i class="bi bi-play-circle-fill"></i>
			</button>
			<button
				type="button"
				class="btn btn-primary w-100 rounded-pill mt-2 d-flex align-items-center justify-content-center"
				onclick={() => (videoLoaded = true)}
				aria-label={`Riproduci ${material.title}`}
			>
				<i class="bi bi-play-fill"></i>
			</button>
			{#if material.description}
				<div class="text-artid-text-muted small mb-0 pt-1 material-card__description">
					{@html sanitizeHtml(material.description)}
				</div>
			{/if}
		{/if}
	{:else if kind === 'image'}
		<img
			class="w-100 rounded-2 material-card__image"
			src={material.url}
			alt={material.title}
			loading="lazy"
		/>
		{#if material.description}
			<div class="text-artid-text-muted small mb-0 mt-auto pt-1 material-card__description">
			{@html sanitizeHtml(material.description)}
		</div>
		{/if}
	{:else}
		<a
			href={material.url}
			target="_blank"
			rel="noopener"
			class="btn btn-outline-primary btn-sm align-self-start d-flex align-items-center gap-2"
		>
			<i class="bi bi-box-arrow-up-right"></i>
			Apri il file
		</a>
		{#if material.description}
			<div class="text-artid-text-muted small mb-0 mt-auto pt-1 material-card__description">
			{@html sanitizeHtml(material.description)}
		</div>
		{/if}
	{/if}
</div>

<style>
	/* Pulsante play/pausa audio: cerchio brand, in alto a destra come nel mockup. */
	.material-card__play {
		width: 2.25rem;
		height: 2.25rem;
		border: none;
		background-color: var(--artid-primary);
		color: #fff;
		transition: filter 0.15s ease;
	}

	.material-card__play:hover {
		filter: brightness(1.06);
	}

	/* Waveform: barre verticali centrate; quelle "suonate" si tingono di brand col progresso. */
	.material-card__waveform {
		height: 2.75rem;
		gap: 3px;
	}

	.material-card__bar {
		flex: 1 1 0;
		min-width: 0;
		border-radius: 1rem;
		background-color: var(--artid-text-muted);
		opacity: 0.35;
		transition:
			background-color 0.1s ease,
			opacity 0.1s ease;
	}

	.material-card__bar.is-played {
		background-color: var(--artid-primary);
		opacity: 1;
	}

	/* Poster video: blocco neutro con play; nessun byte caricato finché non si clicca. */
	.material-card__poster {
		aspect-ratio: 16 / 9;
		max-height: 24rem;
		color: #fff;
		cursor: pointer;
	}

	.material-card__poster i {
		font-size: 3.25rem;
		opacity: 0.85;
		transition: opacity 0.15s ease;
	}

	.material-card__poster:hover i {
		opacity: 1;
	}

	.material-card__video {
		max-height: 24rem;
	}

	.material-card__image {
		max-height: 16rem;
		object-fit: cover;
	}

	/* La descrizione del materiale è rich-text (Quill): come per la descrizione dell'ArtID
	   azzeriamo i margini di primo/ultimo blocco così resta compatta (equivalente al vecchio mb-0). */
	.material-card__description :global(> :first-child) {
		margin-top: 0;
	}

	.material-card__description :global(> :last-child) {
		margin-bottom: 0;
	}
</style>
