<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { api } from '$lib/api/browser-client';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import type Quill from 'quill';
	import 'quill/dist/quill.snow.css';
	import type { components } from '$lib/api/schema';

	type Profile = components['schemas']['ProfileResponse'];

	let { profile }: { profile: Profile } = $props();

	let model = $state({
		name: profile.name ?? '',
		surname: profile.surname ?? '',
		birthdate: profile.birthdate ?? '',
		birthplace: profile.birthplace ?? '',
		address: profile.address ?? '',
		biography: profile.biography ?? '',
		linkedinId: profile.linkedinId ?? '',
		profession: profile.profession ?? '',
		phone: profile.phone ?? '',
		isPublic: profile.isPublic ?? false,
		// propic = byte[] lato Spring → stringa Base64 in JSON.
		propic: profile.propic ?? '',
		internalShareEnabled: profile.internalShareEnabled ?? false
	});

	// Snapshot iniziale per il dirty-check. La biografia viene riallineata al
	// valore "canonico" di Quill dopo il caricamento (vedi $effect sotto).
	let baseline = $state({
		name: profile.name ?? '',
		surname: profile.surname ?? '',
		birthdate: profile.birthdate ?? '',
		birthplace: profile.birthplace ?? '',
		address: profile.address ?? '',
		biography: profile.biography ?? '',
		linkedinId: profile.linkedinId ?? '',
		profession: profile.profession ?? '',
		phone: profile.phone ?? '',
		isPublic: profile.isPublic ?? false,
		propic: profile.propic ?? '',
		internalShareEnabled: profile.internalShareEnabled ?? false
	});

	// "Salva" attivo solo se almeno un campo differisce dallo snapshot.
	let isDirty = $derived(
		model.name !== baseline.name ||
		model.surname !== baseline.surname ||
		model.birthdate !== baseline.birthdate ||
		model.birthplace !== baseline.birthplace ||
		model.address !== baseline.address ||
		model.biography !== baseline.biography ||
		model.linkedinId !== baseline.linkedinId ||
		model.profession !== baseline.profession ||
		model.phone !== baseline.phone ||
		model.isPublic !== baseline.isPublic ||
		model.propic !== baseline.propic ||
		model.internalShareEnabled !== baseline.internalShareEnabled
	);

	let fieldErrors = $state<Record<string, string>>({});
	let isSaving = $state(false);
	let propicInput: HTMLInputElement | null = $state(null);

	// Div che Quill trasforma in editor + istanza Quill.
	let bioContainer = $state<HTMLDivElement | undefined>(undefined);
	let quill = $state.raw<Quill | null>(null);

	const err = (field: string) => fieldErrors[field];

	// Valore iniziale della bio, catturato fuori dal grafo reattivo così l'$effect
	// dipende solo da bioContainer (e non si re-inizializza ad ogni battitura).
	const initialBiography = profile.biography ?? '';

	// Inizializza Quill quando il div è montato. Import dinamico: Quill usa
	// window/document, quindi niente SSR.
	$effect(() => {
		if (!bioContainer) {
			quill = null;
			return;
		}
		const node = bioContainer;
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
			if (initialBiography) {
				instance.clipboard.dangerouslyPasteHTML(initialBiography);
			}
			const canonical = instance.root.innerHTML;
			model.biography = canonical;
			baseline.biography = canonical;
			instance.on('text-change', () => {
				const html = instance.root.innerHTML;
				if (html !== model.biography) model.biography = html;
			});
			quill = instance;
		})();
		return () => {
			cancelled = true;
		};
	});

	// Quill su vuoto produce "<p><br></p>": lo normalizzo a undefined per il salvataggio.
	function bioForSave(): string | undefined {
		const plain = model.biography.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
		return plain ? model.biography : undefined;
	}

	// La propic arriva come Base64 grezzo: deduco il MIME dai primi byte per il data URL.
	function propicSrc(b64: string): string {
		let mime = 'image/jpeg';
		if (b64.startsWith('iVBORw0KGgo')) mime = 'image/png';
		else if (b64.startsWith('R0lGOD')) mime = 'image/gif';
		else if (b64.startsWith('UklGR')) mime = 'image/webp';
		return `data:${mime};base64,${b64}`;
	}

	// Legge un File come Base64 puro (senza prefisso "data:*;base64,") per spedirlo nel JSON.
	function fileToBase64(file: File): Promise<string> {
		return new Promise((resolve, reject) => {
			const reader = new FileReader();
			reader.onerror = () => reject(reader.error);
			reader.onload = () => {
				const result = reader.result as string;
				const comma = result.indexOf(',');
				resolve(comma >= 0 ? result.slice(comma + 1) : result);
			};
			reader.readAsDataURL(file);
		});
	}

	async function handlePropicPick(event: Event): Promise<void> {
		const input = event.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		if (!file.type.startsWith('image/')) {
			toast.error('Seleziona un file immagine');
			return;
		}
		if (file.size > 2 * 1024 * 1024) {
			toast.error('Immagine troppo grande (max 2MB)');
			return;
		}
		model.propic = await fileToBase64(file);
		input.value = ''; // consente di riselezionare lo stesso file
	}

	async function save(): Promise<void> {
		if (isSaving) return;
		isSaving = true;
		fieldErrors = {};
		try {
			const body = {
				name: model.name.trim(),
				surname: model.surname.trim(),
				birthdate: model.birthdate || undefined,
				birthplace: model.birthplace || undefined,
				address: model.address || undefined,
				biography: bioForSave(),
				linkedinId: model.linkedinId || undefined,
				profession: model.profession || undefined,
				phone: model.phone || undefined,
				isPublic: model.isPublic,
				// Rimando indietro la propic corrente così il PUT non la azzera;
				// '' (rimossa) → undefined → il backend la pulisce.
				propic: model.propic || undefined,
				internalShareEnabled: model.internalShareEnabled
			};

			// Nessun token, nessun id: same-origin → il +server.ts inietta il Bearer
			// e Spring ricava l'utente dal JWT.
			const { data, error: apiError } = await api.PUT('/api/profile', { body });

			if (!data) {
				const errs = (apiError as { errors?: Record<string, string> } | undefined)?.errors;
				if (errs) {
					fieldErrors = errs;
					toast.error('Controlla i campi evidenziati');
				} else {
					toast.error('Errore durante il salvataggio');
				}
				return;
			}

			// Il profilo persistito ora coincide col model: azzero il dirty-check.
			baseline = { ...model };
			toast.success('Profilo aggiornato');
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}
</script>

<div class="bg-artid-section h-100 mh-100 overflow-y-auto w-75 rounded-3 border border-artid-border p-4 d-flex flex-column gap-3">
	<div class="row flex-grow-1">
		<!-- Propic -->
		<div class="col-12 col-md-2 d-flex flex-column align-items-center gap-2">
			<div class="position-absolute">
				<button
					type="button"
					class="propic-btn rounded-circle border border-artid-border d-flex align-items-center justify-content-center overflow-hidden p-0"
					style="width: 8rem; height: 8rem;"
					onclick={() => propicInput?.click()}
					aria-label="Cambia foto profilo"
				>
					{#if model.propic}
						<img src={propicSrc(model.propic)} alt="Foto profilo" class="w-100 h-100 object-fit-cover" />
					{:else}
						<i class="bi bi-person fs-1 text-primary"></i>
					{/if}
				</button>

				<input
					type="file"
					accept="image/*"
					class="d-none"
					bind:this={propicInput}
					onchange={handlePropicPick}
				/>

				<div class="d-flex flex-column align-items-center">
					<button
						type="button"
						class="btn btn-link btn-sm p-0 text-decoration-none"
						onclick={() => propicInput?.click()}
					>
						{model.propic ? 'Cambia foto' : 'Carica foto'}
					</button>
					{#if model.propic}
						<button
							type="button"
							class="btn btn-link btn-sm p-0 text-decoration-none text-danger"
							onclick={() => (model.propic = '')}
						>
							Rimuovi
						</button>
					{/if}
				</div>
			</div>

		</div>

		<!-- Dati -->
		<div class="col-12 col-md-10 d-flex flex-column">
			<div class="row">
				<div class="col-12 col-md-4 p-1">
					<ArtidInput name="name" label="Nome" bind:value={model.name} error={err('name')} />
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput name="surname" label="Cognome" bind:value={model.surname} error={err('surname')} />
				</div>
				<div class="col-12 col-md-4 p-1">
					<!-- Email = identità di login: sola lettura -->
					<div class="form-floating w-100">
						<input
							type="email"
							id="email"
							class="form-control bg-artid-section rounded-1"
							value={profile.email ?? ''}
							disabled
							placeholder=""
						/>
						<label for="email" class="text-primary">Email (non modificabile)</label>
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-12 col-md-4 p-1">
					<ArtidInput type="date" name="birthdate" label="Data di nascita" bind:value={model.birthdate} error={err('birthdate')} />
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput name="birthplace" label="Luogo di nascita" bind:value={model.birthplace} error={err('birthplace')} />
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput name="profession" label="Professione" bind:value={model.profession} error={err('profession')} />
				</div>
			</div>


			<div class="row">
				<div class="col-12 col-md-6 p-1">
					<ArtidInput name="address" label="Indirizzo" bind:value={model.address} error={err('address')} />
				</div>
				<div class="col-12 col-md-6 p-1">
					<ArtidInput type="tel" name="phone" label="Telefono" bind:value={model.phone} error={err('phone')} />
				</div>
			</div>

			<div class="row">
				<div class="col-12 p-1">
					<ArtidInput name="linkedinId" label="LinkedIn" bind:value={model.linkedinId} error={err('linkedinId')} />
				</div>
			</div>

			<div class="row">
				<div class="col-12 p-1">
					<label for="biography" class="text-primary small fw-medium ps-1 mb-1 d-block">Biografia</label>
					<!-- Wrapper: Quill inserisce .ql-toolbar come sibling del container. -->
					<div class="bio-editor">
						<div id="biography" bind:this={bioContainer}></div>
					</div>
					{#if err('biography')}
						<div class="text-danger small mt-1 ms-1">{err('biography')}</div>
					{/if}
				</div>
			</div>

			<div class="row">
				<div class="col-12 col-md-6 p-1">
					<div class="form-check form-switch ms-1">
						<input
							type="checkbox"
							role="switch"
							id="isPublic"
							class="form-check-input"
							bind:checked={model.isPublic}
						/>
						<label for="isPublic" class="form-check-label">Profilo pubblico</label>
					</div>
				</div>
				<div class="col-12 col-md-6 p-1">
					<div class="form-check form-switch ms-1">
						<input
							type="checkbox"
							role="switch"
							id="internalShareEnabled"
							class="form-check-input"
							bind:checked={model.internalShareEnabled}
						/>
						<label for="internalShareEnabled" class="form-check-label">Condivisione interna</label>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Action bar -->
	<div class="d-flex align-items-center justify-content-end gap-3 border-top border-artid-border pt-3">
		<div style="width: 12rem;">
			<ArtidButton
				label={isSaving ? 'Salvataggio…' : 'Salva'}
				icon="check-lg"
				btnStyle="success"
				disabled={isSaving || !isDirty}
				onclick={save}
			/>
		</div>
	</div>
</div>

<style lang="scss">
	.propic-btn {
		background: var(--artid-section, transparent);
		cursor: pointer;
		transition: filter 0.15s ease-in-out;
	}

	.propic-btn:hover {
		filter: brightness(0.97);
	}

	// Allinea Quill (snow theme) al brand artid. :global perché il DOM lo crea Quill.
	.bio-editor :global(.ql-toolbar.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top-left-radius: 0.5rem;
		border-top-right-radius: 0.5rem;
		background-color: var(--artid-section);
	}

	.bio-editor :global(.ql-container.ql-snow) {
		border: 1px solid var(--artid-border);
		border-top: 0;
		border-bottom-left-radius: 0.5rem;
		border-bottom-right-radius: 0.5rem;
		background-color: var(--artid-section);
		font-family: var(--artid-font-sans);
		font-size: 1rem;
	}

	.bio-editor :global(.ql-editor) {
		min-height: 6rem;
	}
</style>
