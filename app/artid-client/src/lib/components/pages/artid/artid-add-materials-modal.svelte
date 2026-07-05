<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { ResourceResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import { badgeColorForExtension, badgeLabelForExtension, formatFileSize } from '$lib/utilities';
	import { toast } from 'svelte-sonner';

	let {
		isOpen = $bindable(),
		artidMaterials = $bindable(),
		artidId
	}: {
		isOpen: boolean;
		artidMaterials: ResourceResponse[];
		artidId: number;
	} = $props();

	let loading = $state(true);
	let userMaterials: ResourceResponse[] = $state([]);
	let isSaving = $state(false);

	const artidMaterialsIds = $derived(new Set(artidMaterials.map((m) => m.id)));

	// Funzione che recupera i materiali totali (esegue il fetch solo all'occorrenza)
	async function fetchAllMaterials() {
		loading = true;

		try {
			const result = await fetch('/api/resources');
			if (!result.ok) {
				toast.error('Errore nel caricamento dei materiali');
				return;
			}

			userMaterials = await result.json();
		} catch {
			toast.error('Errore nel caricamento dei materiali');
		} finally {
			loading = false;
		}
	}

	$effect(() => {
		if (isOpen) {
			fetchAllMaterials();
		}
	});

	async function handleSubmit() {
		isSaving = true;
		try {
			const results = [];

			// const results = await Promise.all(
			// 	// [...selectedMaterialsIds].map((materialId) =>
			// 	// 	api.POST('/api/artids/{id}/resources', {
			// 	// 		params: { path: { id: artidId } },
			// 	// 		body: materialId
			// 	// 	})
			// 	// )

			// );

			for (const materialId of selectedMaterialsIds) {
				const res = await api.POST('/api/artids/{id}/resources', {
					params: { path: { id: artidId } },
					body: materialId
				});
				results.push(res);
			}

			await invalidateAll();
			if (results.some((r) => r.error)) {
				toast.error('Errore nel caricamento delle risorse');
			} else {
				toast.success('Risorse caricate con successo');
			}
		} catch {
			toast.error('Errore nel caricamento delle risorse');
		} finally {
			isSaving = false;
			isOpen = false;
		}
	}

	// Stato locale della UI: query di ricerca e set di id selezionati.
	let searchQuery = $state('');
	let selectedMaterialsIds = $state<Set<number>>(new Set());

	// Filtra le risorse in base alla query (match su titolo o nome file).
	const filteredMaterials = $derived.by(() => {
		const normalizedQuery = searchQuery.trim().toLowerCase();
		if (!normalizedQuery) return userMaterials;
		return userMaterials.filter((material) => {
			const titleMatches = (material.title ?? '').toLowerCase().includes(normalizedQuery);
			const fileNameMatches = (material.fileName ?? '').toLowerCase().includes(normalizedQuery);
			return titleMatches || fileNameMatches;
		});
	});

	// True se ogni risorsa filtrata è selezionata (controlla lo stato del checkbox header).
	const areAllFilteredSelected = $derived(
		filteredMaterials.filter((material) => !artidMaterialsIds.has(material.id)).length > 0 &&
			filteredMaterials
				.filter((material) => !artidMaterialsIds.has(material.id))
				.every((material) => material.id != null && selectedMaterialsIds.has(material.id))
	);

	function toggleResourceSelection(resourceId: number | undefined): void {
		if (resourceId == null || artidMaterialsIds.has(resourceId)) return;
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const nextSelection = new Set(selectedMaterialsIds);
		if (nextSelection.has(resourceId)) {
			nextSelection.delete(resourceId);
		} else {
			nextSelection.add(resourceId);
		}
		selectedMaterialsIds = nextSelection;
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
			selectedMaterialsIds = new Set();
			return;
		}
		selectedMaterialsIds = new Set(
			filteredMaterials
				.filter((material) => !artidMaterialsIds.has(material.id))
				.map((material) => material.id)
				.filter((materialId): materialId is number => materialId != null)
		);
	}
</script>

<ArtidEditorModal bind:isOpen customHeight="60" customWidth="50">
	<div class="d-flex flex-column gap-4 resource-editor pt-1">
		<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
			<i class="bi bi-plus-lg fs-5 text-primary"></i>
			<span>Aggiungi Materiali</span>
		</div>
		<div class="border-0 border-artid-border">
			<div class="position-relative">
				<i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-artid"
				></i>
				<input
					type="text"
					class="form-control rounded-3 ps-5 py-2 search-input"
					placeholder="Cerca materiali"
					bind:value={searchQuery}
				/>
			</div>
		</div>
		<div class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border">
			<div
				class="row g-0 align-items-center px-3 py-2 sticky-header bg-artid-surface border-bottom border-artid-border text-artid-text small fw-semibold text-nowrap"
			>
				<div class="col-1 me-2">
					<input
						type="checkbox"
						class="form-check-input"
						checked={areAllFilteredSelected}
						onchange={toggleAllFilteredSelection}
						disabled={filteredMaterials.filter((material) => !artidMaterialsIds.has(material.id))
							.length <= 0}
						aria-label="Seleziona tutto"
					/>
				</div>
				<div class="col-6">Nome</div>
				<div class="col-2">Dimensioni</div>
			</div>
			{#if loading}{:else if filteredMaterials.length <= 0}
				<div class="text-center text-artid-text-muted py-5">
					<i class="bi bi-folder2-open fs-2 d-block mb-2"></i>
					Nessun materiale trovato
				</div>
			{:else}
				{#each filteredMaterials as material (material.id)}
					{@const isMaterialSelected = material.id != null && selectedMaterialsIds.has(material.id)}
					{@const isMaterialInArtid = material.id != null && artidMaterialsIds.has(material.id)}

					<div
						class="row g-0 align-items-center px-3 py-2 border-bottom border-artid-border resource-row"
						class:disabled={isMaterialInArtid}
						class:selected={isMaterialSelected}
						role="button"
						tabindex="0"
						onclick={() => (!isMaterialInArtid ? toggleResourceSelection(material.id) : '')}
						onkeydown={(event) => (!isMaterialInArtid ? handleRowKeydown(event, material.id) : '')}
						aria-disabled={isMaterialInArtid}
					>
						<div class="col-1 d-flex align-items-center gap-2 me-2">
							<input
								type="checkbox"
								class="form-check-input flex-shrink-0"
								disabled={isMaterialInArtid}
								checked={!isMaterialInArtid && isMaterialSelected}
								onchange={() => toggleResourceSelection(material.id)}
								onclick={(event) => event.stopPropagation()}
								aria-label={`Seleziona ${material.title}`}
							/>
							<span
								class="badge-type fw-bold text-white"
								style:background-color={badgeColorForExtension(material.extension)}
							>
								{badgeLabelForExtension(material.extension)}
							</span>
						</div>
						<div
							class="col-6 pe-3 text-truncate fw-medium text-artid-text resource-title"
							title={material.title}
						>
							{material.title}
						</div>
						<div class="col-2 text-artid-text">{formatFileSize(material.fileSize)}</div>
						<div class="col-2">
							{#if isMaterialInArtid}
								<span class="text-artid-text-muted" style="font-size: 14px;"
									>Questo materiale è già nell'artid</span
								>
							{/if}
						</div>
					</div>
				{/each}
			{/if}
		</div>
		<div class="d-flex justify-content-end gap-2">
			<ArtidButton
				label="Chiudi"
				btnStyle="secondary"
				outline={true}
				fullWidth={false}
				disabled={isSaving}
				onclick={() => (isOpen = false)}
			/>
			<ArtidButton
				label="Aggiungi"
				icon="check2"
				btnStyle="success"
				fullWidth={false}
				disabled={isSaving || selectedMaterialsIds.size <= 0}
				onclick={handleSubmit}
			/>
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
	.resource-editor {
		width: 100%;
		min-height: min(48rem, 50vh);
		max-height: min(48rem, 50vh);
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
	.resource-row:not(.disabled) {
		transition: 0.2s ease all;
		cursor: pointer;
	}

	.resource-row.disabled {
		cursor: not-allowed;
		opacity: 0.6;
	}

	.resource-row:hover:not(.disabled) {
		transition: 0.2s ease all;
		background-color: var(--artid-surface);
	}

	/* Riga selezionata: tinta brand, evidenziata anche in hover. */
	.resource-row.selected:not(.disabled) {
		transition: 0.2s ease all;
		box-shadow: inset 6px 0px 0px -3px var(--artid-primary);
	}

	/* Consente al titolo di troncare con ellissi dentro la colonna flex. */
	.resource-title {
		min-width: 0;
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
