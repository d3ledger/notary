package contract;

import java.math.BigInteger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class Failer extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50610182806100206000396000f3fe6080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663a9059cbb81146100a7575b604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152601360248201527f657468207472616e736665722072657665727400000000000000000000000000604482015290519081900360640190fd5b3480156100b357600080fd5b506100ed600480360360408110156100ca57600080fd5b5073ffffffffffffffffffffffffffffffffffffffff81351690602001356100ef565b005b604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152601660248201527f4552432d3230207472616e736665722072657665727400000000000000000000604482015290519081900360640190fdfea165627a7a72305820f57671c731765c63bbb46852e2e0caab2f76f0f44633b63b2528e4f963cbc31f0029";

    public static final String FUNC_TRANSFER = "transfer";

    @Deprecated
    protected Failer(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Failer(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Failer(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Failer(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public void transfer(String param0, BigInteger param1) {
        throw new RuntimeException("cannot call constant function with void return type");
    }

    public static RemoteCall<Failer> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Failer.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Failer> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Failer.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Failer> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Failer.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Failer> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Failer.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static Failer load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Failer(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Failer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Failer(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Failer load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Failer(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Failer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Failer(contractAddress, web3j, transactionManager, contractGasProvider);
    }
}
