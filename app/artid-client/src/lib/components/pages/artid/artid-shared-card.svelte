<script lang="ts">
	import artidImage from '$lib/assets/artid_logo_outline_primary.svg';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import { toast } from 'svelte-sonner';
	import { invalidateAll } from '$app/navigation';
	import type { InternalShareArtIDExtendedResponse } from '$lib/api/types';

	let { sharedArtid }: { sharedArtid: InternalShareArtIDExtendedResponse } = $props();

	let deleteLabel = 'Rimuovi condivisione';

	async function removeInternalShare(event: MouseEvent) {
		event.preventDefault();
		event.stopPropagation();

		try {
			const response = await api.PUT('/api/shares/internal/decline', {
				body: Number(sharedArtid.idArtid),
				credentials: 'include'
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
	href={resolve('/explore/[id]/artid/[artidId]', {
		id: String(sharedArtid.idUserFrom),
		artidId: String(sharedArtid.idArtid)
	})}
	data-sveltekit-preload-data="off"
	class="rounded-3 border border-artid-border w-100 d-block text-decoration-none artid-card z-2"
>
	<div class="d-flex align-items-center card-image p-1">
		{#if sharedArtid.filePath}
			<img src={sharedArtid.filePath ?? artidImage} alt={`${sharedArtid.title} image`} />
		{:else}
			<div class="artid-image-placeholder" title="Carica un'immagine">
				<i class="bi bi-image-fill"></i>
			</div>
		{/if}

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
	</div>
	<div
		class="rounded-bottom-3 border-top border-artid-border p-2 px-3 text-artid-text bg-artid-surface card-description"
	>
		<div class="d-flex flex-column">
			<div class="d-flex justify-content-between">
				<span class="fw-semibold" style="font-size: 16px;">{sharedArtid.title}</span>
			</div>
			{sharedArtid.isAccepted}
			<span class="text-artid-text-muted" style="font-size: 14px;">Da: {sharedArtid.name}</span>
		</div>
	</div>
</a>

<style lang="scss">
	.card-image {
		position: relative;
		height: 200px;
		align-items: center;
	}

	.card-image img {
		width: 100%;
		object-fit: cover;
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
</style>
