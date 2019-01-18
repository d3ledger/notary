package registration;

import com.github.kittinunf.result.Result;
import jp.co.soramitsu.iroha.Hash;
import jp.co.soramitsu.iroha.UnsignedTx;
import kotlin.jvm.functions.Function0;
import notary.IrohaOrderedBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sidechain.iroha.consumer.IrohaConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IrohaAccountCreatorTest {

    private IrohaAccountCreator irohaAccountCreator;
    private IrohaConsumer irohaConsumer;

    @BeforeEach
    public void setUp() {
        irohaConsumer = mock(IrohaConsumer.class);
        when(irohaConsumer.getCreator()).thenReturn("creator");
        irohaAccountCreator = spy(new IrohaAccountCreator(irohaConsumer, "notary_account", "currency_name"));
    }

    /**
     * @given few unsigned transactions and no passed transactions lists
     * @when status of batch is checked
     * @then batch is considered failed, because there are no passed transactions
     */
    @Test
    public void testIsAccountCreationBatchFailedNoPassed() {
        List<UnsignedTx> unsignedTransactions = Arrays.asList(mock(UnsignedTx.class), mock(UnsignedTx.class));
        assertTrue(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, new ArrayList<>()));
    }

    /**
     * @given all unsigned transactions hashes are in passed transactions list
     * @when status of batch is checked
     * @then batch is considered succeeded
     */
    @Test
    public void testIsAccountCreationBatchFailedAllPassed() {
        Hash firstHash = mock(Hash.class);
        when(firstHash.hex()).thenReturn("123");
        UnsignedTx firstUnsignedTx = mock(UnsignedTx.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        Hash secondHash = mock(Hash.class);
        when(secondHash.hex()).thenReturn("456");
        UnsignedTx secondUnsignedTx = mock(UnsignedTx.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        List<String> passedTransactions = Arrays.asList("123", "456");

        List<UnsignedTx> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
        assertFalse(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, passedTransactions));
    }

    /**
     * @given all unsigned transactions hashes except for the first transaction are in passed transactions list
     * @when status of batch is checked
     * @then batch is considered succeeded, because the first transaction may fail
     */
    @Test
    public void testIsAccountCreationBatchFailedFirstNotPassed() {
        Hash firstHash = mock(Hash.class);
        when(firstHash.hex()).thenReturn("123");
        UnsignedTx firstUnsignedTx = mock(UnsignedTx.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        Hash secondHash = mock(Hash.class);
        when(secondHash.hex()).thenReturn("456");
        UnsignedTx secondUnsignedTx = mock(UnsignedTx.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        List<String> passedTransactions = Arrays.asList("456");

        List<UnsignedTx> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
        assertFalse(irohaAccountCreator.isAccountCreationBatchFailed(unsignedTransactions, passedTransactions));
    }

    /**
     * @given all unsigned transactions hashes except for the second transaction are in passed transactions list
     * @when status of batch is checked
     * @then batch is considered failed
     */
    @Test
    public void testIsAccountCreationBatchFailedSecondNotPassed() {
        Hash firstHash = mock(Hash.class);
        when(firstHash.hex()).thenReturn("123");
        UnsignedTx firstUnsignedTx = mock(UnsignedTx.class);
        when(firstUnsignedTx.hash()).thenReturn(firstHash);

        Hash secondHash = mock(Hash.class);
        when(secondHash.hex()).thenReturn("456");
        UnsignedTx secondUnsignedTx = mock(UnsignedTx.class);
        when(secondUnsignedTx.hash()).thenReturn(secondHash);

        List<String> passedTransactions = Arrays.asList("123");

        List<UnsignedTx> unsignedTransactions = Arrays.asList(firstUnsignedTx, secondUnsignedTx);
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
        String domain = "domain";
        String pubKey = "pub_key";
        List<UnsignedTx> unsignedTransactions = new ArrayList<>();
        List<String> passedTransactions = new ArrayList<>();
        IrohaOrderedBatch batch = new IrohaOrderedBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaAccountCreator)
                .convertBatchToTx(batch);
        when(irohaConsumer.sendAndCheck(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(false).when(irohaAccountCreator)
                .isAccountCreationBatchFailed(unsignedTransactions, passedTransactions);
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
        List<UnsignedTx> unsignedTransactions = new ArrayList<>();
        List<String> passedTransactions = new ArrayList<>();
        IrohaOrderedBatch batch = new IrohaOrderedBatch(new ArrayList<>());

        doReturn(batch).when(irohaAccountCreator)
                .createAccountCreationBatch(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy);
        doReturn(unsignedTransactions).when(irohaAccountCreator)
                .convertBatchToTx(batch);
        when(irohaConsumer.sendAndCheck(unsignedTransactions)).thenReturn(Result.Companion.of(() -> passedTransactions));
        doReturn(true).when(irohaAccountCreator)
                .isAccountCreationBatchFailed(unsignedTransactions, passedTransactions);
        irohaAccountCreator.create(currencyAddress, whitelistKey, whitelist, userName, domain, pubKey, notaryStorageStrategy)
                .fold(address -> {
                    fail();
                    return "";
                }, e -> "");
    }
}
