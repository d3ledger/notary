package registration;

import com.github.kittinunf.result.Result;
import jp.co.soramitsu.iroha.java.Transaction;
import kotlin.jvm.functions.Function0;
import notary.IrohaAtomicBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sidechain.iroha.consumer.IrohaConsumer;
import sidechain.iroha.consumer.IrohaConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        irohaAccountCreator = spy(new IrohaAccountCreator(irohaConsumer, "notary_account", "currency_name"));
        irohaConverter = IrohaConverter.INSTANCE;
    }

    /**
     * @given few unsigned transactions and no passed transactions lists
     * @when status of batch is checked
     * @then batch is considered failed, because there are no passed transactions
     */
    @Test
    public void testIsAccountCreationBatchFailedNoPassed() {
        List<Transaction> unsignedTransactions = Arrays.asList(
                mock(Transaction.class),
                mock(Transaction.class)
        );
        assertTrue(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, new ArrayList<>()));
    }

    /**
     * @given all unsigned transactions hashes are in passed transactions list
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

        List<byte[]> passedTransactions = Arrays.asList("123".getBytes(), "456".getBytes());

        List<Transaction> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
        assertFalse(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, passedTransactions));
    }

    /**
     * @given all unsigned transactions hashes except for the first transaction are in passed transactions list
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

        List<byte[]> passedTransactions = Collections.singletonList("456".getBytes());

        List<Transaction> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
        assertFalse(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, passedTransactions));
    }

    /**
     * @given all unsigned transactions hashes except for the second transaction are in passed transactions list
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

        List<byte[]> passedTransactions = Collections.singletonList("123".getBytes());

        List<Transaction> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
        assertTrue(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, passedTransactions));
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
        String pubKey = "pub_key";
        List<Transaction> unsignedTransactions = new ArrayList<>();
        List<byte[]> passedTransactions = new ArrayList<>();
        IrohaAtomicBatch batch = new IrohaAtomicBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaConverter)
                .convert(batch);
        when(irohaConsumer.send(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(false).when(irohaAccountCreator)
                .isAccountCreationBatchFailed(unsignedTransactions, passedTransactions);
        irohaAccountCreator.create(currencyAddress, whitelistKey, whitelist, userName, pubKey, notaryStorageStrategy)
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
        String pubKey = "pub_key";
        List<Transaction> unsignedTransactions = new ArrayList<>();
        List<byte[]> passedTransactions = new ArrayList<>();
        IrohaAtomicBatch batch = new IrohaAtomicBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaConverter)
                .convert(batch);
        when(irohaConsumer.send(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(true).when(irohaAccountCreator)
                .isAccountCreationBatchFailed(unsignedTransactions, passedTransactions);
        irohaAccountCreator.create(currencyAddress, whitelistKey, whitelist, userName, pubKey, notaryStorageStrategy)
                .fold(address -> {
                    fail();
                    return "";
                }, e -> "");
    }
}
