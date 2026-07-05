<script lang="ts">
	import { invalidateAll, goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api } from '$lib/api/browser-client';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidLogoIconWhite from '$lib/assets/artid_logo_icon_white.svg';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import { isExpired } from '$lib/utilities';

	import {
		badgeColorForExtension,
		badgeLabelForExtension,
		formatFileSize,
		formatItalianDate
	} from '$lib/utilities';
	import { toast } from '$lib/toast';
	import type { InternalShareArtIDResponse, ExternalShareArtIDResponse } from '$lib/api/types';

	let {
		shares,
		filter
	}: {
		shares: InternalShareArtIDResponse[] | ExternalShareArtIDResponse[];
		filter: 'externals' | 'internals' | 'expired';
	} = $props();

	let selectedSharesIds = $state<Set<number>>(new Set());

	let showDeleteModal = $state(false);
	let showModifyDescriptionModal = $state(false);
	let showExtendModal = $state(false);

	let isSaving = $state(false);

	// Data (yyyy-MM-dd) scelta nella modale "Prolunga scadenza".
	let newExpirationDate = $state('');

	// Bozza descrizione modificata nella modale "Modifica descrizione".
	let descriptionDraft = $state('');

	const singleSelectedShare = $derived(
		selectedSharesIds.size === 1
			? shares.find((share) => share.id === [...selectedSharesIds][0])
			: undefined
	);

	// Cambiando filtro sidebar il parent passa un nuovo array `shares`:
	// resettiamo la selezione per evitare di trattenere id non più visibili.
	$effect(() => {
		shares;
		selectedSharesIds = new Set();
	});

	const filteredShares = $derived.by(() => {
		return shares;
	});

	const areAllFilteredSelected = $derived(
		filteredShares.length > 0 &&
		filteredShares.every((share) => share.id != null && selectedSharesIds.has(share.id))
	);

	const hasSelection = $derived(selectedSharesIds.size > 0);
	const hasSingleSelection = $derived(selectedSharesIds.size == 1);

	const hasNonActiveSelection = $derived(
		shares.some(
			(share) =>
				share.id != null &&
				selectedSharesIds.has(share.id) &&
				'isActive' in share &&
				!share.isActive
		)
	);

	const hasNonExpiredSelection = $derived(
		shares.some(
			(share) =>
				share.id != null &&
				selectedSharesIds.has(share.id) &&
				'expirationDate' in share &&
				!isExpired(share.expirationDate)
		)
	);

	const hasActiveSelection = $derived(
		shares.some(
			(share) =>
				share.id != null &&
				selectedSharesIds.has(share.id) &&
				'isActive' in share &&
				share.isActive === true
		)
	);

	// Share esterno singolo selezionato: "Prolunga scadenza" è attivo solo su externals/expired.
	const selectedExternalShare = $derived(
		filter === 'externals' || filter === 'expired'
			? (singleSelectedShare as ExternalShareArtIDResponse | undefined)
			: undefined
	);

	// Converte una Date in stringa yyyy-MM-dd (ora locale) per l'input type=date.
	function toDateInputValue(date: Date): string {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		const day = String(date.getDate()).padStart(2, '0');
		return `${year}-${month}-${day}`;
	}

	// Data minima selezionabile: il giorno dopo la scadenza attuale, mai prima di domani
	// (la proroga deve spostare la scadenza in avanti e nel futuro).
	const minExpirationDate = $derived.by(() => {
		const now = new Date();
		let min = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
		// const current = selectedExternalShare?.expirationDate;
		// if (current) {
		// 	const currentDate = new Date(current);
		// 	const dayAfterCurrent = new Date(
		// 		currentDate.getFullYear(),
		// 		currentDate.getMonth(),
		// 		currentDate.getDate() + 1
		// 	);
		// 	if (dayAfterCurrent > min) min = dayAfterCurrent;
		// }
		return toDateInputValue(min);
	});

	// Le stringhe yyyy-MM-dd si confrontano lessicograficamente come date.
	const extendError = $derived(
		newExpirationDate && newExpirationDate < minExpirationDate
			? 'La nuova scadenza deve essere successiva a quella attuale.'
			: ''
	);
	const isExtendValid = $derived(!!newExpirationDate && !extendError);

	function toggleShareSelection(shareId: number | undefined): void {
		if (shareId == null) return;
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const nextSelection = new Set(selectedSharesIds);
		if (nextSelection.has(shareId)) {
			nextSelection.delete(shareId);
		} else {
			nextSelection.add(shareId);
		}
		selectedSharesIds = nextSelection;
	}

	function handleRowKeydown(event: KeyboardEvent, shareId: number | undefined): void {
		if (event.key === 'Enter' || event.key === ' ') {
			event.preventDefault();
			toggleShareSelection(shareId);
		}
	}

	function toggleAllFilteredSelection(): void {
		if (areAllFilteredSelected) {
			selectedSharesIds = new Set();
			return;
		}
		selectedSharesIds = new Set(
			filteredShares
				.map((share) => share.id)
				.filter((shareId): shareId is number => shareId != null)
		);
	}

	async function handleStatus(action: 'enable' | 'disable'): Promise<void> {
		if (isSaving) return;
		if (selectedSharesIds.size === 0) return; //anche se non serve

		isSaving = true;
		try {
			const response = await fetch('/api/shares/' + action, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(Array.from(selectedSharesIds))
			});

			if (!response.ok) {
				toast.error('Errore nell\'aggiornamento dello stato delle condivisioni');
				return;
			}

			let msg;
			switch (action) {
				case 'enable':
					msg = 'Link attivati con successo. Puoi disattivarli in qualsiasi momento.';
					break;
				case 'disable':
					msg = 'Link attivati con successo. Puoi disattivarli in qualsiasi momento.';
					break;
			}
			toast.success(msg);
			selectedSharesIds = new Set();
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	async function handleExtend(): Promise<void> {
		if (isSaving) return;
		if (!selectedExternalShare?.id || !isExtendValid) return;

		isSaving = true;
		try {
			// Scadenza a fine giornata: il link resta valido per tutto il giorno scelto.
			const expirationDate = new Date(`${newExpirationDate}T23:59:59`).toISOString();
			const response = await fetch('/api/shares/external/' + selectedExternalShare.id + '/expiration', {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ expirationDate })
			});

			if (!response.ok) {
				toast.error('Errore nella proroga della scadenza.');
				return;
			}

			toast.success('Scadenza prorogata con successo.');
			selectedSharesIds = new Set();
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			showExtendModal = false;
			isSaving = false;
		}
	}

	async function handleUpdateDescription(): Promise<void> {
		if (isSaving) return;
		if (!selectedExternalShare?.id) return;

		isSaving = true;
		try {
			const response = await fetch('/api/shares/external/' + selectedExternalShare.id + '/description', {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ description: descriptionDraft })
			});

			if (!response.ok) {
				toast.error('Errore nella modifica della descrizione.');
				return;
			}

			toast.success('Descrizione aggiornata con successo.');
			selectedSharesIds = new Set();
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			showModifyDescriptionModal = false;
			isSaving = false;
		}
	}

	// Pulsante "Apri": anteprima in-app dell'ArtID collegato alla condivisione.
	async function openPreview(shareId: number | undefined) {
		if (shareId == null) {
			toast.error('Qualcosa è andato storto.');
			return;
		}
		const response = await fetch('/api/shares/external/' + shareId + '/link');
		if (!response.ok) {
			toast.error('Errore nella generazione del link.');
			return;
		}
		const { token } = await response.json();
		goto(resolve('/s/[token]', {token: token}));
	}

	// Click sul titolo: apre la pagina di dettaglio/modifica dell'ArtID.
	function openDetails(artidId: number | undefined): void {
		if (artidId == null) {
			toast.error("L'ArtID selezionato è stato cancellato.");
			return;
		}
		goto(resolve('/(app)/artid/details/[id]', { id: String(artidId) }));
	}

	// Pulsante "Copia": genera il link pubblico della condivisione e lo copia negli appunti.
	async function copyShareLink(shareId: number | undefined): Promise<void> {
		if (shareId == null) return;
		try {
			const response = await fetch('/api/shares/external/' + shareId + '/link');
			if (!response.ok) {
				toast.error('Errore nella generazione del link.');
				return;
			}
			const { token } = await response.json();
			const url = `${window.location.origin}/s/${token}`;
			await navigator.clipboard.writeText(url);
			toast.success('Link di condivisione copiato negli appunti.');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		}
	}

	let deleteLinkMessage = $derived.by(() => {
		if (selectedSharesIds.size === 0) return '';
		if (filter == 'externals' || filter == 'expired') {
			if (selectedSharesIds.size === 1) {
				let share = singleSelectedShare as ExternalShareArtIDResponse;
				if (!isExpired(share!.expirationDate)) {
					if (share.clickCounter == 0) {
						return 'Il contenuto non è stato visualizzato ma il link è ancora valido. Eliminare?';
					} else {
						return 'Il link è ancora valido. Eliminare?';
					}
				} else {
					if (share.clickCounter == 0) {
						return (
							'Il link è scaduto ma il ' +
							'contenuto non è stato ancora visualizzato. Puoi annullare la ' +
							'cellazione e rimandare la scadenza del link con l’apposito pulsante. ' +
							'Eliminare comunque?'
						);
					} else {
						return 'Il link è scaduto. Eliminare?';
					}
				}
			} else {
				return `Eliminare i ${selectedSharesIds.size} link selezionati?`;
			}
		} else {
			return 'Sei sicuro di voler cancellare le condivisioni selezionate?';
		}
	});

	async function handleDelete(): Promise<void> {
		if (isSaving) return;
		if (selectedSharesIds.size === 0) return; //anche se non serve
		if (selectedSharesIds.size > 1 && (filter == "externals" || filter == "expired")) {
			if (hasNonExpiredSelection) {
				toast.error('Non è possibile eliminare contemporaneamente link non scaduti. Non è stato eliminato alcun link.');
				showDeleteModal = false;
				return;
			}
		}

		isSaving = true;
		let type = (filter == 'externals' || filter == 'expired') ? 'external' : 'internal';
		try {
			const response = await fetch('/api/shares/' + type, {
				method: 'DELETE',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(Array.from(selectedSharesIds))
			});

			if (!response.ok) {
				toast.error('Errore nell\'eliminazione delle condivisioni.');
				return;
			}

			toast.success(type == 'external' ? 'I link selezionati sono stati eliminati correttamente.' :
				'Le condivisioni selezionate sono state eliminate correttamente');
			selectedSharesIds = new Set();
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			showDeleteModal = false;
			isSaving = false;
		}
	}
</script>

<div
	class="bg-artid-section h-100 w-80 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
>
	<div class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border">
		<div
			class="row g-0 align-items-center px-3 py-2 sticky-header bg-artid-muted border-bottom border-artid-border text-artid-text small fw-semibold text-nowrap"
		>
			<div class="col-1">
				<input
					type="checkbox"
					class="form-check-input"
					checked={areAllFilteredSelected}
					onchange={toggleAllFilteredSelection}
					aria-label="Seleziona tutto"
				/>
			</div>
			{#if filter === 'externals' || filter === 'expired'}
				<div class="col-1">ArtID</div>
				<div class="col-1">Creazione</div>
				<div class="col-1">Scadenza</div>
				<div class="col-1" title="Numero Visualizzazioni">Numero Vis.</div>
				<div class="col-1" title="Prima Visione">Prima Vis.</div>
				<div class="col-1" title="Ultima Visione">Ultima Vis.</div>
				<div class="col-1">Stato</div>
				<div class="col-3">Descrizione</div>
				<div class="col-1 text-center">Azioni</div>
			{:else}
				<div class="col-2">ArtID</div>
				<div class="col-1">Creazione</div>
				<div class="col-2">Condiviso con</div>
			{/if}
		</div>

		{#each filteredShares as share (share.id)}
			{@const isShareSelected = share.id != null && selectedSharesIds.has(share.id)}
			{@const titleSize = filter === 'externals' || filter === 'expired' ? '1' : '2'}
			<div
				class="row g-0 align-items-center px-3 py-2 border-bottom border-artid-border share-row"
				class:selected={isShareSelected}
				role="button"
				tabindex="0"
				onclick={() => toggleShareSelection(share.id)}
				onkeydown={(event) => handleRowKeydown(event, share.id)}
			>
				<div class="col-1 d-flex align-items-center gap-2">
					<input
						type="checkbox"
						class="form-check-input flex-shrink-0"
						checked={isShareSelected}
						onchange={() => toggleShareSelection(share.id)}
						onclick={(event) => event.stopPropagation()}
						aria-label={`Seleziona ${share.title}`}
					/>
					{#if !share.filePath}
						<div class="badge-type fw-bold text-white" style:background-color="grey">
							<img src={ArtidLogoIconWhite} class="h-60 w-60" />
						</div>
					{:else}
						<img class="badge-type preview-small" src={share.filePath} />
					{/if}
				</div>
				<div
					class="col-{titleSize} pe-{titleSize} text-truncate fw-medium text-artid-text share-title"
					title={share.title}
				>
					{#if share.title}
						<a href={resolve('/(app)/artid/details/[id]', { id: String(share.idArtid) })}
						   onclick={(event) => {
								event.stopPropagation();
								event.preventDefault();
								openDetails(share.idArtid);
							}}>{share.title}</a>
					{:else}
						<a href="" onclick={(event) => {
								event.stopPropagation();
								event.preventDefault();
								openDetails(share.idArtid);
							}}
							 class="text-artid-text-muted">Non trovato</a>
					{/if}
				</div>

				<div class="col-1 text-artid-text text-nowrap">
					{formatItalianDate(share.createdAt)}
				</div>
				{#if filter === 'externals' || filter === 'expired'}
					{@const extShare = share as ExternalShareArtIDResponse}
					<div class="col-1 {isExpired(extShare.expirationDate) ? 'text-danger' : 'text-artid-text'} text-nowrap">
						{#if isExpired(extShare.expirationDate)}
							<i class="bi bi-clock-history fs-6"></i>
						{/if}
						{formatItalianDate(extShare.expirationDate)}
					</div>
					<div class="col-1 text-artid-text text-nowrap d-flex justify-content-center">{extShare.clickCounter}</div>
					<div class="col-1 text-artid-text text-nowrap">
						{formatItalianDate(extShare.firstOpened)}
					</div>
					<div class="col-1 text-artid-text text-nowrap">
						{formatItalianDate(extShare.lastOpened)}
					</div>
					<div class="col-1 text-artid-text text-nowrap d-flex gap-2 flex-column">
						<div>
							<span
								class="rounded-circle {extShare.isActive
									? 'bg-success'
									: 'bg-warning'} circle-small"
							>
							</span>
							{extShare.isActive ? 'Attivo' : 'Disattivo'}
						</div>
					</div>
					<div class="col-3 text-artid-text">
						{extShare.description}
					</div>
					<div class="col-1 d-flex align-items-center justify-content-center gap-3">
						<button
							class="btn btn-link p-0 text-artid-text"
							title="Apri anteprima"
							onclick={(event) => {
								event.stopPropagation();
								openPreview(extShare.id);
							}}
						>
							<i class="bi bi-box-arrow-up-right fs-6"></i>
						</button>
						<button
							class="btn btn-link p-0 text-artid-text"
							title="Copia link"
							onclick={(event) => {
								event.stopPropagation();
								copyShareLink(extShare.id);
							}}
						>
							<i class="bi bi-copy fs-6"></i>
						</button>
					</div>
				{:else}
					{@const intShare = share as InternalShareArtIDResponse}
					<div class="col-2 text-artid-text text-nowrap">{intShare.recipientMail}</div>
				{/if}
			</div>
		{/each}

		{#if filteredShares.length === 0}
			<div class="text-center text-artid-text-muted py-5">
				<i class="bi bi-folder2-open fs-2 d-block mb-2"></i>
				Nessuna condivisione trovata
			</div>
		{/if}
	</div>

	<div class="d-flex align-items-center gap-2">
		{#if filter === 'externals' || filter === 'expired'}
			<ArtidButton
				label="Prolunga scadenza"
				icon="hourglass"
				disabled={!hasSingleSelection}
				fullWidth={false}
				onclick={() => {
					newExpirationDate = '';
					showExtendModal = true;
				}}
			/>
			<ArtidButton
				label="Attiva"
				icon="arrow-clockwise"
				outline={true}
				btnStyle="success"
				disabled={!hasNonActiveSelection}
				fullWidth={false}
				onclick={() => {
					handleStatus('enable');
				}}
			/>
			<ArtidButton
				label="Disattiva"
				icon="ban"
				btnStyle="warning"
				outline={true}
				disabled={!hasActiveSelection}
				fullWidth={false}
				onclick={() => {
					handleStatus('disable');
				}}
			/>
			<ArtidButton
				label="Modifica descrizione"
				icon="pencil-square"
				btnStyle="secondary"
				disabled={!hasSingleSelection}
				fullWidth={false}
				onclick={() => {
					descriptionDraft = selectedExternalShare?.description ?? '';
					showModifyDescriptionModal = true;
				}}
			/>
		{/if}
		<ArtidButton
			icon="trash"
			btnStyle="danger"
			outline={true}
			disabled={!hasSelection}
			fullWidth={false}
			ariaLabel="Elimina selezionati"
			onclick={() => {
				if(selectedExternalShare && isExpired(selectedExternalShare.expirationDate) && selectedExternalShare.clickCounter! > 0) {
						handleDelete();
				} else {
						showDeleteModal = true;
				}
			}}
		/>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={handleDelete}
	message={deleteLinkMessage}
	btnStyle="danger"
/>

<ArtidEditorModal bind:isOpen={showExtendModal} customHeight="35">
	<div class="d-flex flex-column align-items-start justify-content-between w-100 h-100 flex-fill">
		<div class="text-artid-primary fw-semibold w-100 d-flex align-items-center gap-2">
			<i class="bi bi-hourglass-split text-primary"></i>
			<span>Prolunga scadenza</span>
		</div>

		<div class="w-100 d-flex flex-column gap-3">
			{#if selectedExternalShare}
				<span class="text-artid-text-muted">
					Scadenza attuale:
					<span class="fw-semibold">{formatItalianDate(selectedExternalShare.expirationDate)}</span>
				</span>
			{/if}

			<ArtidInput
				type="date"
				name="newExpirationDate"
				label="Nuova scadenza"
				bind:value={newExpirationDate}
				min={minExpirationDate}
				error={extendError}
			/>
		</div>

		<div class="w-100">
			<div class="d-flex justify-content-end gap-2">
				<ArtidButton
					label="Chiudi"
					fullWidth={false}
					btnStyle="secondary"
					outline={true}
					disabled={false}
					onclick={() => (showExtendModal = false)}
				/>
				<ArtidButton
					label="Prolunga"
					fullWidth={false}
					btnStyle="success"
					icon="check2"
					disabled={!isExtendValid || isSaving}
					onclick={handleExtend}
				/>
			</div>
		</div>
	</div>
</ArtidEditorModal>

<ArtidEditorModal bind:isOpen={showModifyDescriptionModal} customHeight="40">
	<div class="d-flex flex-column align-items-start justify-content-between w-100 h-100 flex-fill">
		<div class="text-artid-primary fw-semibold w-100 d-flex align-items-center gap-2">
			<i class="bi bi-pencil text-primary"></i>
			<span>Inserisci la descrizione</span>
		</div>

		<div class="w-100 mb-5 d-flex flex-column justify-content-between">
			<div class="my-2">
				<textarea name="description" placeholder="Descrizione" class="w-100 form-control" style="resize: none;" rows="8"
					bind:value={descriptionDraft}
				></textarea>
			</div>

			<div class="w-100 bottom-0">
				<div class="d-flex justify-content-end gap-2">
					<ArtidButton
						label="Chiudi"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						disabled={false}
						onclick={() => (showModifyDescriptionModal = false)}
					/>
					<ArtidButton
						label="Salva"
						fullWidth={false}
						btnStyle="success"
						icon="check2"
						disabled={isSaving}
						onclick={handleUpdateDescription}
					/>
				</div>
			</div>
		</div>
	</div>
</ArtidEditorModal>

<style>
    .preview-small {
        aspect-ratio: 1 !important;
        object-fit: cover !important;
    }

    .circle-small {
        width: 10px;
        height: 10px;
        display: inline-block;
    }

    .artid-list {
        min-width: 60rem;
    }

    /*.search-input {*/
    /*    border-color: var(--artid-border);*/
    /*}*/

    /*.search-input:focus {*/
    /*    border-color: var(--artid-primary);*/
    /*    box-shadow: 0 0 0 0.2rem var(--artid-primary-subtle);*/
    /*}*/

    /* Header sticky: resta sopra le righe ma sotto le modali (z-index basso). */
    .sticky-header {
        position: sticky;
        top: 0;
        z-index: 1;
    }

    /* Righe cliccabili con feedback hover (sostituisce .table-hover). */
    .share-row {
        transition: 0.2s ease all;
        cursor: pointer;
    }

    .share-row:hover {
        transition: 0.2s ease all;
        background-color: var(--artid-surface);
    }

    /* Riga selezionata: tinta brand, evidenziata anche in hover. */
    .share-row.selected {
        transition: 0.2s ease all;
        box-shadow: inset 6px 0px 0px -3px var(--artid-primary);
    }

    /* Consente al titolo di troncare con ellissi dentro la colonna flex. */
    .share-title {
        min-width: 0;
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
