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

	let { data }: { data: PageData } = $props();

	// Valore del filtro sidebar attivo: raccolta ("recent"|"favourite").
	let activeFilterValue: ArtIdFilterType = $state('mine');

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
		if (trimmed.length <= 0) {
			toast.error('Inserisci il nome del tag');
			return;
		}

		isSaving = true;

		console.log(trimmed);

		try {
			const { data, response } = await api.POST('/api/tags', {
				body: { title: trimmed },
				credentials: 'include'
			});

			console.log(data);

			if (!response.ok) {
				const status = response.status;

				if (status === 409) {
					toast.error(`Il tag "${trimmed}" esiste già!`);
				} else if (status === 400) {
					toast.error('Il nome contiene termini non consentiti.');
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
			recent: data.artids.length,
			favourite: data.artids.filter((artid) => artid.favourite).length
		};
		return counts;
	});

	// Sottoinsieme di risorse mostrato in tabella, in base al filtro sidebar.
	const visibleArtids = $derived.by(() => {
		switch (activeFilterValue) {
			case 'mine':
				return data.artids;
			case 'recent':
				return data.artids.filter((artid) => isRecent(artid.lastModified));
			case 'sharedWithMe':
				return data.sharedArtids;
			case 'favourite':
				return data.artids.filter((artid) => artid.favourite);
			default:
				return [];
		}
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
		{sidebarActions}
	/>

	<div
		class="bg-artid-section h-100 w-60 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
	>
		<div class="row">
			{#each visibleArtids as artid (artid.id)}
				<div class="col-3">
					<ArtidCard {artid} filter={activeFilterValue} />
				</div>
			{/each}
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
				disabled={isSaving}
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
				<ArtidInput name="tag" label="Tag" bind:value={tag} />
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
