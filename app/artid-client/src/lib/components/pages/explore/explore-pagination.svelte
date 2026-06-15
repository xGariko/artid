<script lang="ts">
	// Paginazione riutilizzabile: finestra scorrevole di numeri pagina centrata sulla corrente.
	// Lo stato (currentPage) resta nel genitore; qui solo rendering + callback onChange.
	let {
		currentPage,
		totalPages,
		onChange,
		class: className = ''
	}: {
		currentPage: number;
		totalPages: number;
		onChange: (page: number) => void;
		class?: string;
	} = $props();

	const MAX_PAGE_BUTTONS = 5;

	const pageNumbers = $derived.by(() => {
		if (totalPages <= MAX_PAGE_BUTTONS) {
			return Array.from({ length: totalPages }, (_, index) => index + 1);
		}
		const half = Math.floor(MAX_PAGE_BUTTONS / 2);
		const end = Math.min(totalPages, Math.max(currentPage + half, MAX_PAGE_BUTTONS));
		const start = Math.max(1, end - MAX_PAGE_BUTTONS + 1);
		return Array.from({ length: end - start + 1 }, (_, index) => start + index);
	});
</script>

{#if totalPages > 1}
	<nav class={className} aria-label="Paginazione risultati">
		<ul class="pagination justify-content-center mb-0">
			<li class="page-item" class:disabled={currentPage === 1}>
				<button
					type="button"
					class="page-link"
					aria-label="Pagina precedente"
					onclick={() => onChange(currentPage - 1)}
				>
					<i class="bi bi-chevron-left"></i>
				</button>
			</li>

			{#each pageNumbers as pageNumber (pageNumber)}
				<li class="page-item" class:active={pageNumber === currentPage}>
					<button type="button" class="page-link" onclick={() => onChange(pageNumber)}>
						{pageNumber}
					</button>
				</li>
			{/each}

			<li class="page-item" class:disabled={currentPage === totalPages}>
				<button
					type="button"
					class="page-link"
					aria-label="Pagina successiva"
					onclick={() => onChange(currentPage + 1)}
				>
					<i class="bi bi-chevron-right"></i>
				</button>
			</li>
		</ul>
	</nav>
{/if}
