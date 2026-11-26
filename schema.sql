CREATE DATABASE IF NOT EXISTS db_anime;
USE db_anime;

CREATE TABLE IF NOT EXISTS anime (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    judul  VARCHAR(150) NOT NULL,
    genre  VARCHAR(50)  NOT NULL,
    rating DECIMAL(3,1) NOT NULL DEFAULT 0.0
);

INSERT INTO anime (judul, genre, rating) VALUES
('Fullmetal Alchemist: Brotherhood', 'Action', 9.1),
('Your Name', 'Romance', 8.8),
('Spy x Family', 'Comedy', 8.6);