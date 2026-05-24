<script lang="ts">
	import ResourceTypeBadge from "./resource-type-badge.svelte";
	import { formatBytes, formatDateIt, type ResourceListItem } from "./resource-types";

	let {
		item,
		selected,
		onToggleSelected,
		onToggleFavorite
	}: {
		item: ResourceListItem;
		selected: boolean;
		onToggleSelected: () => void;
		onToggleFavorite: () => void;
	} = $props();
</script>

<tr>
	<td class="align-middle">
		<input
			type="checkbox"
			class="form-check-input"
			checked={selected}
			onchange={onToggleSelected}
			aria-label="Seleziona {item.title}"
		/>
	</td>
	<td class="align-middle">
		<ResourceTypeBadge kind={item.kind} />
	</td>
	<td class="align-middle">{item.title}</td>
	<td class="align-middle text-secondary">{formatBytes(item.sizeBytes)}</td>
	<td class="align-middle text-secondary">{formatDateIt(item.createdAt)}</td>
	<td class="align-middle text-secondary">{formatDateIt(item.lastModified)}</td>
	<td class="align-middle">
		<span class="{item.linkedArtidCount === 0 ? 'text-secondary opacity-50' : 'text-artid-text'}">
			{item.linkedArtidCount} <span class="text-primary">ArtID</span>
		</span>
	</td>
	<td class="align-middle">
		<button
			type="button"
			class="btn btn-link p-0 border-0 {item.favorite ? 'text-warning' : 'text-secondary'}"
			onclick={onToggleFavorite}
			aria-label={item.favorite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'}
			aria-pressed={item.favorite}
		>
			<i class="bi bi-star{item.favorite ? '-fill' : ''} fs-5"></i>
		</button>
	</td>
</tr>
