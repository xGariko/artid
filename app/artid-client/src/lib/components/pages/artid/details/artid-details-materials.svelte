<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { ResourceResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { badgeColorForExtension, badgeLabelForExtension } from '$lib/utilities';

	import { dndzone } from 'svelte-dnd-action';
	import { toast } from 'svelte-sonner';
	import ArtidAddMaterialsModal from '../artid-add-materials-modal.svelte';

	let { artidId, artidMaterials }: { artidId: number; artidMaterials: ResourceResponse[] } =
		$props();

	// Gestione filtri ricerca
	let searchQuery = $state('');
	let draggableMaterials = $state<ResourceResponse[]>([]);
	$effect.pre(() => {
		draggableMaterials = artidMaterials;
	});
	// Filtra i materiali in base alla query (match su titolo o nome file).
	const filteredMaterials = $derived.by(() => {
		const normalizedQuery = searchQuery.trim().toLowerCase();
		if (!normalizedQuery) return draggableMaterials;
		return draggableMaterials.filter((material) => {
			const titleMatches = (material.title ?? '').toLowerCase().includes(normalizedQuery);
			const fileNameMatches = (material.fileName ?? '').toLowerCase().includes(normalizedQuery);
			return titleMatches || fileNameMatches;
		});
	});

	let isOpen = $state(false);
	let isDeleting = $state(false);

	// Gestione reorder dei materiali
	const flipDurationMs = 300;

	function handleDndConsider(e: CustomEvent) {
		draggableMaterials = e.detail.items;
	}

	async function handleDndFinalize(e: CustomEvent) {
		// draggableMaterials = e.detail.items;
		const { trigger, id } = e.detail.info;

		console.log(trigger);

		if (trigger === 'droppedIntoZone') {
			// Aggiorna lo stato locale con il NUOVO ordine
			const updatedItems = e.detail.items as ResourceResponse[];

			// Trova la posizione VECCHIA dell'elemento prima di applicare il nuovo ordine
			const oldIndex = artidMaterials.findIndex((m) => m.id === id);
			// Trova la NUOVA posizione dell'elemento
			const newIndex = updatedItems.findIndex((m) => m.id === id);

			// AGGIORNAMENTO STATO LOCALE
			draggableMaterials = updatedItems;

			// 4. CONTROLLO DI GUARDIA: Se la posizione non è cambiata, ci fermiamo qui!
			if (oldIndex === newIndex) {
				return;
			}

			const newRank = newIndex + 1; // Backend basato su 1

			try {
				const { response } = await api.PUT('/api/artids/{id}/resources/{resourceId}/reorder', {
					params: {
						path: { id: artidId, resourceId: id }
					},
					body: newRank,
					credentials: 'include'
				});

				if (!response.ok) {
					toast.error("Errore nel salvataggio dell'ordine.");
				} else {
					toast.success('Ordinamento aggiornato.');
				}
			} catch {
				toast.error('Errore di rete durante il riordinamento.');
			} finally {
				await invalidateAll();
			}
		}
	}

	// Funzione per rimuovere il materiale dall'artid
	async function handleDeleteMaterial(materialId: number) {
		if (isDeleting) return;

		isDeleting = true;

		try {
			const response = await api.DELETE('/api/artids/{id}/resources/{resourceId}', {
				params: {
					path: {
						id: artidId,
						resourceId: materialId
					}
				}
			});

			if (!response.error) {
				draggableMaterials = draggableMaterials.filter((m) => m.id !== materialId);
				toast.success('Materiale rimosso con successo');
			} else {
				toast.error('Errore durante la rimozione del materiale');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isDeleting = false;
		}
	}
</script>

<div
	class="bg-artid-surface border-0 border-bottom border-artid-border px-3 py-2 text-artid-text fw-semibold fs-5"
>
	Materiali
</div>

<div
	class="d-flex flex-column justify-content-between align-items-end flex-grow-1 m-2 p-2"
	style="min-height: 0;"
>
	<div
		class="flex-grow-1 w-100 d-flex flex-column rounded-3 border border-artid-border mb-2"
		style="min-height: 0;"
	>
		<div class="border-0 border-bottom border-artid-border px-2 py-3">
			<div class="position-relative">
				<i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-artid"
				></i>
				<input
					type="text"
					class="form-control rounded-3 ps-5 py-2 search-input"
					placeholder="Cerca materiali"
					disabled={filteredMaterials.length === 0}
					bind:value={searchQuery}
				/>
			</div>
		</div>

		<div class="p-2 flex-grow-1" style="overflow-y: auto; min-height: 0;">
			{#if filteredMaterials.length === 0}
				<div class="w-100 h-100 d-flex align-items-center justify-content-center">
					<span class="fw-bold fst-italic text-artid-text-muted">
						Aggiungi un materiale per iniziare.
					</span>
				</div>
			{:else}
				<ul
					class="list-group list-unstyled"
					use:dndzone={{
						items: draggableMaterials,
						flipDurationMs,
						dragDisabled: searchQuery.trim() !== '',
						dropTargetClasses: ['dndzone']
					}}
					onconsider={handleDndConsider}
					onfinalize={handleDndFinalize}
				>
					{#each filteredMaterials as material (material.id)}
						<li class="list-group-item d-flex justify-content-between align-items-center gap-3">
							<i
								class="bi bi-grip-horizontal fs-4 text-artid-text-muted"
								style:cursor={searchQuery.trim() !== '' ? 'not-allowed' : 'grab'}
								style:opacity={searchQuery.trim() !== '' ? 0.3 : 1}
							></i>
							<span
								class="badge-type fw-bold text-white"
								style:background-color={badgeColorForExtension(material.extension)}
							>
								{badgeLabelForExtension(material.extension)}
							</span>
							<span class="flex-grow-1 text-truncate">
								{material.title}
							</span>
							<button
								class="border-0 bg-transparent"
								onclick={() => handleDeleteMaterial(material.id!)}
								aria-label="remove material"
							>
								<i class="bi bi-x fs-4 text-danger"></i>
							</button>
						</li>
					{/each}
				</ul>
			{/if}
		</div>
	</div>

	<div class="w-100 d-flex justify-content-end flex-shrink-0 pt-1">
		<ArtidButton
			label="Aggiungi Materiale"
			icon="plus-lg"
			fullWidth={false}
			onclick={() => (isOpen = !isOpen)}
		/>
	</div>
</div>

<ArtidAddMaterialsModal bind:isOpen bind:artidMaterials={draggableMaterials} {artidId} />

<style lang="scss">
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

	:global(.dndzone) {
		outline: 2px solid var(--artid-primary) !important;
	}
</style>
