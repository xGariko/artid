<script lang="ts">
	import ArtidButton from "$lib/components/ui/artid-button.svelte";
	import ResourceSidebarItem from "./resource-sidebar-item.svelte";
	import { useResourcesStore } from "./resource-store.svelte";
	import type { ResourceCategory, ResourceCollection, ResourceFilter } from "./resource-types";

	const store = useResourcesStore();

	// Definizione dichiarativa: aggiungere una voce = aggiungere una riga,
	// niente modifiche al template. Il `getCount` legge i contatori live
	// dallo store, quindi resta reattivo.
	interface SidebarEntry {
		filter: ResourceFilter;
		icon: string;
		label: string;
		getCount: () => number;
	}

	const typeEntries: SidebarEntry[] = [
		{ filter: { kind: "all" },                                 icon: "folder",        label: "Tutti",     getCount: () => store.counts.all },
		{ filter: categoryFilter("document"),                      icon: "file-earmark",  label: "Documenti", getCount: () => store.counts.byCategory.document },
		{ filter: categoryFilter("image"),                         icon: "image",         label: "Immagini",  getCount: () => store.counts.byCategory.image },
		{ filter: categoryFilter("video"),                         icon: "film",          label: "Video",     getCount: () => store.counts.byCategory.video },
		{ filter: categoryFilter("audio"),                         icon: "music-note",    label: "Audio",     getCount: () => store.counts.byCategory.audio }
	];

	const collectionEntries: SidebarEntry[] = [
		{ filter: collectionFilter("recent"),   icon: "clock",   label: "Recenti",  getCount: () => store.counts.byCollection.recent },
		{ filter: collectionFilter("favorite"), icon: "star",    label: "Preferiti", getCount: () => store.counts.byCollection.favorite },
		{ filter: collectionFilter("shared"),   icon: "share",   label: "Condivisi", getCount: () => store.counts.byCollection.shared }
	];

	function categoryFilter(category: ResourceCategory): ResourceFilter {
		return { kind: "category", category };
	}
	function collectionFilter(collection: ResourceCollection): ResourceFilter {
		return { kind: "collection", collection };
	}
</script>

<div class="h-100 d-flex flex-column gap-2">
	<ArtidButton label="Nuovo" icon="plus-lg" />

	<div class="text-uppercase fw-bolder small text-secondary mt-3 mb-1 px-2">Tipo</div>
	{#each typeEntries as entry (entry.label)}
		<ResourceSidebarItem
			icon={entry.icon}
			label={entry.label}
			count={entry.getCount()}
			active={store.isActive(entry.filter)}
			onclick={() => store.setFilter(entry.filter)}
		/>
	{/each}

	<div class="text-uppercase fw-bolder small text-secondary mt-3 mb-1 px-2">Raccolte</div>
	{#each collectionEntries as entry (entry.label)}
		<ResourceSidebarItem
			icon={entry.icon}
			label={entry.label}
			count={entry.getCount()}
			active={store.isActive(entry.filter)}
			onclick={() => store.setFilter(entry.filter)}
		/>
	{/each}
</div>
