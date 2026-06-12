<script lang="ts">
	import type { Pathname } from '$app/types';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ArtidLogoWhite from '$lib/assets/artid_logo_white.svg';
	import ArtidLink from '$lib/components/ui/artid-link.svelte';

	// Presigned URL della foto profilo (o null): l'<img> punta diretto a Supabase.
	let { propicUrl }: { propicUrl?: string | null } = $props();

	const navLinks: { href: Pathname; label: string }[] = [
		{ href: '/artid', label: 'ArtID' },
		{ href: '/resources', label: 'Materiali' },
		{ href: '/shares', label: 'Condivisioni' },
		{ href: '/certifications', label: 'Certificazioni' },
		{ href: '/profile', label: 'Profilo' }
	];

	async function handleLogout() {
		await fetch(resolve('/logout'), { method: 'POST' });
		await goto(resolve('/login'));
	}

	// Stesso match per prefisso usato da ArtidLink, replicato per i dropdown-item.
	function isLinkActive(href: Pathname): boolean {
		const resolvedHref = resolve(href);
		return page.url.pathname === resolvedHref || page.url.pathname.startsWith(resolvedHref + '/');
	}

</script>

<nav class="w-100 bg-primary justify-content-around align-items-center position-absolute top-0 left-0 d-flex px-2 z-2">
	<div class="row w-100">
		<div class="col-6 col-lg-4 d-flex align-items-center justify-content-start">
			<a href="{resolve('/dashboard')}" class="mh-75 h-75">
				<img src="{ArtidLogoWhite}" alt="logo_artid" class="h-100">
			</a>
		</div>
		<div class="col-lg-4 d-none d-lg-flex align-items-center justify-content-center gap-4">
			{#each navLinks as link (link.href)}
				<ArtidLink href={link.href} color="white" label={link.label} />
			{/each}
		</div>
		<div class="col-6 col-lg-4 d-flex align-items-center justify-content-end gap-2">
			<button class="btn btn-light rounded-pill text-primary fs-5" title="notifications">
				<i class="bi bi-bell-fill"></i>
			</button>

			<div class="dropdown">
				<button
					type="button"
					class="btn btn-light rounded-pill text-primary fs-5"
					title="profile"
					data-bs-toggle="dropdown"
					data-bs-display="static"
					aria-expanded="false"
				>

					{#if propicUrl}
						<img src={propicUrl} alt="Foto profilo" class="rounded-circle object-fit-cover d-block" style="width: 1.5rem; height: 1.5rem;" />
					{:else}
						<i class="bi bi-person-fill"></i>
					{/if}
				</button>

				<ul class="dropdown-menu dropdown-menu-end shadow-sm mt-2">
					<li>
						<a class="dropdown-item" href={resolve('/profile')}>
							<i class="bi bi-person me-2"></i>Profilo
						</a>
					</li>
					<li>
						<button type="button" class="dropdown-item" onclick={handleLogout}>
							<i class="bi bi-box-arrow-right me-2"></i>Logout
						</button>
					</li>
				</ul>
			</div>

			<div class="dropdown d-lg-none">
				<button
					type="button"
					class="btn btn-light rounded-pill text-primary fs-5"
					title="menu"
					aria-label="Apri menu"
					data-bs-toggle="dropdown"
					data-bs-display="static"
					aria-expanded="false"
				>
					<i class="bi bi-list"></i>
				</button>

				<ul class="dropdown-menu dropdown-menu-end shadow-sm mt-2">
					{#each navLinks as link (link.href)}
						<li>
							<a
								class="dropdown-item"
								class:active={isLinkActive(link.href)}
								href={resolve(link.href)}
							>
								{link.label}
							</a>
						</li>
					{/each}
				</ul>
			</div>
		</div>
	</div>
</nav>

<style>
    nav {
        height: var(--artid-navbar-height);
        max-height: var(--artid-navbar-height);
        min-height: var(--artid-navbar-height);
    }
</style>
