<script lang="ts">
	import ResourceTableRow from "./resource-table-row.svelte";
	import { useResourcesStore } from "./resource-store.svelte";

	const store = useResourcesStore();

	// Definizione delle colonne in array: rendere il template del thead
	// dichiarativo e tenere etichette e classi in un unico posto.
	const columns: { label: string; className?: string }[] = [
		{ label: "", className: "w-auto" }, // checkbox
		{ label: "" },                       // badge
		{ label: "Nome" },
		{ label: "Dimensioni" },
		{ label: "Creato" },
		{ label: "Modificato" },
		{ label: "Collegato" },
		{ label: "Preferito" }
	];
</script>

<div class="flex-grow-1 overflow-auto artid-scroll-primary">
	<table class="table table-hover align-middle mb-0">
		<thead>
			<tr>
				{#each columns as col, i (i)}
					<th class="position-sticky top-0 bg-artid-section text-secondary fw-normal small border-bottom {col.className ?? ''}">{col.label}</th>
				{/each}
			</tr>
		</thead>
		<tbody>
			{#each store.filtered as item (item.id)}
				<ResourceTableRow
					{item}
					selected={store.selected.has(item.id)}
					onToggleSelected={() => store.toggleSelected(item.id)}
					onToggleFavorite={() => store.toggleFavorite(item.id)}
				/>
			{:else}
				<tr>
					<td colspan={columns.length} class="text-center text-secondary py-5">
						Nessun materiale trovato.
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
