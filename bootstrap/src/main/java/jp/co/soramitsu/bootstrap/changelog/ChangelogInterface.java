package jp.co.soramitsu.bootstrap.changelog;

import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo;
import jp.co.soramitsu.bootstrap.dto.Peer;
import jp.co.soramitsu.iroha.java.Transaction;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface of changelog scripts
 */
public interface ChangelogInterface {

    // Name of superuser
    String superuserAccount = "superuser";
    // Superuser domain
    String superuserDomain = "bootstrap";
    //Superuser account id
    String superuserAccountId = superuserAccount + "@" + superuserDomain;

    // Main script logic goes here
    List<Transaction> createChangelog(List<AccountPublicInfo> accounts, List<Peer> peers);

    /**
     * Returns public keys
     * @param accountName - name of account which public keys are requested
     * @param accounts    - list full of account information(name, domain, pubKeys, quorum)
     * @return list of public keys
     * @throws IllegalArgumentException if there is no public keys we are interested in
     */
    static List<byte[]> getPubKeys(String accountName, List<AccountPublicInfo> accounts) {
        return accounts.stream().filter(
                account -> accountName.equals(account.getAccountName())
        ).findFirst().map(accountPublicInfo -> {
            if (accountPublicInfo.getPubKeys().isEmpty()) {
                throw new IllegalArgumentException("Account " + accountName + " has no pubKeys");
            }
            return accountPublicInfo.getPubKeys().stream().map(DatatypeConverter::parseHexBinary
            ).collect(Collectors.toList());
        }).orElseThrow(() ->
                new IllegalArgumentException("No pubKeys for " + accountName + " was found"));
    }
}
