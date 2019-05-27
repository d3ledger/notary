/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.config

import jp.co.soramitsu.bootstrap.genesis.GenesisInterface
import jp.co.soramitsu.bootstrap.genesis.d3.D3TestGenesisFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class BootstrapConfig {

    @Bean
    fun genesisFactories(): List<GenesisInterface> {
        return listOf(D3TestGenesisFactory())
    }
}

