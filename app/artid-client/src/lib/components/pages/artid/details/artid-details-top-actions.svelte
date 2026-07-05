<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import type { ArtidResponse, TagResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import type { ArtIdVisibilityType } from '$lib/utilities';
	import { toast } from 'svelte-sonner';
	import ArtidAddTagsModal from '../artid-add-tags-modal.svelte';
	import ArtidAddInternalShareModal from '../artid-add-internal-share-modal.svelte';
	import ArtidCreateLinkModal from '../artid-create-link-modal.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';

	let {
		artidId,
		artid,
		artidTags
	}: { artidId: number; artid: ArtidResponse; artidTags: TagResponse[] } = $props();

	let isDeleting = $state(false);
	let isTagOpen = $state(false);
	let showDeleteModal = $state(false);
	let isInternalShareOpen = $state(false);
	let isCreateLinkOpen = $state(false);

	async function handleFavourite() {
		const newFavouriteState = !artid.favourite;

		try {
			const response = await api.PUT('/api/artids/{id}/favourite', {
				params: { path: { id: artidId } },
				body: newFavouriteState
			});

			if (!response.error) {
				artid.favourite = newFavouriteState;

				const successMessage = newFavouriteState
					? 'ArtID aggiunto ai preferiti'
					: 'ArtID rimosso dai preferiti';

				toast.success(successMessage);
			} else {
				const errorMessage = newFavouriteState
					? 'Impossibile aggiungere ArtID ai preferiti'
					: 'Impossibile rimuovere ArtID dai preferiti';
				toast.error(errorMessage);
			}
		} catch {
			toast.error('Errore di rete');
		}
	}

	async function handleDeleteTag(tagId: number) {
		if (isDeleting) return;

		isDeleting = true;

		try {
			const response = await api.DELETE('/api/artids/{id}/tags/{tagId}', {
				params: {
					path: {
						id: artidId,
						tagId: tagId
					}
				}
			});

			if (!response.error) {
				artidTags = artidTags.filter((t) => t.id !== tagId);
				toast.success('Tag rimosso con successo');
			} else {
				toast.error('Errore durante la rimozione del tag');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isDeleting = false;
		}
	}

	async function handleDelete() {
		try {
			const response = await api.DELETE('/api/artids/{id}', {
				params: { path: { id: artidId } }
			});

			if (!response.error) {
				toast.success('ArtId eliminato con successo');
				await goto(resolve('/(app)/artid'));
			} else {
				toast.error("Errore durante l'eliminazione dell'artid");
			}
		} catch {
			toast.error('Errore di rete');
		}
		showDeleteModal = false;
	}
</script>

<div class="bg-artid-section mh-100 w-75 rounded-3 border border-artid-border flex-shrink-0">
	<div class="row">
		<div class="col-6 a d-flex flex-row border-artid-border">
			<button
				class="rounded-3 border-0 bg-transparent border-end border-artid-border outline-0 py-3 px-4 me-2 artid-favourite"
				aria-label="favourite"
				onclick={handleFavourite}
			>
				{#if artid.favourite}
					<i class="bi bi-star-fill text-warning fs-4"></i>
				{:else}
					<i class="bi bi-star text-artid-muted fs-4"></i>
				{/if}
			</button>
			<div class="tag-container">
				<ul class="list-unstyled mb-0 d-flex gap-2 flex-wrap">
					{#each artidTags as tag (tag.id)}
						<li
							class="tag d-flex align-items-center px-1 rounded-4 bg-artid-surface border border-artid-border"
						>
							<span class="tag-color me-1" style="background-color: #{tag.color};"></span>
							<span class="tag-name">{tag.title}</span>
							<button
								class="p-0 m-0 border-0 bg-transparent"
								onclick={() => handleDeleteTag(tag.id!)}
								aria-label="button"
							>
								<i class="bi bi-x fs-6 text-danger"></i>
							</button>
						</li>
					{/each}
				</ul>
				<button
					class="text-artid text-decoration-underline bg-transparent border-0 p-0"
					onclick={() => (isTagOpen = !isTagOpen)}
					disabled={artidTags.length >= 5}
				>
					<span>Aggiungi Tag +</span>
				</button>
			</div>
		</div>
		<div class="col-6 d-flex align-items-center justify-content-between">
			<ArtidButton
				icon="trash"
				fullWidth={false}
				btnStyle="danger"
				outline={true}
				onclick={() => (showDeleteModal = true)}
			/>
			<div style="padding-right: calc(var(--bs-gutter-x)*0.5);">
				<ArtidButton
					label="Anteprima"
					icon="eye"
					fullWidth={false}
					btnStyle="secondary"
					outline={true}
					onclick={() => {
						goto(resolve('/(app)/artid/details/[id]/preview', { id: String(artidId) }));
					}}
				/>
				<ArtidButton
					label="Condividi"
					icon="share-fill"
					fullWidth={false}
					btnStyle="primary"
					outline={true}
					disabled={(artid.visibilityState as ArtIdVisibilityType) === 'private'}
					onclick={() => (isInternalShareOpen = !isInternalShareOpen)}
				/>
				<ArtidButton
					label="Crea link"
					icon="link-45deg"
					fullWidth={false}
					btnStyle="primary"
					onclick={() => (isCreateLinkOpen = true)}
				/>
			</div>
		</div>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={handleDelete}
	message="Sei sicuro di voler cancellare '{artid.title}' ?"
	btnStyle="danger"
/>

<ArtidAddTagsModal bind:isOpen={isTagOpen} bind:artidTags {artidId} />
<ArtidAddInternalShareModal bind:isOpen={isInternalShareOpen} {artidId} />
<ArtidCreateLinkModal bind:isOpen={isCreateLinkOpen} {artidId} />

<style lang="scss">
	.artid-favourite {
		border-right: 1px solid;
	}

	.a {
		border-right: 1px solid;
	}

	.tag-container {
		width: 100%;
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.tag {
		position: relative;
		/* display: flex;
		align-items: center; */
		/* gap: 1rem; */
		/* padding-left: 1rem; */

		.tag-color {
			width: 8px;
			height: 8px;
			border-radius: 100%;
		}

		.tag-name {
			font-size: 12px;
			font-weight: bold;
		}
	}

	button:disabled {
		color: var(--artid-text-muted) !important;
		cursor: not-allowed;
	}
</style>
