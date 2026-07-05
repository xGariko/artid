<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import { toDateInputValue } from '$lib/utilities';
	import { toast } from '$lib/toast';

	let {
		isOpen = $bindable(),
		artidId
	}: {
		isOpen: boolean;
		artidId: number;
	} = $props();

	const STEPS = [
		{ n: 1, label: 'Scadenza' },
		{ n: 2, label: 'Descrizione' },
		{ n: 3, label: 'Fatto' }
	];

	// Step corrente dello stepper (1 → 2 → 3).
	let step = $state(1);
	let isSaving = $state(false);

	// Dati raccolti nei primi due step.
	let expirationDateInput = $state('');
	let description = $state('');

	// Risultato dello step 2: link pubblico della condivisione creata.
	let shareUrl = $state('');
	let shareToken = $state('');

	// Ripristina lo stepper ad ogni (ri)apertura, così non conserva dati di una creazione precedente.
	$effect(() => {
		if (isOpen) {
			step = 1;
			isSaving = false;
			expirationDateInput = '';
			description = '';
			shareUrl = '';
		}
	});

	// Data minima selezionabile: domani (la scadenza deve essere una data futura).
	const minDate = $derived.by(() => {
		const now = new Date();
		return toDateInputValue(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1));
	});

	// Le stringhe yyyy-MM-dd si confrontano lessicograficamente come date.
	const dateError = $derived(
		expirationDateInput && expirationDateInput < minDate
			? 'La scadenza deve essere una data futura.'
			: ''
	);
	const isDateValid = $derived(!!expirationDateInput && !dateError);

	// Step 2 → conferma: crea il record di condivisione e passa allo step di riepilogo.
	async function createShare(): Promise<void> {
		if (isSaving || !isDateValid) return;

		isSaving = true;
		try {
			// Scadenza a fine giornata: il link resta valido per tutto il giorno scelto.
			const expirationDate = new Date(`${expirationDateInput}T23:59:59`).toISOString();
			const response = await fetch('/api/shares/external', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ artidId, expirationDate, description })
			});

			if (!response.ok) {
				toast.error('Errore nella creazione del link di condivisione.');
				return;
			}

			const { token } = await response.json();
			shareUrl = `${window.location.origin}/s/${token}`;
			shareToken = token;
			step = 3;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	async function copyLink(): Promise<void> {
		try {
			await navigator.clipboard.writeText(shareUrl);
			toast.success('Link di condivisione copiato negli appunti.');
		} catch {
			toast.error('Impossibile copiare il link.');
		}
	}

	// Anteprima in-app dell'ArtID collegato (come nelle azioni della lista condivisioni).
	function openPreview(): void {
		isOpen = false;
		goto(resolve('/s/[token]', {token: shareToken}));
	}
</script>

<ArtidEditorModal bind:isOpen customHeight="50" customWidth="50">
	<div class="d-flex flex-column gap-3 pt-1 h-100">
		<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
			<i class="bi bi-link-45deg fs-5 text-primary"></i>
			<span>Crea link</span>
			<span class="text-artid-text-muted fw-normal ms-auto small">Passo {step} di {STEPS.length}</span>
		</div>

		<!-- Indicatore dello stepper: pallino numerato + etichetta sotto, connettori tra i passi. -->
		<div class="d-flex align-items-start w-100 px-1 mb-1">
			{#each STEPS as s (s.n)}
				<div class="d-flex flex-column align-items-center gap-1 flex-shrink-0">
					<span class="step-circle" class:current={step === s.n} class:done={step > s.n}>
						{#if step > s.n}
							<i class="bi bi-check2"></i>
						{:else}
							{s.n}
						{/if}
					</span>
					<span class="step-label small" class:current={step === s.n}>{s.label}</span>
				</div>
				{#if s.n < STEPS.length}
					<span class="step-connector flex-grow-1" class:done={step > s.n}></span>
				{/if}
			{/each}
		</div>

		<div class="flex-grow-1 d-flex flex-column justify-content-between w-100 align-items-center">
			{#if step === 1}
				<div class="d-flex flex-column gap-2 w-50">
					<span class="text-artid-text-muted small">
						Scegli fino a quando il link di condivisione resterà valido.
					</span>
					<ArtidInput
						type="date"
						name="expirationDate"
						label="Data di scadenza"
						bind:value={expirationDateInput}
						min={minDate}
						error={dateError}
					/>
				</div>

				<div class="d-flex justify-content-between w-100 gap-2">
					<ArtidButton
						label="Chiudi"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						onclick={() => (isOpen = false)}
					/>
					<ArtidButton
						label="Avanti"
						icon="arrow-right"
						fullWidth={false}
						disabled={!isDateValid}
						onclick={() => (step = 2)}
					/>
				</div>
			{:else if step === 2}
				<div class="d-flex flex-column gap-2 w-100">
					<span class="text-artid-text-muted small">
						Aggiungi una descrizione per ricordarti a chi è destinato il link (facoltativa).
					</span>
					<textarea
						name="description"
						placeholder="Descrizione (facoltativa)"
						class="w-100 form-control bg-artid-section rounded-3"
						style="resize: none;"
						rows="6"
						bind:value={description}
					></textarea>
				</div>

				<div class="d-flex justify-content-between gap-2">
					<ArtidButton
						label="Indietro"
						icon="arrow-left"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						onclick={() => (step = 1)}
					/>
					<ArtidButton
						label={isSaving ? 'Creazione…' : 'Conferma'}
						icon="check2"
						fullWidth={false}
						btnStyle="success"
						disabled={isSaving}
						onclick={createShare}
					/>
				</div>
			{:else}
				<div class="d-flex flex-column gap-3 w-100">
					<div class="d-flex align-items-center gap-2 text-success fw-semibold">
						<i class="bi bi-check-circle-fill fs-5"></i>
						<span>Condivisione creata!</span>
					</div>
					<span class="text-artid-text-muted small">
						Copia il link e invialo a chi vuoi, oppure apri l'anteprima dell'ArtID.
					</span>
					<div class="d-flex gap-1">
						<input
							type="text"
							class="form-control rounded-3 py-2 bg-artid-section"
							readonly
							value={shareUrl}
							aria-label="Link di condivisione"
						/>
						<ArtidButton icon="copy" fullWidth={false} ariaLabel="Copia link" onclick={copyLink} />
					</div>
				</div>

				<div class="d-flex justify-content-between gap-2">
					<ArtidButton
						label="Apri anteprima"
						icon="box-arrow-up-right"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						onclick={openPreview}
					/>
					<ArtidButton label="Chiudi" fullWidth={false} onclick={() => (isOpen = false)} />
				</div>
			{/if}
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
	// Stepper flat: profondità data da colore/bordi, coerente con il resto della UI.
	.step-circle {
		width: 2rem;
		height: 2rem;
		border-radius: 50%;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border: 2px solid var(--artid-border);
		background-color: var(--artid-surface);
		color: var(--artid-text-muted);
		font-weight: 600;
	}

	// Passo corrente: pieno, massima evidenza. Passo completato: segno primary su fondo tenue.
	.step-circle.current {
		background-color: var(--artid-primary);
		border-color: var(--artid-primary);
		color: #fff;
	}

	.step-circle.done {
		background-color: var(--artid-primary-subtle);
		border-color: var(--artid-primary);
		color: var(--artid-primary);
	}

	.step-label {
		color: var(--artid-text-muted);
	}

	.step-label.current {
		color: var(--artid-primary);
		font-weight: 600;
	}

	.step-connector {
		height: 2px;
		// 1rem in alto = metà del pallino (2rem): allinea il connettore al centro dei cerchi.
		margin: 1rem 0.5rem 0;
		background-color: var(--artid-border);
	}

	.step-connector.done {
		background-color: var(--artid-primary);
	}
</style>
