package contract;

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
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.5.0.
 */
public class RelayRegistry extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5060008054600160a060020a03191633179055610434806100326000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166305ea0de0811461005b57806374f1e4961461008a578063a10cda99146100fb575b600080fd5b34801561006757600080fd5b5061008860048035600160a060020a03169060248035908101910135610136565b005b34801561009657600080fd5b506100ab600160a060020a03600435166101e8565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156100e75781810151838201526020016100cf565b505050509050019250505060405180910390f35b34801561010757600080fd5b50610122600160a060020a0360043581169060243516610297565b604080519115158252519081900360200190f35b600054600160a060020a0316331461014d57600080fd5b600160a060020a0383166000908152600160205260409020541561017057600080fd5b600160a060020a0383166000908152600160205260409020610193908383610364565b5081816040518083836020028082843760405192018290038220945050600160a060020a03871692507fe7350f93df43f07cf9bb430c2fbd1e1b3dd1c564cb0656cb069aa29376cdb1d49150600090a3505050565b6060600160a060020a03821615156101ff57600080fd5b600160a060020a038216600090815260016020526040902054151561022357600080fd5b600160a060020a0382166000908152600160209081526040918290208054835181840281018401909452808452909183018282801561028b57602002820191906000526020600020905b8154600160a060020a0316815260019091019060200180831161026d575b50505050509050919050565b600160a060020a038216600090815260016020526040812054819015156102c1576001915061035d565b600160a060020a0384166000908152600160205260408120541115610358575060005b600160a060020a03841660009081526001602052604090205481101561035857600160a060020a038416600090815260016020526040902080548290811061032857fe5b600091825260209091200154600160a060020a0384811691161415610350576001915061035d565b6001016102e4565b600091505b5092915050565b8280548282559060005260206000209081019282156103c4579160200282015b828111156103c457815473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a03843516178255602090920191600190910190610384565b506103d09291506103d4565b5090565b61040591905b808211156103d057805473ffffffffffffffffffffffffffffffffffffffff191681556001016103da565b905600a165627a7a72305820351a3ad322bf1df98f251c7fab18fc900e2f4a4c2a8985d7ead37169adad92140029";

    public static final String FUNC_ADDNEWRELAYADDRESS = "addNewRelayAddress";

    public static final String FUNC_GETWHITELISTBYRELAY = "getWhiteListByRelay";

    public static final String FUNC_ISWHITELISTED = "isWhiteListed";

    public static final Event ADDNEWRELAY_EVENT = new Event("AddNewRelay", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Address>>(true) {}));
    ;

    public static final Event NEWWHITELISTED_EVENT = new Event("newWhiteListed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    protected RelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<TransactionReceipt> addNewRelayAddress(String relay, List<String> whiteList) {
        final Function function = new Function(
                FUNC_ADDNEWRELAYADDRESS, 
                Arrays.<Type>asList(new Address(relay),
                new DynamicArray<Address>(
                        org.web3j.abi.Utils.typeMap(whiteList, Address.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<List> getWhiteListByRelay(String relay) {
        final Function function = new Function(FUNC_GETWHITELISTBYRELAY, 
                Arrays.<Type>asList(new Address(relay)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
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
                Arrays.<Type>asList(new Address(relay),
                new Address(who)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public List<AddNewRelayEventResponse> getAddNewRelayEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(ADDNEWRELAY_EVENT, transactionReceipt);
        ArrayList<AddNewRelayEventResponse> responses = new ArrayList<AddNewRelayEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AddNewRelayEventResponse> addNewRelayEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AddNewRelayEventResponse>() {
            @Override
            public AddNewRelayEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(ADDNEWRELAY_EVENT, log);
                AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
                typedResponse.log = log;
                typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AddNewRelayEventResponse> addNewRelayEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDNEWRELAY_EVENT));
        return addNewRelayEventObservable(filter);
    }

    public List<NewWhiteListedEventResponse> getNewWhiteListedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(NEWWHITELISTED_EVENT, transactionReceipt);
        ArrayList<NewWhiteListedEventResponse> responses = new ArrayList<NewWhiteListedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            NewWhiteListedEventResponse typedResponse = new NewWhiteListedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.white = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<NewWhiteListedEventResponse> newWhiteListedEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, NewWhiteListedEventResponse>() {
            @Override
            public NewWhiteListedEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(NEWWHITELISTED_EVENT, log);
                NewWhiteListedEventResponse typedResponse = new NewWhiteListedEventResponse();
                typedResponse.log = log;
                typedResponse.white = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<NewWhiteListedEventResponse> newWhiteListedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NEWWHITELISTED_EVENT));
        return newWhiteListedEventObservable(filter);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class AddNewRelayEventResponse {
        public Log log;

        public String relayAddress;

        public byte[] whiteList;
    }

    public static class NewWhiteListedEventResponse {
        public Log log;

        public String white;
    }
}
