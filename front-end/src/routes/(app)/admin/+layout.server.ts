import { error } from '@sveltejs/kit';

export async function load({ parent }) {
	const { user } = await parent();

	if (user.role !== 'ADMIN') {
		throw error(404, 'Puslapis nerastas');
	}

	return { user };
}
