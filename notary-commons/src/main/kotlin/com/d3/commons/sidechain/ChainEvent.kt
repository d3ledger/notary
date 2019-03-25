/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain

import iroha.protocol.Commands
import java.math.BigInteger

/**
 * All events emitted by side chains.
 */
sealed class SideChainEvent {

    /**
     * Class represents events in Iroha chain
     */
    sealed class IrohaEvent : SideChainEvent() {

        /**
         * Event which raised on adding new peer in Iroha network
         *
         * @param address peer's address, ip and port
         * @param key peer's key
         */
        data class AddPeer(
            val address: String,
            val key: String
        ) : IrohaEvent() {

            companion object {

                /**
                 * Generate [AddPeer] from proto
                 */
                fun fromProto(cmd: Commands.AddPeer): AddPeer {
                    return AddPeer(cmd.peer.address, cmd.peer.peerKey)
                }
            }

        }

        /**
         * Event which is raised when custodian transfer assets to notary account to withdraw asset
         *
         * @param srcAccount source of transfer
         * @param dstAccount destination of transfer
         * @param asset is asset id in Iroha
         * @param amount of ethereum to withdraw
         * @param description description field of transfer
         * @param hash hash of transaction in Iroha
         */
        data class SideChainTransfer(
            val srcAccount: String,
            val dstAccount: String,
            val asset: String,
            val amount: String,
            val description: String,
            val hash: String
        ) : IrohaEvent() {

            companion object {

                /**
                 * Generate [SideChainTransfer] from proto
                 */
                fun fromProto(cmd: Commands.TransferAsset, hash: String): SideChainTransfer {
                    return SideChainTransfer(
                        cmd.srcAccountId, cmd.destAccountId,
                        cmd.assetId, cmd.amount, cmd.description, hash
                    )
                }
            }
        }
    }

    /**
     * Common class for all interested primary blockchain events
     */
    sealed class PrimaryBlockChainEvent : SideChainEvent() {

        /**
         * Event which occures when custodian deposits some amount of certain asset
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param asset asset name
         * @param amount amount of tokens
         * @param from - from primary blockchain address
         */
        data class OnPrimaryChainDeposit(
            val hash: String,
            val time: BigInteger,
            val user: String,
            val asset: String,
            val amount: String,
            val from: String
        ) : PrimaryBlockChainEvent()
    }
}
