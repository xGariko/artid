<script lang="ts">
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import { goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { dndzone } from 'svelte-dnd-action';
	import type { PageData } from './$types';

	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import { badgeColorForExtension, badgeLabelForExtension } from '$lib/utilities';
	import ArtidAddMaterialsModal from '$lib/components/pages/artid/artid-add-materials-modal.svelte';
	import type { ResourceResponse, TagResponse } from '$lib/api/types';
	import { toast } from 'svelte-sonner';
	import { api } from '$lib/api/browser-client';
	import 'quill/dist/quill.snow.css';
	import ArtidDropdown from '$lib/components/ui/artid-dropdown.svelte';
	import ArtidAddTagsModal from '$lib/components/pages/artid/artid-add-tags-modal.svelte';
	import ArtidAddInternalShareModal from '$lib/components/pages/artid/artid-add-internal-share-modal.svelte';
	import ArtidCreateLinkModal from '$lib/components/pages/artid/artid-create-link-modal.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';

	const id = $derived(page.params.id);

	let { data }: { data: PageData } = $props();

	// svelte-ignore state_referenced_locally
	let artid = $state(data.artid);

	let draggableMaterials: ResourceResponse[] = $state([]);
	let artidTags: TagResponse[] = $state([]);

	$effect(() => {
		draggableMaterials = data.materials ?? [];
		artidTags = data.tags ?? [];
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

	let selectedImage: File | null = $state(null);
	let imagePreview = $state('');

	// Preview locale durante la selezione, altrimenti la thumbnail salvata (presigned URL da data,
	// rinfrescata via invalidateAll dopo il salvataggio). Se assente, il template mostra il placeholder.
	let srcImage = $derived(selectedImage ? imagePreview : data.artid.thumbnailUrl);

	const modelArtIDTitle = data.artid.title ?? '';
	let inputTitleValue = $state(modelArtIDTitle);

	let modelDescription = $state(data.artid.description ?? '');

	// Snapshot iniziale per il dirty-check. La description viene riallineata al
	// valore "canonico" di Quill dopo il caricamento (vedi $effect sotto).
	let baselineDescription = $state(data.artid.description ?? '');

	let isTitleModified = $derived(inputTitleValue !== modelArtIDTitle);
	let isDescriptionModified = $derived(modelDescription !== baselineDescription);
	let isImageChanged = $derived(selectedImage !== null);

	// "Salva" attivo se ha modificato la description
	let isDirty = $derived(
		isDescriptionModified || (isTitleModified && inputTitleValue.length > 0) || isImageChanged
	);
	let isSaving = $state(false);

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
				placeholder: 'Descrivi il contenuto di questo ArtID…',
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

	async function handleDndFinalize(e: CustomEvent) {
		draggableMaterials = e.detail.items;
		const { trigger, id } = e.detail.info;

		if (trigger === 'droppedIntoZone') {
			// 1. Trova la posizione VECCHIA dell'elemento prima di applicare il nuovo ordine
			// Guardiamo dove si trovava nell'array stabile corrente
			const oldIndex = draggableMaterials.findIndex((m) => m.id === id);

			// 2. Aggiorna lo stato locale con il NUOVO ordine
			const updatedItems = e.detail.items as ResourceResponse[];

			// 3. Trova la NUOVA posizione dell'elemento
			const newIndex = updatedItems.findIndex((m) => m.id === id);

			// AGGIORNAMENTO STATO LOCALE
			draggableMaterials = updatedItems;

			// 4. CONTROLLO DI GUARDIA: Se la posizione non è cambiata, ci fermiamo qui!
			if (oldIndex === newIndex) {
				return;
			}

			const newRank = newIndex + 1; // Backend basato su 1

			try {
				const { response } = await api.PUT('/api/artids/{id}/resources/{resourceId}/reorder', {
					params: {
						path: { id: Number(artid.id), resourceId: id }
					},
					body: newRank,
					credentials: 'include'
				});

				if (!response.ok) {
					toast.error("Errore nel salvataggio dell'ordine.");
				} else {
					toast.success('Ordinamento aggiornato.');
				}
			} catch {
				toast.error('Errore di rete durante il riordinamento.');
			} finally {
				await invalidateAll();
			}
		}
	}

	let isOpen = $state(false);
	let isTagOpen = $state(false);
	let isInternalShareOpen = $state(false);
	let isCreateLinkOpen = $state(false);

	let isDeleting = $state(false);

	// Funzione per rimuovere il materiale dall'artid
	async function handleDeleteMaterial(materialId: number) {
		if (isDeleting) return;

		isDeleting = true;

		try {
			const response = await api.DELETE('/api/artids/{id}/resources/{resourceId}', {
				params: {
					path: {
						id: Number(id),
						resourceId: materialId
					}
				}
			});

			if (!response.error) {
				draggableMaterials = draggableMaterials.filter((m) => m.id !== materialId);
				toast.success('Materiale rimosso con successo');
			} else {
				toast.error('Errore durante la rimozione del materiale');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isDeleting = false;
		}
	}

	let showDeleteModal = $state(false);

	function askDelete() {
		showDeleteModal = true;
	}

	async function handleDelete() {
		try {
			const response = await api.DELETE('/api/artids/{id}', {
				params: { path: { id: Number(id) } }
			});

			if (!response.error) {
				toast.success('ArtId cancellato con successo');
				await goto(resolve('/(app)/artid'));
			} else {
				toast.error("Errore durante la cancellazione dell'artid");
			}
		} catch {
			toast.error('Errore di rete');
		}
		showDeleteModal = false;
	}

	async function handleFavourite() {
		const newFavouriteState = !artid.favourite;

		try {
			const response = await api.PUT('/api/artids/{id}/favourite', {
				params: { path: { id: Number(id) } },
				body: newFavouriteState
			});

			if (!response.error) {
				artid.favourite = newFavouriteState;

				const successMessage = newFavouriteState
					? 'ArtID aggiunto ai preferiti'
					: 'ArtID rimosso dai preferiti';

				toast.success(successMessage);
			} else {
				const errorMessage = newFavouriteState
					? 'Impossibile aggiungere ArtID ai preferiti'
					: 'Impossibile rimuovere ArtID dai preferiti';
				toast.error(errorMessage);
			}
		} catch {
			toast.error('Errore di rete');
		}
	}

	function handleImageSelect(event: Event) {
		const target = event.target as HTMLInputElement;
		if (target.files && target.files.length > 0) {
			selectedImage = target.files[0];
			imagePreview = URL.createObjectURL(selectedImage);
			toast.warning('Ricordati di cliccare il tasto "Salva" per aggiornare l\'immagine');
		}
	}

	async function handleUpdateDetails() {
		isSaving = true;

		const formData = new FormData();

		if (isTitleModified && inputTitleValue.length > 0) {
			formData.append('title', inputTitleValue);
		}

		if (isDescriptionModified && descriptionForSave() !== undefined) {
			formData.append('description', descriptionForSave()!);
		}

		if (isImageChanged && selectedImage) {
			formData.append('image', selectedImage);
		}

		try {
			const response = await api.PUT('/api/artids/{id}/details', {
				params: { path: { id: Number(id) } },
				body: formData
			});

			if (!response.error) {
				// La PUT risponde 204 senza body: ricarico la load per ottenere il thumbnailUrl
				// (presigned) aggiornato dal server prima di azzerare il preview locale.
				await invalidateAll();
				toast.success('Dettagli aggiornati con successo');
				selectedImage = null;
				imagePreview = '';
			} else {
				toast.error('Errore durante la modifica dei dettagli');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	//TODO da qualche parte c'è sicuramente un ENUM con i 3 valori
	type VisibilityType = 'public' | 'private' | 'unlisted';
	const visibilityValues = [
		{ label: 'Pubblico', value: 'public' as VisibilityType },
		{ label: 'Privato', value: 'private' as VisibilityType },
		{ label: 'Unlisted', value: 'unlisted' as VisibilityType }
	];

	let selectedVisibility: VisibilityType | null = $state(
		(data.artid.visibilityState as VisibilityType) ?? null
	);

	$effect(() => {
		if (selectedVisibility && selectedVisibility !== artid.visibilityState) {
			console.log(`L'utente ha scelto l'azione: ${selectedVisibility}`);
			handleUpdateVisibility(selectedVisibility);
		}
	});

	async function handleUpdateVisibility(selectedVisibility: VisibilityType) {
		try {
			const response = await api.PUT('/api/artids/{id}/visibility', {
				params: { path: { id: Number(id) } },
				body: { visibility: selectedVisibility }
			});

			if (!response.error) {
				toast.success('Visibilità aggiornata con successo');
				artid.visibilityState = selectedVisibility;
			} else {
				toast.error('Errore durante la modifica della visibilità');
			}
		} catch {
			toast.error('Errore di rete');
		}
	}

	async function handleDeleteTag(tagId: number) {
		if (isDeleting) return;

		isDeleting = true;

		try {
			const response = await api.DELETE('/api/artids/{id}/tags/{tagId}', {
				params: {
					path: {
						id: Number(id),
						tagId: tagId
					}
				}
			});

			console.log(response);

			if (!response.error) {
				artidTags = artidTags.filter((t) => t.id !== tagId);
				toast.success('Tag rimosso con successo');
			} else {
				toast.error('Errore durante la rimozione del tag');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isDeleting = false;
		}
	}
</script>

<div class="w-100 h-100 d-flex flex-column align-items-center gap-4 p-5">
	<div class="bg-artid-section mh-100 w-75 rounded-3 border border-artid-border flex-shrink-0">
		<div class="row">
			<div class="col-6 a d-flex flex-row border-artid-border">
				<button
					class="rounded-3 border-0 bg-transparent border-end border-artid-border outline-0 py-3 px-4 me-2 artid-favourite"
					aria-label="favourite"
					onclick={handleFavourite}
				>
					{#if artid.favourite}
						<i class="bi bi-star-fill text-warning fs-4"></i>
					{:else}
						<i class="bi bi-star text-artid-muted fs-4"></i>
					{/if}
				</button>
				<div class="tag-container">
					<ul class="list-unstyled mb-0 d-flex gap-2 flex-wrap">
						{#each artidTags as tag (tag.id)}
							<li
								class="tag d-flex align-items-center px-1 rounded-4 bg-artid-surface border border-artid-border"
							>
								<span class="tag-color me-1" style="background-color: #{tag.color};"></span>
								<span class="tag-name">{tag.title}</span>
								<button
									class="p-0 m-0 border-0 bg-transparent"
									onclick={() => handleDeleteTag(tag.id!)}
									aria-label="button"
								>
									<i class="bi bi-x fs-6 text-danger"></i>
								</button>
							</li>
						{/each}
					</ul>
					<button
						class="text-artid text-decoration-underline bg-transparent border-0 p-0"
						onclick={() => (isTagOpen = !isTagOpen)}
						disabled={artidTags.length >= 5}
					>
						<span>Aggiungi Tag +</span>
					</button>
				</div>
			</div>
			<div class="col-6 d-flex align-items-center justify-content-between">
				<ArtidButton
					icon="trash"
					fullWidth={false}
					btnStyle="danger"
					outline={true}
					onclick={askDelete}
				/>
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
						disabled={(artid.visibilityState as VisibilityType) === 'private'}
						onclick={() => (isInternalShareOpen = !isInternalShareOpen)}
					/>
					<ArtidButton
						label="Crea link"
						icon="link-45deg"
						fullWidth={false}
						btnStyle="primary"
						onclick={() => (isCreateLinkOpen = true)}
					/>
				</div>
			</div>
		</div>
	</div>

	<div
		class="bg-artid-section overflow-hidden w-75 rounded-3 border border-artid-border d-flex justify-content-between"
		style="min-height: 0;"
	>
		<div class="w-50 h-100 overflow-y-auto border-end border-artid-border d-flex flex-column">
			<div
				class="bg-artid-surface border-0 border-bottom border-artid-border px-3 py-2 text-artid-text fw-semibold fs-5"
			>
				Informazioni
			</div>
			<div class="rounded-3 border border-artid-border m-2 p-2 d-flex gap-4">
				<input
					type="file"
					accept="image/*"
					id="image-input"
					style="display: none;"
					onchange={handleImageSelect}
				/>
				<label for="image-input" style="cursor: pointer">
					{#if srcImage}
						<img src={srcImage} alt="" class="artid-image" />
					{:else}
						<div class="artid-image-placeholder" title="Carica un'immagine">
							<i class="bi bi-image-fill"></i>
						</div>
					{/if}
				</label>
				<div class="flex-grow-1 d-flex flex-column justify-content-between">
					<ArtidInput name="artid" label="Titolo" bind:value={inputTitleValue} />
					<div>
						<span class="fw-semibold" style="color: #565759;">Visibilità artid</span>
						<div>
							<ArtidDropdown
								btnLabel="Visibilità"
								btnStyle="secondary"
								outline={true}
								items={visibilityValues}
								bind:value={selectedVisibility}
							/>
						</div>
					</div>
				</div>
			</div>
			<div class="rounded-3 border border-artid-border m-2 p-2 d-flex flex-column gap-2">
				<div class="d-flex align-items-center gap-2 text-artid-text fw-semibold">
					<i class="bi bi-card-text text-artid"></i>
					Descrizione
				</div>
				<div class="description-editor">
					<div id="description" bind:this={descriptionContainer}></div>
				</div>
				<div class="d-flex align-items-center gap-3">
					<span class="artid-description text-artid-text-muted small flex-grow-1">
						La descrizione dell'ArtID viene mostrata all'inizio della pagina di presentazione e
						contiene le informazioni essenziali sul contenuto.
					</span>
					<div class="flex-shrink-0">
						<ArtidButton
							label="Salva"
							icon="floppy-fill"
							btnStyle="success"
							fullWidth={false}
							disabled={!isDirty || isSaving}
							onclick={handleUpdateDetails}
						/>
					</div>
				</div>
			</div>
		</div>

		<div class="w-50 d-flex flex-column overflow-hidden">
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
								disabled={filteredMaterials.length === 0}
								bind:value={searchQuery}
							/>
						</div>
					</div>

					<div class="p-2 flex-grow-1" style="overflow-y: auto; min-height: 0;">
						{#if filteredMaterials.length === 0}
							<div class="w-100 h-100 d-flex align-items-center justify-content-center">
								<span class="fw-bold fst-italic text-artid-text-muted">
									Aggiungi un materiale per iniziare.
								</span>
							</div>
						{:else}
							<ul
								class="list-group list-unstyled"
								use:dndzone={{
									items: draggableMaterials,
									flipDurationMs,
									dragDisabled: searchQuery.trim() !== '',
									dropTargetClasses: ['dndzone']
								}}
								onconsider={handleDndConsider}
								onfinalize={handleDndFinalize}
							>
								{#each filteredMaterials as material (material.id)}
									<li
										class="list-group-item d-flex justify-content-between align-items-center gap-3"
									>
										<i
											class="bi bi-grip-horizontal fs-4 text-artid-text-muted"
											style:cursor={searchQuery.trim() !== '' ? 'not-allowed' : 'grab'}
											style:opacity={searchQuery.trim() !== '' ? 0.3 : 1}
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
										<button
											class="border-0 bg-transparent"
											onclick={() => handleDeleteMaterial(material.id!)}
											aria-label="remove material"
										>
											<i class="bi bi-x fs-4 text-danger"></i>
										</button>
									</li>
								{/each}
							</ul>
						{/if}
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

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={handleDelete}
	message="Sei sicuro di voler cancellare '{artid.title}' ?"
	btnStyle="danger"
/>

<ArtidAddMaterialsModal bind:isOpen bind:artidMaterials={draggableMaterials} artidId={Number(id)} />
<ArtidAddTagsModal bind:isOpen={isTagOpen} bind:artidTags artidId={Number(id)} />
<ArtidAddInternalShareModal bind:isOpen={isInternalShareOpen} artidId={Number(id)} />
<ArtidCreateLinkModal bind:isOpen={isCreateLinkOpen} artidId={Number(id)} />

<style lang="scss">
	.artid-favourite {
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
		/* padding-left: 1rem; */

		.tag-color {
			width: 8px;
			height: 8px;
			border-radius: 100%;
		}

		.tag-name {
			font-size: 12px;
			font-weight: bold;
		}
	}

	button:disabled {
		color: var(--artid-text-muted) !important;
		cursor: not-allowed;
	}

	.a {
		border-right: 1px solid;
	}

	.artid-image {
		width: 150px;
		object-fit: contain;
		aspect-ratio: 1 / 1;
	}

	// Placeholder mostrato quando l'ArtID non ha ancora una thumbnail: stesso
	// ingombro dell'immagine, tile flat con bordo tratteggiato = "slot vuoto".
	.artid-image-placeholder {
		width: 150px;
		aspect-ratio: 1 / 1;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2px dashed var(--artid-border);
		border-radius: 0.75rem;
		background-color: var(--artid-surface);
		color: var(--artid-primary);
		transition:
			border-color 0.15s ease,
			background-color 0.15s ease,
			color 0.15s ease;

		i {
			font-size: 3rem;
			line-height: 1;
		}

		&:hover {
			border-color: var(--artid-primary);
			background-color: var(--artid-muted);
		}
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

	.description-editor {
		:global(.ql-toolbar.ql-snow) {
			border-color: var(--artid-border);
			border-top-left-radius: 0.5rem;
			border-top-right-radius: 0.5rem;
		}

		:global(.ql-container.ql-snow) {
			height: 10rem;
			border-color: var(--artid-border);
			border-bottom-left-radius: 0.5rem;
			border-bottom-right-radius: 0.5rem;
		}
	}

	:global(.dndzone) {
		outline: 2px solid var(--artid-primary) !important;
	}
</style>
