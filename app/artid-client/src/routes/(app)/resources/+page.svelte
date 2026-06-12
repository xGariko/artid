<script lang="ts">
	import ArtidSidebar from "$lib/components/layout/artid-sidebar.svelte";
	import ResourceEditor from "$lib/components/pages/resources/resource-editor.svelte";
	import ResourcesList from "$lib/components/pages/resources/resources-list.svelte";
	import type { ResourceResponse } from "$lib/api/types";
	import { isRecent, resourceTypeFromMime, type ResourceType } from "$lib/utilities";
	import type { SidebarAction, SidebarButtonGroup } from "$lib/models/sidebar-buttons";
	import type { PageData } from "./$types";

	let { data }: { data: PageData } = $props();

	// Valore del filtro sidebar attivo: macro-tipo ("document"|"image"|...) o raccolta ("recent"|"favourite"|"shared").
	let activeFilterValue = $state("all");

	// Editor condiviso tra "Nuovo" (sidebar) e "Modifica" (list): undefined = add mode.
	let editorOpen = $state(false);
	let editingResource = $state<ResourceResponse | undefined>(undefined);

	function handleNewResource(): void {
		editingResource = undefined;
		editorOpen = true;
	}

	function handleEditRequest(resource: ResourceResponse): void {
		editingResource = resource;
		editorOpen = true;
	}

	// Conteggi per ciascun filtro — calcolati in un singolo pass sulle risorse.
	const filterCounts = $derived.by(() => {
		const counts = {
			all: data.resources.length,
			document: 0,
			image: 0,
			video: 0,
			audio: 0,
			recent: 0,
			favourite: 0,
			shared: 0,
		};
		for (const resource of data.resources) {
			counts[resourceTypeFromMime(resource.mimeType)]++;
			if (resource.favorite) counts.favourite++;
			if (isRecent(resource.lastModified)) counts.recent++;
		}
		return counts;
	});

	// Sottoinsieme di risorse mostrato in tabella, in base al filtro sidebar.
	const visibleResources = $derived.by(() => {
		switch (activeFilterValue) {
			case "all":
				return data.resources;
			case "favourite":
				return data.resources.filter((resource) => resource.favorite);
			case "recent":
				return data.resources.filter((resource) => isRecent(resource.lastModified));
			case "shared":
				// TODO: collegare alle condivisioni quando l'endpoint sarà disponibile.
				return [];
			default: {
				const targetType = activeFilterValue as ResourceType;
				return data.resources.filter(
					(resource) => resourceTypeFromMime(resource.mimeType) === targetType,
				);
			}
		}
	});

	// I conteggi entrano nei pulsanti sidebar — derivato così resta in sync se i counts cambiano.
	const sidebarButtonGroups: SidebarButtonGroup[] = $derived([
		{
			label: "Tipo",
			type: "button",
			buttons: [
				{ icon: "folder", label: "Tutti", value: "all", count: filterCounts.all },
				{ icon: "file-earmark-richtext", label: "Documenti", value: "document", count: filterCounts.document },
				{ icon: "images", label: "Immagini", value: "image", count: filterCounts.image },
				{ icon: "film", label: "Video", value: "video", count: filterCounts.video },
				{ icon: "music-note-list", label: "Audio", value: "audio", count: filterCounts.audio },
			],
		},
		{
			label: "Raccolte",
			type: "button",
			buttons: [
				{ icon: "clock", label: "Recenti", value: "recent", count: filterCounts.recent },
				{ icon: "star", label: "Preferiti", value: "favourite", count: filterCounts.favourite },
				{ icon: "share", label: "Condivisi", value: "shared", count: filterCounts.shared },
			],
		},
	]);

	const sidebarActions: SidebarAction[] = [
		{ label: "Nuovo", icon: "plus-lg", callback: handleNewResource, type: "button" },
	];
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ArtidSidebar
		buttonsGroups={sidebarButtonGroups}
		bind:activeButton={activeFilterValue}
		{sidebarActions}
	/>
	<ResourcesList resources={visibleResources} onEditRequest={handleEditRequest} />
</div>

<ResourceEditor bind:isOpen={editorOpen} resource={editingResource} artids={data.artids} />
