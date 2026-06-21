CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE,
    password_hash TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,

    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE user_keys(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    encrypted_master_key BYTEA NOT NULL,
    kdf_salt BYTEA NOT NULL,
    kdf_params JSONB NOT NULL,
    wrap_nonce BYTEA NOT NULL,
    vault_version int NOT NULL,
    kdf_contexts JSONB NOT NULL,
    recipient_public_key BYTEA NOT NULL,
    encrypted_recipient_private_key BYTEA NOT NULL,
    recipient_private_key_wrap_nonce BYTEA NOT NULL,
    owner_sign_public_key BYTEA NOT NULL,
    encrypted_owner_sign_private_key BYTEA NOT NULL,
    owner_sign_private_key_wrap_nonce BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE files(
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_type VARCHAR(16) NOT NULL,
    storage_ref TEXT NOT NULL,
    subkey_id BIGINT NOT NULL,
    content_hash bytea NOT NULL,
    meta_nonce BYTEA NOT NULL,
    meta_cipher BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (owner_user_id, subkey_id),
    UNIQUE (owner_user_id, content_hash),

    CONSTRAINT chk_file_storage_type CHECK (storage_type IN ('AWS_S3', 'FILEBASE_IPFS'))
);

CREATE TABLE conditional_release_policies(
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    policy_name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    inactivity_days INTEGER,
    grace_days INTEGER,
    scheduled_release_at TIMESTAMPTZ,
    warning_channel VARCHAR(32) NOT NULL DEFAULT 'TELEGRAM',
    warning_every_hours INTEGER,
    warn_before_days INTEGER,
    last_heartbeat_at TIMESTAMPTZ,
    grace_started_at TIMESTAMPTZ,
    last_warning_sent_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_inactivity_days
            CHECK (inactivity_days IS NULL OR inactivity_days > 0),
     CONSTRAINT chk_grace_days
            CHECK (grace_days IS NULL OR grace_days > 0),
    CONSTRAINT chk_warning_every_hours
            CHECK (warning_every_hours IS NULL OR warning_every_hours > 0),
    CONSTRAINT chk_warn_before_days
            CHECK (warn_before_days IS NULL OR warn_before_days > 0)
);

CREATE TABLE conditional_release_policy_recipients(
    policy_id UUID NOT NULL REFERENCES conditional_release_policies(id) ON DELETE CASCADE,
    recipient_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_policy_key_for_recipient BYTEA NOT NULL,
    manifest JSONB NOT NULL,
    signature_by_owner BYTEA NOT NULL,
    access_count INTEGER NOT NULL DEFAULT 0,
    first_access_at TIMESTAMPTZ,
    last_access_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (policy_id, recipient_user_id)
);

CREATE TABLE conditional_release_policy_files(
    policy_id UUID NOT NULL REFERENCES conditional_release_policies(id) ON DELETE CASCADE,
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    encrypted_file_key_by_policy BYTEA NOT NULL,
    encrypted_file_key_nonce BYTEA NOT NULL,
    encrypted_meta_key_by_policy BYTEA NOT NULL,
    encrypted_meta_key_nonce BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (policy_id, file_id)
);

CREATE TABLE user_telegram_links (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    telegram_chat_id BIGINT NOT NULL UNIQUE,
    telegram_username VARCHAR(255),
    connected_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE telegram_link_codes (
    code VARCHAR(6) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE TABLE audit_logs(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(255),
    audit_level VARCHAR(16) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    previous_hash BYTEA,
    current_hash BYTEA NOT NULL
);

CREATE TABLE oauth_accounts(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,
    provider_user_id TEXT NOT NULL,
    username TEXT,

    UNIQUE(provider, provider_user_id)
);