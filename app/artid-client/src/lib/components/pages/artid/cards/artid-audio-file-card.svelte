<script lang="ts">
	import type { ResourceResponse } from '$lib/api/types';

	let { resource } : { resource: ResourceResponse } = $props();
</script>

AUDIO
{resource.title}