<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import { user } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import { toast } from '$lib/toast';

	let {
		isOpen = $bindable(),
		artidId
	}: {
		isOpen: boolean;
		artidId: number;
	} = $props();

	let isSaving = $state(false);

	const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

	let toShareEmails = $state<string[]>([]);
	let newEmail = $state('');

	function addEmail() {
		const trimmedEmail = newEmail.trim();

		if (!trimmedEmail) return;

		if (!EMAIL_REGEX.test(trimmedEmail)) {
			toast.error('Inserire una mail valida');
			return;
		}

		if (get(user)?.email === trimmedEmail) {
			toast.error('Non puoi condividere con te stesso');
			return;
		}

		const alreadyExists = toShareEmails.some(
			(email) => email.toLowerCase() === trimmedEmail.toLowerCase()
		);

		if (alreadyExists) return;

		toShareEmails.push(trimmedEmail);
		newEmail = '';
	}

	function removeEmail(emailToRemove: string) {
		toShareEmails = toShareEmails.filter((email) => email !== emailToRemove);
	}

	async function shareArtid() {
		isSaving = true;

		try {
			const results = await Promise.all(
				[...toShareEmails].map((email) =>
					api.POST('/api/artids/{id}/share/internal', {
						params: { path: { id: artidId } },
						body: email
					})
				)
			);

			await invalidateAll();

			if (results.some((r) => r.error)) {
				toast.error('Errore durante la condivisione');
			} else {
				toShareEmails.forEach((email) =>
					toast.success(`Condivisione con ${email} completata con successo. Se questa mail esiste e se il destinatario accetta
				condivisioni, riceverà la tua condivisione.`)
				);
			}
		} catch {
			toast.error('Errore di rete');
		} finally {
			toShareEmails = [];
			isSaving = false;
			isOpen = false;
		}
	}
</script>

<ArtidEditorModal bind:isOpen customHeight="50" customWidth="40">
	<div class="d-flex flex-column gap-3 pt-1 justify-content-between h-100">
		<div class="d-flex align-items-center gap-2 text-artid-primary fw-semibold">
			<i class="bi bi-share-fill fs-5 text-primary"></i>
			<span>Condividi ArtID</span>
		</div>

		<div class="d-flex gap-1">
			<div class="border-0 w-100">
				<input
					type="text"
					class="form-control rounded-3 py-2 search-input"
					placeholder="Mail del destinatario"
					bind:value={newEmail}
					onkeydown={(e) => e.key === 'Enter' && addEmail()}
				/>
			</div>
			<ArtidButton icon="plus-lg" fullWidth={false} onclick={addEmail} />
		</div>

		<div class="border rounded-3 border-artid-border w-100 bg-artid-surface scrollable-list">
			<ul class="list-unstyled my-1 pe-1">
				{#each toShareEmails as email, index (email)}
					<li
						class="d-flex align-items-center gap-2 border-bottom border-artid-border p-2
                           {index === 0 ? 'border-top' : ''}"
					>
						<button
							class="bg-transparent border-0 p-0 d-flex align-items-center"
							aria-label="Rimuovi"
							onclick={() => removeEmail(email)}
						>
							<i class="bi bi-x fs-4 text-danger"></i>
						</button>
						<span>{email}</span>
					</li>
				{/each}
			</ul>
		</div>
		<div class="d-flex justify-content-end gap-2">
			<ArtidButton
				label="Chiudi"
				fullWidth={false}
				outline={true}
				btnStyle="secondary"
				onclick={() => (isOpen = !isOpen)}
			/>
			<ArtidButton
				label="Condividi"
				fullWidth={false}
				icon="share-fill"
				disabled={isSaving || toShareEmails.length <= 0}
				onclick={shareArtid}
			/>
		</div>
	</div>
</ArtidEditorModal>

<style lang="scss">
	.scrollable-list {
		max-height: 180px;
		height: 180px;

		overflow-y: auto;
	}
</style>
