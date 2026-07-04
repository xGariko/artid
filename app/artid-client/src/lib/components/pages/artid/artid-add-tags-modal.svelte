<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { TagResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import { toast } from 'svelte-sonner';

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
	<div class="d-flex flex-column gap-0 pt-1">
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
				<button
					class="d-inline border rounded-3 p-2"
					class:disabled={isTagInArtid}
					class:selected={isTagSelected}
					onclick={() => (!isTagInArtid ? toggleTagSelection(tag.id) : '')}
					aria-disabled={isTagInArtid}
				>
					<span>{tag.title}</span>
				</button>
			{/each}
		</div>
		<div class="d-flex justify-content-between align-items-center border">
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

<style lang="scss">
	.tag-container {
		// height: 100%;
	}

	.disabled {
		cursor: not-allowed;
		opacity: 0.6;
	}
	.selected {
		background-color: var(--artid-primary);
	}
</style>
