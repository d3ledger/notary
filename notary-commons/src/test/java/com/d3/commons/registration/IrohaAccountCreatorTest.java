package com.d3.commons.registration;

import com.github.kittinunf.result.Result;
import jp.co.soramitsu.iroha.java.Transaction;
import kotlin.jvm.functions.Function0;
import org.mockito.Mockito;
import com.d3.commons.notary.IrohaOrderedBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer;
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IrohaAccountCreatorTest {

    private IrohaAccountCreator irohaAccountCreator;
    private IrohaConsumer irohaConsumer;
    private IrohaConverter irohaConverter;

    @BeforeEach
    public void setUp() {
        irohaConsumer = mock(IrohaConsumer.class);
        when(irohaConsumer.getCreator()).thenReturn("creator");
        irohaAccountCreator = Mockito.spy(new IrohaAccountCreator(irohaConsumer, "notary_account", "currency_name"));
        irohaConverter = IrohaConverter.INSTANCE;
    }

    /**
     * @given map of several not passed transactions
     * @when status of batch is checked
     * @then batch is considered failed, because there are no passed transactions
     */
    @Test
    public void testIsAccountCreationBatchFailedNoPassed() {
        Map<String, Boolean> transactionsResultsMap = new HashMap<>();
        transactionsResultsMap.put("first", false);
        transactionsResultsMap.put("second", false);
        assertFalse(irohaAccountCreator.isAccountCreationBatchSuccessful(transactionsResultsMap));
    }

    /**
     * @given all unsigned transactions hashes are in passed transactions map
     * @when status of batch is checked
     * @then batch is considered succeeded
     */
    @Test
    public void testIsAccountCreationBatchFailedAllPassed() {
        byte[] firstHash = mock(byte[].class);
        when(firstHash).thenReturn("123".getBytes());
        Transaction firstUnsignedTx = mock(Transaction.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        byte[] secondHash = mock(byte[].class);
        when(secondHash).thenReturn("456".getBytes());
        Transaction secondUnsignedTx = mock(Transaction.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        Map<String, Boolean> passedTransactions = new HashMap<>();
        passedTransactions.put(Arrays.toString(firstHash), true);
        passedTransactions.put(Arrays.toString(secondHash), true);

        assertTrue(irohaAccountCreator.isAccountCreationBatchSuccessful(passedTransactions));
    }

    /**
     * @given all unsigned transactions hashes except for the first transaction are in passed transactions map
     * @when status of batch is checked
     * @then batch is considered succeeded, because the first transaction may fail
     */
    @Test
    public void testIsAccountCreationBatchFailedFirstNotPassed() {
        byte[] firstHash = mock(byte[].class);
        when(firstHash).thenReturn("123".getBytes());
        Transaction firstUnsignedTx = mock(Transaction.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        byte[] secondHash = mock(byte[].class);
        when(secondHash).thenReturn("456".getBytes());
        Transaction secondUnsignedTx = mock(Transaction.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        Map<String, Boolean> passedTransactions = new HashMap<>();
        passedTransactions.put(Arrays.toString(firstHash), false);
        passedTransactions.put(Arrays.toString(secondHash), true);

        assertTrue(irohaAccountCreator.isAccountCreationBatchSuccessful(passedTransactions));

    }

    /**
     * @given all unsigned transactions hashes except for the second transaction are in passed transactions map
     * @when status of batch is checked
     * @then batch is considered failed
     */
    @Test
    public void testIsAccountCreationBatchFailedSecondNotPassed() {
        byte[] firstHash = mock(byte[].class);
        when(firstHash).thenReturn("123".getBytes());
        Transaction firstUnsignedTx = mock(Transaction.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        byte[] secondHash = mock(byte[].class);
        when(secondHash).thenReturn("456".getBytes());
        Transaction secondUnsignedTx = mock(Transaction.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        Map<String, Boolean> passedTransactions = new HashMap<>();
        passedTransactions.put(Arrays.toString(firstHash), true);
        passedTransactions.put(Arrays.toString(secondHash), false);

        assertFalse(irohaAccountCreator.isAccountCreationBatchSuccessful(passedTransactions));

    }

    /**
     * @given data that generates succeeded batch
     * @when data is passed to create Iroha account
     * @then new Iroha account crypto currency address is returned
     */
    @Test
    public void testCreate() {
        Function0<String> notaryStorageStrategy = () -> "";
        String currencyAddress = "123";
        String whitelistKey = "white_list_key";
        List<String> whitelist = new ArrayList<>();
        String userName = "user_name";
        String domain = "domain";
        String pubKey = "pub_key";
        List<Transaction> unsignedTransactions = new ArrayList<>();
        Map<String, Boolean> passedTransactions = new HashMap<>();
        IrohaOrderedBatch batch = new IrohaOrderedBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaConverter)
                .convert(batch);
        when(irohaConsumer.send(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(false).when(irohaAccountCreator)
                .isAccountCreationBatchSuccessful(passedTransactions);
        irohaAccountCreator.create(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy)
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
    public void testCreateFailed() {
        Function0<String> notaryStorageStrategy = () -> "";
        String currencyAddress = "123";
        String whitelistKey = "white_list_key";
        List<String> whitelist = new ArrayList<>();
        String userName = "user_name";
        String domain = "domain";
        String pubKey = "pub_key";
        List<Transaction> unsignedTransactions = new ArrayList<>();
        Map<String, Boolean> passedTransactions = new HashMap<>();
        IrohaOrderedBatch batch = new IrohaOrderedBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaConverter)
                .convert(batch);
        when(irohaConsumer.send(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(true).when(irohaAccountCreator)
                .isAccountCreationBatchSuccessful(passedTransactions);
        irohaAccountCreator.create(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy)
                .fold(address -> {
                    fail();
                    return "";
                }, e -> "");
    }
}
