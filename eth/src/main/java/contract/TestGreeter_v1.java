package contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
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
public class TestGreeter_v1 extends Contract {
    private static final String BINARY = "60c0604052600a60809081527f48692c20576f726c64210000000000000000000000000000000000000000000060a05261004181640100000000610047810204565b50610116565b60005460ff161561005757600080fd5b805161006a90600190602084019061007b565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106100bc57805160ff19168380011785556100e9565b828001600101855582156100e9579182015b828111156100e95782518255916020019190600101906100ce565b506100f59291506100f9565b5090565b61011391905b808211156100f557600081556001016100ff565b90565b61041c806101256000396000f3fe608060405234801561001057600080fd5b5060043610610068577c010000000000000000000000000000000000000000000000000000000060003504634ed3885e811461006d578063cfae321714610115578063eca386af14610192578063f62d18881461019a575b600080fd5b6101136004803603602081101561008357600080fd5b81019060208101813564010000000081111561009e57600080fd5b8201836020820111156100b057600080fd5b803590602001918460018302840111640100000000831117156100d257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610240945050505050565b005b61011d610257565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561015757818101518382015260200161013f565b50505050905090810190601f1680156101845780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b61011d6102ed565b610113600480360360208110156101b057600080fd5b8101906020810181356401000000008111156101cb57600080fd5b8201836020820111156101dd57600080fd5b803590602001918460018302840111640100000000831117156101ff57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610324945050505050565b8051610253906001906020840190610358565b5050565b60018054604080516020601f600260001961010087891615020190951694909404938401819004810282018101909252828152606093909290918301828280156102e25780601f106102b7576101008083540402835291602001916102e2565b820191906000526020600020905b8154815290600101906020018083116102c557829003601f168201915b505050505090505b90565b60408051808201909152600981527f476f6f6420627965210000000000000000000000000000000000000000000000602082015290565b60005460ff161561033457600080fd5b8051610347906001906020840190610358565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061039957805160ff19168380011785556103c6565b828001600101855582156103c6579182015b828111156103c65782518255916020019190600101906103ab565b506103d29291506103d6565b5090565b6102ea91905b808211156103d257600081556001016103dc56fea165627a7a72305820aea5e1d9601d36988a18de70ea283599267fcbcadadebfb7d182f933f7263d300029";

    public static final String FUNC_SET = "set";

    public static final String FUNC_GREET = "greet";

    public static final String FUNC_FAREWELL = "farewell";

    public static final String FUNC_INITIALIZE = "initialize";

    @Deprecated
    protected TestGreeter_v1(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TestGreeter_v1(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TestGreeter_v1(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TestGreeter_v1(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> set(String greeting) {
        final Function function = new Function(
                FUNC_SET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> greet() {
        final Function function = new Function(FUNC_GREET,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> farewell() {
        final Function function = new Function(FUNC_FAREWELL,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> initialize(String greeting) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v1(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v1(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v1(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v1(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
