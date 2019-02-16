package withdrawal.btc.transaction;

import com.d3.btc.provider.BtcRegisteredAddressesProvider;
import com.d3.btc.provider.network.BtcRegTestConfigProvider;
import com.github.kittinunf.result.Result;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.Wallet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import withdrawal.btc.provider.BtcChangeAddressProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.*;

public class TransactionHelperTest {

    private static final int CONFIDENCE_LEVEL = 6;
    private static TransactionHelper transactionHelper = spy(new TransactionHelper(new BtcRegTestConfigProvider(), mock(BtcRegisteredAddressesProvider.class), mock(BtcChangeAddressProvider.class)));

    @BeforeClass
    public static void setUp() {
        doReturn(true).when(transactionHelper).isAvailableOutput(anySet(), any(TransactionOutput.class));
    }

    /**
     * @given wallet with 1_000_000 SAT as one unspent
     * @when try to collect 1000 SAT
     * @then one unspent with 1_000_000 SAT is collected
     */
    @Test
    public void testCollectUnspents() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);
        List<TransactionOutput> unspents = new ArrayList<>();
        TransactionOutput output = mock(TransactionOutput.class);
        when(output.getValue()).thenReturn(Coin.valueOf(1_000_000L));
        when(output.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);
        unspents.add(output);
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            assertEquals(1, transactionOutputs.size());
            assertTrue(transactionOutputs.contains(output));
            return null;
        }, Assertions::fail);
    }

    /**
     * @given wallet with 36_000 SAT as 3 unspents(9_000*3)
     * @when try to collect 10_000 SAT
     * @then two unspents with total amount 18_000 SAT are collected
     */
    @Test
    public void testCollectMultipleUnspents() {
        long amountToSpendSat = 10_000;
        Wallet wallet = mock(Wallet.class);
        List<TransactionOutput> unspents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TransactionOutput output = mock(TransactionOutput.class);
            when(output.getValue()).thenReturn(Coin.valueOf(9_000));
            when(output.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);
            unspents.add(output);
        }
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            assertEquals(2, transactionOutputs.size());
            assertEquals(18_000, transactionHelper.getTotalUnspentValue(transactionOutputs));
            return null;
        }, Assertions::fail);
    }

    /**
     * @given wallet with 1_000_001 SAT as 2 unspents(1_000_000 + 1)
     * @when try to collect 1000 SAT
     * @then one unspent with amount 1_000_000 SAT is collected
     */
    @Test
    public void testCollectUnspentsOrder() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);

        TransactionOutput outputBigValue = mock(TransactionOutput.class);
        when(outputBigValue.getValue()).thenReturn(Coin.valueOf(1_000_000L));
        when(outputBigValue.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);

        TransactionOutput outputSmallValue = mock(TransactionOutput.class);
        when(outputSmallValue.getValue()).thenReturn(Coin.valueOf(1L));
        when(outputSmallValue.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);

        List<TransactionOutput> unspents = new ArrayList<>(Arrays.asList(outputSmallValue, outputBigValue));
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            assertEquals(1, transactionOutputs.size());
            assertTrue(transactionOutputs.contains(outputBigValue));
            return null;
        }, Assertions::fail);
    }

    /**
     * @given wallet with 1_000 SAT as 1 unspent
     * @when try to collect 1_000 SAT
     * @then exception is thrown, because no money is left to pay fees
     */
    @Test
    public void testCollectUnspentsNoFee() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);

        TransactionOutput output = mock(TransactionOutput.class);
        when(output.getValue()).thenReturn(Coin.valueOf(amountToSpendSat));
        when(output.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);

        List<TransactionOutput> unspents = new ArrayList<>();
        unspents.add(output);
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            fail();
            return null;
        }, e -> null);
    }

    /**
     * @given wallet with 1_000_000 SAT as 1 not properly confirmed unspent
     * @when try to collect 1_000 SAT
     * @then exception is thrown, because there is no confirmed money in wallet
     */
    @Test
    public void testCollectUnspentsBadConfidenceLevel() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);

        TransactionOutput output = mock(TransactionOutput.class);
        when(output.getValue()).thenReturn(Coin.valueOf(1_000_000));
        when(output.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL - 1);

        List<TransactionOutput> unspents = new ArrayList<>();
        unspents.add(output);
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            fail();
            return null;
        }, e -> null);
    }

    /**
     * @given wallet with 1_001 SAT as 2 unspent (1_000 + 1)
     * @when try to collect 1_000 SAT
     * @then exception is thrown, because there is no money to pay fees
     */
    @Test
    public void testCollectUnspentsMultipleOutputsNoFee() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);

        TransactionOutput smallOutput = mock(TransactionOutput.class);
        when(smallOutput.getValue()).thenReturn(Coin.valueOf(amountToSpendSat));
        when(smallOutput.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);

        TransactionOutput evenSmallerOutput = mock(TransactionOutput.class);
        when(evenSmallerOutput.getValue()).thenReturn(Coin.valueOf(1));
        when(evenSmallerOutput.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);

        List<TransactionOutput> unspents = new ArrayList<>(Arrays.asList(smallOutput, evenSmallerOutput));
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            fail();
            return null;
        }, e -> null);
    }

    /**
     * @given wallet with 100_000 SAT as 100 unspent (1_000 * 100)
     * @when try to collect 1_000 SAT
     * @then exception is thrown, because every unspent takes 2000 SAT fee on average,
     * meaning that you can never collect 1_000 SAT using 100 little unspents,
     * even if total value in a wallet is 100_000 SAT
     */
    @Test
    public void testCollectUnspentsMultipleLittleOutputsNoFee() {
        long amountToSpendSat = 1000;
        Wallet wallet = mock(Wallet.class);
        List<TransactionOutput> unspents = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            TransactionOutput output = mock(TransactionOutput.class);
            when(output.getValue()).thenReturn(Coin.valueOf(amountToSpendSat));
            when(output.getParentTransactionDepthInBlocks()).thenReturn(CONFIDENCE_LEVEL);
            unspents.add(output);
        }
        when(wallet.getUnspents()).thenReturn(unspents);
        Result<List<TransactionOutput>, Exception> result = transactionHelper.collectUnspents(new HashSet<>(), wallet, amountToSpendSat, CONFIDENCE_LEVEL);
        result.fold(transactionOutputs -> {
            fail();
            return null;
        }, e -> null);
    }
}
