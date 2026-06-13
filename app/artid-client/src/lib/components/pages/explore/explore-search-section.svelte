<script lang="ts">
	let { searchQuery } : { searchQuery: string } = $props();
</script>

<div class="bg-artid-section border border-artid-border rounded-3 w-75 h-25 p-3">
	<div class="w-100 d-flex flex-column">
		<div class="text-muted text-uppercase fw-semibold fs-4 text-center w-100">catalogo profili pubblici</div>
		<div class="w-100 text-center font-lora fs-2 fw-bold">Scopri artisti e professionisti</div>
		<div class="w-100 mt-3 d-flex align-items-center justify-content-center">
			<div class="position-relative w-70">
				<i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-primary"></i>
				<input
					type="text"
					name="search"
					class="form-control rounded-3 ps-5 py-2 search-input w-100"
					placeholder="Cerca artisti"
					bind:value={searchQuery}
				>
			</div>
		</div>
	</div>

</div>