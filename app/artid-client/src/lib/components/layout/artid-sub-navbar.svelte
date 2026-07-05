<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { fly } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import ArtidLogoIconWhite from '$lib/assets/artid_logo_icon_white.svg';
	import { breadcrumb, type BreadcrumbCrumb } from '$lib/stores/breadcrumb';

	const home: BreadcrumbCrumb = {
		label: 'Home',
		href: resolve('/home'),
		icon: 'bi-grid-fill'
	};

	const sections: BreadcrumbCrumb[] = [
		{ label: 'ArtID', href: resolve('/artid'), iconSrc: ArtidLogoIconWhite },
		{ label: 'Materiali', href: resolve('/resources'), icon: 'bi-folder-fill' },
		{ label: 'Condivisioni', href: resolve('/shares'), icon: 'bi-share-fill' },
		{ label: 'Certificazioni', href: resolve('/certifications'), icon: 'bi-patch-check-fill' },
		{ label: 'Profilo', href: resolve('/profile'), icon: 'bi-person-fill' }
	];

	function matchSection(path: string): BreadcrumbCrumb | undefined {
		return sections
			.filter((s) => path === s.href || path.startsWith(s.href + '/'))
			.sort((a, b) => b.href.length - a.href.length)[0];
	}

	let currentPath  = $derived(page.url.pathname);
	let section      = $derived(matchSection(currentPath));
	// Solo i crumb che sono antenati (o uguali) del path corrente, ordinati per
	// profondità: così i crumb annidati si compongono e quelli "stantii" di route
	// sorelle non vengono mostrati durante le transizioni.
	let extras       = $derived(
		[...$breadcrumb.values()]
			.filter((c) => currentPath === c.href || currentPath.startsWith(c.href + '/'))
			.sort((a, b) => a.href.length - b.href.length)
	);
	let visibleLinks = $derived(
		section ? [home, section, ...extras] : [home, ...extras]
	);
</script>

{#if currentPath !== home.href}
	<div
		class="sub-nav w-100 bg-artid-dark position-absolute d-flex align-items-center gap-3 px-3 z-1"
		transition:fly={{ y: -35, duration: 220, easing: cubicOut }}
	>
		{#each visibleLinks as { label, href, icon, iconSrc }, i (href)}
			{#if i > 0}
				<span class="sub-nav-sep" aria-hidden="true"></span>
			{/if}
			<a
				{href}
				class="sub-nav-link text-decoration-none d-inline-flex align-items-center gap-2"
				class:active={currentPath === href}
			>
				{#if iconSrc}
					<img src={iconSrc} alt="" class="sub-nav-img" aria-hidden="true" />
				{:else if icon}
					<i class="bi {icon}" aria-hidden="true"></i>
				{/if}
				<span>{label}</span>
			</a>
		{/each}
	</div>
{/if}

<style>
    .sub-nav {
        top: var(--artid-navbar-height);
        height: calc(var(--artid-navbar-height) / 2);
    }

    .sub-nav-link {
        color: rgba(255, 255, 255, 0.85);
        font-size: 0.95rem;
        transition: color 0.15s ease;
    }

    .sub-nav-link:hover {
        color: #fff;
    }

    .sub-nav-link.active {
        color: #fff;
        font-weight: 600;
    }

    .sub-nav-sep {
        width: 2.25rem;
        height: 1px;
        background-color: rgba(255, 255, 255, 0.55);
        flex-shrink: 0;
    }

    .sub-nav-img {
        height: 1em;
        width: auto;
        display: block;
    }
</style>
