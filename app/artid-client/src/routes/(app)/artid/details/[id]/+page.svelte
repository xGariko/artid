<script lang="ts">
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { dndzone } from 'svelte-dnd-action';
	import type { PageData } from './$types';

	import artidimage from '$lib/assets/artid_logo_outline_primary.svg';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import { badgeColorForExtension, badgeLabelForExtension } from '$lib/utilities';
	import ArtidAddMaterialsModal from '$lib/components/pages/artid/artid-add-materials-modal.svelte';
	import type { ResourceResponse } from '$lib/api/types';

	const id = $derived(page.params.id);

	let { data }: { data: PageData } = $props();

	let draggableMaterials: ResourceResponse[] = $state([]);
	$effect(() => {
		draggableMaterials = data.materials ?? [];
	});

	let searchQuery = $state('');

	// Filtra i materiali in base alla query (match su titolo o nome file).
	const filteredMaterials = $derived.by(() => {
		const normalizedQuery = searchQuery.trim().toLowerCase();
		if (!normalizedQuery) return draggableMaterials;
		return draggableMaterials.filter((material) => {
			const titleMatches = (material.title ?? '').toLowerCase().includes(normalizedQuery);
			const fileNameMatches = (material.fileName ?? '').toLowerCase().includes(normalizedQuery);
			return titleMatches || fileNameMatches;
		});
	});

	let modelDescription = $state(data.artid.description ?? '');

	// Snapshot iniziale per il dirty-check. La description viene riallineata al
	// valore "canonico" di Quill dopo il caricamento (vedi $effect sotto).
	let baselineDescription = $state(data.artid.description ?? '');

	// "Salva" attivo se ha modificato la description
	let isDirty = $derived(modelDescription !== baselineDescription);

	let descriptionContainer = $state<HTMLDivElement | undefined>(undefined);

	// Valore iniziale della description, catturato fuori dal grafo reattivo così l'$effect
	// dipende solo da descriptionContainer (e non si re-inizializza ad ogni battitura).
	const initialDescription = data.artid.description ?? '';

	// Inizializza Quill quando il div è montato. Import dinamico: Quill usa
	// window/document, quindi niente SSR.
	$effect(() => {
		if (!descriptionContainer) {
			return;
		}
		const node = descriptionContainer;
		let cancelled = false;
		(async () => {
			const { default: QuillCtor } = await import('quill');
			if (cancelled) return;
			const instance = new QuillCtor(node, {
				theme: 'snow',
				placeholder: 'Scrivi qualcosa su di te…',
				modules: {
					toolbar: [
						[{ header: [1, 2, 3, false] }],
						['bold', 'italic', 'underline', 'strike'],
						[{ list: 'ordered' }, { list: 'bullet' }],
						['link', 'clean']
					]
				}
			});
			// Carica il contenuto iniziale e riallinea model + baseline alla forma
			// canonica di Quill: così all'apertura il form non risulta "modificato".
			if (initialDescription) {
				instance.clipboard.dangerouslyPasteHTML(initialDescription);
			}
			const canonical = instance.root.innerHTML;
			modelDescription = canonical;
			baselineDescription = canonical;
			instance.on('text-change', () => {
				const html = instance.root.innerHTML;
				if (html !== modelDescription) modelDescription = html;
			});
		})();
		return () => {
			cancelled = true;
		};
	});

	// Quill su vuoto produce "<p><br></p>": lo normalizzo a undefined per il salvataggio.
	function descriptionForSave(): string | undefined {
		const plain = modelDescription
			.replace(/<[^>]*>/g, '')
			.replace(/&nbsp;/g, ' ')
			.trim();
		return plain ? modelDescription : undefined;
	}

	const flipDurationMs = 300;

	function handleDndConsider(e: CustomEvent) {
		draggableMaterials = e.detail.items;
	}

	function handleDndFinalize(e: CustomEvent) {
		draggableMaterials = e.detail.items;
		// Qui puoi fare una chiamata API per salvare il nuovo ordine nel database Java!
	}

	let isOpen = $state(false);
</script>

<div class="w-100 h-100 d-flex flex-column align-items-center gap-4 p-5">
	<div class="bg-artid-section mh-100 w-75 rounded-3 border border-artid-border flex-shrink-0">
		<div class="row">
			<div class="col-6 a d-flex flex-row border-artid-border">
				<button
					class="rounded-3 border-0 bg-transparent border-end border-artid-border outline-0 py-3 px-4 me-2 artid-preferite"
					aria-label="preferite"
				>
					<i class="bi bi-star text-artid-muted fs-4"></i>
				</button>
				<div class="tag-container">
					<ul class="list-unstyled mb-0 d-flex gap-2 flex-wrap">
						{#each Array(10) as _, i (i)}
							<li class="tag d-flex align-items-center px-1 rounded-4">
								<span class="tag-color me-1"></span>
								<span class="tag-name"> Tag{i}</span>
								<i class="bi bi-x fs-6"></i>
							</li>
						{/each}
					</ul>
					<span class="text-artid text-decoration-underline" style="cursor: pointer;"
						>Aggiungi Tag +</span
					>
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
		class="bg-artid-section h-100 overflow-hidden w-75 rounded-3 border border-artid-border d-flex justify-content-between"
		style="min-height: 0;"
	>
		<div class="w-50 h-100 overflow-y-auto border-end border-artid-border">
			<div
				class="bg-artid-surface border-0 border-bottom border-artid-border px-3 py-2 text-artid-text fw-semibold fs-5"
			>
				Informazioni
			</div>
			<div class="rounded-3 border border-artid-border m-2 p-2 d-flex gap-4">
				<img src={artidimage} alt="" />
				<div class="flex-grow-1 d-flex flex-column justify-content-between">
					<ArtidInput name="artid" label="Titolo" />
					<div>
						<span class="fw-semibold" style="color: #565759;">Visibilità artid</span>
						<div>
							<ArtidButton label="test" fullWidth={false} />
						</div>
					</div>
				</div>
			</div>
			<div class="description-editor p-2">
				<div id="desciption" bind:this={descriptionContainer}></div>
			</div>
			<div class="d-flex gap-4 p-2">
				<ArtidButton
					label="Salva"
					icon="floppy-fill"
					btnStyle="success"
					fullWidth={false}
					disabled={!isDirty}
				/>
				<span class="artid-description text-artid-text-muted">
					La descrizione dell'ArtID viene mostrata all'inizio della pagina di presentazione e
					contiene le informazioni essenziali sul contenuto
				</span>
			</div>
		</div>

		<div class="w-50 h-100 d-flex flex-column overflow-hidden">
			<div
				class="bg-artid-surface border-0 border-bottom border-artid-border px-3 py-2 text-artid-text fw-semibold fs-5"
			>
				Materiali
			</div>

			<div
				class="d-flex flex-column justify-content-between align-items-end flex-grow-1 m-2 p-2"
				style="min-height: 0;"
			>
				<div
					class="flex-grow-1 w-100 d-flex flex-column rounded-3 border border-artid-border mb-2"
					style="min-height: 0;"
				>
					<div class="border-0 border-bottom border-artid-border px-2 py-3">
						<div class="position-relative">
							<i
								class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-artid"
							></i>
							<input
								type="text"
								class="form-control rounded-3 ps-5 py-2 search-input"
								placeholder="Cerca materiali"
								bind:value={searchQuery}
							/>
						</div>
					</div>

					<div class="p-2 flex-grow-1" style="overflow-y: auto; min-height: 0;">
						<ul
							class="list-group list-unstyled"
							use:dndzone={{ items: filteredMaterials, flipDurationMs }}
							onconsider={handleDndConsider}
							onfinalize={handleDndFinalize}
						>
							{#each filteredMaterials as material (material.id)}
								<li class="list-group-item d-flex justify-content-between align-items-center gap-3">
									<i class="bi bi-grip-horizontal fs-4 text-artid-text-muted" style="cursor: grab;"
									></i>
									<span
										class="badge-type fw-bold text-white"
										style:background-color={badgeColorForExtension(material.extension)}
									>
										{badgeLabelForExtension(material.extension)}
									</span>
									<span class="flex-grow-1 text-truncate">
										{material.title}
									</span>
									<button class="border-0 bg-transparent" aria-label="remove material">
										<i class="bi bi-x fs-4" style="color: red;"></i>
									</button>
								</li>
							{/each}
						</ul>
					</div>
				</div>

				<div class="w-100 d-flex justify-content-end flex-shrink-0 pt-1">
					<ArtidButton
						label="Aggiungi Materiale"
						icon="plus-lg"
						fullWidth={false}
						onclick={() => (isOpen = !isOpen)}
					/>
				</div>
			</div>
		</div>
	</div>
</div>

<ArtidAddMaterialsModal bind:isOpen bind:artidMaterials={draggableMaterials} artidId={Number(id)} />

<style lang="scss">
	.artid-preferite {
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

	// Allinea Quill (snow theme) al brand artid. :global perché il DOM lo crea Quill.
	.description-editor :global(.ql-toolbar.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top-left-radius: 0.5rem;
		border-top-right-radius: 0.5rem;
		background-color: var(--artid-section);
	}

	.description-editor :global(.ql-container.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top: 0;
		border-bottom-left-radius: 0.5rem;
		border-bottom-right-radius: 0.5rem;
		background-color: var(--artid-section);
		font-family: var(--artid-font-sans);
		font-size: 1rem;
	}

	.description-editor :global(.ql-editor) {
		min-height: 10rem;
	}

	.artid-description {
		font-size: 13px;
	}

	.badge-type {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 2.25rem;
		height: 2.25rem;
		flex-shrink: 0;
		border-radius: 0.4rem;
		font-size: 0.7rem;
		letter-spacing: 0.02em;
	}
</style>
