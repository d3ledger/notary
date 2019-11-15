/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer;
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter;
import com.github.kittinunf.result.Result;
import jp.co.soramitsu.iroha.java.Transaction;
import kotlin.jvm.functions.Function0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class IrohaAccountRegistratorTest {

  private SideChainRegistrator sidechainRegistrator;
  private IrohaConsumer irohaConsumer;
  private IrohaConverter irohaConverter;

  @BeforeEach
  public void setUp() {
    irohaConsumer = mock(IrohaConsumer.class);
    when(irohaConsumer.getCreator()).thenReturn("creator");
    sidechainRegistrator = Mockito
        .spy(new SideChainRegistrator(irohaConsumer, "wallets@domain", "currency_name"));
    irohaConverter = IrohaConverter.INSTANCE;
  }

  /**
   * @given data that generates succeeded batch
   * @when data is passed to create Iroha account
   * @then new Iroha account crypto currency address is returned
   */
  @Test
  public void testRegister() {
    Function0<String> notaryStorageStrategy = () -> "";
    String currencyAddress = "123";
    String userId = "user_name@domain";

    when(irohaConsumer.send(any(Transaction.class)))
        .thenReturn(Result.Companion.of(() -> "passed_hash"));

    sidechainRegistrator
        .register(currencyAddress, userId, System.currentTimeMillis(), notaryStorageStrategy)
        .fold(address -> {
          assertEquals(currencyAddress, address);
          return null;
        }, Assertions::fail);
  }

  /**
   * @given data that generates failed batch
   * @when data is passed to create Iroha account
   * @then exception is thrown
   */
  @Test
  public void testRegisterFailed() {
    Function0<String> notaryStorageStrategy = () -> "";
    String currencyAddress = "123";
    String userId = "wron_user_id";

    when(irohaConsumer.send(any(Transaction.class)))
        .thenReturn(Result.Companion.of(() -> "passed_hash"));

    sidechainRegistrator
        .register(currencyAddress, userId, System.currentTimeMillis(), notaryStorageStrategy)
        .fold(address -> {
          fail();
          return "";
        }, e -> "");
  }
}
