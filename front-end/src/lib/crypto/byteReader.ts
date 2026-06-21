/**
 * ByteReader reads a `ReadableStream<Uint8Array>` as a continuous byte stream through this we can
 * request an exact number of bytes at a time.
 *
 * `reader.read()` returns arbitrary-sized chunks. This class buffers incoming chunks
 * so we ca reliably read fixed-size pieces.
 *
 * Behavior:
 * - `readExact(n)` returns exactly `n` bytes when available.
 * - Returns `null` if the stream ends before `n` bytes can be collected.
 * - Any extra bytes read are kept internally for the next call.
 */

export class ByteReader {
	private buf = new Uint8Array(0);
	constructor(private reader: ReadableStreamDefaultReader<Uint8Array>) {}

	private concat(a: Uint8Array, b: Uint8Array) {
		const out = new Uint8Array(a.length + b.length);
		/**
		 * Copying all bytes of "a" into "out" starting at index 0
		 * Copying all bytes of b into out starting right after a, so b is appended
		 */
		out.set(a, 0);
		out.set(b, a.length);
		return out;
	}

	async readExact(n: number): Promise<Uint8Array | null> {
		while (this.buf.length < n) {
			const { value, done } = await this.reader.read();
			if (done) return null;
			this.buf = this.concat(this.buf, value);
		}
		const out = this.buf.slice(0, n);
		this.buf = this.buf.slice(n);
		return out;
	}
}
