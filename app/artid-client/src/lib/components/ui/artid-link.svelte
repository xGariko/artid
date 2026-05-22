<script lang="ts">
	import type { Pathname } from '$app/types';
	import { resolve } from '$app/paths';

	let {
		href,
		color = "primary",
		label
	}:
		{
			href: Pathname,
			color?: string,
			label: string
		} = $props();
</script>

<a href="{resolve(href)}" class="text-{color}">{label}</a>