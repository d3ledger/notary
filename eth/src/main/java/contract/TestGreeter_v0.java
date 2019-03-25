/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.web3j.abi.FunctionEncoder;
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
public class TestGreeter_v0 extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5060405161054e38038061054e8339810180604052602081101561003357600080fd5b81019080805164010000000081111561004b57600080fd5b8201602081018481111561005e57600080fd5b815164010000000081118282018710171561007857600080fd5b50509291905050506100988161009e640100000000026401000000009004565b5061016d565b60005460ff16156100ae57600080fd5b80516100c19060019060208401906100d2565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061011357805160ff1916838001178555610140565b82800160010185558215610140579182015b82811115610140578251825591602001919060010190610125565b5061014c929150610150565b5090565b61016a91905b8082111561014c5760008155600101610156565b90565b6103d28061017c6000396000f3fe608060405234801561001057600080fd5b506004361061005d577c010000000000000000000000000000000000000000000000000000000060003504634ed3885e8114610062578063cfae32171461010a578063f62d188814610187575b600080fd5b6101086004803603602081101561007857600080fd5b81019060208101813564010000000081111561009357600080fd5b8201836020820111156100a557600080fd5b803590602001918460018302840111640100000000831117156100c757600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092955061022d945050505050565b005b610112610244565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561014c578181015183820152602001610134565b50505050905090810190601f1680156101795780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6101086004803603602081101561019d57600080fd5b8101906020810181356401000000008111156101b857600080fd5b8201836020820111156101ca57600080fd5b803590602001918460018302840111640100000000831117156101ec57600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295506102da945050505050565b805161024090600190602084019061030e565b5050565b60018054604080516020601f600260001961010087891615020190951694909404938401819004810282018101909252828152606093909290918301828280156102cf5780601f106102a4576101008083540402835291602001916102cf565b820191906000526020600020905b8154815290600101906020018083116102b257829003601f168201915b505050505090505b90565b60005460ff16156102ea57600080fd5b80516102fd90600190602084019061030e565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061034f57805160ff191683800117855561037c565b8280016001018555821561037c579182015b8281111561037c578251825591602001919060010190610361565b5061038892915061038c565b5090565b6102d791905b80821115610388576000815560010161039256fea165627a7a72305820f7809ad9599dc0ccf2769fce2389ac716bb2c5196e63ec566d23c4bccae0abb20029";

    public static final String FUNC_SET = "set";

    public static final String FUNC_GREET = "greet";

    public static final String FUNC_INITIALIZE = "initialize";

    @Deprecated
    protected TestGreeter_v0(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TestGreeter_v0(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TestGreeter_v0(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TestGreeter_v0(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
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

    public RemoteCall<TransactionReceipt> initialize(String greeting) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v0(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v0(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v0(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v0(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }
}
