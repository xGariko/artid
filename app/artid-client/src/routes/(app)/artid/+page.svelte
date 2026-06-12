<script lang="ts">
	import ArtidSidebar from '$lib/components/layout/artid-sidebar.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import type { SidebarAction, SidebarButtonGroup } from '$lib/models/sidebar-buttons';
	import { isRecent } from '$lib/utilities';
	import { toast } from 'svelte-sonner';
	import type { PageData } from './$types';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	let { data }: { data: PageData } = $props();

	// Valore del filtro sidebar attivo: raccolta ("recent"|"favourite").
	let activeFilterValue = $state('all');

	let isOpen = $state(false);
	let isSaving = $state(false);

	function handleNewArtid(): void {
		isOpen = true;
		console.log('New artid');
	}

	async function createNewArtid(): Promise<void> {
		if (isSaving) return;

		if (title.length <= 0) {
			toast.error("Inserisci il nome dell'artid");
			return;
		}

		isSaving = true;
		try {
			isOpen = false;

			// redirect to artidDetails
			await goto(resolve(`/(app)/artid/details/[idArtid]`, { idArtid: '1' }));
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	// Conteggi per ciascun filtro — calcolati in un singolo pass sulle risorse.
	const filterCounts = $derived.by(() => {
		const counts = {
			all: data.artids.length,
			mine: 0,
			sharedWithMe: 0,
			recent: 0,
			favourite: 0
		};
		// for (const resource of data.resources) {
		// 	counts[resourceTypeFromMime(resource.mimeType)]++;
		// 	if (resource.favorite) counts.favourite++;
		// 	if (isRecent(resource.lastModified)) counts.recent++;
		// }
		return counts;
	});

	// Sottoinsieme di risorse mostrato in tabella, in base al filtro sidebar.
	const visibleArtids = $derived.by(() => {
		switch (activeFilterValue) {
			case 'all':
				return data.artids;
			case 'mine':
				return data.artids.filter((artid) => artid);
			case 'recent':
				return data.artids.filter((artid) => isRecent(artid.lastModified));
			case 'sharedWithMe':
				// TODO: collegare alle condivisioni quando l'endpoint sarà disponibile.
				return [];
			default:
			// {
			// 	const targetType = activeFilterValue as ResourceType;
			// 	return data.resources.filter(
			// 		(resource) => resourceTypeFromMime(resource.mimeType) === targetType,
			// 	);
			// }
		}
	});

	// I conteggi entrano nei pulsanti sidebar — derivato così resta in sync se i counts cambiano.
	const sidebarButtonGroups: SidebarButtonGroup[] = $derived([
		{
			label: 'Raccolte',
			type: 'button',
			buttons: [
				{ icon: 'folder', label: 'Tutti', value: 'all', count: filterCounts.all },
				{ icon: 'file-earmark-richtext', label: 'I miei', value: 'mine', count: filterCounts.mine },
				{
					icon: 'share',
					label: 'Condivisi con me',
					value: 'sharedWithMe',
					count: filterCounts.sharedWithMe
				},
				{ icon: 'clock', label: 'Recenti', value: 'recent', count: filterCounts.recent },
				{ icon: 'star', label: 'Preferiti', value: 'favourite', count: filterCounts.favourite }
			]
		},
		{
			label: 'Tag',
			type: 'button',
			buttons: [
				// { icon: "clock", label: "Recenti", value: "recent", count: filterCounts.recent },
				// { icon: "star", label: "Preferiti", value: "favourite", count: filterCounts.favourite },
				// { icon: "share", label: "Condivisi", value: "shared", count: filterCounts.shared },
			]
		}
	]);

	const sidebarActions: SidebarAction[] = [
		{ label: 'Nuovo ArtID', icon: 'plus', callback: handleNewArtid, type: 'button' }
	];

	let title = $state('');
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ArtidSidebar
		buttonsGroups={sidebarButtonGroups}
		bind:activeButton={activeFilterValue}
		{sidebarActions}
	/>

	<div
		class="bg-artid-section h-100 w-60 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
	>
		{#each visibleArtids as artid (artid.id)}
			<div>{artid.title}</div>
		{/each}
		<a href={resolve('/(app)/artid/details/[idArtid]', { idArtid: '1' })}>Vai a dettagli</a>
	</div>
</div>

<ArtidEditorModal bind:isOpen>
	<div class="text-artid-primary fw-semibold new-artid-modal">
		<i class="bi bi-folder2-open fs-5 text-primary"></i>
		<span>Nuovo ArtID</span>
		<div class="my-2">
			<ArtidInput name="artid-title" label="Titolo" bind:value={title} />
		</div>
		<div class="d-flex justify-content-end gap-2">
			<ArtidButton
				label="Chiudi"
				fullWidth={false}
				btnStyle="secondary"
				outline={true}
				disabled={isSaving}
				onclick={() => (isOpen = false)}
			/>
			<ArtidButton
				label="Crea"
				fullWidth={false}
				btnStyle="success"
				icon="check2"
				disabled={isSaving}
				onclick={createNewArtid}
			/>
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
	.new-artid-modal {
		width: min(22rem, 50vw);
	}
</style>
