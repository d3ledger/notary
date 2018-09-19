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
    private static final String BINARY = "608060405234801561001057600080fd5b5060008054600160a060020a0319163317905561051f806100326000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166305ea0de0811461005b57806374f1e4961461009c578063a10cda991461010d575b600080fd5b34801561006757600080fd5b5061008860048035600160a060020a03169060248035908101910135610134565b604080519115158252519081900360200190f35b3480156100a857600080fd5b506100bd600160a060020a036004351661032e565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156100f95781810151838201526020016100e1565b505050509050019250505060405180910390f35b34801561011957600080fd5b50610088600160a060020a03600435811690602435166103dd565b600080548190600160a060020a0316331461014e57600080fd5b606483111561015c57600080fd5b600160a060020a038516151561017157600080fd5b600160a060020a0385166000908152600160205260409020541561019457600080fd5b5060005b828110156102b0578383828181106101ac57fe5b90506020020135600160a060020a0316600160a060020a03166000141515156101d457600080fd5b600260008585848181106101e457fe5b60209081029290920135600160a060020a03168352508101919091526040016000205460ff161561021457600080fd5b60016002600086868581811061022657fe5b60209081029290920135600160a060020a0316835250810191909152604001600020805460ff191691151591909117905583838281811061026357fe5b90506020020135600160a060020a0316600160a060020a03167f0a5461c538fab5f7aed44b6d046f4234e0c7c8aa2bff643de536abd8eb7d890160405160405180910390a2600101610198565b600160a060020a03851660009081526001602052604090206102d390858561044f565b5083836040518083836020028082843760405192018290038220945050600160a060020a03891692507fe7350f93df43f07cf9bb430c2fbd1e1b3dd1c564cb0656cb069aa29376cdb1d49150600090a3506001949350505050565b6060600160a060020a038216151561034557600080fd5b600160a060020a038216600090815260016020526040902054151561036957600080fd5b600160a060020a038216600090815260016020908152604091829020805483518184028101840190945280845290918301828280156103d157602002820191906000526020600020905b8154600160a060020a031681526001909101906020018083116103b3575b50505050509050919050565b600160a060020a038216600090815260016020526040812054151561040457506001610449565b600160a060020a03831660009081526001602052604081205411156104455750600160a060020a03811660009081526002602052604090205460ff16610449565b5060005b92915050565b8280548282559060005260206000209081019282156104af579160200282015b828111156104af57815473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0384351617825560209092019160019091019061046f565b506104bb9291506104bf565b5090565b6104f091905b808211156104bb57805473ffffffffffffffffffffffffffffffffffffffff191681556001016104c5565b905600a165627a7a7230582060d0d7d7831ac916b51c7d9b99bd20ee09d55b46d59a563f699058b158003c250029";

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

    public RemoteCall<TransactionReceipt> addNewRelayAddress(String relay, List<String> newWhiteList) {
        final Function function = new Function(
                FUNC_ADDNEWRELAYADDRESS, 
                Arrays.<Type>asList(new Address(relay),
                new DynamicArray<Address>(
                        org.web3j.abi.Utils.typeMap(newWhiteList, Address.class))),
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
