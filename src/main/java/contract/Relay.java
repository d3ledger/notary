package contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
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
public class Relay extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5060405160208061060d833981016040525160008054600160a060020a03928316600160a060020a0319918216179182905560018054909116919092161790556105ae8061005f6000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166322f86655811461008b5780636ff3d9f414610174575b361561005657600080fd5b6040805133815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a1005b34801561009757600080fd5b50604080516084356004818101356020818102858101820190965281855261017295600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a9989019892975090820195509350839250850190849080828437509497506101959650505050505050565b005b34801561018057600080fd5b50610172600160a060020a0360043516610356565b60005460408051600160a060020a039092168252517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a16001546040517f22f86655000000000000000000000000000000000000000000000000000000008152600160a060020a0389811660048301908152602483018a905288821660448401526064830188905260e060848401908152875160e4850152875192909416936322f86655938c938c938c938c938c938c938c939092909160a482019160c4810191610104909101906020808a01910280838360005b8381101561028a578181015183820152602001610272565b50505050905001848103835286818151815260200191508051906020019060200280838360005b838110156102c95781810151838201526020016102b1565b50505050905001848103825285818151815260200191508051906020019060200280838360005b838110156103085781810151838201526020016102f0565b505050509050019a5050505050505050505050600060405180830381600087803b15801561033557600080fd5b505af1158015610349573d6000803e3d6000fd5b5050505050505050505050565b600154604080517f9f1a156c000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015291516000939290921691639f1a156c9160248082019260209290919082900301818787803b1580156103c157600080fd5b505af11580156103d5573d6000803e3d6000fd5b505050506040513d60208110156103eb57600080fd5b505115156103f857600080fd5b600160a060020a03821615156104495760008054604051600160a060020a0390911691303180156108fc02929091818181858888f19350505050158015610443573d6000803e3d6000fd5b5061057e565b5060008054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518493600160a060020a038086169463a9059cbb9491169285926370a0823192602480820193602093909283900390910190829087803b1580156104bf57600080fd5b505af11580156104d3573d6000803e3d6000fd5b505050506040513d60208110156104e957600080fd5b5051604080517c010000000000000000000000000000000000000000000000000000000063ffffffff8616028152600160a060020a03909316600484015260248301919091525160448083019260209291908290030181600087803b15801561055157600080fd5b505af1158015610565573d6000803e3d6000fd5b505050506040513d602081101561057b57600080fd5b50505b50505600a165627a7a723058206f54d0efe0d7514771c84a666ea620535d19cdc1c4bb4688c70101e9be1b0fe10029";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_SENDTOMASTER = "sendToMaster";

    public static final Event ADDRESS_EVENT_EVENT = new Event("address_event", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event STRING_EVENT_EVENT = new Event("string_event", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    public static final Event BYTES_EVENT_EVENT = new Event("bytes_event", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final Event NUMBER_EVENT_EVENT = new Event("number_event", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    protected Relay(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Relay(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<TransactionReceipt> withdraw(String token_address, BigInteger amount, String to, byte[] tx_hash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_WITHDRAW,
                Arrays.<Type>asList(new Address(token_address),
                        new Uint256(amount),
                        new Address(to),
                        new Bytes32(tx_hash),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)),
                        new org.web3j.abi.datatypes.DynamicArray<Bytes32>(
                                org.web3j.abi.Utils.typeMap(r, Bytes32.class)),
                        new org.web3j.abi.datatypes.DynamicArray<Bytes32>(
                                org.web3j.abi.Utils.typeMap(s, Bytes32.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> sendToMaster(String token_address) {
        final Function function = new Function(
                FUNC_SENDTOMASTER,
                Arrays.<Type>asList(new Address(token_address)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(master)));
        return deployRemoteCall(Relay.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(master)));
        return deployRemoteCall(Relay.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<Address_eventEventResponse> getAddress_eventEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(ADDRESS_EVENT_EVENT, transactionReceipt);
        ArrayList<Address_eventEventResponse> responses = new ArrayList<Address_eventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            Address_eventEventResponse typedResponse = new Address_eventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<Address_eventEventResponse> address_eventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, Address_eventEventResponse>() {
            @Override
            public Address_eventEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESS_EVENT_EVENT, log);
                Address_eventEventResponse typedResponse = new Address_eventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<Address_eventEventResponse> address_eventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDRESS_EVENT_EVENT));
        return address_eventEventObservable(filter);
    }

    public List<String_eventEventResponse> getString_eventEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(STRING_EVENT_EVENT, transactionReceipt);
        ArrayList<String_eventEventResponse> responses = new ArrayList<String_eventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            String_eventEventResponse typedResponse = new String_eventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<String_eventEventResponse> string_eventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, String_eventEventResponse>() {
            @Override
            public String_eventEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(STRING_EVENT_EVENT, log);
                String_eventEventResponse typedResponse = new String_eventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<String_eventEventResponse> string_eventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STRING_EVENT_EVENT));
        return string_eventEventObservable(filter);
    }

    public List<Bytes_eventEventResponse> getBytes_eventEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(BYTES_EVENT_EVENT, transactionReceipt);
        ArrayList<Bytes_eventEventResponse> responses = new ArrayList<Bytes_eventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            Bytes_eventEventResponse typedResponse = new Bytes_eventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<Bytes_eventEventResponse> bytes_eventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, Bytes_eventEventResponse>() {
            @Override
            public Bytes_eventEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(BYTES_EVENT_EVENT, log);
                Bytes_eventEventResponse typedResponse = new Bytes_eventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<Bytes_eventEventResponse> bytes_eventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BYTES_EVENT_EVENT));
        return bytes_eventEventObservable(filter);
    }

    public List<Number_eventEventResponse> getNumber_eventEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(NUMBER_EVENT_EVENT, transactionReceipt);
        ArrayList<Number_eventEventResponse> responses = new ArrayList<Number_eventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            Number_eventEventResponse typedResponse = new Number_eventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<Number_eventEventResponse> number_eventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, Number_eventEventResponse>() {
            @Override
            public Number_eventEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBER_EVENT_EVENT, log);
                Number_eventEventResponse typedResponse = new Number_eventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<Number_eventEventResponse> number_eventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NUMBER_EVENT_EVENT));
        return number_eventEventObservable(filter);
    }

    public static Relay load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Relay load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class Address_eventEventResponse {
        public Log log;

        public String input;
    }

    public static class String_eventEventResponse {
        public Log log;

        public String input;
    }

    public static class Bytes_eventEventResponse {
        public Log log;

        public byte[] input;
    }

    public static class Number_eventEventResponse {
        public Log log;

        public BigInteger input;
    }
}
