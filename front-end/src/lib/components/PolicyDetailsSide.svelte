<script lang="ts">
	import {
		File,
		Play,
		Users,
		Eye,
		EyeOff,
		User,
		Rocket,
		Pencil,
		Trash2,
		Pause,
		Calendar,
		ArrowLeft
	} from 'lucide-svelte';
	import {
		formatDate,
		convertFileSize,
		shortFileName,
		type PolicyDetails,
		type DecryptPurpose,
		translatePolicyType,
		translatePolicyStatus
	} from '$lib';
	let {
		policyDetails,
		initLoading,
		onDecryptOne,
		onHideOne,
		openEditPolicyDialog,
		onDeletePolicy,
		onReleaseNow,
		onPausePolicy,
		onResumePolicy,
		onClosePolicyDetailsDialiog
	} = $props<{
		policyDetails: PolicyDetails | null;
		initLoading: boolean;
		onDecryptOne: (fileId: string, purpose: DecryptPurpose) => void;
		onHideOne: (fileId: string, purpose: DecryptPurpose) => void;
		openEditPolicyDialog: (policyId: string) => void;
		onDeletePolicy: (policyId: string) => void;
		onReleaseNow: (policyId: string) => void;
		onPausePolicy: (policyId: string) => void;
		onResumePolicy: (policyId: string) => void;
		onClosePolicyDetailsDialiog: () => void;
	}>();
</script>

<div class="header">
	<div class="go-back-btn">
		<button onclick={onClosePolicyDetailsDialiog}
			><ArrowLeft size={18} strokeWidth={1.5} />Atgal</button
		>
	</div>
	<div class="header-upper">
		{#if initLoading}
			<div class="skeleton skeleton-title"></div>
			<div class="skeleton skeleton-pill"></div>
		{:else}
			<h1>{policyDetails?.policyName ?? '—'}</h1>
			<div class="pills">
				<span class="pill releaseType">{translatePolicyType(policyDetails?.policyType ?? '—')}</span
				>
				<span class="pill" data-status={policyDetails?.policyStatus}
					>{translatePolicyStatus(policyDetails?.policyStatus ?? '—')}</span
				>
			</div>
		{/if}
	</div>
	<div class="header-down">
		{#if initLoading}
			<div class="skeleton skeleton-meta"></div>
		{:else}
			<span>{policyDetails?.policyFiles.length ?? '—'} failai</span>
			<span>{policyDetails?.policyRecipients.length ?? '—'} gavėjai</span>
			<span>Sukurta {formatDate(policyDetails?.createdAt ?? '—')}</span>
		{/if}
	</div>
</div>
<div class="body">
	{#if initLoading}
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-button-block"></div>
	{:else}
		<!-- <div class="block">
			<div class="content">
				<p>Kitas veiksmas</p>
				<p>Įspėjimas už: <span>30 dienų</span></p>
				<p>Paskutinis aktyvumo patvirtinimas: <span>2 val.</span></p>
			</div>
		</div> -->
		<div class="block">
			<div class="block-header">
				<Play size={16} strokeWidth={1.5} />
				<h3>Paleidimo sąlyga</h3>
			</div>
			<hr />
			<div class="content">
				{#if policyDetails?.releaseType === 'INACTIVITY'}
					<p>Neaktyvumas</p>
					<p>Neaktyvus: <span>30 dienų</span></p>
					<p>Atidėjimo laikotarpis: <span>{policyDetails?.graceDays ?? '–'} dienų</span></p>
					<div class="channel">
						<p>Įspėjimų kanalas: <span>{policyDetails?.channel ?? 'Telegram'}</span></p>
					</div>
				{:else if policyDetails?.releaseType === 'DATE_TIME'}
					<p>Suplanuotas atskleidimas</p>
					<p>Atskleidžiama: {formatDate(policyDetails?.scheduledReleaseAt ?? '–')}</p>
				{:else if policyDetails?.releaseType === 'MANUAL_RELEASE'}
					<p>Rankinis atkleidimas</p>
				{/if}
			</div>
		</div>
		<div class="files block">
			<div class="block-header">
				<File size={16} strokeWidth={1.5} />
				<h3>Failai</h3>
			</div>
			<hr />
			<div class="content">
				{#if !policyDetails?.policyFiles?.length}
					<p class="empty-files-message">Failas pašalintas...</p>
				{:else}
					{#each policyDetails?.policyFiles ?? [] as file (file.id)}
						<div class="file">
							<img src="/img/file-icon.png" alt="file-icon" width="18" />
							<div class="file-details">
								{#if file.fileName}
									<p>Failo pavadinimas: {shortFileName(file.fileName)}</p>
								{/if}
								{#if file.fileSize != null}
									<p>Dydis: {convertFileSize(file.fileSize)}</p>
								{/if}
								<p>Sukurta: {formatDate(file.createdAt)}</p>
							</div>
							{#if file.fileName}
								<button
									onclick={(e) => {
										e.stopPropagation();
										onHideOne(file.id, 'view');
									}}><Eye size={16} strokeWidth={1.5} /></button
								>
							{:else}
								<button
									onclick={(e) => {
										e.stopPropagation();
										onDecryptOne(file.id, 'view');
									}}><EyeOff size={16} strokeWidth={1.5} /></button
								>
							{/if}
						</div>
					{/each}
				{/if}
			</div>
		</div>
		<div class="recipients block">
			<div class="block-header">
				<Users size={16} strokeWidth={1.5} />
				<h3>Gavėjai</h3>
			</div>
			<hr />
			<div class="content">
				{#if !policyDetails?.policyRecipients?.length}
					<p class="empty-recipients-message">
						Gavėjas galimai pašalino šią politiką iš savo sąrašo...
					</p>
				{:else}
					{#each policyDetails?.policyRecipients ?? [] as recipient (recipient.id)}
						<div class="recipient">
							<div class="recipient-details">
								<div class="recipient-user">
									<div class="recipient-icon">
										<User size={15} strokeWidth={1.4} />
									</div>
									<div class="email-wrap">
										<p class="recipient-email">{recipient.email}</p>
										<span class="tooltip">{recipient.email}</span>
									</div>
								</div>
								<div class="recipient-access-pill">
									<p>
										<Eye size={12} strokeWidth={1.5} />Peržiūrų skaičius: {recipient.totalAccesses}
									</p>
								</div>
							</div>
							<div class="recipient-footer">
								<div class="access-box">
									<p class="access-label">
										<Calendar size={12} strokeWidth={1.5} />Pirmoji prieiga
									</p>
									<p class="access-value">{formatDate(recipient.firstAccessedAt ?? '–')}</p>
								</div>
								<div class="access-divider"></div>
								<div class="access-box">
									<p class="access-label">
										<Calendar size={12} strokeWidth={1.5} />Paskutinė prieiga
									</p>
									<p class="access-value">{formatDate(recipient.lastAccessedAt ?? '–')}</p>
								</div>
							</div>
						</div>
					{/each}
				{/if}
			</div>
		</div>
		<div class="button block">
			<div class="content">
				{#if policyDetails?.releaseType === 'INACTIVITY'}
					{#if policyDetails?.policyStatus !== 'RELEASED'}
						<!-- <button class="action-btn dark"><HeartPulse size={16} />Mark Alive</button> -->
						{#if policyDetails?.policyStatus !== 'PAUSED'}
							<button class="action-btn" onclick={() => onPausePolicy(policyDetails?.policyId)}
								><Pause size={16} />Pristabdyti</button
							>
						{:else}
							<button class="action-btn" onclick={() => onResumePolicy(policyDetails?.policyId)}
								><Play size={16} />Tęsti</button
							>
						{/if}
						<button class="action-btn" onclick={() => openEditPolicyDialog(policyDetails?.policyId)}
							><Pencil size={16} />Redaguoti</button
						>
					{/if}
					<button class="action-btn delete" onclick={() => onDeletePolicy(policyDetails?.policyId)}
						><Trash2 size={16} />Ištrinti</button
					>
				{:else if policyDetails?.releaseType === 'DATE_TIME'}
					{#if policyDetails?.policyStatus !== 'RELEASED'}
						<button class="action-btn dark" onclick={() => onReleaseNow(policyDetails?.policyId)}
							><Rocket size={16} />Atskleisti dabar</button
						>
						{#if policyDetails?.policyStatus !== 'PAUSED'}
							<button class="action-btn" onclick={() => onPausePolicy(policyDetails?.policyId)}
								><Pause size={16} />Pristabdyti</button
							>
						{:else}
							<button class="action-btn" onclick={() => onResumePolicy(policyDetails?.policyId)}
								><Play size={16} />Tęsti</button
							>
						{/if}
						<button class="action-btn" onclick={() => openEditPolicyDialog(policyDetails?.policyId)}
							><Pencil size={16} />Redaguoti</button
						>
					{/if}
					<button class="action-btn delete" onclick={() => onDeletePolicy(policyDetails?.policyId)}
						><Trash2 size={16} />Ištrinti</button
					>
				{:else if policyDetails?.releaseType === 'MANUAL_RELEASE'}
					{#if policyDetails?.policyStatus !== 'RELEASED'}
						<button class="action-btn dark" onclick={() => onReleaseNow(policyDetails?.policyId)}
							><Rocket size={16} />Atskleisti dabar</button
						>
						<button class="action-btn" onclick={() => openEditPolicyDialog(policyDetails?.policyId)}
							><Pencil size={16} />Redaguoti</button
						>
					{/if}
					<button class="action-btn delete" onclick={() => onDeletePolicy(policyDetails?.policyId)}
						><Trash2 size={16} />Ištrinti</button
					>
				{/if}
			</div>
		</div>
	{/if}
</div>

<style>
	h3 {
		font-weight: 500;
	}

	hr {
		opacity: 0.2;
		margin: 5px 0;
	}

	.block {
		background-color: #fff;
		border-radius: 7px;
		padding: 15px;
		margin-top: 15px;
	}

	.block-header {
		display: inline-flex;
		align-items: center;
		gap: 0.8rem;
		font-weight: 400;
	}

	.header-upper {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		margin-bottom: 0.5rem;
		column-gap: 0.5rem;
		row-gap: 0.4rem;
	}

	.header-upper h1 {
		line-height: 1.1;
		overflow-wrap: anywhere;
	}

	.header-down {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		font-size: 0.85rem;
	}

	.header-down span:not(:last-child)::after {
		content: '•';
		margin: 0 0.6rem;
	}

	.pill {
		font-size: 0.85rem;
	}

	.pills .pill:last-child {
		margin-left: 0.2rem;
	}

	.files.block .content {
		max-height: 110px;
		overflow-y: auto;
		padding-right: 0.4rem;
		scrollbar-gutter: stable; /* prevents layout shift when scrollbar appears */
	}

	.files.block .content .file {
		display: flex;
		align-items: center;
		gap: 1rem;
		font-size: 0.85rem;
	}

	.files.block .content button {
		display: flex;
		margin-left: auto;
		cursor: pointer;
		background-color: transparent;
		border: none;
	}

	.file:not(:first-child) {
		margin-top: 1rem;
	}

	.recipients.block .content {
		font-size: 0.85rem;
		max-height: 110px;
		overflow-y: auto;
		padding-right: 0.4rem;
		scrollbar-gutter: stable;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar {
		width: 0.4rem;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-button {
		width: 0;
		height: 0;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-track {
		background: transparent;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-thumb {
		background: rgba(120, 120, 120, 0.55);
		border-radius: 999px;
	}

	.recipient {
		display: flex;
		flex-direction: column;
		border-bottom: 1px solid #e8e8ec;
	}

	.recipient:last-child {
		border-bottom: none;
	}

	.recipient-details {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
	}

	.recipient-user {
		display: flex;
		align-items: center;
		gap: 0.9rem;
		min-width: 0;
	}

	.recipient-icon {
		width: 1.5rem;
		height: 1.5rem;
		border-radius: 50%;
		background: #f3f4f6;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.email-wrap {
		position: relative;
		min-width: 0;
	}

	.recipient-email {
		margin: 0;
		font-weight: 500;
		font-size: 0.9rem;
		color: #111827;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.tooltip {
		position: absolute;
		left: 0;
		top: 100%;
		margin-top: 0.35rem;
		padding: 0.35rem 0.6rem;
		border-radius: 6px;
		background: #111827;
		color: white;
		font-size: 0.75rem;
		white-space: nowrap;
		opacity: 0;
		pointer-events: none;
		z-index: 20;
	}

	.email-wrap:hover .tooltip {
		opacity: 1;
	}

	.recipient-access-pill {
		padding: 0.25rem 0.6rem;
		border-radius: 999px;
		background: #f3f6fb;
		border: 1px solid #e5e7eb;
	}

	.recipient-access-pill p {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.2rem;
		font-weight: 500;
		font-size: 0.7rem;
		color: #374151;
		white-space: nowrap;
	}

	.recipient-footer {
		display: flex;
		justify-content: space-between;
		margin-top: 0.5rem;
	}

	.access-box {
		display: flex;
		flex-direction: column;
	}

	.access-label {
		display: flex;
		align-items: center;
		gap: 0.2rem;
		color: #6b7280;
	}

	.access-value {
		font-weight: 500;
		color: #111827;
		font-size: 0.7rem;
	}

	.access-divider {
		width: 1px;
		background: #e5e7eb;
	}

	.button.block {
		margin-top: 0.5rem;
	}

	.button.block .content {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(9.5rem, 1fr));
		gap: 0.75rem;
		margin-left: 0;
	}

	.action-btn {
		display: flex;
		justify-content: center;
		align-items: center;
		background-color: #fff;
		border-radius: 0.5rem;
		padding: 0.4rem 0;
		font-size: 0.85rem;
		font-weight: 500;
		border: 1px solid rgba(83, 83, 83, 0.3);
		gap: 0.6rem;
		box-shadow: 1px 2px 4px rgba(0, 0, 0, 0.1);
		transition: transform 180ms ease;
	}

	.action-btn:active {
		transform: translateY(0) scale(0.98);
	}

	.action-btn.dark {
		background: linear-gradient(135deg, #4b505b, #2f343d);
		color: #fff;
	}

	.action-btn.delete {
		background: #fff5f5;
		border: 1px solid #f1b5b5;
		color: #b42318;
	}

	.skeleton {
		display: inline-block;
		border-radius: 999px;
		width: 100%;
		position: relative;
		overflow: hidden;
		background: rgba(0, 0, 0, 0.1);
	}

	.skeleton::after {
		content: '';
		position: absolute;
		inset: 0;
		transform: translateX(-100%);
		background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
		animation: skeleton-loading 1.1s infinite;
	}

	.skeleton-title {
		width: 250px;
		height: 30px;
	}

	.skeleton-pill {
		width: 90px;
		height: 25px;
		border-radius: 999px;
	}

	.skeleton-meta {
		width: 100%;
		height: 20px;
	}

	.skeleton-block {
		margin-top: 15px;
		width: 100%;
		height: 130px;
		border-radius: 16px;
	}

	@keyframes skeleton-loading {
		100% {
			transform: translateX(100%);
		}
	}

	.empty-files-message,
	.empty-recipients-message {
		padding: 0.5rem;
		font-size: 0.8rem;
		font-style: italic;
	}
</style>
