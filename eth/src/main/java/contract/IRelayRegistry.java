/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
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
public class IRelayRegistry extends Contract {
    private static final String BINARY = "";

    public static final String FUNC_ADDNEWRELAYADDRESS = "addNewRelayAddress";

    public static final String FUNC_GETWHITELISTBYRELAY = "getWhiteListByRelay";

    public static final String FUNC_ISWHITELISTED = "isWhiteListed";

    public static final Event ADDNEWRELAY_EVENT = new Event("AddNewRelay",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }, new TypeReference<DynamicArray<Address>>(true) {
            }));
    ;

    @Deprecated
    protected IRelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IRelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IRelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IRelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> addNewRelayAddress(String relay, List<String> whiteList) {
        final Function function = new Function(
                FUNC_ADDNEWRELAYADDRESS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                                org.web3j.abi.Utils.typeMap(whiteList, org.web3j.abi.datatypes.Address.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<List> getWhiteListByRelay(String relay) {
        final Function function = new Function(FUNC_GETWHITELISTBYRELAY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {
                }));
        return new RemoteCall<List>(
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteCall<Boolean> isWhiteListed(String relay, String who) {
        final Function function = new Function(FUNC_ISWHITELISTED,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay),
                        new org.web3j.abi.datatypes.Address(who)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public List<AddNewRelayEventResponse> getAddNewRelayEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDNEWRELAY_EVENT, transactionReceipt);
        ArrayList<AddNewRelayEventResponse> responses = new ArrayList<AddNewRelayEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AddNewRelayEventResponse> addNewRelayEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AddNewRelayEventResponse>() {
            @Override
            public AddNewRelayEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDNEWRELAY_EVENT, log);
                AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
                typedResponse.log = log;
                typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AddNewRelayEventResponse> addNewRelayEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDNEWRELAY_EVENT));
        return addNewRelayEventFlowable(filter);
    }

    @Deprecated
    public static IRelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new IRelayRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IRelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new IRelayRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IRelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new IRelayRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IRelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IRelayRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<IRelayRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IRelayRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IRelayRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IRelayRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<IRelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IRelayRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IRelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IRelayRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class AddNewRelayEventResponse {
        public Log log;

        public String relayAddress;

        public byte[] whiteList;
    }
}
