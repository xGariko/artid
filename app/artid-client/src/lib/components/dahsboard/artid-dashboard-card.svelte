<script lang="ts">
	let { label, icon, subLabel, subLabelData, customIcon = false }: {
		label: string,
		icon: string,
		customIcon?: string,
		subLabel: string,
		subLabelData: string | number,
	} = $props();
</script>
<div class="h-100 bg-artid-section border border-artid-border rounded-3 p-3 artid-dashboard-card">
	{#if customIcon}
		<img src="{icon}" alt="">
	{:else}
		<i class="bi bi-{icon}"></i>
	{/if}
	<h2 class="fw-bold">{label}</h2>
</div>


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