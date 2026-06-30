<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import type Quill from 'quill';
	import 'quill/dist/quill.snow.css';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';

	export interface CertificationResponse {
		id?: number;
		title?: string;      // Modificato in opzionale (?) per allinearsi a +page.svelte
		description?: string;
		isPublic?: boolean;
		public?: boolean;    // Aggiunto per tollerare il mapping nativo del backend
		extension?: string;
		fileSize?: number;
		createdAt?: string;
		lastModified?: string;
	}

	let {
		isOpen = $bindable(),
		certification = undefined
	}: {
		isOpen: boolean;
		certification?: CertificationResponse;
	} = $props();

	const isEditMode = $derived(certification != null);

	let title = $state('');
	let description = $state('');
	let isPublic = $state(false);
	let selectedFile = $state<File | null>(null);
	let fileInput: HTMLInputElement | null = $state(null);
	let isDragging = $state(false);
	let isSaving = $state(false);

	let quillContainer = $state<HTMLDivElement | undefined>(undefined);
	let quill = $state.raw<Quill | null>(null);

	// Resetta i campi all'apertura del modale
	$effect(() => {
		if (!isOpen) return;
		title = certification?.title ?? '';
		description = certification?.description ?? '';
		isPublic = certification?.isPublic ?? certification?.public ?? false;
		selectedFile = null;
	});

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
				placeholder: 'Aggiungi una descrizione o delle note per questa certificazione…',
				modules: {
					toolbar: [
						[{ header: [1, 2, false] }],
						['bold', 'italic', 'underline'],
						[{ list: 'ordered' }, { list: 'bullet' }],
						['clean']
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

	async function handleSubmit(): Promise<void> {
		if (isSaving) return;

		if (!isEditMode && !selectedFile) {
			toast.error('Carica un file PDF o immagine per salvare la certificazione');
			return;
		}

		isSaving = true;
		try {
			const formData = new FormData();
			formData.append('title', title);
			formData.append('description', description ?? '');
			formData.append('isPublic', String(isPublic));
			if (selectedFile) {
				formData.append('file', selectedFile);
			}

			const url = isEditMode ? `/api/certifications/${certification!.id}` : '/api/certifications';
			const response = await fetch(url, {
				method: isEditMode ? 'PUT' : 'POST',
				body: formData
			});

			if (!response.ok) {
				toast.error(isEditMode ? "Errore nell'aggiornamento" : "Errore nella creazione");
				return;
			}

			toast.success(isEditMode ? 'Certificato aggiornato' : 'Nuovo certificato aggiunto');
			isOpen = false;
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}
</script>

<ArtidEditorModal bind:isOpen customHeight="70" customWidth="50">
	<div class="d-flex flex-column gap-4 certification-editor pt-1 w-100">
		<div class="d-flex align-items-center justify-content-between pe-5">
			<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
				<i class="bi bi-patch-check fs-5 text-primary"></i>
				<span>{isEditMode ? 'Modifica certificato' : 'Nuovo certificato'}</span>
			</div>

			<button
				type="button"
				class="btn btn-link p-0 d-flex align-items-center gap-2 text-decoration-none"
				onclick={() => (isPublic = !isPublic)}
				aria-pressed={isPublic}
			>
				<i class="bi bi-{isPublic ? 'globe text-success' : 'lock-fill text-muted'} fs-5"></i>
				<span class="text-artid-dark font-medium small">
      {isPublic ? 'Visibilità: Pubblica' : 'Visibilità: Privata'}
     </span>
			</button>
		</div>

		<div class="row g-3">
			<div class="col-12">
				<ArtidInput name="cert-title" label="Titolo dell'attestato" bind:value={title}/>
			</div>
		</div>

		<div class="d-flex flex-column gap-1">
			<label for="cert-description" class="text-primary small fw-medium ps-1">Descrizione / Note</label>
			<div class="quill-editor">
				<div id="cert-description" bind:this={quillContainer}></div>
			</div>
		</div>

		<button
			type="button"
			class="dropzone d-flex align-items-center justify-content-between p-4 rounded-3 bg-artid-section text-start w-100"
			class:is-dragging={isDragging}
			onclick={() => fileInput?.click()}
			ondragover={(e) => { e.preventDefault(); isDragging = true; }}
			ondragleave={() => isDragging = false}
			ondrop={(e) => { e.preventDefault(); isDragging = false; selectedFile = e.dataTransfer?.files?.[0] ?? null; }}
		>
			<div class="d-flex flex-column gap-1">
     <span class="fw-bold text-artid-text">
      {selectedFile?.name ?? 'Trascina qui il certificato (PDF, PNG, JPG)'}
     </span>
				<span class="text-artid-primary text-decoration-underline small">
      O clicca qui per caricarlo dal tuo dispositivo
     </span>
			</div>
			<i class="bi bi-upload text-artid-primary fs-3"></i>
		</button>
		<input
			type="file"
			class="d-none"
			bind:this={fileInput}
			onchange={handleFilePick}
			accept=".pdf,image/*"
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
				disabled={isSaving || title.trim().length === 0 || (!selectedFile && !isEditMode)}
				onclick={handleSubmit}
			/>
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
  .certification-editor {
    width: min(48rem, 92vw);
  }

  .dropzone {
    border: 2px dashed var(--artid-primary);
    transition: background-color 0.15s ease-in-out;
    cursor: pointer;
  }

  .dropzone.is-dragging {
    background-color: var(--artid-primary-subtle) !important;
  }

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