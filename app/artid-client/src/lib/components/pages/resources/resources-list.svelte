<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { components } from '$lib/api/schema';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';

	import {
		badgeColorForExtension,
		badgeLabelForExtension,
		formatFileSize,
		formatItalianDate
	} from '$lib/utilities';
	import { toast } from 'svelte-sonner';

	type ResourceResponse = components['schemas']['ResourceResponse'];

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
				toast.error('Errore nell\'eliminazione di alcune risorse');
			} else {
				toast.success('Risorse eliminate con successo');
			}
		} catch {
			toast.error('Errore nell\'eliminazione delle risorse');
		}
	}
</script>

<div class="bg-artid-section h-100 w-60 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list">
	<div class="position-relative">
		<i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-artid-primary"></i>
		<input
			type="text"
			class="form-control rounded-3 ps-5 py-2 search-input"
			placeholder="Cerca materiali"
			bind:value={searchQuery}
		/>
	</div>

	<div class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border">
		<table class="table table-hover align-middle mb-0 resources-table">
			<thead>
			<tr class="text-artid-text-muted small">
				<th class="ps-3" style="width: 3rem;">
					<input
						type="checkbox"
						class="form-check-input"
						checked={areAllFilteredSelected}
						onchange={toggleAllFilteredSelection}
						aria-label="Seleziona tutto"
					/>
				</th>
				<th>Nome</th>
				<th>Dimensioni</th>
				<th>Creato</th>
				<th>Modificato</th>
				<th>Collegato</th>
				<th class="pe-3 text-center">Preferito</th>
			</tr>
			</thead>
			<tbody>
			{#each filteredResources as resource (resource.id)}
				{@const isResourceSelected = resource.id != null && selectedResourceIds.has(resource.id)}
				<tr
					class:selected={isResourceSelected}
					onclick={() => toggleResourceSelection(resource.id)}
				>
					<td class="ps-3">
						<input
							type="checkbox"
							class="form-check-input"
							checked={isResourceSelected}
							onchange={() => toggleResourceSelection(resource.id)}
							onclick={(event) => event.stopPropagation()}
							aria-label={`Seleziona ${resource.title}`}
						/>
					</td>
					<td>
						<div class="d-flex align-items-center gap-3">
								<span
									class="badge-type fw-bold text-white"
									style:background-color={badgeColorForExtension(resource.extension)}
								>
									{badgeLabelForExtension(resource.extension)}
								</span>
							<span class="fw-medium">{resource.title}</span>
						</div>
					</td>
					<td class="text-artid-text">{formatFileSize(resource.fileSize)}</td>
					<td class="text-artid-text">{formatItalianDate(resource.createdAt)}</td>
					<td class="text-artid-text">{formatItalianDate(resource.lastModified)}</td>
					<td class="text-artid-text">
						{resource.artidCount ?? 0}
						<span class="text-artid-text-muted ms-1">ArtID</span>
					</td>
					<td class="pe-3 text-center">
						<i
							class="bi bi-star{resource.favorite ? '-fill text-warning' : ' text-artid-text-muted'} fs-5"
						></i>
					</td>
				</tr>
			{/each}
			</tbody>
			{#if filteredResources.length === 0}
				<caption class="text-center text-artid-text-muted py-5">
					<i class="bi bi-folder2-open fs-2 d-block mb-2"></i>
					Nessun materiale trovato
				</caption>
			{/if}

		</table>
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
			onclick={() => { showDeleteModal = true; }}
		/>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={deleteSelectedResources}
	message="Una volta eliminata la risorsa non sarà recuperabile."
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

    .resources-table {
        --bs-table-hover-bg: var(--artid-primary-subtle);
        /* Default browser è border-spacing 2px → lasciava una strisciolina bianca
					 tra il bordo del container e l'inizio del bg dell'header. */
        border-collapse: collapse;
        border-spacing: 0;
    }

    /* Header sticky così resta visibile mentre si scorre la tabella. */
    .resources-table thead th {
        position: sticky;
        top: 0;
        z-index: 1;
        background-color: var(--artid-muted);
        border-bottom: 1px solid var(--artid-border);
        font-weight: 600;
        font-size: 0.8rem;
        padding: 0.6rem 0.5rem;
        vertical-align: middle;
        white-space: nowrap;
    }

    .resources-table tbody td {
        padding: 0.75rem 0.5rem;
        vertical-align: middle;
    }

    .resources-table tbody tr {
        border-left: 3px solid transparent;
        cursor: pointer;
        transition: border-color 0.15s ease-in-out;
    }

    .resources-table tbody tr.selected {
        border-left-color: var(--artid-primary);
        background-color: var(--artid-primary-subtle);
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
