CREATE DATABASE IF NOT EXISTS supergestor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'supergestor_user'@'localhost'
  IDENTIFIED BY '123456';

GRANT ALL PRIVILEGES ON supergestor.* TO 'supergestor_user'@'localhost';

FLUSH PRIVILEGES;
