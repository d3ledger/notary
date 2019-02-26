package contract;

import io.reactivex.Flowable;
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
public class Relay extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b506040516020806108348339810180604052602081101561003057600080fd5b50516100448164010000000061004a810204565b506100a3565b60005460ff161561005a57600080fd5b600080546001805461010061010060a860020a0319909316600160a060020a03958616840217928304909416600160a060020a031990941693909317835560ff19169091179055565b610782806100b26000396000f3fe608060405260043610610050577c010000000000000000000000000000000000000000000000000000000060003504636ff3d9f48114610090578063c4d66de8146100c5578063eea29e3e146100f8575b361561005b57600080fd5b6040805133815290517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a1005b34801561009c57600080fd5b506100c3600480360360208110156100b357600080fd5b5035600160a060020a03166102e2565b005b3480156100d157600080fd5b506100c3600480360360208110156100e857600080fd5b5035600160a060020a0316610511565b34801561010457600080fd5b506100c3600480360361010081101561011c57600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561015d57600080fd5b82018360208201111561016f57600080fd5b8035906020019184602083028401116401000000008311171561019157600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156101e157600080fd5b8201836020820111156101f357600080fd5b8035906020019184602083028401116401000000008311171561021557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561026557600080fd5b82018360208201111561027757600080fd5b8035906020019184602083028401116401000000008311171561029957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a031691506105849050565b600154604080517f9f1a156c000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015291519190921691639f1a156c916024808301926020929190829003018186803b15801561034857600080fd5b505afa15801561035c573d6000803e3d6000fd5b505050506040513d602081101561037257600080fd5b5051151561037f57600080fd5b600160a060020a03811615156103d75760008054604051600160a060020a036101009092049190911691303180156108fc02929091818181858888f193505050501580156103d1573d6000803e3d6000fd5b5061050e565b600054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518392600160a060020a038085169363a9059cbb936101009093049091169184916370a08231916024808301926020929190829003018186803b15801561044e57600080fd5b505afa158015610462573d6000803e3d6000fd5b505050506040513d602081101561047857600080fd5b5051604080517c010000000000000000000000000000000000000000000000000000000063ffffffff8616028152600160a060020a03909316600484015260248301919091525160448083019260209291908290030181600087803b1580156104e057600080fd5b505af11580156104f4573d6000803e3d6000fd5b505050506040513d602081101561050a57600080fd5b5050505b50565b60005460ff161561052157600080fd5b600080546001805461010074ffffffffffffffffffffffffffffffffffffffff0019909316600160a060020a0395861684021792830490941673ffffffffffffffffffffffffffffffffffffffff1990941693909317835560ff19169091179055565b60005460408051610100909204600160a060020a03168252517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a49916020908290030190a16001546040517feea29e3e000000000000000000000000000000000000000000000000000000008152600160a060020a038a811660048301908152602483018b905289821660448401526064830189905284821660e48401526101006084840190815288516101048501528851929094169363eea29e3e938d938d938d938d938d938d938d938d939192909160a481019160c482019161012401906020808b01910280838360005b83811015610688578181015183820152602001610670565b50505050905001848103835287818151815260200191508051906020019060200280838360005b838110156106c75781810151838201526020016106af565b50505050905001848103825286818151815260200191508051906020019060200280838360005b838110156107065781810151838201526020016106ee565b505050509050019b505050505050505050505050600060405180830381600087803b15801561073457600080fd5b505af1158015610748573d6000803e3d6000fd5b50505050505050505050505056fea165627a7a72305820bfa0670bb38891547d29a7e39da54b56c1e19e0f252359599f95de0c7e05ffbd0029";

    public static final String FUNC_SENDTOMASTER = "sendToMaster";

    public static final String FUNC_INITIALIZE = "initialize";

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

    public RemoteCall<TransactionReceipt> initialize(String master) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)), 
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

    public Flowable<AddressEventEventResponse> addressEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AddressEventEventResponse>() {
            @Override
            public AddressEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESSEVENT_EVENT, log);
                AddressEventEventResponse typedResponse = new AddressEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AddressEventEventResponse> addressEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDRESSEVENT_EVENT));
        return addressEventEventFlowable(filter);
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

    public Flowable<StringEventEventResponse> stringEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, StringEventEventResponse>() {
            @Override
            public StringEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(STRINGEVENT_EVENT, log);
                StringEventEventResponse typedResponse = new StringEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<StringEventEventResponse> stringEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STRINGEVENT_EVENT));
        return stringEventEventFlowable(filter);
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

    public Flowable<BytesEventEventResponse> bytesEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, BytesEventEventResponse>() {
            @Override
            public BytesEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BYTESEVENT_EVENT, log);
                BytesEventEventResponse typedResponse = new BytesEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<BytesEventEventResponse> bytesEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BYTESEVENT_EVENT));
        return bytesEventEventFlowable(filter);
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

    public Flowable<NumberEventEventResponse> numberEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, NumberEventEventResponse>() {
            @Override
            public NumberEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBEREVENT_EVENT, log);
                NumberEventEventResponse typedResponse = new NumberEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<NumberEventEventResponse> numberEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NUMBEREVENT_EVENT));
        return numberEventEventFlowable(filter);
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
