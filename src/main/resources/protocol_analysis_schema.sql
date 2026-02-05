-- Protocol Analysis Database Schema
-- This schema stores captured Q-Link protocol traffic for analysis and reverse engineering

-- Table for storing protocol captures
CREATE TABLE IF NOT EXISTS protocol_captures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    capture_timestamp DATETIME NOT NULL,
    session_id VARCHAR(50),
    user_handle VARCHAR(20),
    state_name VARCHAR(50),
    direction ENUM('INBOUND', 'OUTBOUND') NOT NULL,
    mnemonic CHAR(2),
    raw_hex TEXT,
    payload_hex TEXT,
    is_unknown BOOLEAN DEFAULT FALSE,
    action_class VARCHAR(50),
    INDEX idx_mnemonic (mnemonic),
    INDEX idx_user_handle (user_handle),
    INDEX idx_state_name (state_name),
    INDEX idx_is_unknown (is_unknown),
    INDEX idx_capture_time (capture_timestamp),
    INDEX idx_direction (direction)
);

-- Optional: Table for storing pattern analysis results
CREATE TABLE IF NOT EXISTS protocol_patterns (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mnemonic VARCHAR(10),
    pattern_type VARCHAR(50),
    pattern_description TEXT,
    frequency INT,
    confidence DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mnemonic (mnemonic),
    INDEX idx_pattern_type (pattern_type)
);

-- Optional: Table for storing response experiment results
CREATE TABLE IF NOT EXISTS protocol_experiments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50),
    user_handle VARCHAR(20),
    test_mnemonic VARCHAR(10),
    test_type VARCHAR(50),
    test_data TEXT,
    response_data TEXT,
    response_time_ms INT,
    success BOOLEAN,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_user_handle (user_handle),
    INDEX idx_test_mnemonic (test_mnemonic),
    INDEX idx_success (success)
);

-- Sample data insertion (for testing)
-- INSERT INTO protocol_captures (capture_timestamp, session_id, user_handle, state_name, direction, mnemonic, raw_hex, payload_hex, is_unknown, action_class)
-- VALUES (NOW(), 'session_001', 'TESTUSER', 'AUTH', 'INBOUND', 'XX', '5A000000000000000058580D', '00', TRUE, 'UnknownAction');