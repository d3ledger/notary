import jp.co.soramitsu.bootstrap.changelog.ChangelogAccountPublicInfo
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.changelog.ChangelogPeer
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.iroha.java.Transaction
import org.jetbrains.annotations.NotNull

//Sample changelog
class SampleChangeLog implements ChangelogInterface {

    @Override
    String getSchemaVersion() {
        // Must match regex [A-Za-z0-9_]{1,64}
        return "schema_version"
    }

    @Override
    Transaction createChangelog(@NotNull List<ChangelogAccountPublicInfo> accounts,
                                @NotNull List<ChangelogPeer> peers) {
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
