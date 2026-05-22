<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Pathname } from '$app/types';

	let { label, icon, subLabel, subLabelData, customIcon, subLabelColor = 'primary', destinationRoute }: {
		label: string,
		icon?: string,
		customIcon?: string,
		subLabel: string,
		subLabelData: string | number,
		subLabelColor?: string,
		destinationRoute: Pathname
	} = $props();
</script>
<a
	href="{resolve(destinationRoute)}"
	class="h-100 bg-artid-section border border-artid-border rounded-3 p-4 artid-dashboard-card d-flex flex-column justify-content-between text-decoration-none text-reset"
	aria-roledescription="Artid dashboard card"
>

	<div class="d-flex flex-column align-items-start">
		{#if customIcon}
			<img src="{customIcon}" class="h-50" alt="Dashboard card icon">
		{:else}
			<div class="d-flex align-items-center justify-content-center">
				<i class="bi bi-{icon} text-primary display-5"></i>
			</div>
		{/if}
		<h2 class="fw-bold mt-2">{label}</h2>
	</div>


	<p class="m-0">
		<b class="fs-1 text-primary me-2 text-{subLabelColor}">
			{subLabelData}
		</b>
		{subLabel}
	</p>
</a>


<style>
    .artid-dashboard-card {
        transition: all 0.2s ease-in-out;
        cursor: pointer;
    }

    .artid-dashboard-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }


</style>