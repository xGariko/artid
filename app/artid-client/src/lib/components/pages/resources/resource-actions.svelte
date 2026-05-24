<script lang="ts">
	import { useResourcesStore } from "./resource-store.svelte";

	const store = useResourcesStore();

	// Le azioni footer dipendono solo da "c'è qualcosa di selezionato?".
	// `count` è derivato per poter mostrare un eventuale contatore in futuro
	// (es. "Scarica (3)") senza ricalcolare.
	const count = $derived(store.selected.size);
	const disabled = $derived(count === 0);

	// Handler placeholder: la pagina è puro frontend mock, qui si limita
	// a stampare e azzerare la selezione per simulare il completamento.
	function handleDownload() {
		console.info("[resources] download", [...store.selected]);
	}
	function handleEdit() {
		console.info("[resources] edit", [...store.selected]);
	}
	function handleDelete() {
		console.info("[resources] delete", [...store.selected]);
		store.clearSelected();
	}
</script>

<div class="d-flex align-items-center gap-2">
	<button
		type="button"
		class="btn btn-primary rounded-1 px-3 fw-semibold"
		{disabled}
		onclick={handleDownload}
	>
		<i class="bi bi-download me-1" aria-hidden="true"></i>
		Scarica
	</button>

	<button
		type="button"
		class="btn btn-outline-primary rounded-1 px-3 fw-semibold"
		{disabled}
		onclick={handleEdit}
	>
		<i class="bi bi-pencil-square me-1" aria-hidden="true"></i>
		Modifica
	</button>

	<button
		type="button"
		class="btn btn-outline-danger rounded-1 px-2"
		{disabled}
		onclick={handleDelete}
		aria-label="Elimina selezionati"
	>
		<i class="bi bi-trash" aria-hidden="true"></i>
	</button>
</div>
