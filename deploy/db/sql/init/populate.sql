-- Populate wallets table
INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2', 'user1@notary');

INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e', 'user2@notary');

-- Populate tokens table
INSERT INTO notary.tokens (wallet, token) VALUES
('0xd26114cd6EE289AccF82350c8d8487fedB8A0C07', 'omg'),
('0x0d8775f648430679a709e98d2b0cb6250d2887ef', 'bat'),
('0x1985365e9f78359a9B6AD760e32412f4a445E862', 'rep'),
('0xcb97e65f07da24d46bcdd078ebebd7c6e6e3d750', 'btm'),
('0x86fa049857e0209aa7d9e616f7eb3b3b78ecfdb0', 'eos'),
('0xa74476443119A942dE498590Fe1f2454d7D4aC0d', 'gnt'),
('0xe0b7927c4af23765cb51314a0e0521a9645f0e2a', 'dgd'),
('0xb63b606ac810a52cca15e44bb630fd42d8d1d83d', 'mco'),
('0x4156D3342D5c385a87D264F90653733592000581', 'salt'),
('0x888666CA69E0f178DED6D75b5726Cee99A87D698', 'icn');
