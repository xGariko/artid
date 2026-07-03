<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidLogoIconWhite from '$lib/assets/artid_logo_Icon_white.svg';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';

	import { badgeColorForExtension, badgeLabelForExtension, formatFileSize, formatItalianDate } from '$lib/utilities';
	import { toast } from 'svelte-sonner';
	import type { InternalShareResponse, ExternalShareArtIDResponse } from '$lib/api/types';

	let {
		shares,
		filter
	}: {
		shares: InternalShareResponse[] | ExternalShareArtIDResponse[];
		filter: "externals" | "internals" | "expired";
	} = $props();

	let selectedSharesIds = $state<Set<number>>(new Set());

	let showDeleteModal = $state(false);

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
		filteredShares.every(
			(share) => share.id != null && selectedSharesIds.has(share.id)
		)
	);

	const hasSelection = $derived(selectedSharesIds.size > 0);
	const hasSingleSelection = $derived(selectedSharesIds.size == 1);

	const hasNonActiveSelection = $derived(
		shares.some((share) =>
			share.id != null &&
			selectedSharesIds.has(share.id) &&
			'isActive' in share &&
			!share.isActive
		)
	);

	const hasActiveSelection = $derived(
		shares.some((share) =>
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
	</script>

<div class="bg-artid-section h-100 w-80 rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list">
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
			<div class="col-1">ArtID</div>
			{#if filter === 'externals' || filter === 'expired'}
				<div class="col-1">Creazione</div>
				<div class="col-1">Scadenza</div>
				<div class="col-1" title="Numero Visualizzazioni">Numero Vis.</div>
				<div class="col-1" title="Prima Visione">Prima Vis.</div>
				<div class="col-1" title="Ultima Visione">Ultima Vis.</div>
				<div class="col-1">Stato</div>
				<div class="col-3">Descrizione</div>
				<div class="col-1 text-center">Azioni</div>
			{:else}
				<div class="col-2">Condiviso con</div>
			{/if}
		</div>

		{#each filteredShares as share (share.id)}
			{@const isShareSelected = share.id != null && selectedSharesIds.has(share.id)}

			{#if filter === 'externals' || filter === 'expired'}
				{@const extShare = share as ExternalShareArtIDResponse}
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
							aria-label={`Seleziona ${extShare.title}`}
						/>
						{#if !extShare.file_path}
							<div
								class="badge-type fw-bold text-white"
								style:background-color="grey"
							> <img src="{ArtidLogoIconWhite}" class="h-60 w-60"> </div>
						{:else}
							<img class="badge-type" src="{extShare.file_path}">
						{/if}
					</div>
					<div class="col-1 pe-1 text-truncate fw-medium text-artid-text share-title" title={extShare.title}>
						{#if extShare.title}
							<a href="">{extShare.title}</a>
						{:else}
							<a href="" class="text-artid-text-muted ms-1">Non trovato</a>
						{/if}
					</div>
					<div class="col-1 text-artid-text text-nowrap">{formatItalianDate(extShare.createdAt)}</div>
					<div class="col-1 text-artid-text text-nowrap">{formatItalianDate(extShare.expirationDate)}</div>
					<div class="col-1 text-artid-text text-nowrap">{extShare.clickCounter}</div>
					<div class="col-1 text-artid-text text-nowrap">{formatItalianDate(extShare.firstOpened)}</div>
					<div class="col-1 text-artid-text text-nowrap">{formatItalianDate(extShare.lastOpened)}</div>
					<div class="col-1 text-artid-text text-nowrap d-flex align-items-center gap-2">
						<span
							class="rounded-circle {extShare.isActive ? 'bg-success' : 'bg-warning'}"
							style="width: 10px; height: 10px; display: inline-block;">
						</span>
						{extShare.isActive ? "Attivo" : "Disattivo"}
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
				</div>
				{/if}
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
				onclick={()=>{return;}}
			/>
			<ArtidButton
				label="Attiva"
				icon="arrow-clockwise"
				outline={true}
				btnStyle="success"
				disabled={!hasNonActiveSelection}
				fullWidth={false}
				onclick={()=>{return;}}
			/>
			<ArtidButton
				label="Disattiva"
				icon="ban"
				btnStyle="warning"
				outline={true}
				disabled={!hasActiveSelection}
				fullWidth={false}
				onclick={()=>{return;}}
			/>
			<ArtidButton
				label="Modifica descrizione"
				icon="pencil-square"
				btnStyle="secondary"
				disabled={!hasSingleSelection}
				fullWidth={false}
				onclick={()=>{return;}}
			/>
		{/if}
		<ArtidButton
			icon="trash"
			btnStyle="danger"
			outline={true}
			disabled={!hasSelection}
			fullWidth={false}
			ariaLabel="Elimina selezionati"
			onclick={() => { showDeleteModal = true; }}
		/>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={()=>{return;}}
	message="Una volta eliminata la risorsa non sarà recuperabile."
	btnStyle="danger"
/>


<style>
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