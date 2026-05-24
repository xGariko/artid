<script lang="ts">
	import ResourceActions from "./resource-actions.svelte";
	import ResourceTable from "./resource-table.svelte";
	import { useResourcesStore } from "./resource-store.svelte";

	const store = useResourcesStore();
</script>

<div class="h-100 d-flex flex-column gap-3">
	<!-- Search: input-group con icona prefisso. Non riuso ArtidInput perché
	     usa form-floating (paradigma label flottante), incompatibile con il
	     pattern "icona + placeholder" richiesto qui. -->
	<div class="input-group">
		<span class="input-group-text bg-artid-section border-end-0 text-primary">
			<i class="bi bi-search" aria-hidden="true"></i>
		</span>
		<input
			type="search"
			class="form-control border-start-0 ps-0"
			placeholder="Cerca materiali"
			aria-label="Cerca materiali"
			value={store.search}
			oninput={(e) => store.setSearch(e.currentTarget.value)}
		/>
	</div>

	<ResourceTable />

	<ResourceActions />
</div>
