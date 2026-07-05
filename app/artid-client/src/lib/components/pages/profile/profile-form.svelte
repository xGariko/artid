<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { toast } from '$lib/toast';
	import { api } from '$lib/api/browser-client';
	import ArtidInput from '$lib/components/ui/artid-input.svelte';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import 'quill/dist/quill.snow.css';
	import type { Profile } from '$lib/api/types';
	import ArtidSpidButton from '$lib/components/ui/artid-spid-button.svelte';
	import ArtidEditorModal from '$lib/components/ui/artid-editor-modal.svelte';
	import ArtidOtpInput from '$lib/components/ui/artid-otp-input.svelte';
	import { toDateInputValue } from '$lib/utilities';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	let { profile }: { profile: Profile } = $props();

	// Data odierna in formato yyyy-MM-dd: limite massimo della data di nascita (oggi ammesso, futuro no).
	const todayDateInput = toDateInputValue(new Date());
	const BIRTHDATE_FUTURE_MESSAGE = 'La data di nascita non può essere successiva a oggi.';

	let model = $state({
		name: profile.name ?? '',
		surname: profile.surname ?? '',
		birthdate: profile.birthdate ?? '',
		birthplace: profile.birthplace ?? '',
		address: profile.address ?? '',
		biography: profile.biography ?? '',
		linkedinId: profile.linkedinId ?? '',
		facebookId: profile.facebookId ?? '',
		instagramId: profile.instagramId ?? '',
		profession: profile.profession ?? '',
		phone: profile.phone ?? '',
		businessEmail: profile.businessEmail ?? '',
		isPublic: profile.isPublic ?? false,
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
		facebookId: profile.facebookId ?? '',
		instagramId: profile.instagramId ?? '',
		profession: profile.profession ?? '',
		phone: profile.phone ?? '',
		businessEmail: profile.businessEmail ?? '',
		isPublic: profile.isPublic ?? false,
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
			model.facebookId !== baseline.facebookId ||
			model.instagramId !== baseline.instagramId ||
			model.profession !== baseline.profession ||
			model.phone !== baseline.phone ||
			model.businessEmail !== baseline.businessEmail ||
			model.isPublic !== baseline.isPublic ||
			model.internalShareEnabled !== baseline.internalShareEnabled
	);

	// I campi obbligatori alla registrazione (nome e cognome) non possono restare vuoti: se svuotati,
	// "Salva" è disabilitato. Email e password non rientrano perché non modificabili da questo form.
	let requiredFilled = $derived(model.name.trim() !== '' && model.surname.trim() !== '');

	// La data di nascita non può in nessun caso essere successiva a oggi (confronto tra stringhe
	// yyyy-MM-dd). Blocca il "Salva" e mostra l'errore sul campo; la guardia in save() è l'ultima difesa.
	let birthdateInFuture = $derived(!!model.birthdate && model.birthdate > todayDateInput);

	let fieldErrors = $state<Record<string, string>>({});
	let isSaving = $state(false);
	let propicInput: HTMLInputElement | null = $state(null);

	// Avatar: gestito a parte dal resto del form. Upload/rimozione immediati verso
	// /api/profile/avatar (multipart), NON dal bottone "Salva". `avatarUrl` è il presigned
	// URL corrente (vuoto = nessuna foto); aggiornato in locale per feedback immediato.
	let avatarUrl = $state(profile.propicUrl ?? '');
	let avatarBusy = $state(false);

	// Div che Quill trasforma in editor + istanza Quill.
	let bioContainer = $state<HTMLDivElement | undefined>(undefined);

	const err = (field: string) => fieldErrors[field];

	// Valore iniziale della bio, catturato fuori dal grafo reattivo così l'$effect
	// dipende solo da bioContainer (e non si re-inizializza ad ogni battitura).
	const initialBiography = profile.biography ?? '';

	// Inizializza Quill quando il div è montato. Import dinamico: Quill usa
	// window/document, quindi niente SSR.
	$effect(() => {
		if (!bioContainer) {
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
		})();
		return () => {
			cancelled = true;
		};
	});

	// Quill su vuoto produce "<p><br></p>": lo normalizzo a undefined per il salvataggio.
	function bioForSave(): string | undefined {
		const plain = model.biography
			.replace(/<[^>]*>/g, '')
			.replace(/&nbsp;/g, ' ')
			.trim();
		return plain ? model.biography : undefined;
	}

	// Upload immediato della foto su /api/profile/avatar (multipart). Passa da openapi-fetch:
	// il bodySerializer costruisce un FormData, così il client non serializza in JSON e il
	// browser imposta il boundary multipart. `file` è tipizzato come stringa (binary) nello
	// schema → cast necessario per passargli il File vero.
	async function handlePropicPick(event: Event): Promise<void> {
		const input = event.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = ''; // consente di riselezionare lo stesso file
		if (!file) return;
		if (!file.type.startsWith('image/')) {
			toast.error('Seleziona un file immagine');
			return;
		}
		if (file.size > 5 * 1024 * 1024) {
			toast.error('Immagine troppo grande (max 5MB)');
			return;
		}

		avatarBusy = true;
		try {
			const { data } = await api.PUT('/api/profile/avatar', {
				// body tipizzato come { file: string } dallo schema → cast per il File reale.
				// Il serializer ignora il body e usa il File dalla closure, costruendo il FormData
				// (così openapi-fetch non serializza in JSON e il browser mette il boundary).
				body: { file: file as unknown as string },
				bodySerializer: () => {
					const fd = new FormData();
					fd.append('file', file);
					return fd;
				}
			});
			if (!data?.url) {
				toast.error('Errore nel caricamento della foto');
				return;
			}
			avatarUrl = data.url;
			toast.success('Foto profilo aggiornata');
			invalidateAll(); // rinfresca l'avatar in navbar
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			avatarBusy = false;
		}
	}

	async function removePropic(): Promise<void> {
		avatarBusy = true;
		try {
			const { response } = await api.DELETE('/api/profile/avatar');
			if (!response.ok) {
				toast.error('Errore nella rimozione della foto');
				return;
			}
			avatarUrl = '';
			toast.success('Foto profilo rimossa');
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			avatarBusy = false;
		}
	}

	async function save(): Promise<void> {
		if (isSaving) return;
		if (birthdateInFuture) {
			fieldErrors = { birthdate: BIRTHDATE_FUTURE_MESSAGE };
			toast.error(BIRTHDATE_FUTURE_MESSAGE);
			return;
		}
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
				facebookId: model.facebookId || undefined,
				instagramId: model.instagramId || undefined,
				profession: model.profession || undefined,
				phone: model.phone || undefined,
				businessEmail: model.businessEmail || undefined,
				isPublic: model.isPublic,
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
			toast.success('Informazioni aggiornate con successo');
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			isSaving = false;
		}
	}

	let deleteAccountModalOpen = $state(false);
	let password = $state('');
	let elimina = $state('');
	// Credenziali SPID per l'eliminazione di un account nato da SPID (senza password reale).
	let spidCf = $state('');
	let spidPassword = $state('');

	// Un account nato da SPID non ha una password a DB (passwordSet=false): la chiusura account
	// verifica le credenziali SPID (codice fiscale + password) invece della password. Default true
	// (flusso classico con password) finché il campo non è presente nel contratto rigenerato.
	let deletesWithPassword = $derived(profile.passwordSet ?? true);

	function deleteAccountModal() {
		// Ripulisce i campi a ogni apertura: nessun residuo tra un tentativo e l'altro.
		password = '';
		spidCf = '';
		spidPassword = '';
		elimina = '';
		deleteAccountModalOpen = true;
	}

	async function deleteAccount(): Promise<void> {
		if (elimina !== 'Elimina') {
			toast.error('Per confermare devi digitare esattamente la parola "Elimina" ');
			return;
		}

		// Le credenziali richieste dipendono dal tipo di account.
		if (deletesWithPassword ? !password.trim() : !spidCf.trim() || !spidPassword.trim()) {
			toast.error('Bisogna compilare tutti i campi!');
			return;
		}

		isSaving = true;

		try {
			// Verifica delle credenziali prima dell'eliminazione: password oppure identità SPID collegata.
			if (deletesWithPassword) {
				const verifyResponse = await api.POST('/api/auth/verify-password', {
					body: { password }
				});
				const isPasswordValid = verifyResponse?.data?.['passwordCorretta'] ?? false;
				if (!isPasswordValid) {
					toast.error('Password Sbagliata');
					isSaving = false;
					return;
				}
			} else {
				const verifyResponse = await api.POST('/api/auth/verify-spid', {
					body: { username: spidCf.trim(), password: spidPassword }
				});
				const isSpidValid = verifyResponse?.data?.['spidCorretta'] ?? false;
				if (!isSpidValid) {
					toast.error('Credenziali SPID non valide');
					isSaving = false;
					return;
				}
			}

			const actualUserId = profile?.id; // Letto sul momento, al click, quindi è sicuro al 100%
			if (actualUserId === undefined || actualUserId === null) {
				toast.error("Impossibile recuperare l'ID identificativo dell'utente.");
				return;
			}

			const deleteResponse = await api.DELETE('/api/users/{id}', {
				params: {
					path: { id: actualUserId }
				}
			});

			if (!deleteResponse.response.ok) {
				toast.error("Errore nella rimozione dell'account dal sistema");
				return;
			}

			toast.success('Account eliminato con successo');
			deleteAccountModalOpen = false;
			logoutLocal();
		} catch (e) {
			// Gestione di crash di rete o errori del server
			console.error(e);
			toast.error(e instanceof Error ? e.message : 'Errore di rete o del server');
		} finally {
			isSaving = false;
		}
	}

	async function logoutLocal() {
		localStorage.clear();
		await goto(resolve('/welcome'));
	}

	// --- Cambio password (RAD, caso d'uso MODIFICA PASSWORD): la nuova password scelta dall'utente
	// viene applicata solo dopo la verifica di un OTP inviato via email. Due fasi nella stessa modale:
	// "form" (nuova + conferma) → "otp" (codice a 6 cifre). ---

	// Formato password RAD: ≥8 caratteri con maiuscola, minuscola, numero e carattere speciale.
	const PASSWORD_FORMAT = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

	let changePasswordModalOpen = $state(false);
	let changePwPhase = $state<'form' | 'confirm' | 'otp'>('form');
	let newPassword = $state('');
	let confirmNewPassword = $state('');
	let otpCode = $state('');
	// Forza il remount dell'input OTP dopo un errore/rinvio: svuota le caselle e rifocalizza la prima.
	let otpResetKey = $state(0);
	let changingPassword = $state(false);

	function openChangePasswordModal() {
		newPassword = '';
		confirmNewPassword = '';
		otpCode = '';
		changePwPhase = 'form';
		changePasswordModalOpen = true;
	}

	// Step 1 (RAD passi 5-6): valida i campi con i messaggi del RAD, poi mostra la conferma di invio.
	// L'OTP non parte qui: viene inviato solo all'"Ok" (vedi confirmSendChangePasswordOtp).
	function submitNewPassword(): void {
		if (!newPassword.trim() || !confirmNewPassword.trim()) {
			toast.error('Errore: Bisogna compilare tutti i campi!');
			return;
		}
		if (!PASSWORD_FORMAT.test(newPassword)) {
			toast.error(
				'Errore: Formato della password non corretto! Almeno 8 caratteri con maiuscola, minuscola, numero e carattere speciale.'
			);
			return;
		}
		if (newPassword !== confirmNewPassword) {
			toast.error(
				'Errore: I campi "Nuova password" e "Conferma nuova password" devono essere uguali!'
			);
			return;
		}
		changePwPhase = 'confirm';
	}

	// Step 2 (RAD passi 7-8): all'"Ok" invia davvero l'OTP all'email del Membro e passa alla verifica.
	async function confirmSendChangePasswordOtp(): Promise<void> {
		if (changingPassword) return;
		changingPassword = true;
		try {
			const { response } = await api.POST('/api/profile/change-password/request-otp', {});
			if (!response.ok) {
				toast.error('Impossibile inviare il codice OTP. Riprova tra poco.');
				return;
			}
			otpCode = '';
			otpResetKey++;
			changePwPhase = 'otp';
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			changingPassword = false;
		}
	}

	// Step 2 (RAD passi 9-11): verifica l'OTP e applica la nuova password. Auto-submit a 6 cifre.
	async function submitChangePasswordOtp(): Promise<void> {
		if (changingPassword) return;
		changingPassword = true;
		try {
			const { response } = await api.POST('/api/profile/change-password', {
				body: { code: otpCode, newPassword }
			});
			if (response.ok) {
				changePasswordModalOpen = false;
				toast.success('Password modificata correttamente');
				return;
			}
			// 400 = formato non valido (già filtrato lato client); 401 = OTP errato/scaduto.
			toast.error(
				response.status === 400
					? 'Errore: Formato della password non corretto!'
					: 'Errore: codice non valido, controlla nella mail che non sia scaduto. Se è scaduto clicca Invia di nuovo.'
			);
			otpCode = '';
			otpResetKey++;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			changingPassword = false;
		}
	}

	// "Invia di nuovo": rigenera e rinvia l'OTP. Fire-and-forget (il server risponde 200 comunque).
	async function resendChangePasswordOtp(): Promise<void> {
		try {
			await api.POST('/api/profile/change-password/request-otp', {});
			toast.info('Ti abbiamo inviato un nuovo codice.');
			otpCode = '';
			otpResetKey++;
		} catch {
			// Nessun feedback d'errore sul rinvio: l'utente può ritentare.
		}
	}

	// --- Collega SPID (RAD, caso d'uso COL_SPID). Il provider è mockato lato server: qui inviamo le
	// credenziali della schermata del provider e, al successo, l'anagrafica viene SOVRASCRITTA con i
	// dati del provider (e l'account risulta verificato). ---
	const spidProviders = [
		'PosteID',
		'Aruba ID',
		'Lepida ID',
		'InfoCert ID',
		'Namirial ID',
		'Sielte ID',
		'SpidItalia',
		'TIM id'
	];

	let spidLinked = $state(profile.spidLinked ?? false);
	let spidModalOpen = $state(false);
	let spidBusy = $state(false);
	let spidCreds = $state({ providerId: 'PosteID', username: '', password: '' });

	function openSpidModal() {
		spidCreds = { providerId: 'PosteID', username: '', password: '' };
		spidModalOpen = true;
	}

	async function linkSpid(): Promise<void> {
		if (spidBusy) return;
		if (!spidCreds.username.trim() || !spidCreds.password.trim()) {
			toast.error('Inserisci codice fiscale e password');
			return;
		}
		spidBusy = true;
		try {
			const { data, response } = await api.POST('/api/profile/spid', {
				body: {
					providerId: spidCreds.providerId,
					username: spidCreds.username.trim(),
					password: spidCreds.password
				}
			});
			if (!response.ok || !data) {
				if (response.status === 409) {
					toast.error('Questa identità SPID è già collegata a un altro account ArtID');
				} else if (response.status === 401) {
					toast.error('Credenziali SPID non valide');
				} else {
					toast.error('Errore: impossibile contattare il provider');
				}
				return;
			}
			// L'anagrafica è stata sostituita dal provider: riallineo form e snapshot così il
			// dirty-check non segnala modifiche "fantasma" sui campi appena sovrascritti.
			model.name = data.name ?? '';
			model.surname = data.surname ?? '';
			model.birthdate = data.birthdate ?? '';
			model.birthplace = data.birthplace ?? '';
			baseline.name = model.name;
			baseline.surname = model.surname;
			baseline.birthdate = model.birthdate;
			baseline.birthplace = model.birthplace;
			spidLinked = data.spidLinked ?? true;
			spidModalOpen = false;
			toast.success('SPID collegato: profilo verificato');
			invalidateAll();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Errore di rete');
		} finally {
			spidBusy = false;
		}
	}
</script>

<div
	class="bg-artid-section h-100 mh-100 overflow-y-auto w-md-75 w-100 rounded-3 border border-artid-border p-4 d-flex flex-column gap-3"
>
	<div class="row flex-grow-1">
		<!-- Propic -->
		<div class="col-12 col-md-2 d-flex flex-column align-items-center gap-2">
			<div class="position-md-absolute">
				<button
					type="button"
					class="propic-btn rounded-circle border border-artid-border d-flex align-items-center justify-content-center overflow-hidden p-0"
					style="width: 8rem; height: 8rem;"
					onclick={() => propicInput?.click()}
					disabled={avatarBusy}
					aria-label="Cambia foto profilo"
				>
					{#if avatarUrl}
						<img src={avatarUrl} alt="Foto profilo" class="w-100 h-100 object-fit-cover" />
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
						disabled={avatarBusy}
					>
						{avatarBusy ? 'Caricamento…' : avatarUrl ? 'Cambia foto' : 'Carica foto'}
					</button>
					{#if avatarUrl}
						<button
							type="button"
							class="btn btn-link btn-sm p-0 text-decoration-none text-danger"
							onclick={removePropic}
							disabled={avatarBusy}
						>
							Rimuovi
						</button>
					{/if}
				</div>
			</div>
		</div>

		<!-- Dati -->
		<div class="col-12 col-md-10 d-flex flex-column row">
			<div class="row">
				<div class="col-12 col-xl-4 p-1">
					<ArtidInput
						name="name"
						label="Nome"
						bind:value={model.name}
						error={err('name')}
						disabled={spidLinked}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="surname"
						label="Cognome"
						bind:value={model.surname}
						error={err('surname')}
						disabled={spidLinked}
					/>
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
					<ArtidInput
						type="date"
						name="birthdate"
						label="Data di nascita"
						max={todayDateInput}
						bind:value={model.birthdate}
						error={birthdateInFuture ? BIRTHDATE_FUTURE_MESSAGE : err('birthdate')}
						disabled={spidLinked}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="birthplace"
						label="Luogo di nascita"
						bind:value={model.birthplace}
						error={err('birthplace')}
						disabled={spidLinked}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="profession"
						label="Professione"
						bind:value={model.profession}
						error={err('profession')}
					/>
				</div>
			</div>

			<div class="row">
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="address"
						label="Indirizzo"
						bind:value={model.address}
						error={err('address')}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						type="tel"
						name="phone"
						label="Telefono"
						bind:value={model.phone}
						error={err('phone')}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						type="email"
						name="businessEmail"
						label="Email di contatto"
						bind:value={model.businessEmail}
						error={err('businessEmail')}
					/>
				</div>
			</div>

			<hr class="mt-3" />

			<div class="row">
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="linkedinId"
						label="LinkedIn"
						bind:value={model.linkedinId}
						error={err('linkedinId')}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="facebookId"
						label="Facebook"
						bind:value={model.facebookId}
						error={err('facebookId')}
					/>
				</div>
				<div class="col-12 col-md-4 p-1">
					<ArtidInput
						name="instagramId"
						label="Instagram"
						bind:value={model.instagramId}
						error={err('instagramId')}
					/>
				</div>
			</div>

			<hr class="mt-3" />

			<div class="row">
				<div class="col-12 p-1">
					<label for="biography" class="text-primary small fw-medium ps-1 mb-1 d-block"
						>Biografia</label
					>
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
	<!-- flex-wrap: su viewport stretti i bottoni vanno a capo invece di traboccare.
	     Su desktop stanno su una riga, quindi justify-content-between resta invariato. -->
	<div
		class="d-flex flex-wrap align-items-center justify-content-between gap-3 border-top border-artid-border pt-3"
	>
		<div class="d-flex flex-wrap gap-3">
			<ArtidButton
				label={isSaving ? 'Salvataggio…' : 'Salva'}
				icon="check-lg"
				btnStyle="success"
				disabled={isSaving || !isDirty || !requiredFilled || birthdateInFuture}
				onclick={save}
				fullWidth={false}
			/>
			<ArtidButton
				label="Cambia password"
				icon="pencil-square"
				btnStyle="primary"
				onclick={openChangePasswordModal}
				fullWidth={false}
			/>

			{#if spidLinked}
				<ArtidSpidButton label="SPID Collegato" disabled fullWidth={false} onclick={() => {}} />
			{:else}
				<ArtidSpidButton label="Collega SPID" fullWidth={false} onclick={openSpidModal} />
			{/if}
		</div>
		<div>
			<ArtidButton
				label="Chiudi account"
				btnStyle="danger"
				onclick={deleteAccountModal}
				fullWidth={false}
			/>
		</div>
	</div>
</div>

<ArtidEditorModal bind:isOpen={deleteAccountModalOpen} customHeight={deletesWithPassword ? '40' : '50'}>
	<div class="d-flex flex-column align-items-start justify-content-around w-100 h-100 flex-fill">
		<div class="text-artid-primary fw-semibold w-100">
			<i class="bi bi-person-x fs-5 text-danger"></i>
			<span>Chiudi account</span>
		</div>

		<div class="w-100 mb-3">
			<div class="my-2">
				{#if deletesWithPassword}
					<ArtidInput
						type="password"
						name="password"
						label="Password"
						placeholder="Password"
						bind:value={password}
						addClass="mb-2"
					/>
				{:else}
					<p class="text-muted small mb-2">
						Questo account accede con SPID e non ha una password: conferma con le credenziali
						SPID (codice fiscale e password).
					</p>
					<ArtidInput
						type="text"
						name="spidCf"
						label="Codice fiscale"
						placeholder="Codice fiscale"
						bind:value={spidCf}
						addClass="mb-2"
					/>
					<ArtidInput
						type="password"
						name="spidPassword"
						label="Password SPID"
						placeholder="Password SPID"
						bind:value={spidPassword}
						addClass="mb-2"
					/>
				{/if}

				<hr />
				<span class="text-muted fst-italic"
					>Digita <span class="fw-semibold">"Elimina"</span> per procedere</span
				>
				<ArtidInput
					type="text"
					name="confirm_close"
					label="Elimina"
					placeholder="Elimina"
					bind:value={elimina}
					addClass="mb-2"
				/>
			</div>
		</div>

		<div class="w-100">
			<div class="d-flex justify-content-end gap-2">
				<ArtidButton
					label="Chiudi"
					fullWidth={false}
					btnStyle="secondary"
					outline={true}
					disabled={isSaving}
					onclick={() => (deleteAccountModalOpen = false)}
				/>
				<ArtidButton
					label="Elimina account"
					fullWidth={false}
					btnStyle="danger"
					icon="exclamation-triangle-fill"
					disabled={isSaving}
					onclick={deleteAccount}
				/>
			</div>
		</div>
	</div>
</ArtidEditorModal>

<ArtidEditorModal bind:isOpen={changePasswordModalOpen} customHeight="40">
	<div class="d-flex flex-column align-items-start justify-content-around w-100 h-100 flex-fill">
		<div class="text-artid-primary fw-semibold w-100 d-flex align-items-center gap-2">
			<i class="bi bi-shield-lock fs-5 text-primary"></i>
			<span>Modifica password</span>
		</div>

		{#if changePwPhase === 'form'}
			<div class="w-100 mb-3">
				<ArtidInput
					type="password"
					name="new_password"
					label="Nuova password"
					placeholder="Nuova password"
					bind:value={newPassword}
					addClass="mb-2"
				/>
				<ArtidInput
					type="password"
					name="confirm_new_password"
					label="Conferma nuova password"
					placeholder="Conferma nuova password"
					bind:value={confirmNewPassword}
					addClass="mb-2"
				/>
				<span class="text-muted small fst-italic">
					Almeno 8 caratteri, con maiuscola, minuscola, numero e carattere speciale.
				</span>
			</div>

			<div class="w-100">
				<div class="d-flex justify-content-end gap-2">
					<ArtidButton
						label="Annulla"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						disabled={changingPassword}
						onclick={() => (changePasswordModalOpen = false)}
					/>
					<ArtidButton
						label="Conferma"
						fullWidth={false}
						btnStyle="primary"
						icon="check-lg"
						disabled={changingPassword}
						onclick={submitNewPassword}
					/>
				</div>
			</div>
		{:else if changePwPhase === 'confirm'}
			<div class="w-100 mb-3">
				<p class="text-muted mb-0">
					Verrà inviato un codice a 6 cifre all'indirizzo <strong>{profile.email}</strong>.
				</p>
			</div>

			<div class="w-100">
				<div class="d-flex justify-content-end gap-2">
					<ArtidButton
						label="Indietro"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						disabled={changingPassword}
						onclick={() => (changePwPhase = 'form')}
					/>
					<ArtidButton
						label="Ok"
						fullWidth={false}
						btnStyle="primary"
						disabled={changingPassword}
						onclick={confirmSendChangePasswordOtp}
					/>
				</div>
			</div>
		{:else}
			<div class="w-100 mb-3">
				<p class="text-muted small mb-3">
					Ti abbiamo inviato un codice a 6 cifre all'indirizzo <strong>{profile.email}</strong>.
				</p>
				{#key otpResetKey}
					<ArtidOtpInput
						name="code"
						bind:value={otpCode}
						oncomplete={submitChangePasswordOtp}
						autofocus
					/>
				{/key}
				<p class="mt-3 mb-0 small">
					Non hai ricevuto il codice?
					<button
						type="button"
						class="btn btn-link p-0 align-baseline"
						disabled={changingPassword}
						onclick={resendChangePasswordOtp}>Invia di nuovo</button
					>
				</p>
			</div>

			<div class="w-100">
				<div class="d-flex justify-content-end gap-2">
					<ArtidButton
						label="Annulla"
						fullWidth={false}
						btnStyle="secondary"
						outline={true}
						disabled={changingPassword}
						onclick={() => (changePasswordModalOpen = false)}
					/>
				</div>
			</div>
		{/if}
	</div>
</ArtidEditorModal>

<ArtidEditorModal bind:isOpen={spidModalOpen} customHeight="65" customWidth="40">
	<div class="d-flex flex-column w-100 h-100">
		<div class="text-artid-primary fw-semibold w-100 mb-2">
			<i class="bi bi-shield-check fs-5 text-primary"></i>
			<span>Associa SPID</span>
		</div>

		<div class="alert alert-warning d-flex gap-2 align-items-start" role="alert">
			<i class="bi bi-exclamation-triangle-fill mt-1"></i>
			<div>
				Collegando SPID i tuoi dati anagrafici (<strong
					>nome, cognome, data e luogo di nascita</strong
				>) verranno
				<strong>sostituiti</strong> con quelli forniti dal provider.
			</div>
		</div>

		<div class="mb-2">
			<label for="spidProvider" class="text-primary small fw-medium ps-1 mb-1 d-block">
				Provider
			</label>
			<select
				id="spidProvider"
				class="form-select bg-artid-section"
				bind:value={spidCreds.providerId}
			>
				{#each spidProviders as provider (provider)}
					<option value={provider}>{provider}</option>
				{/each}
			</select>
		</div>

		<ArtidInput
			name="spidUsername"
			label="Codice fiscale"
			bind:value={spidCreds.username}
			addClass="mb-2"
		/>
		<ArtidInput
			type="password"
			name="spidPassword"
			label="Password"
			bind:value={spidCreds.password}
			addClass="mb-2"
		/>

		<p class="text-secondary small mb-0">
			Demo: es. <code>RSSMRA85M01H501Z</code> / <code>Spid!2024</code>.
		</p>

		<div class="mt-auto w-100">
			<div class="d-flex justify-content-end gap-2">
				<ArtidButton
					label="Annulla"
					fullWidth={false}
					btnStyle="secondary"
					outline={true}
					disabled={spidBusy}
					onclick={() => (spidModalOpen = false)}
				/>
				<ArtidButton
					label={spidBusy ? 'Collegamento…' : 'Collega SPID'}
					fullWidth={false}
					btnStyle="primary"
					icon="shield-check"
					disabled={spidBusy}
					onclick={linkSpid}
				/>
			</div>
		</div>
	</div>
</ArtidEditorModal>

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
