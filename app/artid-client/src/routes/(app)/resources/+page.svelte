<script lang="ts">
	import ArtidListSidebar from "$lib/components/layout/artid-list-sidebar.svelte";
	import ResourceList from "$lib/components/pages/resources/resource-list.svelte";
	import ResourceSidebar from "$lib/components/pages/resources/resource-sidebar.svelte";
	import { MOCK_RESOURCES } from "$lib/components/pages/resources/resource-mock";
	import { createResourcesStore, setResourcesStore } from "$lib/components/pages/resources/resource-store.svelte";

	// Bootstrap dello store + injection nel context: i discendenti (sidebar,
	// list, table, actions) lo consumano via useResourcesStore() — niente
	// prop drilling. Quando arriverà l'API basterà rimpiazzare MOCK_RESOURCES
	// con `data` da un eventuale +page.server.ts: nessun componente cambia.
	setResourcesStore(createResourcesStore(MOCK_RESOURCES));
</script>

<ArtidListSidebar>
	{#snippet sidebar()}
		<ResourceSidebar />
	{/snippet}

	{#snippet list()}
		<ResourceList />
	{/snippet}
</ArtidListSidebar>
