package contract;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
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
import org.web3j.tx.gas.ContractGasProvider;
import rx.Observable;
import rx.functions.Func1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class Relay extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50604051602080610624833981016040525160008054600160a060020a03928316600160a060020a0319918216179182905560018054909116919092161790556105c58061005f6000396000f30060806040526004361061004b5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416636ff3d9f4811461008b578063eea29e3e146100ae575b361561005657600080fd5b6040805133815290517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a1005b34801561009757600080fd5b506100ac600160a060020a03600435166101a0565b005b3480156100ba57600080fd5b5060408051608435600481810135602081810285810182019096528185526100ac95600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a99890198929750908201955093508392508501908490808284375094975050509235600160a060020a031693506103cc92505050565b600154604080517f9f1a156c000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015291516000939290921691639f1a156c9160248082019260209290919082900301818787803b15801561020b57600080fd5b505af115801561021f573d6000803e3d6000fd5b505050506040513d602081101561023557600080fd5b5051151561024257600080fd5b600160a060020a03821615156102935760008054604051600160a060020a0390911691303180156108fc02929091818181858888f1935050505015801561028d573d6000803e3d6000fd5b506103c8565b5060008054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518493600160a060020a038086169463a9059cbb9491169285926370a0823192602480820193602093909283900390910190829087803b15801561030957600080fd5b505af115801561031d573d6000803e3d6000fd5b505050506040513d602081101561033357600080fd5b5051604080517c010000000000000000000000000000000000000000000000000000000063ffffffff8616028152600160a060020a03909316600484015260248301919091525160448083019260209291908290030181600087803b15801561039b57600080fd5b505af11580156103af573d6000803e3d6000fd5b505050506040513d60208110156103c557600080fd5b50505b5050565b60005460408051600160a060020a039092168252517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a16001546040517feea29e3e000000000000000000000000000000000000000000000000000000008152600160a060020a038a811660048301908152602483018b905289821660448401526064830189905284821660e48401526101006084840190815288516101048501528851929094169363eea29e3e938d938d938d938d938d938d938d938d939192909160a481019160c482019161012401906020808b01910280838360005b838110156104cb5781810151838201526020016104b3565b50505050905001848103835287818151815260200191508051906020019060200280838360005b8381101561050a5781810151838201526020016104f2565b50505050905001848103825286818151815260200191508051906020019060200280838360005b83811015610549578181015183820152602001610531565b505050509050019b505050505050505050505050600060405180830381600087803b15801561057757600080fd5b505af115801561058b573d6000803e3d6000fd5b5050505050505050505050505600a165627a7a72305820ba9c53baae781a92af73069b662ed018f4523de89e2d3de834a89d37474987790029";

    public static final String FUNC_SENDTOMASTER = "sendToMaster";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event ADDRESSEVENT_EVENT = new Event("AddressEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event STRINGEVENT_EVENT = new Event("StringEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    public static final Event BYTESEVENT_EVENT = new Event("BytesEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final Event NUMBEREVENT_EVENT = new Event("NumberEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Relay(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Relay(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Relay(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Relay(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> sendToMaster(String tokenAddress) {
        final Function function = new Function(
                FUNC_SENDTOMASTER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdraw(String tokenAddress, BigInteger amount, String to, byte[] tx_hash, List<BigInteger> v, List<byte[]> r, List<byte[]> s, String from) {
        final Function function = new Function(
                FUNC_WITHDRAW,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress),
                        new org.web3j.abi.datatypes.generated.Uint256(amount),
                        new org.web3j.abi.datatypes.Address(to),
                        new org.web3j.abi.datatypes.generated.Bytes32(tx_hash),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                                org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                                org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class)),
                        new org.web3j.abi.datatypes.Address(from)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Relay> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Relay> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<AddressEventEventResponse> getAddressEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDRESSEVENT_EVENT, transactionReceipt);
        ArrayList<AddressEventEventResponse> responses = new ArrayList<AddressEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddressEventEventResponse typedResponse = new AddressEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AddressEventEventResponse> addressEventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AddressEventEventResponse>() {
            @Override
            public AddressEventEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESSEVENT_EVENT, log);
                AddressEventEventResponse typedResponse = new AddressEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AddressEventEventResponse> addressEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDRESSEVENT_EVENT));
        return addressEventEventObservable(filter);
    }

    public List<StringEventEventResponse> getStringEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(STRINGEVENT_EVENT, transactionReceipt);
        ArrayList<StringEventEventResponse> responses = new ArrayList<StringEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            StringEventEventResponse typedResponse = new StringEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<StringEventEventResponse> stringEventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, StringEventEventResponse>() {
            @Override
            public StringEventEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(STRINGEVENT_EVENT, log);
                StringEventEventResponse typedResponse = new StringEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<StringEventEventResponse> stringEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STRINGEVENT_EVENT));
        return stringEventEventObservable(filter);
    }

    public List<BytesEventEventResponse> getBytesEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BYTESEVENT_EVENT, transactionReceipt);
        ArrayList<BytesEventEventResponse> responses = new ArrayList<BytesEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BytesEventEventResponse typedResponse = new BytesEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<BytesEventEventResponse> bytesEventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, BytesEventEventResponse>() {
            @Override
            public BytesEventEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BYTESEVENT_EVENT, log);
                BytesEventEventResponse typedResponse = new BytesEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<BytesEventEventResponse> bytesEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BYTESEVENT_EVENT));
        return bytesEventEventObservable(filter);
    }

    public List<NumberEventEventResponse> getNumberEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(NUMBEREVENT_EVENT, transactionReceipt);
        ArrayList<NumberEventEventResponse> responses = new ArrayList<NumberEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            NumberEventEventResponse typedResponse = new NumberEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<NumberEventEventResponse> numberEventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, NumberEventEventResponse>() {
            @Override
            public NumberEventEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBEREVENT_EVENT, log);
                NumberEventEventResponse typedResponse = new NumberEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<NumberEventEventResponse> numberEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NUMBEREVENT_EVENT));
        return numberEventEventObservable(filter);
    }

    @Deprecated
    public static Relay load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Relay load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Relay load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Relay(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Relay load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Relay(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AddressEventEventResponse {
        public Log log;

        public String input;
    }

    public static class StringEventEventResponse {
        public Log log;

        public String input;
    }

    public static class BytesEventEventResponse {
        public Log log;

        public byte[] input;
    }

    public static class NumberEventEventResponse {
        public Log log;

        public BigInteger input;
    }
}
