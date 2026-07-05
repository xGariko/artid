<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import type { ArtidResponse } from '$lib/api/types';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidDropdown from '$lib/components/ui/artid-dropdown.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import type { ArtIdVisibilityType } from '$lib/utilities';
	import { isFileWithinUploadLimit, FILE_TOO_LARGE_MESSAGE } from '$lib/utilities';
	import { toast } from '$lib/toast';
	import 'quill/dist/quill.snow.css';

	let {
		artidId,
		artid,
		isActivelyShared
	}: { artidId: number; artid: ArtidResponse; isActivelyShared: boolean } = $props();

	let showUpdateVisibilityModal = $state(false);

	// Gestione dettagli ArtID

	// Immagine
	let selectedImage: File | null = $state(null);
	let imagePreview = $state('');
	// Preview locale durante la selezione, altrimenti la thumbnail salvata (presigned URL da data,
	// rinfrescata via invalidateAll dopo il salvataggio). Se assente, il template mostra il placeholder.
	let srcImage = $derived(selectedImage ? imagePreview : artid.thumbnailUrl);
	function handleImageSelect(event: Event) {
		const target = event.target as HTMLInputElement;
		const file = target.files?.[0];
		if (!file) return;
		if (!isFileWithinUploadLimit(file)) {
			toast.error(FILE_TOO_LARGE_MESSAGE);
			target.value = ''; // consente di riselezionare (anche lo stesso file dopo averlo ridotto)
			return;
		}
		if (imagePreview) {
			URL.revokeObjectURL(imagePreview);
		}
		selectedImage = file;
		imagePreview = URL.createObjectURL(selectedImage);
		toast.warning('Ricordati di cliccare il tasto "Salva" per aggiornare l\'immagine');
	}

	// Titolo
	const modelArtIDTitle = artid.title ?? '';
	let inputTitleValue = $state(modelArtIDTitle);

	// Descrizione
	let modelDescription = $state(artid.description ?? '');
	// Snapshot iniziale per il dirty-check. La description viene riallineata al
	// valore "canonico" di Quill dopo il caricamento (vedi $effect sotto).
	let baselineDescription = $state(artid.description ?? '');

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
	const initialDescription = artid.description ?? '';

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
				params: { path: { id: artidId } },
				body: formData
			});

			if (!response.error) {
				// La PUT risponde 204 senza body: ricarico la load per ottenere il thumbnailUrl
				// (presigned) aggiornato dal server prima di azzerare il preview locale.
				await invalidateAll();
				toast.success('Informazioni aggiornate correttamente');

				if (imagePreview) {
					URL.revokeObjectURL(imagePreview);
				}
				selectedImage = null;
				imagePreview = '';
				baselineDescription = modelDescription;
			} else {
				toast.error('Errore durante la modifica dei dettagli');
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	// Gestione visibilità ArtID
	const visibilityValues = [
		{ label: 'Pubblico', value: 'public' as ArtIdVisibilityType },
		{ label: 'Privato', value: 'private' as ArtIdVisibilityType },
		{ label: 'Non in elenco', value: 'unlisted' as ArtIdVisibilityType }
	];

	let selectedVisibility: ArtIdVisibilityType | null = $state(
		(artid.visibilityState as ArtIdVisibilityType) ?? null
	);
	$effect(() => {
		if (selectedVisibility && selectedVisibility !== artid.visibilityState) {
			handleUpdateVisibility(selectedVisibility);
		}
	});

	function handleUpdateVisibility(selectedVisibility: ArtIdVisibilityType) {
		if (
			selectedVisibility === 'private' &&
			artid.visibilityState !== 'private' &&
			isActivelyShared
		) {
			showUpdateVisibilityModal = true;
		} else {
			updateVisibility(selectedVisibility);
		}
	}
	async function updateVisibility(selectedVisibility: ArtIdVisibilityType) {
		try {
			const response = await api.PUT('/api/artids/{id}/visibility', {
				params: { path: { id: artidId } },
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
		} finally {
			showUpdateVisibilityModal = false;
			await invalidateAll();
		}
	}
</script>

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
			La descrizione dell'ArtID viene mostrata all'inizio della pagina di presentazione e contiene
			le informazioni essenziali sul contenuto.
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

<ArtidModal
	bind:isOpen={showUpdateVisibilityModal}
	title="Conferma cambio visibilità"
	onConfirm={() => updateVisibility(selectedVisibility!)}
	message="Questo artid è stato già condiviso, se lo rendi privato chi ha il link non potrà più visualizzarlo. Procedere comunque?"
	btnStyle="danger"
/>

<style lang="scss">
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
</style>
