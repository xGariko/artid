<script lang="ts">
	import ArtidSidebar from "$lib/components/layout/artid-sidebar.svelte";
	import SharesList from "$lib/components/pages/shares/shares-list.svelte";
	import { isExpired } from '$lib/utilities';
	import type { SidebarAction, SidebarButtonGroup } from "$lib/models/sidebar-buttons";
	import type { PageData } from "./$types";
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';

	let { data }: { data: PageData } = $props();

	// Valore del filtro sidebar attivo: ("external"|"internal"|"exipred")
	let activeFilterValue: "externals" | "internals" | "expired" = $state("externals");

	// Editor condiviso tra "Nuovo" (sidebar) e "Modifica" (list): undefined = add mode.
	// let editorOpen = $state(false);
	// let editingResource = $state<ResourceResponse | undefined>(undefined);

	// function handleNewResource(): void {
	// 	editingResource = undefined;
	// 	editorOpen = true;
	// }

	// function handleEditRequest(resource: ResourceResponse): void {
	// 	editingResource = resource;
	// 	editorOpen = true;
	// }

	// Conteggi per ciascun filtro — calcolati in un singolo pass sulle risorse.
	const filterCounts = $derived.by(() => {
		const counts = {
			externals: data.externals.length,
			internals: data.internals.length,
			expired: data.externals.filter((externalShare) => isExpired(externalShare.expirationDate)).length,
		};
		return counts;
	});

	// Sottoinsieme di risorse mostrato in tabella, in base al filtro sidebar.
	const visibleShares = $derived.by(() => {
		switch (activeFilterValue) {
			case "externals":
				return data.externals;
			case "internals":
				return data.internals;
			case "expired":
				return data.externals.filter((externalShare) => isExpired(externalShare.expirationDate));
			default: {
				return data.externals;
			}
		}
	});

	// I conteggi entrano nei pulsanti sidebar — derivato così resta in sync se i counts cambiano.
	const sidebarButtonGroups: SidebarButtonGroup[] = $derived([
		{
			label: "Raccolte",
			type: "button",
			buttons: [
				{ icon: "share-fill", label: "Esterne", value: "externals", count: filterCounts.externals },
				{ icon: "person", label: "Interne", value: "internals", count: filterCounts.internals },
				{ icon: "clock-history", label: "Scadute", value: "expired", count: filterCounts.expired },
			],
		},
	]);

	const sidebarActions: SidebarAction[] = [
	];
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ArtidSidebar
		buttonsGroups={sidebarButtonGroups}
		bind:activeButton={activeFilterValue}
		{sidebarActions}
	/>
	<SharesList shares={visibleShares} filter={activeFilterValue}/>
</div>

<!--<ResourceEditor bind:isOpen={editorOpen} resource={editingResource} artids={data.artids}/>-->