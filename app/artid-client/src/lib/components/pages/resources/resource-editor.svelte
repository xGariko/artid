<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import type Quill from 'quill';
	import 'quill/dist/quill.snow.css';
	import type { components } from '$lib/api/schema';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	type ResourceResponse = components['schemas']['ResourceResponse'];
	type ArtidResponse = components['schemas']['ArtidResponse'];

	let {
		isOpen = $bindable(),
		resource = undefined,
		artids = []
	}: {
		isOpen: boolean;
		resource?: ResourceResponse;
		artids?: ArtidResponse[];
	} = $props();

	const isEditMode = $derived(resource != null);

	let title = $state('');
	let description = $state('');
	let favorite = $state(false);
	let selectedArtidId = $state<string>('');
	let selectedFile = $state<File | null>(null);
	let fileInput: HTMLInputElement | null = $state(null);
	let isDragging = $state(false);
	let isSaving = $state(false);

	// $state perché bind:this scrive qui — il form vive dentro {#if isOpen} del modal,
	// quindi quillContainer viene assegnato (a un nuovo div) ad ogni apertura e undefined a chiusura.
	let quillContainer = $state<HTMLDivElement | undefined>(undefined);
	let quill = $state.raw<Quill | null>(null);

	// Risincronizza il form quando la modale (ri)apre o cambia la risorsa target.
	$effect(() => {
		if (!isOpen) return;
		title = resource?.title ?? '';
		description = resource?.description ?? '';
		favorite = resource?.favorite ?? false;
		selectedArtidId = '';
		selectedFile = null;
	});

	// (Re)inizializza Quill ad ogni apertura del modal: l'$effect reagisce al cambio di quillContainer
	// (undefined → div al mount del form, div → undefined alla chiusura). onMount NON va bene qui,
	// perché parte una sola volta al mount di resource-editor, quando il container non esiste ancora.
	// Quill è caricato dinamicamente perché richiede window/document (no SSR).
	$effect(() => {
		if (!quillContainer) {
			quill = null;
			return;
		}
		const node = quillContainer;
		let cancelled = false;
		(async () => {
			const { default: QuillCtor } = await import('quill');
			if (cancelled) return;
			const instance = new QuillCtor(node, {
				theme: 'snow',
				placeholder: 'Descrivi la risorsa…',
				modules: {
					toolbar: [
						[{ header: [1, 2, 3, false] }],
						['bold', 'italic', 'underline', 'strike'],
						[{ list: 'ordered' }, { list: 'bullet' }],
						['link', 'clean']
					]
				}
			});
			instance.on('text-change', () => {
				const html = instance.root.innerHTML;
				if (html !== description) description = html;
			});
			quill = instance;
		})();
		return () => {
			cancelled = true;
		};
	});

	// Sincronizza description → Quill quando description cambia dall'esterno (es. apertura in edit).
	$effect(() => {
		if (!quill) return;
		const target = description || '';
		if (quill.root.innerHTML !== target) {
			quill.root.innerHTML = target;
		}
	});

	function handleFilePick(event: Event): void {
		const input = event.currentTarget as HTMLInputElement;
		selectedFile = input.files?.[0] ?? null;
	}

	function handleDragOver(event: DragEvent): void {
		event.preventDefault();
		isDragging = true;
	}

	function handleDragLeave(): void {
		isDragging = false;
	}

	function handleDrop(event: DragEvent): void {
		event.preventDefault();
		isDragging = false;
		selectedFile = event.dataTransfer?.files?.[0] ?? null;
	}

	async function handleSubmit(): Promise<void> {
		if (isSaving) return;

		if (!isEditMode && !selectedFile) {
			toast.error('Carica un file per creare un nuovo materiale');
			return;
		}

		isSaving = true;
		try {
			// multipart/form-data: i metadati sono campi semplici, il file è la part `file`.
			// Niente più base64: il browser invia i byte grezzi al proxy SvelteKit, che li
			// inoltra a Spring → S3. Usiamo fetch nativo perché openapi-fetch serializza in JSON.
			const formData = new FormData();
			formData.append('title', title);
			formData.append('description', description ?? '');
			formData.append('favorite', String(favorite));
			if (selectedArtidId) formData.append('artidId', selectedArtidId);
			if (selectedFile) formData.append('file', selectedFile);

			const url = isEditMode ? `/api/resources/${resource!.id}` : '/api/resources';
			const response = await fetch(url, {
				method: isEditMode ? 'PUT' : 'POST',
				body: formData
			});

			if (!response.ok) {
				toast.error(isEditMode ? 'Errore nell\'aggiornamento del materiale' : 'Errore nella creazione del materiale');
				return;
			}

			toast.success(isEditMode ? 'Materiale aggiornato' : 'Materiale creato');
			isOpen = false;
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}
</script>

<ArtidEditorModal bind:isOpen>
	<div class="d-flex flex-column gap-4 resource-editor pt-1">
		<div class="d-flex align-items-center justify-content-between pe-5">
			<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
				<i class="bi bi-folder2-open fs-5 text-primary"></i>
				<span>{isEditMode ? 'Modifica materiale' : 'Nuovo materiale'}</span>
			</div>
			<button
				type="button"
				class="btn btn-link p-0 d-flex align-items-center gap-2 text-artid-text-muted text-decoration-none"
				onclick={() => (favorite = !favorite)}
				aria-pressed={favorite}
			>
				<i class="bi bi-star{favorite ? '-fill text-warning' : ''} fs-5"></i>
				<span class="text-artid-dark">Aggiungi ai preferiti</span>
			</button>
		</div>

		<div class="row g-3">
			<div class="col-12 col-md-6">
				<ArtidInput name="resource-title" label="Titolo" bind:value={title}/>
			</div>
			<div class="col-12 col-md-6">
				<!-- Span wrapper: select[disabled] non firea hover events, il title vive sullo span. -->
				<span
					class="d-inline-block w-100"
					title={artids.length === 0 ? 'Nessun ArtID creato — creane uno prima di collegare un materiale' : null}
				>
					<select
						id="resource-artid"
						class="form-select bg-artid-section rounded-1 px-3 artid-select w-100"
						class:text-artid-text-muted={!selectedArtidId}
						bind:value={selectedArtidId}
						disabled={artids.length === 0}
					>
						<option value="" disabled>Aggiungi a un ArtID</option>
						{#each artids as artid (artid.id)}
							<option value={String(artid.id)}>{artid.title}</option>
						{/each}
					</select>
				</span>
			</div>
		</div>

		<div class="d-flex flex-column gap-1">
			<label for="resource-description" class="text-primary small fw-medium ps-1">Descrizione</label>
			<!-- Wrapper necessario: Quill inserisce .ql-toolbar come SIBLING (non figlio) del container,
				 quindi serve un parent comune per scopare gli override CSS. -->
			<div class="quill-editor">
				<div id="resource-description" bind:this={quillContainer}></div>
			</div>
		</div>

		<button
			type="button"
			class="dropzone d-flex align-items-center justify-content-between p-4 rounded-3 bg-artid-section text-start w-100"
			class:is-dragging={isDragging}
			onclick={() => fileInput?.click()}
			ondragover={handleDragOver}
			ondragleave={handleDragLeave}
			ondrop={handleDrop}
		>
			<div class="d-flex flex-column gap-1">
				<span class="fw-bold text-artid-text">
					{selectedFile?.name ?? 'Trascina qui il tuo file'}
				</span>
				<span class="text-artid-primary text-decoration-underline small">
					O clicca qui per sceglierlo dal tuo dispositivo
				</span>
			</div>
			<i class="bi bi-upload text-artid-primary fs-3"></i>
		</button>
		<input
			type="file"
			class="d-none"
			bind:this={fileInput}
			onchange={handleFilePick}
		/>

		<div class="d-flex justify-content-end gap-2">
			<ArtidButton
				label="Chiudi"
				btnStyle="secondary"
				outline={true}
				fullWidth={false}
				disabled={isSaving}
				onclick={() => (isOpen = false)}
			/>
			<ArtidButton
				label={isSaving ? 'Salvataggio…' : 'Salva'}
				icon="check2"
				btnStyle="success"
				fullWidth={false}
				disabled={isSaving || title.length === 0 || !selectedFile}
				onclick={handleSubmit}
			/>
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
	.resource-editor {
		width: min(48rem, 92vw);
	}

	.artid-select {
		height: 3.5rem;
	}

	.dropzone {
		border: 2px dashed var(--artid-primary);
		transition: background-color 0.15s ease-in-out;
	}

	.dropzone.is-dragging {
		background-color: var(--artid-primary-subtle) !important;
	}

	// Allinea Quill (snow theme) al brand artid. :global perché il DOM è creato da Quill.
	.quill-editor :global(.ql-toolbar.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top-left-radius: 0.5rem;
		border-top-right-radius: 0.5rem;
		background-color: var(--artid-section);
	}

	.quill-editor :global(.ql-container.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top: 0;
		border-bottom-left-radius: 0.5rem;
		border-bottom-right-radius: 0.5rem;
		background-color: var(--artid-section);
		font-family: var(--artid-font-sans);
		font-size: 1rem;
	}

	.quill-editor :global(.ql-editor) {
		min-height: 8rem;
	}
</style>
