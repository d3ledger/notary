-- Populate wallets table
INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2', 'user1@notary');

INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e', 'user2@notary');

-- Populate tokens table
INSERT INTO notary.tokens (wallet, token) VALUES
('0x9d65d6209bcd37f1f546315171b000663117d42f', 'omg'),
('0x92fd99502914010942db94181061cdda1ce3b370', 'bat'),
('0x084f2750c7eb6bb6e40cecfed5f54b198ab1b8c7', 'rep'),
('0xbd10c47bfd673f7c0213c10e245bc7ffd4435c5b', 'btm'),
('0x65327d02cf82f65331323a3cc9e908beca95d029', 'eos'),
('0xba93c2539cea86ef4b59555167ac36debf59da4a', 'gnt'),
('0x8e832894260c768d267fe405324f023b10f6bc3d', 'dgd'),
('0x520924212c5ea068abf845e62bf2d531af8af635', 'mco'),
('0x84d6edf34a7a1fb56520094ee671f39783199e7e', 'salt'),
('0x220f2d5184127b01de2f0d0b1442eae96fff611b', 'icn');
