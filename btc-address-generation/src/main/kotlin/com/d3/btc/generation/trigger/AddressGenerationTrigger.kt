package com.d3.btc.generation.trigger

import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.generation.BtcSessionProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.TriggerProvider
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverter

/*
    Class that is used to start address generation process
 */
@Component
class AddressGenerationTrigger(
    @Autowired private val btcSessionProvider: BtcSessionProvider,
    @Autowired private val btcAddressGenerationTriggerProvider: TriggerProvider,
    @Autowired private val btcFreeAddressesProvider: BtcFreeAddressesProvider,
    @Qualifier("addressGenerationConsumer")
    @Autowired private val addressGenerationConsumer: IrohaConsumer
) {
    /**
     * Starts address generation process
     * @param addressType - type of address to generate
     * @param addressesToGenerate - number of addresses to generate. 1 by default.
     * @param nodeId - node id
     */
    fun startAddressGeneration(
        addressType: BtcAddressType,
        addressesToGenerate: Int = 1,
        nodeId: String
    ): Result<Unit, Exception> {
        val txList = ArrayList<IrohaTransaction>()
        for (addresses in 1..addressesToGenerate) {
            val sessionAccountName = addressType.createSessionAccountName()
            txList.add(btcSessionProvider.createPubKeyCreationSessionTx(sessionAccountName, nodeId))
            txList.add(
                btcAddressGenerationTriggerProvider.triggerTx(
                    sessionAccountName
                )
            )
        }
        val utx = IrohaConverter.convert(IrohaOrderedBatch(txList))
        return addressGenerationConsumer.send(utx)
            .map { Unit }
    }

    /**
     * Starts free address generation process, if there is a need to generate.
     * Addresses will be generated if there is not enough free addresses in Iroha.
     * @param addressesThreshold - minimum number of free addresses to keep in Iroha
     * @param nodeId - node id
     */
    fun startFreeAddressGenerationIfNeeded(addressesThreshold: Int, nodeId: String): Result<Unit, Exception> {
        return btcFreeAddressesProvider.getFreeAddresses().flatMap { freeAddresses ->
            if (freeAddresses.size < addressesThreshold) {
                val addressesToGenerate = addressesThreshold - freeAddresses.size
                logger.info { "Generating $addressesToGenerate addresses" }
                startAddressGeneration(BtcAddressType.FREE, addressesToGenerate, nodeId)
            } else {
                logger.info { "No need to generate addresses" }
                Result.of { Unit }
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
