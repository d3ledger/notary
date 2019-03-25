/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
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
public class Greeter2 extends Contract {
    private static final String BINARY = "6080604052607b600155601661001d81640100000000610023810204565b50610047565b60005460ff161561003357600080fd5b60019081556000805460ff19169091179055565b61016d806100566000396000f3fe60806040526004361061005b577c0100000000000000000000000000000000000000000000000000000000600035046360fe47b181146100605780637cf5dab01461008c578063cfae3217146100b6578063fe4b84df146100dd575b600080fd5b34801561006c57600080fd5b5061008a6004803603602081101561008357600080fd5b5035610107565b005b34801561009857600080fd5b5061008a600480360360208110156100af57600080fd5b503561010c565b3480156100c257600080fd5b506100cb610117565b60408051918252519081900360200190f35b3480156100e957600080fd5b5061008a6004803603602081101561010057600080fd5b503561011d565b600155565b600180549091019055565b60015490565b60005460ff161561012d57600080fd5b60019081556000805460ff1916909117905556fea165627a7a723058205c869efe60b65b9a9506fcfb05862cf5bc35dce4d3d39557ad4f6d355c0eac5b0029";

    public static final String FUNC_SET = "set";

    public static final String FUNC_INCREMENT = "increment";

    public static final String FUNC_GREET = "greet";

    public static final String FUNC_INITIALIZE = "initialize";

    @Deprecated
    protected Greeter2(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Greeter2(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Greeter2(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Greeter2(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> set(BigInteger val) {
        final Function function = new Function(
                FUNC_SET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(val)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> increment(BigInteger val) {
        final Function function = new Function(
                FUNC_INCREMENT,
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
    public static Greeter2 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter2(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Greeter2 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter2(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Greeter2 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Greeter2(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Greeter2 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Greeter2(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Greeter2> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Greeter2.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Greeter2> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Greeter2.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Greeter2> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Greeter2.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Greeter2> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Greeter2.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
