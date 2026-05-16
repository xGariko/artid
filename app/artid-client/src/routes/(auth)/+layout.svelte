<script lang="ts">
	let { children } = $props();
</script>

<div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
	<div class="w-100" style="max-width: 480px;">
		{@render children()}
	</div>
</div>
