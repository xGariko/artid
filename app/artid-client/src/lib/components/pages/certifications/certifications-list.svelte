<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import {
		formatFileSize,
		formatItalianDate,
		badgeColorForExtension,
		badgeLabelForExtension
	} from '$lib/utilities';
	import { toast } from 'svelte-sonner';
	import { SvelteSet } from 'svelte/reactivity'; // Ottimizzato per Svelte 5

	export interface CertificationResponse {
		id?: number;
		title?: string;
		description?: string;
		isPublic?: boolean;
		public?: boolean;
		extension?: string;
		fileSize?: number;
		createdAt?: string;
		lastModified?: string;
	}

	let {
		certifications,
		onEditRequest,
		onNewRequest
	}: {
		certifications: CertificationResponse[];
		onEditRequest?: (certification: CertificationResponse) => void;
		onNewRequest?: () => void;
	} = $props();

	// Utilizzo di SvelteSet per far felice l'ESLint ed evitare istanze mutabili non tracciate
	let selectedIds = $state(new SvelteSet<number>());
	let showDeleteModal = $state(false);

	$effect(() => {
		selectedIds.clear();
	});

	const areAllSelected = $derived(
		certifications.length > 0 && certifications.every((c) => c.id != null && selectedIds.has(c.id))
	);

	const hasSelection = $derived(selectedIds.size > 0);

	function toggleSelection(id: number | undefined): void {
		if (id == null) return;
		if (selectedIds.has(id)) {
			selectedIds.delete(id);
		} else {
			selectedIds.add(id);
		}
	}

	function toggleAll(): void {
		if (areAllSelected) {
			selectedIds.clear();
		} else {
			for (const c of certifications) {
				if (c.id != null) selectedIds.add(c.id);
			}
		}
	}

	function downloadSelected(): void {
		for (const id of selectedIds) {
			window.open(`/api/certifications/${id}/file`, '_blank', 'noopener');
		}
	}

	function editSelected(): void {
		if (selectedIds.size !== 1) {
			toast.warning('Seleziona un solo attestato per modificarlo');
			return;
		}
		const [targetId] = selectedIds;
		const target = certifications.find((c) => c.id === targetId);
		if (target) onEditRequest?.(target);
	}

	async function deleteSelected(): Promise<void> {
		showDeleteModal = false;
		try {
			const results = await Promise.all(
				[...selectedIds].map((id) =>
					api.DELETE('/api/certifications/{id}', { params: { path: { id } } })
				)
			);
			await invalidateAll();
			if (results.some((r) => r.error)) {
				toast.error("Errore nell'eliminazione di alcuni certificati");
			} else {
				toast.success('Certificati cancellati con successo');
			}
		} catch {
			toast.error("Errore durante l'eliminazione");
		}
	}
</script>

<div
	class="bg-artid-section rounded-3 border border-artid-border p-3 d-flex flex-column gap-3 artid-list"
>
	<div class="d-flex align-items-center gap-3">
		<i class="bi bi-patch-check-fill text-artid-dark fs-3"></i>
		<h3 class="fs-5 fw-bold text-artid-dark m-0">I tuoi attestati</h3>
	</div>

	<div
		class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border table-scroll-container"
	>
		<div
			class="row g-0 align-items-center px-3 py-2 sticky-header bg-artid-muted border-bottom border-artid-border text-artid-text small fw-semibold text-nowrap"
		>
			<div class="col-1 d-flex align-items-center">
				<input
					type="checkbox"
					class="form-check-input"
					checked={areAllSelected}
					onchange={toggleAll}
					aria-label="Seleziona tutti"
				/>
			</div>
			<div class="col-4">Nome</div>
			<div class="col-2">Dimensioni</div>
			<div class="col-2">Creato</div>
			<div class="col-2">Modificato</div>
			<div class="col-1 text-center">Visibilità</div>
		</div>

		{#each certifications as cert (cert.id)}
			{@const isSelected = cert.id != null && selectedIds.has(cert.id)}
			<div
				class="row g-0 align-items-center px-3 py-2 border-bottom border-artid-border resource-row"
				class:selected={isSelected}
				role="button"
				tabindex="0"
				onclick={() => toggleSelection(cert.id)}
				onkeydown={(e) => (e.key === ' ' || e.key === 'Enter') && toggleSelection(cert.id)}
			>
				<div class="col-1 d-flex align-items-center gap-2">
					<input
						type="checkbox"
						class="form-check-input flex-shrink-0"
						checked={isSelected}
						onchange={() => toggleSelection(cert.id)}
						onclick={(e) => e.stopPropagation()}
						aria-label={`Seleziona ${cert.title}`}
					/>
					<span
						class="badge-type fw-bold text-white"
						style:background-color={badgeColorForExtension(cert.extension || 'pdf')}
					>
						{badgeLabelForExtension(cert.extension || 'pdf')}
					</span>
				</div>

				<div
					class="col-4 pe-3 text-truncate fw-medium text-artid-text certification-title"
					title={cert.title}
				>
					{cert.title ?? ''}
				</div>

				<div class="col-2 text-artid-text">{formatFileSize(cert.fileSize)}</div>

				<div class="col-2 text-artid-text text-nowrap">{formatItalianDate(cert.createdAt)}</div>

				<div class="col-2 text-artid-text text-nowrap">{formatItalianDate(cert.lastModified)}</div>

				<div class="col-1 text-center">
					{#if cert.isPublic || cert.public}
						<i class="bi bi-globe fs-5 text-artid-text-muted" title="Pubblico"></i>
					{:else}
						<i class="bi bi-lock-fill fs-5 text-artid-text-muted" title="Privato"></i>
					{/if}
				</div>
			</div>
		{/each}

		{#if certifications.length === 0}
			<div class="text-center text-artid-text-muted py-5">
				<i class="bi bi-patch-check fs-2 d-block mb-2"></i>
				Nessun attestato caricato
			</div>
		{/if}
	</div>

	<div class="d-flex align-items-center gap-2">
		<ArtidButton label="Nuovo" icon="plus-lg" fullWidth={false} onclick={onNewRequest} />
		<ArtidButton
			label="Scarica"
			icon="download"
			disabled={!hasSelection}
			fullWidth={false}
			onclick={downloadSelected}
		/>
		<ArtidButton
			label="Modifica"
			icon="pencil-square"
			outline={true}
			disabled={selectedIds.size !== 1}
			fullWidth={false}
			onclick={editSelected}
		/>
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
	onConfirm={deleteSelected}
	message="Una volta eliminata la certificazione non sarà recuperabile."
	btnStyle="danger"
/>

<style>
	.artid-list {
		min-width: 60rem;
	}

	.table-scroll-container {
		height: 450px;
	}

	/* Header sticky: resta sopra le righe ma sotto le modali (z-index basso). */
	.sticky-header {
		position: sticky;
		top: 0;
		z-index: 1;
	}

	/* Righe cliccabili con feedback hover (sostituisce .table-hover). */
	.resource-row {
		transition: 0.2s ease all;
		cursor: pointer;
	}

	.resource-row:hover {
		transition: 0.2s ease all;
		background-color: var(--artid-surface);
	}

	/* Riga selezionata: tinta brand. */
	.resource-row.selected {
		transition: 0.2s ease all;
		box-shadow: inset 6px 0px 0px -3px var(--artid-primary);
	}

	/* Consente al titolo di troncare con ellissi dentro la colonna flex. */
	.certification-title {
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
