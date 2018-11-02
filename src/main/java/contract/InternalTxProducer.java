package contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class InternalTxProducer extends Contract {
    public static final String FUNC_BALANCE = "balance";
    public static final String FUNC_SENDFUNDS = "sendFunds";
    private static final String BINARY = "608060405234801561001057600080fd5b50610128806100206000396000f30060806040526004361060485763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663b69ef8a881146052578063cfe52bdd146076575b6000805434019055005b348015605d57600080fd5b50606460a3565b60408051918252519081900360200190f35b348015608157600080fd5b5060a173ffffffffffffffffffffffffffffffffffffffff6004351660a9565b005b60005481565b60008054308031909103825560405173ffffffffffffffffffffffffffffffffffffffff841692913180156108fc0292909190818181858888f1935050505015801560f8573d6000803e3d6000fd5b50505600a165627a7a723058206c210b7736013f4690dc8750d4be86371ae70b5e452310dd176d0e7c10cb30110029";

    @Deprecated
    protected InternalTxProducer(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected InternalTxProducer(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected InternalTxProducer(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected InternalTxProducer(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<InternalTxProducer> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(InternalTxProducer.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<InternalTxProducer> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(InternalTxProducer.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<InternalTxProducer> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(InternalTxProducer.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<InternalTxProducer> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(InternalTxProducer.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static InternalTxProducer load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new InternalTxProducer(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static InternalTxProducer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new InternalTxProducer(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static InternalTxProducer load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new InternalTxProducer(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static InternalTxProducer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new InternalTxProducer(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<BigInteger> balance() {
        final Function function = new Function(FUNC_BALANCE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> sendFunds(String to) {
        final Function function = new Function(
                FUNC_SENDFUNDS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(to)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }
}
