<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import { fly } from 'svelte/transition';
	import { quintOut } from 'svelte/easing';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';
	import spidIcon from '$lib/assets/spid-ico-circle.svg';
	import { loading } from '$lib/stores/loading';
	import type { ActionData } from './$types';

	let { form }: { form: ActionData } = $props();

	// Provider mostrati a video: la scelta è puramente cosmetica (nel mock qualsiasi provider
	// autentica qualsiasi identità). Elenco rappresentativo dei provider SPID reali.
	const providers = [
		'PosteID',
		'Aruba ID',
		'Lepida ID',
		'InfoCert ID',
		'Namirial ID',
		'Sielte ID',
		'SpidItalia',
		'TIM id'
	];

	// Fase OTP: l'utente ha confermato l'invio e il codice è stato spedito all'email esistente.
	let otpPhase = $derived(form?.step === 'otp');

	// Fase conferma: il server ha riconosciuto un'email già registrata. Mostriamo il messaggio con
	// "Ok" PRIMA di inviare l'OTP (RAD AUT_MEM_ID §8.3.3.1); l'invio parte solo all'"Ok".
	let confirmPhase = $derived(form?.step === 'confirm');

	// Provider scelto: fase locale senza round-trip. null = mostra il menu dei provider.
	let selectedProvider = $state<string | null>(null);

	let credentials = $state({ username: form?.username ?? '', password: '' });
	let code = $state('');

	// Indicatore di avanzamento: la verifica OTP è un passaggio extra del ramo "email già nota".
	let steps = $derived(
		otpPhase || confirmPhase ? ['Provider', 'Accesso', 'Verifica'] : ['Provider', 'Accesso']
	);
	let currentStep = $derived(otpPhase ? 3 : confirmPhase ? 2 : selectedProvider ? 2 : 1);

	// Pattern condiviso con login/register: overlay di caricamento durante la submit.
	const withLoading = () => {
		$loading = true;
		return async ({ update }: { update: () => Promise<void> }) => {
			$loading = false;
			await update();
		};
	};

	const fadeIn = { y: 10, duration: 220, easing: quintOut };
</script>

{#snippet lockup(title: string, subtitle?: string, eyebrow?: string)}
	<div class="text-center">
		<span class="spid-mark"><img src={spidIcon} alt="SPID" /></span>
		{#if eyebrow}<p class="spid-eyebrow">{eyebrow}</p>{/if}
		<h1 class="spid-title">{title}</h1>
		{#if subtitle}<p class="spid-subtitle">{subtitle}</p>{/if}
	</div>
{/snippet}

{#snippet stepper()}
	<div class="spid-steps">
		<div class="spid-steps__bar" aria-hidden="true">
			{#each steps as label, i (label)}
				<span class="spid-steps__seg" class:is-active={i + 1 <= currentStep}></span>
			{/each}
		</div>
		<p class="spid-steps__label">
			Passaggio {currentStep} di {steps.length} · {steps[currentStep - 1]}
		</p>
	</div>
{/snippet}

{#snippet trust()}
	<p class="spid-trust">
		<i class="bi bi-lock-fill"></i>
		Connessione sicura · I tuoi dati viaggiano protetti
	</p>
{/snippet}

<div class="spid-shell">
	{@render stepper()}

	{#if otpPhase}
		<div in:fly={fadeIn}>
			{@render lockup(
				'Verifica la tua identità',
				undefined,
				'Account esistente'
			)}
			<p class="spid-lead">
				Esiste già un account ArtID con questa email. Inserisci il codice a 6 cifre inviato a
				<strong>{form?.email}</strong>.
			</p>

			<form method="POST" action="?/verifyOtp" use:enhance={withLoading}>
				<input type="hidden" name="email" value={form?.email ?? ''} />
				<input type="hidden" name="username" value={form?.username ?? ''} />

				<ArtidOtpInput name="code" bind:value={code} error={form?.codeError} autofocus />

				{#if form?.codeError}
					<p class="spid-field-error">{form.codeError}</p>
				{/if}
				{#if form?.resent}
					<p class="spid-note spid-note--ok">
						<i class="bi bi-check-circle-fill"></i> Ti abbiamo inviato un nuovo codice.
					</p>
				{/if}
				{#if form?.formError}
					<p class="spid-note spid-note--err">
						<i class="bi bi-exclamation-triangle-fill"></i> {form.formError}
					</p>
				{/if}

				<div class="spid-cta">
					<ArtidButton label="Verifica e accedi" type="submit" />
				</div>

				<p class="spid-resend">
					Non hai ricevuto il codice?
					<button type="submit" formaction="?/resend" class="spid-linkbtn">Invia di nuovo</button>
				</p>
			</form>

			{@render trust()}

			<p class="spid-back-row">
				<a href={resolve('/login')} class="spid-back"><i class="bi bi-arrow-left"></i> Torna al login</a>
			</p>
		</div>
	{:else if confirmPhase}
		<div in:fly={fadeIn}>
			{@render lockup('Account già esistente', undefined, 'Verifica identità')}

			<p class="spid-lead">
				Abbiamo trovato un account ArtID con l'email <strong>{form?.email}</strong>. Ti invieremo un
				codice OTP da inserire: premi <strong>Ok</strong> per procedere con l'invio della mail.
			</p>

			<form method="POST" action="?/sendOtp" use:enhance={withLoading}>
				<input type="hidden" name="username" value={form?.username ?? ''} />
				<input type="hidden" name="email" value={form?.email ?? ''} />

				<div class="spid-cta">
					<ArtidButton label="Ok" type="submit" />
				</div>
			</form>

			{@render trust()}

			<p class="spid-back-row">
				<a href={resolve('/login')} class="spid-back"><i class="bi bi-arrow-left"></i> Torna al login</a>
			</p>
		</div>
	{:else if selectedProvider}
		<div in:fly={fadeIn}>
			{@render lockup(`Accedi con ${selectedProvider}`, undefined, 'Identity provider')}

			<p class="spid-service">
				<i class="bi bi-shield-lock-fill"></i>
				<span><strong>ArtID</strong> richiede l'accesso alla tua identità digitale</span>
			</p>

			<form method="POST" action="?/authenticate" use:enhance={withLoading}>
				<input type="hidden" name="providerId" value={selectedProvider} />

				<div class="spid-fields">
					<ArtidInput
						name="username"
						label="Codice fiscale"
						bind:value={credentials.username}
						error={form?.errors?.username}
					/>
					<ArtidInput
						type="password"
						name="password"
						label="Password"
						bind:value={credentials.password}
						error={form?.errors?.password}
					/>
				</div>

				{#if form?.formError}
					<p class="spid-note spid-note--err">
						<i class="bi bi-exclamation-triangle-fill"></i> {form.formError}
					</p>
				{/if}

				<div class="spid-hint">
					<i class="bi bi-info-circle-fill"></i>
					<span>
						Demo: codice fiscale <code>RSSMRA85M01H501Z</code>, password <code>Spid!2024</code>.
					</span>
				</div>

				<div class="spid-cta">
					<ArtidButton label="Entra" type="submit" />
				</div>
			</form>

			{@render trust()}

			<p class="spid-back-row">
				<button type="button" class="spid-back" onclick={() => (selectedProvider = null)}>
					<i class="bi bi-arrow-left"></i> Scegli un altro provider
				</button>
			</p>
		</div>
	{:else}
		<div in:fly={fadeIn}>
			{@render lockup(
				'Entra con SPID',
				'Scegli il tuo provider di identità digitale',
				'Sistema Pubblico di Identità Digitale'
			)}

			<div class="provider-list">
				{#each providers as provider (provider)}
					<button type="button" class="provider" onclick={() => (selectedProvider = provider)}>
						<span class="provider__chip"><i class="bi bi-person-vcard"></i></span>
						<span class="provider__name">{provider}</span>
						<i class="bi bi-chevron-right provider__chev"></i>
					</button>
				{/each}
			</div>

			{@render trust()}

			<p class="spid-back-row">
				<a href={resolve('/login')} class="spid-back"><i class="bi bi-arrow-left"></i> Torna al login</a>
			</p>
		</div>
	{/if}
</div>

<style>
	.spid-shell {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	/* --- Header lockup --- */
	.spid-mark {
		display: inline-grid;
		place-items: center;
		width: 3.25rem;
		height: 3.25rem;
		padding: 0.5rem;
		border-radius: 0.9rem;
		background: var(--artid-primary);
	}
	.spid-mark img {
		width: 100%;
		height: 100%;
		display: block;
	}

	.spid-eyebrow {
		margin: 0.85rem 0 0;
		font-size: 0.7rem;
		font-weight: 700;
		letter-spacing: 0.09em;
		text-transform: uppercase;
		color: var(--artid-primary);
	}
	.spid-title {
		margin: 0.35rem 0 0;
		font-size: 1.6rem;
		font-weight: 700;
		letter-spacing: -0.01em;
		color: var(--artid-text);
	}
	.spid-subtitle {
		margin: 0.4rem 0 0;
		color: color-mix(in oklab, var(--artid-text), transparent 32%);
	}
	.spid-lead {
		margin: 0;
		text-align: center;
		line-height: 1.5;
		color: color-mix(in oklab, var(--artid-text), transparent 28%);
	}
	.spid-lead strong {
		color: var(--artid-text);
	}

	/* --- Stepper --- */
	.spid-steps__bar {
		display: flex;
		gap: 0.375rem;
	}
	.spid-steps__seg {
		flex: 1;
		height: 4px;
		border-radius: 2px;
		background: color-mix(in oklab, var(--artid-primary), transparent 82%);
		transition: background-color 0.25s ease;
	}
	.spid-steps__seg.is-active {
		background: var(--artid-primary);
	}
	.spid-steps__label {
		margin: 0.55rem 0 0;
		font-size: 0.72rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
		color: color-mix(in oklab, var(--artid-text), transparent 38%);
	}

	/* --- Provider list (uniforme, senza brand) --- */
	.provider-list {
		display: grid;
		gap: 0.625rem;
	}
	.provider {
		display: flex;
		align-items: center;
		gap: 0.875rem;
		width: 100%;
		padding: 0.7rem 0.9rem;
		text-align: left;
		background: var(--artid-section);
		border: 1px solid color-mix(in oklab, var(--artid-border), transparent 45%);
		border-radius: 0.8rem;
		color: var(--artid-text);
		cursor: pointer;
		transition:
			border-color 0.16s ease,
			background-color 0.16s ease;
	}
	.provider__chip {
		flex: 0 0 auto;
		display: grid;
		place-items: center;
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 0.6rem;
		background: var(--artid-primary-subtle);
		color: var(--artid-primary);
		font-size: 1.15rem;
	}
	.provider__name {
		flex: 1 1 auto;
		font-weight: 600;
	}
	.provider__chev {
		flex: 0 0 auto;
		color: color-mix(in oklab, var(--artid-text), transparent 55%);
		transition:
			transform 0.16s ease,
			color 0.16s ease;
	}
	.provider:hover {
		border-color: var(--artid-primary);
		background: color-mix(in oklab, var(--artid-primary-subtle), var(--artid-section) 35%);
	}
	.provider:hover .provider__chev {
		color: var(--artid-primary);
		transform: translateX(3px);
	}
	.provider:focus-visible {
		outline: 3px solid var(--artid-primary-subtle);
		outline-offset: 2px;
		border-color: var(--artid-primary);
	}

	/* --- Provider credentials --- */
	.spid-service {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.5rem;
		margin: 0;
		font-size: 0.9rem;
		color: color-mix(in oklab, var(--artid-text), transparent 30%);
	}
	.spid-service i {
		color: var(--artid-primary);
	}
	.spid-service strong {
		color: var(--artid-text);
	}

	.spid-fields {
		display: grid;
		gap: 0.75rem;
	}

	.spid-hint {
		display: flex;
		align-items: flex-start;
		gap: 0.6rem;
		margin-top: 1rem;
		padding: 0.7rem 0.85rem;
		background: var(--artid-muted);
		border-radius: 0.6rem;
		font-size: 0.85rem;
		line-height: 1.45;
		color: color-mix(in oklab, var(--artid-text), transparent 18%);
	}
	.spid-hint i {
		margin-top: 0.1rem;
		color: var(--artid-primary);
	}
	.spid-hint code {
		padding: 0.05rem 0.35rem;
		font-weight: 600;
		color: var(--artid-text);
		background: var(--artid-section);
		border-radius: 0.3rem;
	}

	/* --- Note / messaggi --- */
	.spid-note {
		display: flex;
		align-items: flex-start;
		gap: 0.45rem;
		margin: 0.9rem 0 0;
		padding: 0.6rem 0.75rem;
		font-size: 0.85rem;
		line-height: 1.4;
		border-radius: 0.55rem;
	}
	.spid-note--err {
		color: color-mix(in oklab, var(--bs-danger, #dc3545), black 22%);
		background: color-mix(in oklab, var(--bs-danger, #dc3545), white 88%);
	}
	.spid-note--ok {
		color: color-mix(in oklab, var(--bs-success, #198754), black 18%);
		background: color-mix(in oklab, var(--bs-success, #198754), white 88%);
	}
	.spid-field-error {
		margin: 0.75rem 0 0;
		text-align: center;
		font-size: 0.85rem;
		color: var(--bs-danger, #dc3545);
	}

	/* --- CTA & link --- */
	.spid-cta {
		margin-top: 1.25rem;
	}
	.spid-resend {
		margin: 1rem 0 0;
		text-align: center;
		font-size: 0.9rem;
		color: color-mix(in oklab, var(--artid-text), transparent 30%);
	}
	.spid-linkbtn {
		padding: 0;
		border: none;
		background: none;
		font-weight: 600;
		color: var(--artid-primary);
		cursor: pointer;
	}
	.spid-linkbtn:hover {
		text-decoration: underline;
	}

	.spid-trust {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.5rem;
		margin: 0;
		font-size: 0.8rem;
		color: color-mix(in oklab, var(--artid-text), transparent 42%);
	}
	.spid-trust i {
		color: var(--artid-primary);
	}

	.spid-back-row {
		margin: 0;
		text-align: center;
	}
	.spid-back {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		padding: 0.3rem 0.6rem;
		border: none;
		background: none;
		font-weight: 600;
		font-size: 0.9rem;
		color: var(--artid-primary);
		text-decoration: none;
		border-radius: 0.45rem;
		cursor: pointer;
		transition: background-color 0.15s ease;
	}
	.spid-back:hover {
		background: var(--artid-primary-subtle);
	}
	.spid-back:focus-visible {
		outline: 3px solid var(--artid-primary-subtle);
		outline-offset: 2px;
	}

	@media (prefers-reduced-motion: reduce) {
		.provider,
		.provider__chev,
		.spid-steps__seg {
			transition: none;
		}
	}
</style>
