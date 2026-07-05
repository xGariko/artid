<script lang="ts">
	import ArtidSidebar from '$lib/components/layout/artid-sidebar.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import type { SidebarAction, SidebarButtonGroup } from '$lib/models/sidebar-buttons';
	import { isRecent, type ArtIdFilterType } from '$lib/utilities';
	import { toast } from 'svelte-sonner';
	import type { PageData } from './$types';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import ArtidCard from '$lib/components/pages/artid/artid-card.svelte';
	import ArtidSharedCard from '$lib/components/pages/artid/artid-shared-card.svelte';

	let { data }: { data: PageData } = $props();

	// Filtro categoria attivo (sidebar). Il filtro tag è uno stato separato che si SOMMA
	// alla categoria (AND), invece di sostituirla.
	let activeFilterValue: ArtIdFilterType = $state('mine');
	let activeTagId = $state<number | null>(null);

	let isOpen = $state(false);
	let isSaving = $state(false);

	let isTagOpen = $state(false);

	let title = $state('');
	let tag = $state('');

	let tagList = $state(data.tags ?? []);

	function handleNewArtid(): void {
		isOpen = true;
		console.log('New artid');
	}

	function handleNewTag(): void {
		isTagOpen = true;
	}

	async function createNewArtid() {
		if (isSaving) return;

		const trimmed = title.trim();
		if (trimmed.length <= 0) {
			toast.error("Inserisci il nome dell'artid");
			return;
		}

		isSaving = true;
		try {
			// POST same-origin → proxy /api/artids → Spring (id_user = utente loggato).
			const { data, error: err } = await api.POST('/api/artids', {
				body: { title: trimmed }
			});

			if (err || !data?.id) {
				toast.error("Errore nella creazione dell'ArtID");
				return;
			}

			isOpen = false;
			title = '';

			// redirect al dettaglio dell'ArtID appena creato
			await goto(resolve('/(app)/artid/details/[id]', { id: String(data.id) }));
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	async function createNewTag() {
		if (isSaving) return;

		const trimmed = tag.trim();
		if (trimmed.length <= 2) {
			toast.error('Il tag deve essere lungo almeno 3 caratteri');
			return;
		}

		isSaving = true;

		try {
			const { data, response } = await api.POST('/api/tags', {
				body: { title: trimmed },
				credentials: 'include'
			});

			if (!response.ok) {
				const status = response.status;

				if (status === 409) {
					toast.error(`Il tag "${trimmed}" già esiste`);
				} else if (status === 400) {
					toast.error('Il tag non può chiamarsi in questo modo');
				} else {
					toast.error('Errore nella creazione del tag');
				}
				return;
			}

			tagList?.push(data!);

			isTagOpen = false;
			tag = '';

			toast.success('Tag creato con successo');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	// Conteggi per ciascun filtro — calcolati in un singolo pass sulle risorse.
	const filterCounts = $derived.by(() => {
		const counts = {
			mine: data.artids.length,
			sharedWithMe: data.sharedArtids.length,
			recent: data.artids.filter((artid) => isRecent(artid.lastModified)).length,
			favourite: data.artids.filter((artid) => artid.favourite).length
		};
		return counts;
	});

	// Sottoinsieme mostrato in tabella: categoria + (eventuale) tag in AND.
	const visibleArtids = $derived.by(() => {
		// "Condivisi con me" sono ArtID di altri (senza tag lato UI): niente filtro tag qui.
		if (activeFilterValue === 'sharedWithMe') return data.sharedArtids;

		let list = data.artids;
		if (activeFilterValue === 'recent')
			list = data.artids.filter((artid) => isRecent(artid.lastModified));
		else if (activeFilterValue === 'favourite')
			list = data.artids.filter((artid) => artid.favourite);

		if (activeTagId != null) {
			const tagId = activeTagId;
			list = list.filter((artid) => artid.tagIds?.includes(tagId));
		}
		return list;
	});

	// I conteggi entrano nei pulsanti sidebar — derivato così resta in sync se i counts cambiano.
	const sidebarButtonGroups: SidebarButtonGroup[] = $derived([
		{
			label: 'Raccolte',
			buttons: [
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
			tags: tagList.map((tag) => {
				return { id: tag.id!, title: tag.title!, color: tag.color! };
			})
		}
	]);

	const sidebarActions: SidebarAction[] = [
		{ label: 'Nuovo ArtID', icon: 'plus-lg', callback: handleNewArtid, type: 'button' },
		{ label: 'Nuovo Tag +', icon: '', callback: handleNewTag, type: 'tag' }
	];
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ArtidSidebar
		buttonsGroups={sidebarButtonGroups}
		bind:activeButton={activeFilterValue}
		bind:activeTag={activeTagId}
		{sidebarActions}
	/>

	<div
		class="bg-artid-section h-100 w-60 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
	>
		<div class="row">
			{#if activeFilterValue !== 'sharedWithMe'}
				{#each visibleArtids as artid (artid.id)}
					<div class="col-3">
						<ArtidCard {artid} filter={activeFilterValue} />
					</div>
				{/each}
			{:else}
				{#each data.sharedArtids as sharedArtid (sharedArtid.id)}
					<div class="col-3">
						<ArtidSharedCard {sharedArtid} />
					</div>
				{/each}
			{/if}
		</div>
	</div>
</div>

<ArtidEditorModal bind:isOpen customHeight="25">
	<div
		class="text-artid-primary fw-semibold new-artid-modal h-100 d-flex justify-content-between flex-column"
	>
		<div class="d-flex align-items-center gap-2">
			<i class="bi bi-folder2-open fs-5 text-primary"></i>
			<span>Nuovo ArtID</span>
		</div>
		<div class="my-2">
			<ArtidInput
				name="artid-title"
				label="Titolo"
				bind:value={title}
				onkeydown={(e) => e.key === 'Enter' && createNewArtid()}
			/>
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
				disabled={isSaving || title.trim().length <= 0}
				onclick={createNewArtid}
			/>
		</div>
	</div>
</ArtidEditorModal>

<ArtidEditorModal bind:isOpen={isTagOpen} customHeight="25">
	<div class="text-artid-primary fw-semibold new-artid-modal">
		<div class="d-flex align-items-center gap-2">
			<i class="bi bi-tags-fill fs-5 text-primary"></i>
			<span>Nuovo Tag</span>
		</div>
		<div class="d-flex justify-content-between flex-column h-100">
			<div class="my-2">
				<ArtidInput name="tag" label="Tag" bind:value={tag} maxlength={50} />
			</div>
			<div class="d-flex justify-content-end h-auto gap-2">
				<ArtidButton
					label="Chiudi"
					fullWidth={false}
					btnStyle="secondary"
					outline={true}
					disabled={isSaving}
					onclick={() => (isTagOpen = false)}
				/>
				<ArtidButton
					label="Crea"
					fullWidth={false}
					btnStyle="success"
					icon="check2"
					disabled={isSaving}
					onclick={createNewTag}
				/>
			</div>
		</div>
	</div>
</ArtidEditorModal>
