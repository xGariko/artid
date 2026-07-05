<script lang="ts">
	import type { Pathname } from '$app/types';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ArtidLogoWhite from '$lib/assets/artid_logo_white.svg';
	import ArtidLink from '$lib/components/ui/artid-link.svelte';
	import { user } from '$lib/stores/auth';
	import ArtidButton from '../ui/artid-button.svelte';

	const navLinks: { href: Pathname; label: string }[] = [
		{ href: '/artid', label: 'ArtID' },
		{ href: '/resources', label: 'Materiali' },
		{ href: '/shares', label: 'Condivisioni' },
		{ href: '/certifications', label: 'Certificazioni' },
		{ href: '/profile', label: 'Profilo' }
	];

	let userLogged = $derived(!!$user);

	async function handleLogout() {
		await fetch(resolve('/logout'), { method: 'POST' });
		await goto(resolve('/welcome'));
	}

	// Stesso match per prefisso usato da ArtidLink, replicato per i dropdown-item.
	function isLinkActive(href: Pathname): boolean {
		const resolvedHref = resolve(href);
		return page.url.pathname === resolvedHref || page.url.pathname.startsWith(resolvedHref + '/');
	}

</script>

<nav class="w-100 bg-primary justify-content-around align-items-center position-fixed top-0 start-0 d-flex px-2 z-3">
	<div class="row w-100">
		<div class="col-6 col-lg-4 d-flex align-items-center justify-content-start">
			<a href="{resolve('/dashboard')}" class="mh-75 h-75">
				<img src="{ArtidLogoWhite}" alt="logo_artid" class="h-100">
			</a>
		</div>
		{#if userLogged}
			<div class="col-lg-4 d-none d-lg-flex align-items-center justify-content-center gap-4">
				{#each navLinks as link (link.href)}
					<ArtidLink href={link.href} color="white" label={link.label} />
				{/each}
			</div>
			<div class="col-6 col-lg-4 d-flex align-items-center justify-content-end gap-2">
				<div class="dropdown">


					<button
						type="button"
						class="btn btn-light rounded-pill text-primary fs-5"
						title="profile"
						data-bs-toggle="dropdown"
						data-bs-display="static"
						aria-expanded="false"
					>
						<i class="bi bi-person-fill"></i>
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
		{:else}
			<div class="col-6 col-lg-8 d-flex align-items-center justify-content-end gap-4">
				<ArtidButton
					label="Accedi"
					btnStyle="light"
					fullWidth={false}
					onclick={()=>{goto(resolve('/login'))}}
				/>
			</div>
		{/if}

	</div>
</nav>

<style>
    nav {
        height: var(--artid-navbar-height);
        max-height: var(--artid-navbar-height);
        min-height: var(--artid-navbar-height);
    }
</style>
