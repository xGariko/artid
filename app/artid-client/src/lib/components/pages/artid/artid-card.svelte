<script lang="ts">
	import type { components } from '$lib/api/schema';
	import artidImage from '$lib/assets/artid_logo_outline_primary.svg';
	import { formatItalianDate, type ArtIdFilterType } from '$lib/utilities';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import { toast } from '$lib/toast';
	import { invalidateAll } from '$app/navigation';
	import type { TagResponse } from '$lib/api/types';

	let {
		artid,
		filter
	}: { artid: components['schemas']['ArtidResponse']; filter: ArtIdFilterType } = $props();

	$effect(() => {
		if (artid && filter !== 'sharedWithMe') {
			getArtidTags(artid.id!);
		}
	});

	let artidTags = $state<TagResponse[]>([]);

	async function getArtidTags(artidId: number) {
		const response = await api.GET('/api/artids/{id}/tags', {
			params: { path: { id: artidId } }
		});
		if (!response.error) {
			artidTags = response.data;
		}
	}

	// Stato locale ottimistico per la stella: il prop `artid` (dato di load) non è un proxy
	// reattivo. NIENTE $effect di riallineamento dal prop: dopo invalidateAll rimetterebbe il
	// valore del server annullando l'update ottimistico (era la causa del bug "la stella non si
	// riempie" quando il GET risponde prima che la scrittura sia visibile).
	// svelte-ignore state_referenced_locally
	let isFavourite = $state(artid.favourite ?? false);

	let favouriteLabel = $derived(isFavourite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti');
	let deleteLabel = 'Rimuovi condivisione';

	async function toggleFavourite(event: MouseEvent) {
		// il bottone è dentro l'<a> della card: evita che il click navighi ai dettagli
		event.preventDefault();
		event.stopPropagation();

		const newFavouriteState = !isFavourite;

		try {
			const response = await api.PUT('/api/artids/{id}/favourite', {
				params: { path: { id: artid.id! } },
				body: newFavouriteState
			});

			if (!response.error) {
				isFavourite = newFavouriteState;
				toast.success(
					newFavouriteState ? 'ArtID aggiunto ai preferiti' : 'ArtID rimosso dai preferiti'
				);

				await invalidateAll();
			} else {
				toast.error(
					newFavouriteState
						? 'Impossibile aggiungere ArtID ai preferiti'
						: 'Impossibile rimuovere ArtID dai preferiti'
				);
			}
		} catch {
			toast.error('Errore di rete');
		}
	}

	async function removeInternalShare(event: MouseEvent) {
		event.preventDefault();
		event.stopPropagation();

		try {
			const response = await api.PUT('/api/shares/internal/decline', {
				body: Number(artid.id)
			});

			if (!response.error) {
				toast.success("L'artid condiviso con te è stato eliminato.");

				await invalidateAll();
			} else {
				toast.error("Impossibile eliminare l'artid condiviso");
			}
		} catch {
			toast.error('Errore di rete');
		}
	}
</script>

<a
	href={resolve('/(app)/artid/details/[id]', { id: String(artid.id) })}
	class="rounded-3 border border-artid-border w-100 d-block text-decoration-none artid-card z-2"
>
	<div class="d-flex align-items-center card-image p-1">
		{#if artid.thumbnailUrl}
			<img src={artid.thumbnailUrl ?? artidImage} alt={`${artid.title} image`} />
		{:else}
			<div class="artid-image-placeholder" title="Carica un'immagine">
				<i class="bi bi-image-fill"></i>
			</div>
		{/if}

		{#if filter !== 'sharedWithMe'}
			<button
				class="position-absolute w-10 h-10 border-1 border-artid-border rounded-pill p-3 border bg-artid-section z-3"
				style="right: 0px; top: 0px; transform: translate(25%, -25%);"
				title={favouriteLabel}
				aria-label={favouriteLabel}
				onclick={toggleFavourite}
			>
				<span class="w-100 h-100 d-flex justify-content-center align-items-center">
					<i
						class="bi bi-star{isFavourite ? '-fill text-warning' : ''} fs-5"
						style="cursor: pointer; transform: translateY(-1px);"
					></i>
				</span>
			</button>
		{:else if filter === 'sharedWithMe'}
			<button
				class="position-absolute w-10 h-10 border-1 border-artid-border rounded-pill p-3 border bg-artid-section z-3"
				style="right: 0px; top: 0px; transform: translate(25%, -25%);"
				title={deleteLabel}
				aria-label={deleteLabel}
				onclick={removeInternalShare}
			>
				<span class="w-100 h-100 d-flex justify-content-center align-items-center">
					<i class="bi bi-trash fs-5" style="cursor: pointer; transform: translateY(-1px);"></i>
				</span>
			</button>
		{/if}
	</div>
	<div
		class="rounded-bottom-3 border-top border-artid-border p-2 px-3 text-artid-text bg-artid-surface card-description"
	>
		<div class="d-flex flex-column">
			<div class="d-flex justify-content-between">
				<span class="fw-semibold" style="font-size: 16px;">{artid.title}</span>
				<div>
					{#each artidTags as tag (tag.id)}
						<div
							class="d-inline-block ms-1 tag-dot"
							style="background-color: #{tag.color ?? '000'};"
							title={tag.title}
						></div>
					{/each}
				</div>
			</div>
			<span class="text-artid-text-muted" style="font-size: 14px;"
				>{formatItalianDate(artid.lastModified)}</span
			>
		</div>
	</div>
</a>

<style lang="scss">
	.card-image {
		position: relative;
		height: 200px;
		align-items: center;
		justify-content: center;
	}

	.card-image img {
		max-width: 100%;
		object-fit: cover;
		max-height: 100%;
	}

	.artid-card {
		transition: all 0.2s ease;
	}

	.artid-card:hover {
		box-shadow:
			0 4px 8px 0 rgba(117, 117, 117, 0.1),
			0 2px 4px 0 rgba(117, 117, 117, 0.05);
	}

	.artid-image-placeholder {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--artid-text-muted);

		i {
			font-size: 10rem;
			line-height: 1;
		}
	}

	.tag-dot {
		width: 8px;
		height: 8px;
		border-radius: 100%;
	}
</style>
