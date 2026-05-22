<script lang="ts">
	import ArtidLogoWhite from '$lib/assets/artid_logo_white.svg'
	import ArtidLink from '$lib/components/ui/artid-link.svelte'
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	async function handleLogout() {
		await fetch(resolve('/logout'), { method: 'POST' });
		await goto(resolve('/login'));
	}
</script>

<nav class="w-100 bg-primary justify-content-around align-items-center position-absolute top-0 left-0 d-flex px-2 z-2">
	<div class="row w-100">
		<div class="col-4 d-flex align-items-center justify-content-start">
			<a href="{resolve('/dashboard')}" class="mh-75 h-75">
				<img src="{ArtidLogoWhite}" alt="logo_artid" class="h-100">
			</a>
		</div>
		<div class="col-4 d-flex align-items-center justify-content-center gap-4">
			<ArtidLink
				href="/artid"
				color="white"
				label="ArtID"
			/>

			<ArtidLink
				href="/resources"
				color="white"
				label="Materiali"
			/>

			<ArtidLink
				href="/shares"
				color="white"
				label="Condivisioni"
			/>
			<ArtidLink
				href="/certifications"
				color="white"
				label="Certificazioni"
			/>
			<ArtidLink
				href="/profile"
				color="white"
				label="Profilo"
			/>
		</div>
		<div class="col-4 d-flex align-items-center justify-content-end gap-2">
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
		</div>
	</div>
</nav>

<style>
	nav{
			height: var(--artid-navbar-height);
			max-height: var(--artid-navbar-height);
			min-height: var(--artid-navbar-height);
	}
</style>
