<script>
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';

	const id = $derived(page.params.id);
</script>

<div class="w-100 h-100 d-flex flex-column align-items-center gap-4 p-5">
	<div class="bg-artid-section mh-100 w-75 rounded-3 border border-artid-border">
		<div class="row">
			<div class="col-6 a d-flex flex-row border-artid-border">
				<div class="artid-title rounded-3 border-artid-border py-3 px-4 me-2">Mozart</div>
				<div class="tag-container">
					<ul class="list-unstyled mb-0 d-flex gap-2 flex-wrap">
						{#each Array(10) as _, i (i)}
							<li class="tag d-flex align-items-center px-1 rounded-4">
								<span class="tag-color me-1"></span>
								<span class="tag-name"> Tag{i}</span>
								<i class="bi bi-x text-artid-primary fs-6"></i>
							</li>
						{/each}
					</ul>
					<span>Aggiungi Tag +</span>
				</div>
			</div>
			<div class="col-6 d-flex align-items-center justify-content-between">
				<ArtidButton icon="trash" fullWidth={false} btnStyle="danger" outline={true} />
				<div style="padding-right: calc(var(--bs-gutter-x)*0.5);">
					<ArtidButton
						label="Anteprima"
						icon="eye"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						onclick={() => {
							goto(resolve('/(app)/artid/details/[id]/preview', { id: String(id) }));
						}}
					/>
					<ArtidButton
						label="Condividi"
						icon="share-fill"
						fullWidth={false}
						btnStyle="primary"
						outline={true}
					/>
					<ArtidButton label="Crea link" icon="link-45deg" fullWidth={false} btnStyle="primary" />
				</div>
			</div>
		</div>
	</div>

	<div
		class="bg-artid-section h-100 mh-100 overflow-y-auto w-75 rounded-3 border border-artid-border p-4 d-flex flex-column gap-3"
	>
		<div class="row">
			<div class="col-12">test</div>
		</div>
	</div>
</div>

<style>
	.artid-title {
		border-right: 1px solid;
	}

	.tag-container {
		width: 100%;
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.tag {
		position: relative;
		/* display: flex;
		align-items: center; */
		/* gap: 1rem; */
		background-color: rgb(0, 255, 170);
		/* padding-left: 1rem; */
		i {
			color: red;
		}

		.tag-color {
			width: 8px;
			height: 8px;
			background-color: blue;
			border-radius: 100%;
		}

		.tag-name {
			font-size: 12px;
			font-weight: bold;
		}
	}

	/* .tag::before {
		content: '';
		position: absolute;
		width: 8px;
		height: 8px;
		background-color: blue;
		left: 0;
		top: 50%;
		transform: translateY(-50%);

		border-radius: 100%;
	} */

	.a {
		border-right: 1px solid;
	}
</style>
