<script lang="ts">
	import { invalidateAll } from '$app/navigation';
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
	import { toast } from 'svelte-sonner';
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

	let isSaving = $state(false);

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

	const hasActiveSelection = $derived(
		shares.some(
			(share) =>
				share.id != null &&
				selectedSharesIds.has(share.id) &&
				'isActive' in share &&
				share.isActive === true
		)
	);

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
				toast.error("Errore nell'aggiornamento dello stato delle condivisioni");
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

	let deleteLinkMessage = $derived.by(() => {
		if (selectedSharesIds.size === 0) return '';
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
						'Il link è scaduto ma il' +
						'contenuto non + stato ancora visualizzato. Puoi annullare la ' +
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
	});

	async function handleExternalDelete(): Promise<void> {
		if (isSaving) return;
		if (selectedSharesIds.size === 0) return; //anche se non serve

		isSaving = true;
		try {
			const response = await fetch('/api/shares/' + 'external', {
				method: 'DELETE',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(Array.from(selectedSharesIds))
			});

			if (!response.ok) {
				toast.error("Errore nell'eliminazione delle condivisioni.");
				return;
			}

			toast.success('I link selezionati sono stati eliminati correttamente.');
			selectedSharesIds = new Set();
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
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
				<div class="col-3">ArtID</div>
				<div class="col-2">Condiviso con</div>
			{/if}
		</div>

		{#each filteredShares as share (share.id)}
			{@const isShareSelected = share.id != null && selectedSharesIds.has(share.id)}
			{@const titleSize = filter === 'externals' || filter === 'expired' ? '1' : '3'}
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
					{#if !share.file_path}
						<div class="badge-type fw-bold text-white" style:background-color="grey">
							<img src={ArtidLogoIconWhite} class="h-60 w-60" />
						</div>
					{:else}
						<img class="badge-type preview-small" src={share.file_path} />
					{/if}
				</div>
				<div
					class="col-{titleSize} pe-{titleSize} text-truncate fw-medium text-artid-text share-title"
					title={share.title}
				>
					{#if share.title}
						<a href="">{share.title}</a>
					{:else}
						<a href="" class="text-artid-text-muted ms-{titleSize}">Non trovato</a>
					{/if}
				</div>

				{#if filter === 'externals' || filter === 'expired'}
					{@const extShare = share as ExternalShareArtIDResponse}
					<div class="col-1 text-artid-text text-nowrap">
						{formatItalianDate(extShare.createdAt)}
					</div>
					<div class="col-1 text-artid-text text-nowrap">
						{formatItalianDate(extShare.expirationDate)}
					</div>
					<div class="col-1 text-artid-text text-nowrap">{extShare.clickCounter}</div>
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
						{#if isExpired(extShare.expirationDate)}
							<div>
								<span class="rounded-circle bg-danger circle-small"> </span>
								Scaduto
							</div>
						{/if}
					</div>
					<div class="col-3 text-artid-text">
						{extShare.description}
					</div>
					<div class="col-1 d-flex align-items-center justify-content-center gap-3">
						<button class="btn btn-link p-0 text-artid-text" title="Apri">
							<i class="bi bi-box-arrow-up-right fs-6"></i>
						</button>
						<button class="btn btn-link p-0 text-artid-text" title="Copia">
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
					return;
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
				showDeleteModal = true;
			}}
		/>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={handleExternalDelete}
	message={deleteLinkMessage}
	btnStyle="danger"
/>

<ArtidEditorModal bind:isOpen={showModifyDescriptionModal} customHeight="40">
	<div class="d-flex flex-column align-items-start justify-content-around w-100 h-100 flex-fill">
		<div class="text-artid-primary fw-semibold w-100">
			<i class="bi bi-pencil fs-6 text-success"></i>
			<span>Inserisci la descrizione</span>
		</div>

		<div class="w-100 mb-5">
			<div class="my-2">
				<textarea name="description" placeholder="Descrizione" class="w-100 artid-textarea" rows="8"
				></textarea>
			</div>

			<div class="w-100">
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
						disabled={false}
						onclick={() => {
							return;
						}}
					/>
				</div>
			</div>
		</div>
	</div></ArtidEditorModal
>

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

	.artid-textarea {
		padding: 10px;
		min-height: 200px;
		resize: none;
		border-radius: 5px;
	}

	.artid-textarea:focus {
		outline: none !important;
		border: 1px solid #06c !important;
		box-shadow: 0 0 3px #06c !important;
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
