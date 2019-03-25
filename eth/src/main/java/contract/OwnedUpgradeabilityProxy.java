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
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
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
public class OwnedUpgradeabilityProxy extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5061002333640100000000610028810204565b61005d565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e657200000000000000008152905190819003601801902055565b6104fe8061006c6000396000f3fe608060405260043610610066577c01000000000000000000000000000000000000000000000000000000006000350463025313a281146100ac5780633659cfe6146100dd5780634f1ef286146101125780635c60da1b146101c8578063f1739cae146101dd575b6000610070610210565b9050600160a060020a038116151561008757600080fd5b60405136600082376000803683855af43d806000843e8180156100a8578184f35b8184fd5b3480156100b857600080fd5b506100c1610233565b60408051600160a060020a039092168252519081900360200190f35b3480156100e957600080fd5b506101106004803603602081101561010057600080fd5b5035600160a060020a0316610269565b005b6101106004803603604081101561012857600080fd5b600160a060020a03823516919081019060408101602082013564010000000081111561015357600080fd5b82018360208201111561016557600080fd5b8035906020019184600183028401116401000000008311171561018757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610291945050505050565b3480156101d457600080fd5b506100c1610210565b3480156101e957600080fd5b506101106004803603602081101561020057600080fd5b5035600160a060020a031661036f565b60008060405180806104b260219139604051908190036021019020549392505050565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e65720000000000000000815290519081900360180190205490565b610271610233565b600160a060020a0316331461028557600080fd5b61028e816103f4565b50565b610299610233565b600160a060020a031633146102ad57600080fd5b6102b682610269565b600030600160a060020a031634836040518082805190602001908083835b602083106102f35780518252601f1990920191602091820191016102d4565b6001836020036101000a03801982511681845116808217855250505050505090500191505060006040518083038185875af1925050503d8060008114610355576040519150601f19603f3d011682016040523d82523d6000602084013e61035a565b606091505b5050905080151561036a57600080fd5b505050565b610377610233565b600160a060020a0316331461038b57600080fd5b600160a060020a03811615156103a057600080fd5b7f5a3e66efaa1e445ebd894728a69d6959842ea1e97bd79b892797106e270efcd96103c9610233565b60408051600160a060020a03928316815291841660208301528051918290030190a161028e8161045a565b60006103fe610210565b9050600160a060020a03808216908316141561041957600080fd5b6104228261048f565b604051600160a060020a038316907fbc7cd75a20ee27fd9adebab32041f755214dbc6bffa90cc0225b39da2e5c2d3b90600090a25050565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e657200000000000000008152905190819003601801902055565b600060405180806104b26021913960405190819003602101902092909255505056fe636f6d2e64336c65646765722e70726f78792e696d706c656d656e746174696f6ea165627a7a72305820c9963852c0372a4fdb9a2c8f75fb78a6ce482b121e677c5f426b2322079bd1b70029";

    public static final String FUNC_PROXYOWNER = "proxyOwner";

    public static final String FUNC_UPGRADETO = "upgradeTo";

    public static final String FUNC_UPGRADETOANDCALL = "upgradeToAndCall";

    public static final String FUNC_IMPLEMENTATION = "implementation";

    public static final String FUNC_TRANSFERPROXYOWNERSHIP = "transferProxyOwnership";

    public static final Event PROXYOWNERSHIPTRANSFERRED_EVENT = new Event("ProxyOwnershipTransferred",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }));
    ;

    public static final Event UPGRADED_EVENT = new Event("Upgraded",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }));
    ;

    @Deprecated
    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<String> proxyOwner() {
        final Function function = new Function(FUNC_PROXYOWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> upgradeTo(String implementation) {
        final Function function = new Function(
                FUNC_UPGRADETO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(implementation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> upgradeToAndCall(String implementation, byte[] data, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_UPGRADETOANDCALL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(implementation),
                        new org.web3j.abi.datatypes.DynamicBytes(data)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<String> implementation() {
        final Function function = new Function(FUNC_IMPLEMENTATION,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> transferProxyOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFERPROXYOWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<ProxyOwnershipTransferredEventResponse> getProxyOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PROXYOWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<ProxyOwnershipTransferredEventResponse> responses = new ArrayList<ProxyOwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProxyOwnershipTransferredEventResponse typedResponse = new ProxyOwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ProxyOwnershipTransferredEventResponse> proxyOwnershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ProxyOwnershipTransferredEventResponse>() {
            @Override
            public ProxyOwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PROXYOWNERSHIPTRANSFERRED_EVENT, log);
                ProxyOwnershipTransferredEventResponse typedResponse = new ProxyOwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ProxyOwnershipTransferredEventResponse> proxyOwnershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROXYOWNERSHIPTRANSFERRED_EVENT));
        return proxyOwnershipTransferredEventFlowable(filter);
    }

    public List<UpgradedEventResponse> getUpgradedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPGRADED_EVENT, transactionReceipt);
        ArrayList<UpgradedEventResponse> responses = new ArrayList<UpgradedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpgradedEventResponse typedResponse = new UpgradedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.implementation = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UpgradedEventResponse>() {
            @Override
            public UpgradedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPGRADED_EVENT, log);
                UpgradedEventResponse typedResponse = new UpgradedEventResponse();
                typedResponse.log = log;
                typedResponse.implementation = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPGRADED_EVENT));
        return upgradedEventFlowable(filter);
    }

    @Deprecated
    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class ProxyOwnershipTransferredEventResponse {
        public Log log;

        public String previousOwner;

        public String newOwner;
    }

    public static class UpgradedEventResponse {
        public Log log;

        public String implementation;
    }
}
