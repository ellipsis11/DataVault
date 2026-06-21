<script lang="ts">
	import { FileText, ShieldCheck, Inbox, Eye, Send, KeyRound, ChevronRight } from 'lucide-svelte';
	import SecretPhraseFormG from '$lib/components/SecretPhraseFormG.svelte';
	import { onMount } from 'svelte';
	import {
		formatDate,
		getDataForUserDashboard,
		toast,
		translateLogAction,
		type UserDashboardData
	} from '$lib';

	let dashboardData = $state<UserDashboardData | null>(null);
	let initialLoad = $state<boolean>(false);

	onMount(loadDashboardPage);

	async function loadDashboardPage() {
		initialLoad = true;
		try {
			dashboardData = await getDataForUserDashboard();
		} catch (error) {
			toast.error(error instanceof Error ? error.message : 'Nepavyko įkelti duomenų suvestinei!');
		} finally {
			setTimeout(() => {
				initialLoad = false;
			}, 500);
		}
	}
</script>

<svelte:head>
	<title>Pagrindinis</title>
</svelte:head>

<div class="dashboard">
	<div class="header">
		<h2>Jūsų santrauka</h2>
	</div>
	<div class="dashboard-upper">
		{#if initialLoad}
			{#each Array(4) as _}
				<div class="summary-card card">
					<div class="icon skeleton"></div>
					<div class="text">
						<p class="skeleton"></p>
						<p class="skeleton"></p>
					</div>
				</div>
			{/each}
		{:else}
			<div class="summary-card card">
				<div class="icon"><FileText size={30} /></div>
				<div class="text">
					<p>Mano failai</p>
					<p class="number">{dashboardData?.totalFiles ?? 0}</p>
				</div>
			</div>
			<div class="summary-card card">
				<div class="icon"><ShieldCheck size={30} /></div>
				<div class="text">
					<p>Sukurtos politikos</p>
					<p class="number">{dashboardData?.totalPolicies ?? 0}</p>
				</div>
			</div>
			<div class="summary-card card">
				<div class="icon"><Eye size={30} /></div>
				<div class="text">
					<p>Man atskleistos politikos</p>
					<p class="number">{dashboardData?.totalReleasedPolicies ?? 0}</p>
				</div>
			</div>
			<div class="summary-card card">
				<div class="icon"><Inbox size={30} /></div>
				<div class="text">
					<p>Naujos gautos politikos</p>
					<p class="number">{dashboardData?.totalNewPolicies ?? 0}</p>
				</div>
			</div>
		{/if}
	</div>
	<div class="dashboard-lower">
		{#if initialLoad}
			<div class="activity-card card">
				<div class="heading skeleton"></div>
				<div class="items">
					{#each Array(7) as _}
						<div class="item">
							<div class="text">
								<p class="skeleton"></p>
								<p class="skeleton"></p>
							</div>
							<p class="date skeleton"></p>
						</div>
					{/each}
				</div>
				<div class="view-all-btn skeleton"></div>
			</div>
		{:else}
			<div class="activity-card card">
				<div class="heading">
					<h2>Naujausia veikla</h2>
				</div>
				<div class="items">
					{#each dashboardData?.auditLogs.slice(0, 7) as log (log.id)}
						<div class="item">
							<div class="text">
								<p>{translateLogAction(log.actionType)}</p>
								<p>{log.message}</p>
							</div>
							<p class="date">{formatDate(log.createdAt)}</p>
						</div>
					{/each}
				</div>
				<div class="view-all-btn">
					<a href="/logs">Peržiūrėti visą veiklą <ChevronRight size={18} /></a>
				</div>
			</div>
		{/if}
		{#if initialLoad}
			<div class="status-card card">
				<div class="heading skeleton"></div>
				<div class="items">
					{#each Array(2) as _}
						<div class="item">
							<div class="icon skeleton"></div>
							<div class="text skeleton"></div>
							<div class="status">
								<span class="dot skeleton"></span>
								<span class="skeleton"></span>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{:else}
			<div class="status-card card">
				<div class="heading">
					<h2>Būsena</h2>
				</div>
				<div class="items">
					<div class="item">
						<div class="icon"><Send size={20} /></div>
						<div class="text">Telegram</div>
						<div class="status">
							<span
								class="dot"
								style:background-color={dashboardData?.telegramLinked ? 'green' : 'red'}
							></span>
							<span>{dashboardData?.telegramLinked ? 'Prijungta' : 'Neprijungta'}</span>
						</div>
					</div>
					<div class="item">
						<div class="icon">
							<KeyRound size={20} />
						</div>
						<div class="text">Šifravimo raktai</div>
						<div class="status">
							<span
								class="dot"
								style:background-color={dashboardData?.encryptionKeysGenerated ? 'green' : 'red'}
							></span>
							<span>{dashboardData?.encryptionKeysGenerated ? 'Sugeneruoti' : 'Nesugeneruoti'}</span
							>
						</div>
					</div>
				</div>
			</div>
		{/if}
	</div>
</div>
<SecretPhraseFormG />

<style>
	.header {
		margin-bottom: 2rem;
		font-weight: 500;
	}

	.dashboard-upper {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(16.625rem, 1fr));
		gap: 1.7rem;
	}

	.card {
		background-color: #fff;
		text-align: left;
		padding: 0 1.5rem;
		border: 1px solid #e5e7eb;
		box-shadow: 0 4px 12px rgb(15, 23, 42, 0.06);
		border-radius: 12px;
	}

	.summary-card {
		display: flex;
		align-items: center;
		gap: 1.5rem;
		min-width: 16.625rem;
		min-height: 7.5rem;
		border-radius: 10px;
	}

	.icon {
		padding: 0.8rem 1rem;
		border-radius: 10px;
	}

	.summary-card:nth-child(1) .icon {
		background-color: #e8e7f2;
	}

	.summary-card:nth-child(2) .icon {
		background-color: #e5f5ea;
	}

	.summary-card:nth-child(3) .icon {
		background-color: #e8f5f8;
	}

	.summary-card:nth-child(4) .icon {
		background-color: #feecdb;
	}

	.summary-card .number {
		font-size: 1.5rem;
		font-weight: 600;
	}

	.summary-card .text {
		display: flex;
		flex-direction: column;
		flex: 1;
		text-align: start;
		min-width: 0;
		gap: 0.3rem;
	}

	.dashboard-lower {
		display: grid;
		grid-template-columns: 1.4fr 1fr;
		gap: 1.7rem;
		margin-top: 2rem;
	}

	.dashboard-lower .card {
		padding: 1.5rem 2rem;
	}

	.activity-card {
		max-height: max-content;
	}

	.status-card {
		max-height: max-content;
	}

	.heading h2 {
		font-size: 1.2rem;
		font-weight: 600;
	}

	.status-card .items {
		display: flex;
		flex-direction: column;
		margin-top: 1rem;
	}

	.status-card .item {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) minmax(0, 8rem);
		gap: 1rem;
		align-items: center;
		padding: 1rem 0;
	}

	.status-card .item > * {
		min-width: 0;
	}

	.status-card .item:first-child {
		padding-top: 0;
	}

	.status-card .item:last-child {
		padding-bottom: 0;
	}

	.status-card .item:not(:last-child) {
		border-bottom: 1px solid #f2f2f2;
	}

	.status-card .status {
		display: flex;
		gap: 0.5rem;
		align-items: center;
	}

	.status-card .icon {
		display: flex;
		justify-content: center;
		align-items: center;
		background-color: #e5f5ea;
		padding: 0.6rem;
		width: fit-content;
	}

	.status .dot {
		width: 0.5rem;
		height: 0.5rem;
		border-radius: 999px;
	}

	.activity-card .items {
		margin-top: 1rem;
	}

	.activity-card .item {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(0, 9rem);
		align-items: center;
		gap: 2rem;
		padding: 1rem 0;
	}

	.activity-card .item:not(:last-child) {
		border-bottom: 1px solid #f2f2f2;
	}

	.activity-card .item:first-child {
		padding-top: 0;
	}

	.activity-card .item .text {
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
	}

	.activity-card .item .text p:first-child {
		font-size: 0.9rem;
		font-weight: 500;
	}

	.activity-card .item .text p:nth-child(2) {
		font-size: 0.75rem;
		color: #555;
	}

	.activity-card .item .date {
		font-size: 0.8rem;
		color: #888;
	}

	.view-all-btn {
		text-align: center;
		font-size: 0.8rem;
		font-weight: 700;
		color: #286fff;
	}

	.view-all-btn a {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
	}

	.skeleton {
		display: inline-block;
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

	@keyframes skeleton-loading {
		100% {
			transform: translateX(100%);
		}
	}

	.heading.skeleton {
		width: 40%;
		height: 1.2rem;
		border-radius: 12px;
	}

	.icon.skeleton {
		width: 3.87rem;
		height: 3.7rem;
	}

	.text .skeleton {
		width: 100%;
		height: 0.8rem;
		border-radius: 7px;
	}

	.text .skeleton:first-child {
		margin-bottom: 0.5rem;
	}

	.activity-card .text .skeleton:first-child {
		width: 30%;
	}

	.activity-card .text .skeleton:nth-child(2) {
		width: 80%;
		height: 0.6rem;
	}

	.date.skeleton {
		width: 100%;
		height: 0.6rem;
		border-radius: 7px;
		margin-left: auto;
	}

	.view-all-btn.skeleton {
		display: block;
		width: 30%;
		height: 0.9rem;
		border-radius: 7px;
		margin: auto;
		margin-top: 1rem;
	}

	.status-card .text.skeleton {
		max-width: 8rem;
		height: 0.8rem;
		border-radius: 7px;
	}

	.status-card .icon.skeleton {
		width: 2.45rem;
		height: 2.45rem;
		border-radius: 10px;
	}

	.status-card .dot.skeleton {
		width: 0.5rem;
		height: 0.5rem;
		border-radius: 999px;
	}

	.status-card .status .skeleton:nth-child(2) {
		width: 4rem;
		height: 0.8rem;
		border-radius: 7px;
	}

	.status .skeleton:not(.dot) {
		width: 100% !important;
		min-width: 0;
	}

	@media (max-width: 1200px) {
		.dashboard-lower {
			grid-template-columns: 1fr;
		}
	}

	@media (max-width: 768px) {
	}

	@media (max-width: 600px) {
		.dashboard {
			margin-top: 1rem;
		}
	}
</style>
