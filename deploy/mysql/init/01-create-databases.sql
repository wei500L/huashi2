CREATE DATABASE IF NOT EXISTS ef_transfer_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'ef_user'@'%' IDENTIFIED BY 'ef_password';
GRANT ALL PRIVILEGES ON ef_transfer_app.* TO 'ef_user'@'%';
FLUSH PRIVILEGES;
