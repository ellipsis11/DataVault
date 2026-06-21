import type { FileMeta } from '$lib/crypto/encryptFileMeta';
import type {
	FileMetaWithId,
	FileRow,
	FileMetaFull,
	FileMetaFullPage,
	StorageType
} from './fileTypes';

export function patchRowWithMeta(
	rows: FileRow[],
	fileId: string,
	fileMeta: FileMeta,
	fileMetaFull: FileMetaFull
): FileRow[] {
	return rows.map((f) =>
		f.id === fileId
			? {
					...f,
					fileMeta,
					storageType: fileMetaFull?.storageType,
					storageRef: fileMetaFull?.storageRef,
					loading: false
				}
			: f
	);
}

export function patchRowsWithMetas(
	rows: FileRow[],
	filesMetasWithId: FileMetaWithId[],
	fileMetaFullPage: FileMetaFullPage
): FileRow[] {
	const filesMetaskeyedByFileId = new Map<String, FileMetaWithId>(
		filesMetasWithId.map((f) => [f.id, f])
	);
	const metasFullkeyedByFileId = new Map<String, FileMetaFull>(
		fileMetaFullPage.fileMetaFull.map((f) => [f.id, f])
	);
	return rows.map((f) => {
		const fId = f.id;
		const fileMeta = filesMetaskeyedByFileId.get(fId);
		if (!fileMeta) throw new Error(`File key not found in filesMetasWithId ${fId}`);
		const metasF = metasFullkeyedByFileId.get(fId);
		if (!metasF) throw new Error(`File key not found in metasFromApi ${fId}`);
		return {
			...f,
			fileMeta,
			storageType: metasF.storageType,
			storageRef: metasF.storageRef,
			loading: false
		};
	});
}

export function setLoading<T extends { id: string; loading?: boolean }>(
	rows: T[],
	loading: boolean,
	fileId?: string
): T[] {
	return rows.map((f) =>
		fileId == null ? { ...f, loading } : f.id === fileId ? { ...f, loading } : f
	);
}

export function hideRowMeta(rows: FileRow[], fileId: string): FileRow[] {
	return rows.map((f) =>
		f.id === fileId
			? {
					...f,
					fileMeta: undefined,
					expiresAt: undefined,
					loading: false
				}
			: f
	);
}

export function hideAllRowsMeta(rows: FileRow[]): FileRow[] {
	return rows.map((f) => ({
		...f,
		fileMeta: undefined,
		expiresAt: undefined,
		loading: false
	}));
}

export function preserveOldExtension(oldName: string, newName: string): string {
	const trimmed = newName.trim();

	const oldDotIndex = oldName.lastIndexOf('.');
	const newDotIndex = trimmed.lastIndexOf('.');

	const oldExtension = oldDotIndex > 0 ? oldName.slice(oldDotIndex) : '';

	if (newDotIndex > 0) {
		return trimmed;
	}

	return trimmed + oldExtension;
}

export function storageConverter(storageType: StorageType) {
	switch (storageType) {
		case 'AWS_S3':
			return 'AWS S3';
		case 'FILEBASE_IPFS':
			return 'Filebase IPFS';
	}
}
