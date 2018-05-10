package vendor


import com.wavesplatform.wavesj.Account
import com.wavesplatform.wavesj.PrivateKeyAccount
import org.junit.jupiter.api.Test

class WavesUsageTest {
    @Test
    fun keys() {
        val seed = "health lazy lens fix dwarf salad breeze myself silly december endless rent faculty report beyond"
        val account = PrivateKeyAccount.fromSeed(seed, 0, Account.TESTNET)
        val publicKey = account.getPublicKey()
        val address = account.getAddress()
        print("Pubkey=$publicKey, addres=$address")
    }
}
