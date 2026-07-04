<script lang="ts">
	import type { components } from '$lib/api/schema';
	import artidImage from '$lib/assets/artid_logo_outline_primary.svg';
	import { formatItalianDate, type ArtIdFilterType } from '$lib/utilities';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import { toast } from 'svelte-sonner';

	let {
		artid,
		filter
	}: { artid: components['schemas']['ArtidResponse']; filter: ArtIdFilterType } = $props();

	// Il prop `artid` non è un proxy $state: per aggiornare subito l'icona tengo lo
	// stato in locale e lo riallineo al dato del server quando la lista si ricarica.
	// svelte-ignore state_referenced_locally
	let isFavourite = $state(artid.favourite ?? false);
	$effect(() => {
		isFavourite = artid.favourite ?? false;
	});

	let favouriteLabel = $derived(isFavourite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti');

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
</script>

<a
	href={resolve('/(app)/artid/details/[id]', { id: String(artid.id) })}
	class="rounded-3 border border-artid-border w-100 d-block text-decoration-none artid-card z-2"
>
	<div class="card-image p-4">
		<img src={artid.thumbnailUrl ?? artidImage} alt={`${artid.title} image`} />
		{#if filter === 'mine'}
				<button class="position-absolute w-10 h-10 border-1 border-artid-border rounded-pill p-3 border bg-artid-section z-3"
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
			<span class="action">
				<i class="bi bi-trash fs-6 text-primary"></i>
			</span>
		{/if}
	</div>
	<div
		class="rounded-bottom-3 border-top border-artid-border p-2 px-3 text-artid-text bg-artid-surface card-description"
	>
		<div class="d-flex flex-column">
			<span class="fw-semibold fs-5">{artid.title}</span>
			<span class="text-artid-text-muted fs-6"
			>{formatItalianDate(artid.lastModified)}</span>
		</div>
	</div>
</a>

<style lang="scss">
  .card-image {
    position: relative;
  }

  .card-image img {
    width: 100%;
    object-fit: cover;
  }


  .artid-card {
    transition: all 0.2s ease;
  }

  .artid-card:hover {
    box-shadow: 0 4px 8px 0 rgba(117, 117, 117, 0.1), 0 2px 4px 0 rgba(117, 117, 117, 0.05);
  }
</style>
