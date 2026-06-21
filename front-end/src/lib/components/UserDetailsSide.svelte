<script lang="ts">
	import {
		User as UserI,
		Shield,
		Check,
		X,
		Logs,
		CloudUpload,
		FileLock,
		ShieldCheck,
		Trash2,
		BarChart3,
		FileClock,
		ChevronRight,
		ChevronUp,
		ArrowLeft
	} from 'lucide-svelte';
	import {
		formatDate,
		type AdminAuditLogListItemDetails,
		type AdminUserDetails,
		type Color,
		type User
	} from '$lib';
	import UserAvatar from './UserAvatar.svelte';
	import JsonPreview from './JsonPreview.svelte';
	import { goto } from '$app/navigation';
	let {
		userDetails,
		initLoading,
		onDeleteUser,
		user,
		onSelectedLog,
		adminAuditLogDetails,
		onCloseUserDetailsDialog
	} = $props<{
		userDetails: AdminUserDetails | null;
		initLoading: boolean;
		onDeleteUser: (userId: string) => void;
		user: User;
		onSelectedLog: (logId: string) => void;
		adminAuditLogDetails: AdminAuditLogListItemDetails | null;
		onCloseUserDetailsDialog: () => void;
	}>();

	let elemetsColor = $state<Color>();
	let selectedLog = $state<string | null>(null);

	function onColorGenerated(color: Color) {
		elemetsColor = color;
	}

	function toggleLog(logId: string) {
		selectedLog = selectedLog === logId ? null : logId;
		if (selectedLog) onSelectedLog(selectedLog);
	}
</script>

<div class="header">
	<div class="go-back-btn">
		<button onclick={onCloseUserDetailsDialog}
			><ArrowLeft size={18} strokeWidth={1.5} />Atgal</button
		>
	</div>
	{#if initLoading}
		<div class="header-top">
			<div class="profile-icon">
				<div class="skeleton-line skeleton-icon"></div>
			</div>
			<div class="details">
				<div class="skeleton-line skeleton-title"></div>
				<div class="skeleton-line skeleton-p"></div>
				<div class="skeleton-line skeleton-pill"></div>
			</div>
		</div>
	{:else}
		<div class="header-top">
			<div>
				<UserAvatar
					email={userDetails?.email}
					width={4}
					height={4}
					fontSize={1.7}
					{onColorGenerated}
				/>
			</div>
			<div class="details">
				<p class="email">{userDetails?.email ?? '–'}</p>
				<p class="id">{userDetails?.id ?? '–'}</p>
				<p
					class="role-badge badge"
					style:background-color={elemetsColor?.bg}
					style:color={elemetsColor?.text}
					style:border-color={elemetsColor?.border}
				>
					{userDetails?.role ?? '–'}
				</p>
			</div>
		</div>
	{/if}
</div>
<div class="body">
	{#if initLoading}
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
	{:else}
		<div class="account block">
			<div class="heading">
				<i><UserI size={25} color={elemetsColor?.text} /></i>
				<h3>Paskyra</h3>
			</div>
			<div class="item-block">
				<div class="item">
					<span>El. paštas</span>
					<span class="value">{userDetails?.email ?? '–'}</span>
				</div>
				<div class="item">
					<span>Naudotojo ID</span>
					<span class="value">{userDetails?.id ?? '–'}</span>
				</div>
				<div class="item">
					<span>Rolė</span>
					<span
						class="role-badge badge value"
						style:background-color={elemetsColor?.bg}
						style:color={elemetsColor?.text}
						style:border-color={elemetsColor?.border}
						data-status={userDetails?.role ?? '–'}>{userDetails?.role ?? '–'}</span
					>
				</div>
				<div class="item">
					<span class="value">Sukurta</span>
					<span>{formatDate(userDetails?.createdAt ?? '–')}</span>
				</div>
				<div class="item">
					<span class="value">Paskutinis prisijungimas</span>
					<span>{formatDate(userDetails?.lastLoginAt ?? '–')}</span>
				</div>
			</div>
		</div>
		{#if user.userId !== userDetails?.id}
			<div class="security-access block">
				<div class="heading">
					<i><Shield size={25} color={elemetsColor?.text} /></i>
					<h3>Sauga ir prieiga</h3>
				</div>
				<div class="item-block">
					{#if userDetails?.telegramLinked}
						<div class="item">
							<span>Telegram prijungtas</span>
							<span class="telegram-badge badge" data-status="true"
								><i><Check size={13} strokeWidth={3} /></i>Taip</span
							>
						</div>
						<div class="item">
							<span>Telegram vardas</span>
							<span>{userDetails?.telegramUsername ?? '–'}</span>
						</div>
						<div class="item">
							<span>Prijungta</span>
							<span>{formatDate(userDetails?.telegramConnectedAt ?? '–')}</span>
						</div>
					{:else}
						<div class="item">
							<span>Telegram prijungtas</span>
							<span class="telegram-badge badge" data-status="false"
								><i><X size={13} strokeWidth={3} /></i>Ne</span
							>
						</div>
					{/if}
					{#if userDetails?.keysGenerated}
						<div class="item">
							<span>Raktai sugeneruoti</span>
							<span class="telegram-badge badge" data-status="true"
								><i><Check size={13} strokeWidth={3} /></i>Taip</span
							>
						</div>
						<div class="item">
							<span>Sugeneruota</span>
							<span>{formatDate(userDetails?.keysGeneratedAt ?? '–')}</span>
						</div>
					{:else}
						<div class="item">
							<span>Raktai sugeneruoti</span>
							<span class="telegram-badge badge" data-status="false"
								><i><X size={13} strokeWidth={3} /></i>Ne</span
							>
						</div>
					{/if}
				</div>
			</div>
		{/if}
		{#if user.userId !== userDetails?.id}
			<div class="activity-summary block">
				<div class="heading">
					<i><BarChart3 size={25} color={elemetsColor?.text} /></i>
					<h3>Veiklos suvestinė</h3>
				</div>
				<div class="item-block">
					<div class="item">
						<div
							class="icon"
							style:background-color={elemetsColor?.bg}
							style:color={elemetsColor?.text}
						>
							<i><Logs size={18} /> </i>
						</div>
						<div class="details">
							<p>Audito registrai</p>
							<p>{userDetails?.logCount ?? 0}</p>
						</div>
					</div>
					<div class="item">
						<div
							class="icon"
							style:background-color={elemetsColor?.bg}
							style:color={elemetsColor?.text}
						>
							<i><ShieldCheck size={18} /></i>
						</div>
						<div class="details">
							<p>Sukurtos politikos</p>
							<p>{userDetails?.ownedPolicyCount ?? 0}</p>
						</div>
					</div>
					<div class="item">
						<div
							class="icon"
							style:background-color={elemetsColor?.bg}
							style:color={elemetsColor?.text}
						>
							<i><FileLock size={18} /> </i>
						</div>
						<div class="details">
							<p>Gautos politikos</p>
							<p>{userDetails?.receivedPolicyCount ?? 0}</p>
						</div>
					</div>
					<div class="item">
						<div
							class="icon"
							style:background-color={elemetsColor?.bg}
							style:color={elemetsColor?.text}
						>
							<i><CloudUpload size={18} /></i>
						</div>
						<div class="details">
							<p>Įkelti failai</p>
							<p>{userDetails?.uploadedFileCount ?? 0}</p>
						</div>
					</div>
				</div>
			</div>
		{/if}
		<div class="recent-logs block">
			<div class="heading">
				<i><FileClock size={25} color={elemetsColor?.text} /></i>
				<h3>
					{user.userId !== userDetails?.id ? 'Naudotojo audito įrašai' : 'Jūsų audito įrašai'}
				</h3>
				<button
					onclick={() => goto(`/admin/logs?userId=${userDetails?.id}`)}
					style:color={elemetsColor?.text}
					>{user.userId !== userDetails?.id
						? 'Visi naudotojo audito įrašai'
						: 'Visi mano audito įrašai'}</button
				>
			</div>
			{#if userDetails?.recentLogs}
				<div class="item-block">
					{#each userDetails?.recentLogs.slice(0, 5) as log (log.id)}
						<button
							class="item"
							class:active={selectedLog === log.id}
							onclick={() => toggleLog(log.id)}
						>
							<span class="log-badge badge" data-status={log.level}>{log.level}</span>
							<span class="action">{log.actionType}</span>
							<span>{formatDate(log.createdAt ?? '–')}</span>
							{#if selectedLog !== log.id}
								<span style:color={elemetsColor?.text}><ChevronRight /></span>
							{:else}
								<span style:color={elemetsColor?.text}><ChevronUp /></span>
							{/if}
						</button>
						{#if selectedLog === log.id}
							<div class="log-details" style:border-left-color={elemetsColor?.text}>
								<div class="log-items">
									<div class="item">
										<span>Pranešimas</span>
										<span>{adminAuditLogDetails?.message ?? '–'}</span>
									</div>
									<div class="item">
										<span>Metaduomenys</span>
										<div class="metabloc">
											<JsonPreview
												json={adminAuditLogDetails?.metaData}
												padding={0.2}
												fontSize={0.7}
											/>
										</div>
									</div>
								</div>
							</div>
						{/if}
					{/each}
				</div>
			{:else}
				<p>Audito įrašų dar nėra.</p>
			{/if}
		</div>
		{#if user.userId !== userDetails?.id}
			<div class="delete-user">
				<button type="button" onclick={() => onDeleteUser(userDetails?.id)}
					><Trash2 size={20} />Ištrinti naudotoją</button
				>
			</div>
		{/if}
	{/if}
</div>

<style>
	button {
		background: transparent;
		border: none;
	}
	.header {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.header-top {
		display: flex;
		gap: 1rem;
		align-items: center;
	}

	.badge {
		width: fit-content;
		padding: 0.25rem 0.5rem;
		border-radius: 5px;
		font-weight: 900;
	}

	.badge {
		font-size: 0.8rem !important;
	}

	.role-badge[data-status='ADMIN'] {
		background-color: #f1ecff;
		color: #6d47d9;
		border: 1px solid #ded3ff;
	}

	.role-badge[data-status='USER'] {
		background-color: #eef6ff;
		color: #2563eb;
		border: 1px solid #bfdbfe;
	}

	.details {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		margin-top: 0.7rem;
	}

	.details .email {
		font-size: 1.2rem;
		font-weight: 700;
	}

	.details .id {
		font-size: 0.8rem;
		color: #555d71;
		font-weight: 600;
	}

	.body {
		display: flex;
		flex-direction: column;
		gap: 0.9rem;
	}

	.block {
		display: flex;
		flex-direction: column;
		padding: 1rem;
		border: 1.5px solid #ededed;
		border-radius: 7px;
	}

	.block .heading {
		display: flex;
		align-items: center;
		gap: 0.7rem;
	}

	i {
		display: flex;
	}

	.item .role-badge {
		padding: 0.25rem 0.5rem;
		border-radius: 5px;
		font-size: 0.75rem;
		font-weight: 900;
	}

	.item:not(.item-block.activity-summary) {
		display: grid;
		grid-template-columns: 150px minmax(0, 1fr);
		gap: 1rem;
		padding: 1rem 0;
	}

	.item .value:not(.item-block.activity-summary) {
		min-width: 0;
		overflow-wrap: anywhere;
	}

	.item:not(:last-child) {
		border-bottom: 1.5px solid #ededed;
	}

	.item span {
		color: #555d71;
		font-size: 0.9rem;
		font-weight: 600;
	}

	.telegram-badge {
		display: flex;
		align-items: center;
		gap: 0.2rem;
	}

	.telegram-badge[data-status='true'] {
		background-color: #e9fbef;
		color: #43c67e;
		border: 1px solid #def9e8;
	}

	.telegram-badge[data-status='false'] {
		background-color: #fef2f2;
		color: #dc2626;
		border: 1px solid #fee2e2;
	}

	.log-badge[data-status='INFO'] {
		background: #e0f2fe;
		color: #0369a1;
	}

	.log-badge[data-status='ALERT'] {
		background: #ffedd5;
		color: #c2410c;
	}

	.activity-summary .item-block {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 0.6rem;
		margin-top: 0.7rem;
	}

	.activity-summary .item-block .item {
		border: 1px solid rgb(230, 230, 230);
		border-radius: 7px;
		display: flex;
		align-items: center;
		gap: 0.8rem;
		padding: 0.6rem;
	}

	.activity-summary .item-block .item .icon {
		padding: 0.7rem;
		border-radius: 999px;
	}

	.skeleton {
		display: inline-block;
		border-radius: 999px;
		width: 100%;
		position: relative;
		overflow: hidden;
		background: rgba(0, 0, 0, 0.1);
	}

	.activity-summary .item-block .item .details p:first-child {
		font-size: 0.55rem;
		font-weight: 700;
		color: #6a6e83;
	}

	.activity-summary .item-block .item .details p:last-child {
		font-weight: 700;
		color: #161e36;
	}

	.recent-logs .item-block .item {
		display: grid;
		grid-template-columns: 4rem minmax(0, 1fr) 10rem 1.5rem;
		align-items: center;
		gap: 1rem;
		width: 100%;
		padding: 1rem 0;
		box-sizing: border-box;
	}

	.recent-logs .item-block .item .action {
		min-width: 0;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.recent-logs .item-block .item.active {
		border: none;
		padding-bottom: 0.4rem;
	}

	.recent-logs .item-block .item span {
		font-size: 0.8rem;
	}

	.recent-logs .heading button {
		margin-left: auto;
		font-weight: 700;
	}

	.log-details {
		padding: 0 0.5rem;
		border-left: 3px solid;
		border-radius: 3px;
	}

	.log-details .log-items {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.log-details .item {
		grid-template-columns: 1fr 1fr !important;
		border: none;
		padding: 0 !important;
	}

	.metabloc {
		font-size: 0.75rem;
	}

	.delete-user button {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.5rem;
		font-weight: 700;
		padding: 0.5rem;
		background-color: #fef5f5;
		color: #e52629;
		border: 1px solid #e52629;
		border-radius: 7px;
		width: 100%;
	}

	.delete-user button:hover {
		background-color: #ff6969;
		color: white;
		border-color: #ff2222;
		cursor: pointer;
	}

	.skeleton-title {
		width: 15.625rem;
		height: 1.4rem;
	}

	.skeleton-p {
		margin-top: 0.3rem;
		width: 15.625rem;
		height: 0.8rem;
	}

	.skeleton-pill {
		display: inline-block;
		width: 3.2rem;
		height: 1.5rem;
		border-radius: 5px;
	}

	.skeleton-block {
		width: 100%;
		height: 5rem;
		border-radius: 1rem;
	}

	.skeleton-icon {
		width: 4rem;
		height: 4rem;
	}

	.skeleton-block:not(:first-child) {
		margin-top: 0.9375rem;
	}
</style>
