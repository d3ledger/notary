package sideChain.iroha

import Hash
import Transaction
import com.squareup.moshi.Json

data class Block(val height: Int,
                 @Json(name = "prevBlockHash") val prevHash: String,
                 @Json(name = "txNumber") val txs: Short,
                 val transactions: List<Transaction>)