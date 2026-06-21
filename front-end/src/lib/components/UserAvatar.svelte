<script lang="ts">
	import { type Color } from '$lib/users/userTypes';

	type Props = {
		email: string | null;
		width?: number;
		height?: number;
		fontSize?: number;
		onColorGenerated?: (color: Color) => void;
	};

	let { email, width, height, fontSize, onColorGenerated }: Props = $props();

	const initials = $derived(() => {
		if (!email) return '?';

		const username = email.split('@')[0].trim();
		if (!username) return '?';

		// Filter throws out the empty parts
		const parts = username.split(/[._-]/).filter(Boolean);

		if (parts.length >= 2) {
			return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
		}

		return username.slice(0, 1).toUpperCase();
	});

	const avatarColors = [
		{ bg: '#f1ecff', text: '#6d47d9', border: '#ded3ff' },
		{ bg: '#e8fbf8', text: '#008f83', border: '#bdeee7' },
		{ bg: '#edf2ff', text: '#4568e8', border: '#d5defe' },
		{ bg: '#fff4e8', text: '#c46a1d', border: '#ffe1bd' },
		{ bg: '#fff0f6', text: '#c2255c', border: '#ffd6e7' },
		{ bg: '#eefaf0', text: '#2b8a3e', border: '#d3f3d9' }
	];

	/**
	 * Generating a deterministic (predictable / always the same based on the same input) color index for a given string value.
	 *
	 * Generating string hash by iterating through each
	 * character of the provided value and combining its character code with
	 * the current hash value.
	 *
	 * (hash << 5) - hash means bit push to left in 5 positions (32) ===
	 * to multiplying the hash by 31
	 *
	 * (hash << 5) - hash
	 * = (10 * 32) - 10
	 * = 320 - 10
	 * = 310
	 * = 10 * 31
	 *
	 * Result always between 0 and 7, because of abs and modulo.
	 */
	function getColorIndex(value: string | null) {
		if (!value) return 0;
		let hash = 0;

		for (let i = 0; i < value.length; i++) {
			hash = value.charCodeAt(i) + ((hash << 5) - hash);
		}

		return Math.abs(hash) % avatarColors.length;
	}

	const color = $derived(avatarColors[getColorIndex(email)]);

	$effect(() => {
		if (color) {
			onColorGenerated?.(color);
		}
	});
</script>

<div
	class="avatar"
	style:background-color={color.bg}
	style:color={color.text}
	style:border-color={color.border}
	style:width={`${width ?? 2.5}rem`}
	style:height={`${height ?? 2.5}rem`}
	style:font-size={`${fontSize ?? 1.2}rem`}
>
	{initials()}
</div>

<style>
	.avatar {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		border-radius: 999px;
		border: 1px solid;
		font-weight: 900;
	}
</style>
