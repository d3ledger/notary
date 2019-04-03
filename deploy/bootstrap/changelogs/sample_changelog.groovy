import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.iroha.java.Transaction
import org.jetbrains.annotations.NotNull

//Sample changelog
class SampleChangeLog implements ChangelogInterface {

    @Override
    String getSchemaVersion() {
        return "1.0"
    }

    @Override
    Transaction createChangelog(@NotNull List<AccountPublicInfo> accounts,
                                @NotNull List<Peer> peers) {
        def accountName = "script_test"
        def pubKey = getPubKeys(accountName, accounts).first()
        //Create account and set detail
        def createAccountTransaction = Transaction
                .builder(superuserAccountId)
                .createAccount(accountName, "d3", pubKey)
                .setAccountDetail("$accountName@d3", "test_key", "test_value")
        return createAccountTransaction.build()
    }
}
