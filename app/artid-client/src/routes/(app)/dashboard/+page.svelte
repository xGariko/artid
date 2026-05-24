<script lang="ts">
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { user } from '$lib/stores/auth';
	import ArtidLogoOutlinePrimary from '$lib/assets/artid_logo_outline_primary.svg';
	import ArtidDashboardCard from '$lib/components/pages/dahsboard/artid-dashboard-card.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();

	// Stagger di entrata: ogni card entra ~80ms dopo la precedente,
	// in ordine di lettura (in alto-a-sinistra → in basso-a-destra).
	let mounted = $state(false);
	onMount(() => {
		mounted = true;
	});

	const baseTransition = { y: 24, duration: 450, easing: cubicOut };
	const STEP = 80;
</script>

<div class="h-100 d-flex flex-column align-items-center justify-content-center">
	<div class="w-50 h-65 d-flex flex-column justify-content-start">
		{#if mounted}
			<h1
				class="mb-3 text-start"
				in:fly={{ y: -24, duration: 450, easing: cubicOut }}
			>
				Bentornato/a, <span class="fw-bolder">{$user?.name}</span>
			</h1>
		{/if}

		{#if mounted}
			<div class="row g-3 mb-auto h-100">
				<div class="col-12 col-lg-8" in:fly={{ ...baseTransition, delay: 0 * STEP }}>
					<ArtidDashboardCard
						label="ArtID"
						customIcon={ArtidLogoOutlinePrimary}
						subLabel="ArtID creati"
						subLabelData={data.artidCount}
						destinationRoute="/artid"
					/>
				</div>
				<div class="col-12 col-lg-4" in:fly={{ ...baseTransition, delay: 1 * STEP }}>
					<ArtidDashboardCard
						icon="folder"
						label="Materiali"
						subLabel="Materiali caricati"
						subLabelData={data.resourceCount}
						destinationRoute="/resources"
					/>
				</div>
			</div>

			<div class="row g-3 h-100 mt-auto">
				<div class="col-12 col-lg-4" in:fly={{ ...baseTransition, delay: 2 * STEP }}>
					<ArtidDashboardCard
						icon="patch-check"
						label="Certificazioni"
						subLabel="Certificazioni caricate"
						subLabelData={data.certificationCount}
						destinationRoute="/certifications"
					/>
				</div>
				<div class="col-12 col-lg-4" in:fly={{ ...baseTransition, delay: 3 * STEP }}>
					<ArtidDashboardCard
						icon="person"
						label="Profilo"
						subLabel="completato"
						subLabelData={`${data.profileCompletion}%`}
						subLabelColor="warning"
						destinationRoute="/profile"
					/>
				</div>
				<div class="col-12 col-lg-4" in:fly={{ ...baseTransition, delay: 4 * STEP }}>
					<ArtidDashboardCard
						icon="share"
						label="Condivisioni"
						subLabel="Condivisioni create"
						subLabelData={data.shareCount}
						destinationRoute="/shares"
					/>
				</div>
			</div>
		{/if}
	</div>
</div>
