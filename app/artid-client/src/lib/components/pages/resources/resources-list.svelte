<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { ResourceResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import { sanitizeHtml } from '$lib/sanitize';

	import {
		badgeColorForExtension,
		badgeLabelForExtension,
		formatFileSize,
		formatItalianDate
	} from '$lib/utilities';
	import { toast } from '$lib/toast';

	let {
		resources,
		onEditRequest
	}: {
		resources: ResourceResponse[];
		onEditRequest?: (resource: ResourceResponse) => void;
	} = $props();

	// Stato locale della UI: query di ricerca e set di id selezionati.
	let searchQuery = $state('');
	let selectedResourceIds = $state<Set<number>>(new Set());

	let showDeleteModal = $state(false);

	// Tooltip descrizione: un unico elemento position:fixed condiviso, posizionato all'hover
	// dell'icona. Fixed (non absolute) per non essere tagliato dall'overflow del contenitore lista.
	let descTooltip = $state<{ html: string; x: number; y: number } | null>(null);

	function showDescription(event: MouseEvent, description: string | undefined | null): void {
		if (!description) return;
		const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
		descTooltip = { html: sanitizeHtml(description), x: rect.left, y: rect.bottom + 6 };
	}

	function hideDescription(): void {
		descTooltip = null;
	}

	// Cambiando filtro sidebar il parent passa un nuovo array `resources`:
	// resettiamo la selezione per evitare di trattenere id non più visibili.
	$effect(() => {
		resources;
		selectedResourceIds = new Set();
	});

	// Filtra le risorse in base alla query (match su titolo o nome file).
	const filteredResources = $derived.by(() => {
		const normalizedQuery = searchQuery.trim().toLowerCase();
		if (!normalizedQuery) return resources;
		return resources.filter((resource) => {
			const titleMatches = (resource.title ?? '').toLowerCase().includes(normalizedQuery);
			const fileNameMatches = (resource.fileName ?? '').toLowerCase().includes(normalizedQuery);
			return titleMatches || fileNameMatches;
		});
	});

	// True se ogni risorsa filtrata è selezionata (controlla lo stato del checkbox header).
	const areAllFilteredSelected = $derived(
		filteredResources.length > 0 &&
			filteredResources.every(
				(resource) => resource.id != null && selectedResourceIds.has(resource.id)
			)
	);

	const hasSelection = $derived(selectedResourceIds.size > 0);

	function toggleResourceSelection(resourceId: number | undefined): void {
		if (resourceId == null) return;
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const nextSelection = new Set(selectedResourceIds);
		if (nextSelection.has(resourceId)) {
			nextSelection.delete(resourceId);
		} else {
			nextSelection.add(resourceId);
		}
		selectedResourceIds = nextSelection;
	}

	// La riga è cliccabile: replichiamo il toggle anche da tastiera (Invio/Spazio).
	function handleRowKeydown(event: KeyboardEvent, resourceId: number | undefined): void {
		if (event.key === 'Enter' || event.key === ' ') {
			event.preventDefault();
			toggleResourceSelection(resourceId);
		}
	}

	function toggleAllFilteredSelection(): void {
		if (areAllFilteredSelected) {
			selectedResourceIds = new Set();
			return;
		}
		selectedResourceIds = new Set(
			filteredResources
				.map((resource) => resource.id)
				.filter((resourceId): resourceId is number => resourceId != null)
		);
	}

	function downloadSelectedResources(): void {
		// Ogni download passa dal proxy SvelteKit che inietta il JWT dal cookie httpOnly.
		for (const resourceId of selectedResourceIds) {
			window.open(`/api/resources/${resourceId}/file`, '_blank', 'noopener');
		}
	}

	function editSelectedResources(): void {
		if (selectedResourceIds.size !== 1) {
			toast.warning('Seleziona una sola risorsa per modificarla');
			return;
		}
		const [targetId] = selectedResourceIds;
		const targetResource = resources.find((resource) => resource.id === targetId);
		if (targetResource) onEditRequest?.(targetResource);
	}

	async function deleteSelectedResources(): Promise<void> {
		showDeleteModal = false;
		try {
			const results = await Promise.all(
				[...selectedResourceIds].map((id) =>
					api.DELETE('/api/resources/{id}', { params: { path: { id } } })
				)
			);
			await invalidateAll();
			if (results.some((r) => r.error)) {
				toast.error("Errore nell'eliminazione delle risorse");
			} else {
				toast.success('Risorse eliminate con successo');
			}
		} catch {
			toast.error("Errore nell'eliminazione delle risorse");
		}
	}

	async function toggleResourceFavorite(resource: ResourceResponse): Promise<void> {
		if (resource.id == null) return;
		const newFavoriteState = !resource.favorite;
		try {
			const response = await api.PUT('/api/resources/{id}/favourite', {
				params: { path: { id: resource.id } },
				body: newFavoriteState
			});
			if (response.error) {
				toast.error(
					newFavoriteState
						? 'Impossibile aggiungere la risorsa ai preferiti'
						: 'Impossibile rimuovere la risorsa dai preferiti'
				);
				return;
			}
			await invalidateAll();
			toast.success(
				newFavoriteState ? 'Risorsa aggiunta ai preferiti' : 'Risorsa rimossa dai preferiti'
			);
		} catch {
			toast.error('Errore di rete');
		}
	}

	let selectedResources = $derived(resources.filter((r) => selectedResourceIds.has(r.id!)));
</script>

<div
	class="bg-artid-section h-100 w-60 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
>
	<div class="position-relative">
		<i
			class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-artid-primary"
		></i>
		<input
			type="text"
			class="form-control rounded-3 ps-5 py-2 search-input"
			placeholder="Cerca materiali"
			bind:value={searchQuery}
		/>
	</div>

	<div class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border">
		<div
			class="row g-0 align-items-center px-3 py-2 sticky-header bg-artid-muted border-bottom border-artid-border text-artid-text small fw-semibold text-nowrap"
		>
			<div class="col-1">
				<input
					type="checkbox"
					class="form-check-input"
					checked={areAllFilteredSelected}
					onchange={toggleAllFilteredSelection}
					aria-label="Seleziona tutto"
				/>
			</div>
			<div class="col-2">Nome</div>
			<div class="col-1 text-center">Descr.</div>
			<div class="col-2">Dimensioni</div>
			<div class="col-2">Creato</div>
			<div class="col-2">Modificato</div>
			<div class="col-1">Collegato</div>
			<div class="col-1 text-center">Preferito</div>
		</div>

		{#each filteredResources as resource (resource.id)}
			{@const isResourceSelected = resource.id != null && selectedResourceIds.has(resource.id)}
			<div
				class="row g-0 align-items-center px-3 py-2 border-bottom border-artid-border resource-row"
				class:selected={isResourceSelected}
				role="button"
				tabindex="0"
				onclick={() => toggleResourceSelection(resource.id)}
				onkeydown={(event) => handleRowKeydown(event, resource.id)}
			>
				<div class="col-1 d-flex align-items-center gap-2">
					<input
						type="checkbox"
						class="form-check-input flex-shrink-0"
						checked={isResourceSelected}
						onchange={() => toggleResourceSelection(resource.id)}
						onclick={(event) => event.stopPropagation()}
						aria-label={`Seleziona ${resource.title}`}
					/>
					<span
						class="badge-type fw-bold text-white"
						style:background-color={badgeColorForExtension(resource.extension)}
					>
						{badgeLabelForExtension(resource.extension)}
					</span>
				</div>
				<div
					class="col-2 pe-3 text-truncate fw-medium text-artid-text resource-title"
					title={resource.title}
				>
					{resource.title}
				</div>
				<div class="col-1 text-center">
					{#if resource.description}
						<button
							type="button"
							class="btn btn-link p-0 border-0 desc-icon"
							aria-label="Mostra descrizione"
							onmouseenter={(event) => showDescription(event, resource.description)}
							onmouseleave={hideDescription}
							onclick={(event) => event.stopPropagation()}
						>
							<i class="bi bi-card-text fs-5 text-artid-primary"></i>
						</button>
					{:else}
						<i class="bi bi-card-text fs-5 text-artid-border" aria-hidden="true"></i>
					{/if}
				</div>
				<div class="col-2 text-artid-text">{formatFileSize(resource.fileSize)}</div>
				<div class="col-2 text-artid-text text-nowrap">{formatItalianDate(resource.createdAt)}</div>
				<div class="col-2 text-artid-text text-nowrap">
					{formatItalianDate(resource.lastModified)}
				</div>
				<div class="col-1 text-artid-text text-nowrap">
					{resource.artidCount ?? 0}
					<span class="text-artid-text-muted ms-1">ArtID</span>
				</div>
				<div class="col-1 text-center">
					<button
						type="button"
						class="btn btn-link p-0 border-0"
						title={resource.favorite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'}
						aria-label={resource.favorite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'}
						onclick={(event) => {
							event.stopPropagation();
							toggleResourceFavorite(resource);
						}}
					>
						<i
							class="bi bi-star{resource.favorite
								? '-fill text-warning'
								: ' text-artid-text-muted'} fs-5"
						></i>
					</button>
				</div>
			</div>
		{/each}

		{#if filteredResources.length === 0}
			<div class="text-center text-artid-text-muted py-5">
				<i class="bi bi-folder2-open fs-2 d-block mb-2"></i>
				Nessun materiale trovato
			</div>
		{/if}
	</div>

	<div class="d-flex align-items-center gap-2">
		<ArtidButton
			label="Scarica"
			icon="download"
			disabled={!hasSelection}
			fullWidth={false}
			onclick={downloadSelectedResources}
		/>
		<ArtidButton
			label="Modifica"
			icon="pencil-square"
			outline={true}
			disabled={!hasSelection}
			fullWidth={false}
			onclick={editSelectedResources}
		/>
		<ArtidButton
			icon="trash"
			btnStyle="danger"
			outline={true}
			disabled={!hasSelection}
			fullWidth={false}
			ariaLabel="Elimina selezionati"
			onclick={() => {
				showDeleteModal = true;
			}}
		/>
	</div>
</div>

<!-- Tooltip descrizione (HTML sanificato). Fuori dal contenitore con overflow per non essere tagliato. -->
{#if descTooltip}
	<div
		class="desc-tooltip border border-artid-border bg-artid-surface rounded-3 p-3 text-artid-text"
		style="left: {descTooltip.x}px; top: {descTooltip.y}px;"
	>
		{@html descTooltip.html}
	</div>
{/if}

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={deleteSelectedResources}
	message="Sei sicuro di voler cancellare i materiali selezionati? '{selectedResources
		.map((r) => r.title)
		.join(',')}'"
	btnStyle="danger"
/>

<style>
	.artid-list {
		min-width: 60rem;
	}

	.search-input {
		border-color: var(--artid-border);
	}

	.search-input:focus {
		border-color: var(--artid-primary);
		box-shadow: 0 0 0 0.2rem var(--artid-primary-subtle);
	}

	/* Header sticky: resta sopra le righe ma sotto le modali (z-index basso). */
	.sticky-header {
		position: sticky;
		top: 0;
		z-index: 1;
	}

	/* Righe cliccabili con feedback hover (sostituisce .table-hover). */
	.resource-row {
		transition: 0.2s ease all;
		cursor: pointer;
	}

	.resource-row:hover {
		transition: 0.2s ease all;
		background-color: var(--artid-surface);
	}

	/* Riga selezionata: tinta brand, evidenziata anche in hover. */
	.resource-row.selected {
		transition: 0.2s ease all;
		box-shadow: inset 6px 0px 0px -3px var(--artid-primary);
	}

	/* Consente al titolo di troncare con ellissi dentro la colonna flex. */
	.resource-title {
		min-width: 0;
	}

	.desc-icon {
		cursor: default;
	}

	/* Tooltip descrizione: flat (bordo + fondo, niente ombra), non intercetta il mouse. */
	.desc-tooltip {
		position: fixed;
		z-index: 1080;
		width: 20rem;
		max-width: 90vw;
		max-height: 16rem;
		overflow-y: auto;
		font-size: 0.85rem;
		line-height: 1.4;
		pointer-events: none;
	}

	.desc-tooltip :global(p:last-child) {
		margin-bottom: 0;
	}

	.badge-type {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 2.25rem;
		height: 2.25rem;
		flex-shrink: 0;
		border-radius: 0.4rem;
		font-size: 0.7rem;
		letter-spacing: 0.02em;
	}
</style>
