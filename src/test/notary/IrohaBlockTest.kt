package notary

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import sideChain.iroha.Block
import java.io.File

class IrohaBlockTest {
    @Test
    fun jsonToBlock() {
        val file = File("resources/genesis.block")
        val json = file.readText()
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val jsonAdapter = moshi.adapter(Block::class.java)


        val block = jsonAdapter.fromJson(json)

        println(block)
    }
}