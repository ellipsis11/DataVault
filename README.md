# DataVault

A secure web application for confidential data storage and conditional disclosure.

The system allows users to upload files, encrypt them on the client side, store encrypted data using cloud storage, and manage conditional disclosure policies based on inactivity or a selected release date.

## Tech Stack

### Back-end

- Java
- Spring Boot
- PostgreSQL
- OAuth
- JWT
- AWS S3 / IPFS Filebase

### Front-end

- SvelteKit
- TypeScript
- libsodium-wrappers-sumo

## Security and Cryptography

DataVault uses client-side encryption, that means, that the file content is encrypted in the browser before it is uploaded to the server. Selected storage stores only encrypted files (objects) and does not receive plaintext file content. Files metadata also is encrypted in the client side and stored in DB as bytes.

### Cryptographic Algorithms and Concepts

- **XChaCha20-Poly1305** – authenticated encryption for confidentiality, integrity, and authenticity of file data.
- **X25519 / ECDH** – elliptic-curve Diffie-Hellman based key agreement used for deriving shared encryption keys for recipient-based access control.
- **Ed25519** – elliptic-curve digital signature scheme for authenticity and integrity.
- **Argon2id** – password hashing and key derivation function resistant to brute-force attacks.
- **KDF** – used to derive cryptographic subkeys from a master key.
- **CSPRNG** – used to generate master keys, nonces, salts, and other cryptographically secure random values.
- **Master Key** – client-side generated root secret used to derive encryption keys.
- **Password Root Key** – derived from the user password using argon2id and used to encrypt/protect the master key. The password itself is never used directly for file encryption.

### Key Management

Each user has a client-side generated master key. The master key is encrypted before being stored, so the backend cannot access it in plaintext.

File encryption keys are derived from the master key on the client side whenever needed and are never stored.

This design separates authentication from encryption and reduces the risk of exposing file encryption keys in the event of server-side compromise or credential leakage.

### Zero-Knowledge Approach

The application follows a zero-knowledge-inspired model:

- Files are encrypted before upload.
- Plaintext file content is never sent to the backend.
- The backend stores only encrypted file data and encrypted metadata.
- Decryption happens on the client side.
- Storage providers such as AWS S3 or IPFS/Filebase only receive encrypted objects.

## Main Features

- User authentication (Form / OAuth)
- Client-side file encryption
- Secure file upload and download
- Conditional disclosure policies
- Inactivity-based release
- Date/time-based release
- Audit logging
- Telegram notifications
- Admin control panel

## Project Demo

<p align="center">
  <video src="https://github.com/user-attachments/assets/60dbe9de-b72d-4fde-83cc-d0fd3a63349c" width="90%" controls></video>
</p>
