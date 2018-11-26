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
import org.web3j.tx.gas.ContractGasProvider;
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class RelayRegistry extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5060008054600160a060020a031916331790556104b7806100326000396000f3fe6080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166305ea0de0811461005b57806374f1e496146100ea578063a10cda991461016d575b600080fd5b34801561006757600080fd5b506100e86004803603604081101561007e57600080fd5b600160a060020a0382351691908101906040810160208201356401000000008111156100a957600080fd5b8201836020820111156100bb57600080fd5b803590602001918460208302840111640100000000831117156100dd57600080fd5b5090925090506101bc565b005b3480156100f657600080fd5b5061011d6004803603602081101561010d57600080fd5b5035600160a060020a031661026e565b60408051602080825283518183015283519192839290830191858101910280838360005b83811015610159578181015183820152602001610141565b505050509050019250505060405180910390f35b34801561017957600080fd5b506101a86004803603604081101561019057600080fd5b50600160a060020a038135811691602001351661031d565b604080519115158252519081900360200190f35b600054600160a060020a031633146101d357600080fd5b600160a060020a038316600090815260016020526040902054156101f657600080fd5b600160a060020a03831660009081526001602052604090206102199083836103e7565b5081816040518083836020028082843760405192018290038220945050600160a060020a03871692507fe7350f93df43f07cf9bb430c2fbd1e1b3dd1c564cb0656cb069aa29376cdb1d49150600090a3505050565b6060600160a060020a038216151561028557600080fd5b600160a060020a03821660009081526001602052604090205415156102a957600080fd5b600160a060020a0382166000908152600160209081526040918290208054835181840281018401909452808452909183018282801561031157602002820191906000526020600020905b8154600160a060020a031681526001909101906020018083116102f3575b50505050509050919050565b600160a060020a0382166000908152600160205260408120541515610344575060016103e1565b600160a060020a03831660009081526001602052604081205411156103dd5760005b600160a060020a0384166000908152600160205260409020548110156103db57600160a060020a03841660009081526001602052604090208054829081106103aa57fe5b600091825260209091200154600160a060020a03848116911614156103d35760019150506103e1565b600101610366565b505b5060005b92915050565b828054828255906000526020600020908101928215610447579160200282015b8281111561044757815473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a03843516178255602090920191600190910190610407565b50610453929150610457565b5090565b61048891905b8082111561045357805473ffffffffffffffffffffffffffffffffffffffff1916815560010161045d565b9056fea165627a7a72305820db9d7cf8bbda6a7ed0a9c3ae7d3dea3c6817131178f2e2c024480a1b137dc3050029";

    public static final String FUNC_ADDNEWRELAYADDRESS = "addNewRelayAddress";

    public static final String FUNC_GETWHITELISTBYRELAY = "getWhiteListByRelay";

    public static final String FUNC_ISWHITELISTED = "isWhiteListed";

    public static final Event ADDNEWRELAY_EVENT = new Event("AddNewRelay", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Address>>(true) {}));
    ;

    @Deprecated
    protected RelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected RelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected RelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay), 
                new org.web3j.abi.datatypes.Address(who)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RelayRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RelayRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
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

    public Observable<AddNewRelayEventResponse> addNewRelayEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AddNewRelayEventResponse>() {
            @Override
            public AddNewRelayEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDNEWRELAY_EVENT, log);
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

    @Deprecated
    public static RelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static RelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new RelayRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new RelayRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AddNewRelayEventResponse {
        public Log log;

        public String relayAddress;

        public byte[] whiteList;
    }
}
