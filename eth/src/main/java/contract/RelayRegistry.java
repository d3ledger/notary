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
 * <p>Generated with web3j version 4.0.1.
 */
public class RelayRegistry extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5061002333640100000000610028810204565b610067565b60005460ff161561003857600080fd5b6000805460ff19600160a060020a039093166101000261010060a860020a031990911617919091166001179055565b610540806100766000396000f3fe60806040526004361061005b577c0100000000000000000000000000000000000000000000000000000000600035046305ea0de0811461006057806374f1e496146100ef578063a10cda9914610172578063c4d66de8146101c1575b600080fd5b34801561006c57600080fd5b506100ed6004803603604081101561008357600080fd5b600160a060020a0382351691908101906040810160208201356401000000008111156100ae57600080fd5b8201836020820111156100c057600080fd5b803590602001918460208302840111640100000000831117156100e257600080fd5b5090925090506101f4565b005b3480156100fb57600080fd5b506101226004803603602081101561011257600080fd5b5035600160a060020a03166102ab565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561015e578181015183820152602001610146565b505050509050019250505060405180910390f35b34801561017e57600080fd5b506101ad6004803603604081101561019557600080fd5b50600160a060020a038135811691602001351661035a565b604080519115158252519081900360200190f35b3480156101cd57600080fd5b506100ed600480360360208110156101e457600080fd5b5035600160a060020a0316610424565b6000546101009004600160a060020a0316331461021057600080fd5b600160a060020a0383166000908152600160205260409020541561023357600080fd5b600160a060020a0383166000908152600160205260409020610256908383610470565b5081816040518083836020028082843760405192018290038220945050600160a060020a03871692507fe7350f93df43f07cf9bb430c2fbd1e1b3dd1c564cb0656cb069aa29376cdb1d49150600090a3505050565b6060600160a060020a03821615156102c257600080fd5b600160a060020a03821660009081526001602052604090205415156102e657600080fd5b600160a060020a0382166000908152600160209081526040918290208054835181840281018401909452808452909183018282801561034e57602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610330575b50505050509050919050565b600160a060020a03821660009081526001602052604081205415156103815750600161041e565b600160a060020a038316600090815260016020526040812054111561041a5760005b600160a060020a03841660009081526001602052604090205481101561041857600160a060020a03841660009081526001602052604090208054829081106103e757fe5b600091825260209091200154600160a060020a038481169116141561041057600191505061041e565b6001016103a3565b505b5060005b92915050565b60005460ff161561043457600080fd5b6000805460ff19600160a060020a039093166101000274ffffffffffffffffffffffffffffffffffffffff001990911617919091166001179055565b8280548282559060005260206000209081019282156104d0579160200282015b828111156104d057815473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a03843516178255602090920191600190910190610490565b506104dc9291506104e0565b5090565b61051191905b808211156104dc57805473ffffffffffffffffffffffffffffffffffffffff191681556001016104e6565b9056fea165627a7a72305820b1a9faac82569004f7335eecfc7fab654f79590a73b38e406765de88ea37af9b0029";

    public static final String FUNC_ADDNEWRELAYADDRESS = "addNewRelayAddress";

    public static final String FUNC_GETWHITELISTBYRELAY = "getWhiteListByRelay";

    public static final String FUNC_ISWHITELISTED = "isWhiteListed";

    public static final String FUNC_INITIALIZE = "initialize";

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
        if (whiteList.isEmpty()) {
            final Function function = new Function(
                    FUNC_ADDNEWRELAYADDRESS,
                    Arrays.<Type>asList(new Address(relay), DynamicArray.empty("address[]")),
                    Collections.<TypeReference<?>>emptyList());
            return executeRemoteCallTransaction(function);
        } else {
            final Function function = new Function(
                    FUNC_ADDNEWRELAYADDRESS,
                    Arrays.<Type>asList(new Address(relay),
                            new DynamicArray<Address>(
                                    org.web3j.abi.Utils.typeMap(whiteList, Address.class))),
                    Collections.<TypeReference<?>>emptyList());
            return executeRemoteCallTransaction(function);
        }
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

    public RemoteCall<TransactionReceipt> initialize(String owner) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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

    public static class AddNewRelayEventResponse {
        public Log log;

        public String relayAddress;

        public byte[] whiteList;
    }
}
