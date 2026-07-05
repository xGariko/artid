<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { TagResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import { toast } from '$lib/toast';

	let {
		isOpen = $bindable(),
		artidTags = $bindable(),
		artidId
	}: {
		isOpen: boolean;
		artidTags: TagResponse[];
		artidId: number;
	} = $props();

	let userTags: TagResponse[] = $state([]);
	let isSaving = $state(false);

	// Eliminazione definitiva di un tag dalla libreria dell'utente (diversa dalla rimozione del tag
	// da un singolo ArtID): richiede conferma perché il tag viene tolto da tutti gli ArtID.
	let tagToDelete = $state<TagResponse | null>(null);
	let showDeleteTagModal = $state(false);
	let isDeletingTag = $state(false);

	const artidTagsIds = $derived(new Set(artidTags.map((t) => t.id)));
	let selectedTagsIds = $state<Set<number>>(new Set());
	const tagsNumber = $derived(artidTags.length + selectedTagsIds.size);
	const canAddTag = $derived(tagsNumber <= 5);

	async function fetchAllTags() {
		try {
			const result = await api.GET('/api/tags', { credentials: 'include' });
			if (result.error) {
				toast.error('Errore nel caricamento dei tag');
				return;
			}

			userTags = result.data.map((t) => {
				return { id: t.id!, title: t.title!, color: t.color! };
			});
		} catch {
			toast.error('Errore nel caricamento dei tag');
		} finally {
			selectedTagsIds = new Set();
		}
	}

	$effect(() => {
		if (isOpen) {
			fetchAllTags();
		}
	});

	function toggleTagSelection(tagId: number | undefined) {
		if (tagId == null || artidTagsIds.has(tagId)) return;

		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const nextSelection = new Set(selectedTagsIds);
		if (nextSelection.has(tagId)) {
			nextSelection.delete(tagId);
		} else {
			// if (canAddTag) {
			nextSelection.add(tagId);
			// }
		}
		selectedTagsIds = nextSelection;
	}

	function askDeleteTag(tag: TagResponse) {
		tagToDelete = tag;
		showDeleteTagModal = true;
	}

	async function confirmDeleteTag() {
		if (!tagToDelete || isDeletingTag) return;
		const tag = tagToDelete;
		isDeletingTag = true;

		try {
			const response = await api.DELETE('/api/tags/{id}', {
				params: { path: { id: tag.id! } }
			});

			if (response.error) {
				toast.error("Errore durante l'eliminazione del tag");
				return;
			}

			// Rimuove il tag dal listato della libreria e da un'eventuale selezione in corso.
			userTags = userTags.filter((t) => t.id !== tag.id);
			if (tag.id != null && selectedTagsIds.has(tag.id)) {
				const nextSelection = new Set(selectedTagsIds);
				nextSelection.delete(tag.id);
				selectedTagsIds = nextSelection;
			}
			// Il DB rimuove in cascata anche l'associazione con l'ArtID corrente: riallineo il binding
			// (così il contatore e i chip nella pagina dettagli restano coerenti) e ricarico la load.
			artidTags = artidTags.filter((t) => t.id !== tag.id);
			toast.success('Tag eliminato con successo');
			await invalidateAll();
		} catch {
			toast.error('Errore di rete');
		} finally {
			isDeletingTag = false;
			showDeleteTagModal = false;
			tagToDelete = null;
		}
	}

	async function handleSubmit() {
		isSaving = true;

		try {
			const results = await Promise.all(
				[...selectedTagsIds].map((tagId) =>
					api.POST('/api/artids/{id}/tags', {
						params: { path: { id: artidId } },
						body: tagId
					})
				)
			);

			await invalidateAll();
			if (results.some((r) => r.error)) {
				toast.error('Errore nel caricamento dei tag');
			} else {
				toast.success('Tag aggiunti con successo');
			}
		} catch {
			toast.error('Errore nel caricamento dei tag');
		} finally {
			isSaving = false;
			isOpen = false;
		}
	}
</script>

<ArtidEditorModal bind:isOpen customHeight="30" customWidth="30">
	<div class="d-flex h-100 flex-column gap-3 justify-content-between pt-1">
		<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
			<i class="bi bi-plus-lg fs-5 text-primary"></i>
			<span>Aggiungi Tag</span>
		</div>

		<div
			class="flex-grow-1 d-flex gap-2 overflow-y-auto rounded-3 border border-artid-border bg-artid-surface p-2 flex-wrap tag-container"
		>
			{#each userTags as tag (tag.id)}
				{@const isTagSelected = tag.id != null && selectedTagsIds.has(tag.id)}
				{@const isTagInArtid = tag.id != null && artidTagsIds.has(tag.id)}
				<div
					class="tag-chip d-inline-flex align-items-center gap-1 border rounded-3 ps-2 pe-1 py-1"
					class:disabled={isTagInArtid}
					class:selected={isTagSelected}
				>
					<button
						type="button"
						class="tag-chip__label border-0 bg-transparent p-0"
						onclick={() => (!isTagInArtid ? toggleTagSelection(tag.id) : '')}
						aria-disabled={isTagInArtid}
					>
						<span>{tag.title}</span>
					</button>
					<!-- Elimina il tag dalla libreria (non solo da questo ArtID): apre la conferma. -->
					<button
						type="button"
						class="tag-chip__remove border-0 bg-transparent p-0 d-flex lh-1"
						onclick={() => askDeleteTag(tag)}
						aria-label={`Elimina il tag ${tag.title}`}
						title="Elimina definitivamente il tag"
					>
						<i class="bi bi-x fs-6"></i>
					</button>
				</div>
			{/each}
		</div>
		<div
			class="d-flex justify-content-between align-items-center border"
			style="border-color: transparent !important;"
		>
			<span class="text-artid-text-muted">Tag nell'artid: {tagsNumber}/{5}</span>
			<ArtidButton
				label="Aggiungi"
				fullWidth={false}
				disabled={!canAddTag || isSaving}
				onclick={handleSubmit}
			/>
		</div>
	</div>
</ArtidEditorModal>

<ArtidModal
	bind:isOpen={showDeleteTagModal}
	title="Elimina tag"
	message={`Vuoi eliminare definitivamente il tag "${tagToDelete?.title ?? ''}"? Verrà rimosso da tutti gli ArtID a cui è applicato.`}
	onConfirm={confirmDeleteTag}
	btnStyle="danger"
	confirmLabel="Elimina"
/>

<style lang="scss">
	.tag-chip {
		height: min-content;
	}

	// I due bottoni interni (seleziona / elimina) sono trasparenti: ereditano il colore del chip,
	// così restano leggibili anche sullo sfondo primario dello stato .selected.
	.tag-chip__label,
	.tag-chip__remove {
		color: inherit;
	}

	.tag-chip__remove {
		cursor: pointer;
		opacity: 0.6;

		&:hover {
			opacity: 1;
		}
	}

	.disabled {
		cursor: not-allowed;
		opacity: 0.6;
	}
	.selected {
		background-color: var(--artid-primary);
		color: white;
	}
</style>
