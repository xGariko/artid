<script lang="ts">
	import type { Snippet } from 'svelte';

	let { sidebar, list }: { sidebar?: Snippet; list?: Snippet } = $props();
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center">
	<div class="w-75 h-85">
		<div class="w-100 h-100 d-flex gap-2">
			<div class="h-100 col-2 bg-artid-section border rounded-3 border-artid-border p-3">
				{@render sidebar?.()}
			</div>
			<div class="h-100 col-10 bg-artid-section border rounded-3 border-artid-border p-3">
				{@render list?.()}
			</div>
		</div>
	</div>
</div>