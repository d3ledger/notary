package contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.FunctionEncoder;
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

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.1.1.
 */
public class Greeter extends Contract {
    private static final String BINARY = "6080604052607b60015534801561001557600080fd5b5060405160208061019f8339810180604052602081101561003557600080fd5b50516100498164010000000061004f810204565b50610073565b60005460ff161561005f57600080fd5b60019081556000805460ff19169091179055565b61011d806100826000396000f3fe608060405260043610604c577c0100000000000000000000000000000000000000000000000000000000600035046360fe47b181146051578063cfae3217146079578063fe4b84df14609d575b600080fd5b348015605c57600080fd5b50607760048036036020811015607157600080fd5b503560c3565b005b348015608457600080fd5b50608b60c8565b60408051918252519081900360200190f35b34801560a857600080fd5b5060776004803603602081101560bd57600080fd5b503560ce565b600155565b60015490565b60005460ff161560dd57600080fd5b60019081556000805460ff1916909117905556fea165627a7a723058209f41167192e6b48387574f75caefd4667e1e03aa40a2f6518bef351270ddd3ac0029";

    public static final String FUNC_SET = "set";

    public static final String FUNC_GREET = "greet";

    public static final String FUNC_INITIALIZE = "initialize";

    @Deprecated
    protected Greeter(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Greeter(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Greeter(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Greeter(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> set(BigInteger val) {
        final Function function = new Function(
                FUNC_SET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> greet() {
        final Function function = new Function(FUNC_GREET,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> initialize(BigInteger val) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static Greeter load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Greeter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Greeter load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Greeter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Greeter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Greeter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Greeter> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, BigInteger val) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)));
        return deployRemoteCall(Greeter.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Greeter> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, BigInteger val) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)));
        return deployRemoteCall(Greeter.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Greeter> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger val) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)));
        return deployRemoteCall(Greeter.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Greeter> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger val) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)));
        return deployRemoteCall(Greeter.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }
}
