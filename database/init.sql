CREATE DATABASE IF NOT EXISTS machines
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE machines;

CREATE TABLE IF NOT EXISTS equipment (
    asset_tag VARCHAR(100) PRIMARY KEY,
    category VARCHAR(40) NOT NULL,
    manufacturer VARCHAR(160) NOT NULL,
    model_name VARCHAR(160) NOT NULL,
    technical_sheet_filename VARCHAR(255),
    manual_filename VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS predictions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    asset_tag VARCHAR(100),
    predicted_category VARCHAR(40) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_predictions_created_at (created_at),
    CONSTRAINT fk_predictions_equipment
        FOREIGN KEY (asset_tag) REFERENCES equipment(asset_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
