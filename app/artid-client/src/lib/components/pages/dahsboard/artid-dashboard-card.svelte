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
	class="h-lg-100 bg-artid-section border border-artid-border rounded-3 p-3 p-lg-4 artid-dashboard-card d-flex flex-row flex-lg-column align-items-center align-items-lg-stretch justify-content-between text-decoration-none text-reset gap-3 gap-lg-0"
	aria-roledescription="Artid dashboard card"
>

	<div class="d-flex flex-row flex-lg-column align-items-center align-items-lg-start gap-3 gap-lg-2 flex-grow-1 min-w-0">
		{#if customIcon}
			<img src="{customIcon}" class="dashboard-card-icon-img" alt="Dashboard card icon">
		{:else}
			<i class="bi bi-{icon} text-primary dashboard-card-icon"></i>
		{/if}
		<h2 class="fw-bold mt-2">{label}</h2>
	</div>


	<p class="m-0 text-end text-lg-start flex-shrink-0">
		<b class="fs-3 fs-lg-1 text-primary me-2 text-{subLabelColor}">
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

    /* Su mobile l'icona è inline accanto al label, su desktop torna grande sopra il label. */
    .dashboard-card-icon {
        font-size: 2rem;
    }

    .dashboard-card-icon-img {
        height: 2rem;
        width: auto;
    }

    /* `min-w-0` non esiste in Bootstrap 5.3 di default: serve per far funzionare text-truncate dentro un flex item. */
    .min-w-0 {
        min-width: 0;
    }

    @media (min-width: 992px) {
        .dashboard-card-icon {
            font-size: 3rem;
        }

        .dashboard-card-icon-img {
            height: 50%;
            width: auto;
        }
    }
</style>
