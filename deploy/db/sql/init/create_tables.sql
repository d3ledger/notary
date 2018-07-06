CREATE SCHEMA if NOT EXISTS notary;
DROP DATABASE IF EXISTS notary;
CREATE DATABASE notary;
-- Relation ethereum wallet -> iroha username
-- Whitelist of users' relay ethereum wallets
CREATE TABLE notary.wallets (
  wallet VARCHAR(42) NOT NULL,
  irohausername VARCHAR(256) NOT NULL
);

-- Relation ethereum wallet -> token name
-- White list of supported ERC20 tokens
CREATE TABLE notary.tokens (
  wallet VARCHAR(42) NOT NULL,
  token VARCHAR(256) NOT NULL
);
