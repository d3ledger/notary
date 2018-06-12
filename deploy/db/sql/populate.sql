-- Populate wallets table
INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2', 'user1@notary');

INSERT INTO notary.wallets (wallet, irohausername)
VALUES ('0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e', 'user2@notary');

-- Populate tokens table
INSERT INTO notary.tokens (wallet, token)
VALUES ('0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e', 'tkn');
