<script lang="ts">
	import {
		Send,
		WandSparkles,
		Clock,
		Copy,
		Check,
		ExternalLink,
		Link,
		Megaphone,
		CircleCheck
	} from 'lucide-svelte';
	import {
		toast,
		getTelegramCodeFromApi,
		fetchTelegramLinkStatusFromApi,
		formatDate,
		sendTestMessageToTelegram,
		type TelegramLinkStatus,
		unlinkTelegram
	} from '$lib';
	import { onMount, onDestroy } from 'svelte';
	let activeTab = $state<'account-settings' | 'telegram' | 'security'>('telegram');
	let telegramCode = $state<string>();
	let generatingCode = $state<boolean>();
	let copied = $state(false);
	let telegramLinkStatus = $state<TelegramLinkStatus | null>(null);
	let pollingInterval: ReturnType<typeof setInterval> | null = null;
	let loadingTelegramDetails = $state(true);

	onMount(() => {
		getTelegramLinkStatus();
		setTimeout(() => (loadingTelegramDetails = false), 1000);
	});

	onDestroy(() => {
		stopPolling();
	});

	async function getTelegramLinkStatus() {
		if (activeTab === 'telegram') {
			telegramLinkStatus = await fetchTelegramLinkStatusFromApi();
		}

		if (telegramLinkStatus?.telegramLinked) {
			stopPolling();
		}
	}

	function startPolling() {
		if (pollingInterval) return;

		pollingInterval = setInterval(() => {
			getTelegramLinkStatus();
		}, 2000);
	}

	function stopPolling() {
		if (pollingInterval) {
			clearInterval(pollingInterval);
			pollingInterval = null;
		}
	}

	async function onGenerateCode() {
		generatingCode = true;
		try {
			telegramCode = (await getTelegramCodeFromApi()).code;
			setTimeout(() => toast.success('Telegram susiejimo kodas sėkmingai sugeneruotas.'), 1000);
			startPolling();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko sugeneruoti kodo!');
		} finally {
			setTimeout(() => (generatingCode = false), 1000);
		}
	}

	async function copyLinkCommand() {
		await navigator.clipboard.writeText(`/link ${telegramCode}`);
		copied = true;
		setTimeout(() => (copied = false), 2000);
	}

	function maskValue(value: string | number) {
		const text = String(value);
		return `••••••${text.slice(-4)}`;
	}

	async function onTestMessageSend() {
		try {
			await sendTestMessageToTelegram();
			toast.success('Bandomasis pranešimas išsiųstas į Telegram!');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko išsiųsti žinutės į Telegram!');
		}
	}

	function onOpenTelegramBot() {
		// noopener = new tab cannot access/control my original page (new page redirecting my app tab to a fake login page)
		// noreferrer = do not tell the opened page where the user came from
		window.open('https://t.me/DataVaultReleaseBot', '_blank', 'noopener,noreferrer');
	}

	async function onUnlinkTelegram() {
		try {
			await unlinkTelegram();
			toast.success('Telegram paskyra sėkmingai atsieta!');
			telegramLinkStatus = null;
			telegramCode = undefined;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko atsieti Telegram!');
		}
	}
</script>

<svelte:head>
	<title>Nustatymai</title>
</svelte:head>

<div class="settings">
	<div class="header">
		<h2>Nustatymai</h2>
		<p>Susiekite arba atsiekite savo „Telegram“ paskyrą.</p>
	</div>
	<div class="body">
		<div class="sidebar">
			<ul>
				<li class:active={activeTab === 'telegram'}>
					<a
						href="#"
						onclick={(e) => {
							e.preventDefault();
							activeTab = 'telegram';
						}}><i><Send /></i><span>Telegram</span></a
					>
				</li>
			</ul>
		</div>
		{#if activeTab === 'telegram'}
			{#if loadingTelegramDetails}
				<div class="content skeleton-content">
					<div class="content-header">
						<div class="telegram-title-row">
							<span class="skeleton-circle"></span>
							<div class="skeleton-heading-group">
								<span class="skeleton-line title"></span>
								<span class="skeleton-line subtitle"></span>
							</div>
						</div>
						<span class="skeleton-pill"></span>
					</div>
					<div class="content-body">
						<div class="container connection-status">
							<div class="container-header">
								<span class="skeleton-line section-title"></span>
								<span class="skeleton-pill small"></span>
							</div>
							<div class="info-list">
								{#each Array(4) as _}
									<div class="info-row">
										<span class="skeleton-line label"></span>
										<span class="skeleton-line value"></span>
									</div>
								{/each}
							</div>
						</div>
						<div class="container notifications">
							<span class="skeleton-line section-title"></span>
							<span class="skeleton-line subtitle"></span>
							{#each Array(3) as _}
								<div class="notification-row">
									<span class="skeleton-icon"></span>
									<span class="skeleton-line notification-text"></span>
								</div>
							{/each}
						</div>
					</div>
					{#if telegramLinkStatus?.telegramLinked}
						<div class="telegram-actions skeleton-actions">
							<span class="skeleton-button"></span>
							<span class="divider"></span>
							<span class="skeleton-button"></span>
							<span class="divider"></span>
							<span class="skeleton-button"></span>
						</div>
					{/if}
				</div>
			{:else}
				<div class="content">
					<div class="content-header">
						<div class="icon"><img src="/img/telegram-logo2.png" alt="Telegram" /></div>
						<div class="text">
							<h3>Telegram integracija</h3>
							<p>Susiekite savo Telegram paskyrą, kad gautumėte pranešimus.</p>
						</div>
						<div
							class:linked={telegramLinkStatus?.telegramLinked}
							class:not-linked={!telegramLinkStatus?.telegramLinked}
							class="status-flag"
						>
							<span
								class:linked={telegramLinkStatus?.telegramLinked}
								class:not-linked={!telegramLinkStatus?.telegramLinked}
								class="status-indicator"
							></span>
							<span class="status-text">
								{telegramLinkStatus?.telegramLinked ? 'Susieta' : 'Nesusieta'}
							</span>
						</div>
					</div>
					{#if !telegramLinkStatus?.telegramLinked}
						<div class="content-body">
							<div class="container">
								<div class="connection">
									<div class="right">
										<h3>Susiejimas</h3>
										<p>Sugeneruokite susiejimo kodą ir susiekite savo Telegram paskyrą.</p>
									</div>
									<div class="left">
										<button class="red" onclick={onGenerateCode} disabled={generatingCode}
											><Link size={18} strokeWidth={1.5} />
											{generatingCode
												? 'Generuojama...'
												: telegramCode
													? 'Generuoti naują susiejimo kodą'
													: 'Generuoti susiejimo kodą'}</button
										>
									</div>
								</div>
								<div class="link-command">
									<p>Jūsų susiejimo komanda</p>
									<div class="command">
										{#if generatingCode}
											<code class="code-loading">/link ......</code>
										{:else}
											<code>/link {telegramCode ?? 'xxxxxx'}</code>
										{/if}
										<button onclick={copyLinkCommand} disabled={!telegramCode || generatingCode}>
											{#if copied}
												<Check size={20} color="green" />
											{:else}
												<Copy size={20} />
											{/if}
										</button>
									</div>
									<p><Clock size={18} /> Kodas galioja 2 min.</p>
								</div>
							</div>
							<div class="container">
								<div class="how-works">
									<h3>Kaip tai veikia?</h3>
									<div class="item">
										<span class="number">1</span>
										{#if telegramCode && !generatingCode}
											<a
												href="https://t.me/DataVaultReleaseBot"
												target="_blank"
												rel="noopener noreferrer"
												class="text telegram-link">@DataVaultReleaseBot per Telegram.</a
											>
										{:else}
											<span class="text">@DataVaultReleaseBot per Telegram.</span>
										{/if}
									</div>
									<div class="item">
										<span class="number">2</span><span class="text"
											>Išsiųskite sugeneruotą komandą.</span
										>
									</div>
									<div class="item">
										<span class="number">3</span><span class="text"
											>Grįžkite čia, kai susiejimas bus baigtas.</span
										>
									</div>
								</div>
							</div>
						</div>
					{:else}
						<div class="content-body">
							<div class="container">
								<div class="connection-status">
									<div class="heading">
										<h3>Susiejimo būsena</h3>
										<span class="status-active">Aktyvu</span>
									</div>
									<div class="info-list">
										<div class="info-row">
											<div class="info-label">
												<span>Telegram naudotojo vardas</span>
											</div>
											<span class="info-value"
												>{telegramLinkStatus?.telegramUsername ?? 'Nesusieta'}</span
											>
										</div>
										<div class="info-row">
											<div class="info-label">
												<span>Pokalbio būsena</span>
											</div>
											<span class="info-value active">Aktyvus</span>
										</div>
										<div class="info-row">
											<div class="info-label">
												<span>Susieta nuo</span>
											</div>
											<span class="info-value"
												>{formatDate(telegramLinkStatus?.connectedAt ?? 'Nesusieta')}</span
											>
										</div>
										<div class="info-row">
											<div class="info-label">
												<span>Pokalbio ID</span>
											</div>
											<span class="info-value">{maskValue(telegramLinkStatus?.chatId ?? '–')}</span>
										</div>
									</div>
								</div>
							</div>
							<div class="container">
								<div class="notifications-enabled">
									<h3>Pranešimai įjungti</h3>
									<p>Gausite toliau nurodytus pranešimus ir atnaujinimus.</p>
									<div class="items">
										<div class="item">
											<span class="icon"><Clock /></span>
											<span class="text">Atidėjimo laikotarpio įspėjimai</span>
										</div>
										<div class="item">
											<span class="icon"><Megaphone /></span>
											<span class="text">Politikos atskleidimo pranešimai</span>
										</div>
										<div class="item">
											<span class="icon"><CircleCheck /></span>
											<span class="text">Atskleidimo patvirtinimai</span>
										</div>
									</div>
								</div>
							</div>
							<div class="telegram-actions">
								<div class="buttons">
									<button class="send-btn" onclick={onTestMessageSend}
										><Send size={20} /> Siųsti bandomąjį pranešimą</button
									>
									<span class="divider"></span>
									<button class="open-bot-btn" onclick={onOpenTelegramBot}>
										<ExternalLink size={20} /> Atidaryti Telegram botą</button
									>
									<span class="divider"></span>
									<button class="uplink-btn" onclick={onUnlinkTelegram}
										><Link size={20} /> Atsieti „Telegram“</button
									>
								</div>
							</div>
						</div>
					{/if}
				</div>
			{/if}
		{/if}
	</div>
</div>

<style>
	.header {
		background-color: #fff;
		padding: 10px 20px;
		border-bottom: 1px solid var(--border-cl);
	}

	.header p {
		margin-top: 0.2rem;
		font-size: 0.8rem;
		opacity: 0.7;
	}

	.body {
		display: flex;
		gap: 1rem;
		margin-top: 1rem;
	}

	.sidebar {
		width: 250px;
		flex-shrink: 0;
	}

	.sidebar ul {
		list-style: none;
	}

	.sidebar ul li {
		padding: 0.4rem 0.7rem;
	}

	.sidebar a {
		position: relative;
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.7rem 0.9rem;
		border-radius: 10px;
	}

	.sidebar a:hover {
		background-color: #fbefef;
	}

	.sidebar a i {
		display: flex;
	}

	.sidebar a span {
		font-weight: 600;
		color: #6c6c6c;
	}

	.sidebar ul li.active a {
		background-color: #e3e3ef;
	}

	.sidebar ul li.active i {
		color: #940000;
	}

	.sidebar ul li.active span {
		color: #2d2d2d;
	}

	.sidebar ul li.active a::before {
		position: absolute;
		content: '';
		width: 4px;
		height: 80%;
		background-color: rgb(255, 34, 34);
		left: 4px;
		border-radius: 15px;
	}

	.sidebar,
	.content {
		background-color: #fff;
		border: 1px solid var(--border-cl);
		border-radius: 7px;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
	}

	.content {
		flex: 1;
		padding: 1rem 1.5rem;
	}

	.content-header {
		display: flex;
		align-items: center;
		gap: 1.5rem;
		margin-bottom: 1.5rem;
	}

	.content-header .icon img {
		width: 4rem;
	}

	.content-header .text h3 {
		font-size: 1.25rem;
		margin-bottom: 0.5rem;
	}

	.content-header .text p {
		font-weight: 600;
		font-size: 0.9rem;
		color: rgb(121, 121, 121);
	}

	.content-header .status-flag {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.4rem 0.7rem;
		border-radius: 10px;
		margin-left: auto;
		font-weight: 700;
		font-size: 0.8rem;
	}

	.content-header .status-flag .status-indicator {
		width: 0.625rem;
		height: 0.625rem;
		border-radius: 50%;
		display: inline-block;
	}

	.content-header .status-flag .status-indicator.not-linked {
		background-color: #940000;
	}

	.content-header .status-flag .status-indicator.linked {
		background-color: #006400;
	}

	.content-header .status-flag.not-linked {
		background-color: #fbefef;
		color: #940000;
	}

	.content-header .status-flag.linked {
		background-color: #e6f9e6;
		color: #006400;
	}

	.content-body {
		display: grid;
		grid-template-columns: 1.7fr 1fr;
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.container {
		border: 2px solid rgb(242, 242, 242);
		border-radius: 7px;
		padding: 1rem 0.9rem;
	}

	.connection {
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.connection .right h3 {
		margin-bottom: 0.5rem;
	}

	.connection .right p {
		font-size: 0.75rem;
		font-weight: 600;
		color: rgb(121, 121, 121);
	}

	.connection .left {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	.connection .left button {
		display: flex;
		align-items: center;
		padding: 1rem 0.3rem;
		gap: 0.3rem;
		background-color: #fff;
		border: 1px solid rgb(173, 173, 173);
		border-radius: 7px;
		font-weight: 600;
		font-size: 0.7rem;
		height: 50%;
	}

	.connection .left button.red {
		background-color: #940000;
		color: #fff;
		border-color: #940000;
	}

	.link-command {
		border: 2px solid rgb(242, 242, 242);
		border-radius: 7px;
		padding: 1rem 0.9rem;
		background-color: #fcfdfd;
	}

	.link-command p:first-child {
		font-size: 0.9rem;
		font-weight: 700;
		color: rgb(60, 60, 60);
		margin-bottom: 0.8rem;
	}

	.link-command .command {
		display: flex;
		justify-content: space-between;
		align-items: center;
		background-color: #fff;
		padding: 0.5rem 1rem;
		border: 1.5px solid rgb(242, 242, 242);
		border-radius: 7px;
		margin-bottom: 0.8rem;
	}

	.link-command .command code {
		letter-spacing: 1px;
		color: #000;
		font-weight: 500;
	}

	.link-command .command button {
		background-color: transparent;
		border: none;
		color: rgb(81, 81, 81);
	}

	.link-command p:last-child {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		font-size: 0.8rem;
		font-weight: 600;
		color: rgb(121, 121, 121);
	}

	.how-works .item {
		display: flex;
		align-items: center;
		padding: 1rem 0;
	}

	.how-works .number {
		display: inline-flex;
		justify-content: center;
		align-items: center;
		flex-shrink: 0;
		width: 3rem;
		height: 3rem;
		background-color: #e9ebf1;
		margin-right: 1rem;
		border-radius: 100%;
	}

	.how-works .text {
		overflow-wrap: anywhere;
	}

	.connection-status .heading {
		display: flex;
		justify-content: space-between;
		margin-bottom: 1rem;
	}

	.connection-status .heading .status-active {
		padding: 0.4rem 0.7rem;
		border-radius: 10px;
		font-weight: 700;
		font-size: 0.8rem;
		background-color: #ebf5ee;
		color: #338353;
	}

	.connection-status .info-list {
		border: 1px solid #eee;
		border-radius: 14px;
		padding: 0 1rem;
	}

	.connection-status .info-list .info-row {
		display: grid;
		grid-template-columns: 260px 1fr;
		align-items: center;
		padding: 0.9rem 0;
		border-bottom: 1px solid #eee;
	}

	.connection-status .info-list .info-row:last-child {
		border-bottom: none;
	}

	.connection-status .info-list .info-row .info-label {
		color: #666;
	}

	.connection-status .info-list .info-row .info-value {
		color: #222;
		font-weight: 500;
	}

	.connection-status .info-list .info-row .info-value.active {
		color: #198754;
	}

	.link-command .code-loading {
		opacity: 0.6;
		animation: pulse 1s infinite ease-in-out;
	}

	.notifications-enabled h3 {
		margin-bottom: 0.3rem;
	}

	.notifications-enabled p {
		font-size: 0.9rem;
		font-weight: 600;
		color: rgb(121, 121, 121);
		margin-bottom: 0.5rem;
	}

	.notifications-enabled .items .item {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.5rem;
	}

	.notifications-enabled .items .item .icon {
		display: inline-flex;
		justify-content: center;
		align-items: center;
		border-radius: 100%;
		width: 3rem;
		height: 3rem;
		background-color: #f0f8f3;
		color: #429065;
	}

	.notifications-enabled .items .item .text {
		font-weight: 600;
	}

	.telegram-actions .buttons {
		display: flex;
		justify-content: space-around;
	}

	.telegram-actions .buttons button {
		display: flex;
		align-items: center;
		padding: 0.5rem 1rem;
		gap: 0.5rem;
		background-color: transparent;
		border: 1px solid rgb(173, 173, 173);
		border-radius: 7px;
		font-weight: 600;
	}

	.telegram-actions .buttons .send-btn {
		color: #2f80ed;
		border: 1px solid #2f80ed;
		background-color: #eef6ff;
	}

	.telegram-actions .buttons .send-btn:hover {
		background-color: #1f6fd6;
		color: #fff;
	}

	.telegram-actions .buttons .uplink-btn {
		color: #c62828;
		border: 1px solid #ef5350;
		background-color: #fff5f5;
	}

	.telegram-actions .buttons .uplink-btn:hover {
		background-color: #a91f1f;
		color: #fff;
	}

	.telegram-actions .buttons .divider {
		width: 1px;
		align-self: stretch;
		background: #e5e7eb;
		flex-shrink: 0;
	}

	.skeleton-content {
		pointer-events: none;
	}

	.skeleton-line,
	.skeleton-circle,
	.skeleton-pill,
	.skeleton-icon,
	.skeleton-button {
		position: relative;
		overflow: hidden;
		background-color: rgba(0, 0, 0, 0.1);
	}

	.skeleton-line::after,
	.skeleton-circle::after,
	.skeleton-pill::after,
	.skeleton-icon::after,
	.skeleton-button::after {
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

	.skeleton-circle {
		display: inline-block;
		width: 4rem;
		height: 4rem;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.skeleton-pill {
		display: inline-block;
		max-width: 6rem;
		width: 100%;
		height: 1.8rem;
		border-radius: 10px;
		margin-left: auto;
	}

	.skeleton-pill.small {
		max-width: 5rem;
		width: 100%;
		height: 2rem;
	}

	.skeleton-heading-group {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		max-width: 26rem;
		width: 100%;
	}

	.skeleton-line {
		display: block;
		height: 0.875rem;
		border-radius: 999px;
	}

	.skeleton-line.title {
		max-width: 20rem;
		width: 100%;
		height: 1.75rem;
	}

	.skeleton-line.subtitle {
		width: 100%;
		height: 1.125rem;
	}

	.skeleton-line.section-title {
		max-width: 16.25rem;
		width: 100%;
		height: 1.625rem;
	}

	.notifications .skeleton-line.section-title {
		margin-bottom: 1rem;
	}

	.skeleton-line.label {
		max-width: 13.75rem;
		width: 100%;
		height: 1.375rem;
	}

	.skeleton-line.value {
		max-width: 11.875rem;
		min-width: 5rem;
		width: 100%;
		height: 1.375rem;
	}

	.skeleton-line.notification-text {
		width: 100%;
		height: 1.375rem;
	}

	.telegram-title-row {
		display: flex;
		align-items: center;
		gap: 2rem;
		width: 100%;
		min-width: 0;
	}

	.container-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1.5rem;
	}

	.info-list {
		margin-top: 1rem;
		border: 1px solid #eee;
		border-radius: 18px;
		padding: 0 1.5rem;
	}

	.info-row {
		display: grid;
		grid-template-columns: 260px 1fr;
		align-items: center;
		padding: 1.25rem 0;
		border-bottom: 1px solid #eee;
	}

	.info-row:last-child {
		border-bottom: none;
	}

	.notification-row {
		display: flex;
		align-items: center;
		gap: 1rem;
		margin-top: 1.5rem;
	}

	.skeleton-icon {
		display: inline-block;
		width: 3rem;
		height: 3rem;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.skeleton-actions {
		display: flex;
		align-items: center;
		gap: 2rem;
	}

	.skeleton-button {
		display: inline-block;
		width: 10rem;
		height: 2rem;
		border-radius: 12px;
	}

	.divider {
		width: 1px;
		align-self: stretch;
		background-color: #e5e7eb;
		flex-shrink: 0;
	}

	.text.telegram-link {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		color: #229ed9;
		font-weight: 600;
		text-decoration: none;
	}

	.text.telegram-link:hover {
		text-decoration: underline;
	}

	@keyframes pulse {
		0%,
		100% {
			opacity: 0.35;
		}
		50% {
			opacity: 1;
		}
	}

	@media (max-width: 1200px) {
		.content-body {
			grid-template-columns: 1fr;
		}
	}

	@media (max-width: 768px) {
	}

	@media (max-width: 600px) {
		.info-row {
			grid-template-columns: 1fr 0.5fr !important;
			gap: 1rem;
		}

		.settings {
			margin-top: 1rem;
		}

		.content-body .connection {
			flex-direction: column;
			gap: 0.5rem;
			margin-bottom: 0.5rem;
		}

		.content-body .connection .left button {
			width: 100%;
			justify-content: center;
			padding: 0.5rem;
		}

		.content-header {
			flex-direction: column;
			align-items: flex-start;
			gap: 0.5rem;
			margin-bottom: 0.5rem;
		}

		.content-header .text h3 {
			margin-bottom: 0.2rem;
		}

		.content-header .status-flag {
			margin-left: 0;
		}
	}
</style>
