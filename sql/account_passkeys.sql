USE kopfzentruminventardb;

CREATE TABLE IF NOT EXISTS account_passkeys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    credential_id VARCHAR(512) NOT NULL UNIQUE,
    public_key TEXT NOT NULL,
    device_name VARCHAR(255),
    sign_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    active BOOLEAN DEFAULT TRUE,

    CONSTRAINT fk_account_passkeys_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
);
