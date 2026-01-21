-- Protocol Analysis Schema
-- Run with: mysql -uroot -Dqlink < ./protocol_schema.sql

-- Table for storing captured protocol messages
CREATE TABLE IF NOT EXISTS protocol_captures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(50),
    user_handle VARCHAR(20),
    state_name VARCHAR(50),
    direction ENUM('INBOUND', 'OUTBOUND') NOT NULL,
    mnemonic CHAR(2),
    action_class VARCHAR(50),
    raw_hex TEXT,
    payload_hex TEXT,
    is_unknown BOOLEAN DEFAULT FALSE,
    INDEX idx_timestamp (timestamp),
    INDEX idx_mnemonic (mnemonic),
    INDEX idx_user_handle (user_handle),
    INDEX idx_is_unknown (is_unknown)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Summary view for unknown mnemonics
CREATE OR REPLACE VIEW unknown_mnemonic_summary AS
SELECT
    mnemonic,
    COUNT(*) as occurrences,
    COUNT(DISTINCT user_handle) as unique_users,
    COUNT(DISTINCT state_name) as unique_states,
    MIN(timestamp) as first_seen,
    MAX(timestamp) as last_seen
FROM protocol_captures
WHERE is_unknown = TRUE
GROUP BY mnemonic
ORDER BY occurrences DESC;

-- Summary view for traffic by state
CREATE OR REPLACE VIEW state_traffic_summary AS
SELECT
    state_name,
    direction,
    COUNT(*) as message_count,
    COUNT(DISTINCT mnemonic) as unique_mnemonics,
    SUM(CASE WHEN is_unknown THEN 1 ELSE 0 END) as unknown_count
FROM protocol_captures
GROUP BY state_name, direction
ORDER BY state_name, direction;
