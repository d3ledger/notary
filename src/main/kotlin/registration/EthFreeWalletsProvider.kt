package registration

/**
 * Provides with free ethereum relay wallet
 */
class EthFreeWalletsProvider {

    // TODO - a.chernyshov - D3-106 -  add effective implementation
    // Possible solutions:
    // 1) set listener and update Postgres DB
    // 2) query Iroha each time
    fun getWallet(): String {
        return "eth_wallet"
    }
}
