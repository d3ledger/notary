package contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
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
public class Master extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50604051602080610d928339810160405251600160a060020a038116151561003757600080fd5b6000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610d1390819061007f90396000f30060806040526004361061006c5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166322f8665581146100ac5780632e9b50bd146101955780639f1a156c146101b6578063aa6ca808146101eb578063d48bfca714610250575b361561007757600080fd5b6040805133815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a1005b3480156100b857600080fd5b50604080516084356004818101356020818102858101820190965281855261019395600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a9989019892975090820195509350839250850190849080828437509497506102719650505050505050565b005b3480156101a157600080fd5b50610193600160a060020a03600435166107be565b3480156101c257600080fd5b506101d7600160a060020a036004351661088b565b604080519115158252519081900360200190f35b3480156101f757600080fd5b50610200610909565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561023c578181015183820152602001610224565b505050509050019250505060405180910390f35b34801561025c57600080fd5b50610193600160a060020a036004351661096b565b60008060606000806102828c61088b565b151561028d57600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152336004820152600160a060020a038d811660248301529151919092169163a10cda999160448083019260209291908290030181600087803b1580156102fb57600080fd5b505af115801561030f573d6000803e3d6000fd5b505050506040513d602081101561032557600080fd5b5051151561033257600080fd5b60008981526003602052604090205460ff161561034e57600080fd5b6002546001111561035e57600080fd5b865188511461036c57600080fd5b855187511461037a57600080fd5b60025486516003600019830104965090869003945084111561039b57600080fd5b600089815260036020908152604091829020805460ff1916600117905581518d81529151600080516020610cc88339815191529281900390910190a1604080518581529051600080516020610cc88339815191529181900360200190a185516040805191825251600080516020610cc88339815191529181900360200190a18551604051908082528060200260200182016040528015610445578160200160208202803883390190505b509250600091505b85518210156105fc576105848c8c8c8c6040516020018085600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140184815260200183600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182600019166000191681526020019450505050506040516020818303038152906040526040518082805190602001908083835b6020831061050a5780518252601f1990920191602091820191016104eb565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040518091039020898481518110151561054557fe5b90602001906020020151898581518110151561055d57fe5b90602001906020020151898681518110151561057557fe5b90602001906020020151610a32565b838381518110151561059257fe5b600160a060020a0390921660209283029091019091015282516001906000908590859081106105bd57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001146105f157600080fd5b81600101915061044d565b61060583610bb5565b151561061057600080fd5b600160a060020a038c16151561066a5730318b111561062e57600080fd5b604051600160a060020a038b16908c156108fc02908d906000818181858888f19350505050158015610664573d6000803e3d6000fd5b506107b0565b50604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a082319160248083019260209291908290030181600087803b1580156106d057600080fd5b505af11580156106e4573d6000803e3d6000fd5b505050506040513d60208110156106fa57600080fd5b5051101561070757600080fd5b80600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b15801561078357600080fd5b505af1158015610797573d6000803e3d6000fd5b505050506040513d60208110156107ad57600080fd5b50505b505050505050505050505050565b600054600160a060020a031633146107d557600080fd5b600160a060020a03811660009081526001602052604090205460ff16156107fb57600080fd5b600160a060020a038116600090815260016020818152604092839020805460ff191683179055600280549092019182905582519182529151600080516020610cc8833981519152929181900390910190a160408051600160a060020a038316815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a150565b60008080600160a060020a03841615156108a85760019250610902565b5060009050805b6007548110156108fe5783600160a060020a03166007828154811015156108d257fe5b600091825260209091200154600160a060020a031614156108f657600191506108fe565b6001016108af565b8192505b5050919050565b6060600780548060200260200160405190810160405280929190818152602001828054801561096157602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610943575b5050505050905090565b60008054600160a060020a0316331461098357600080fd5b5060005b6007548110156109d25781600160a060020a03166007828154811015156109aa57fe5b600091825260209091200154600160a060020a031614156109ca57600080fd5b600101610987565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b604080518581529051600091829182917f648222d6a0d6f71c03a679bd0e65a7d5b2c10f2aa6efc1b3f66fa46854164533919081900360200190a1604080517f19457468657265756d205369676e6564204d6573736167653a0a333200000000602080830191909152603c8083018b905283518084039091018152605c90920192839052815191929182918401908083835b60208310610ae35780518252601f199092019160209182019101610ac4565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af1158015610b63573d6000803e3d6000fd5b505060408051601f19810151600160a060020a038116825291519193507f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df46925081900360200190a19695505050505050565b60008060015b8351821015610c6057600460008584815181101515610bd657fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610c0e57506000610c60565b6001600460008685815181101515610c2257fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610bbb565b600091505b8351821015610cc0576000600460008685815181101515610c8257fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610c65565b9392505050560027f8b624baf37a6fa6509de46e0bfccb6228384271c48bdea3f8eba1aa7d8343a165627a7a723058208e7f40272263975958d8ec61cc833c691a5fd97c2191bccc807bcd06b7f4f0990029";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_ADDTOKEN = "addToken";

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

    protected Master(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<TransactionReceipt> withdraw(String token_address, BigInteger amount, String to, byte[] tx_hash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new Address(token_address),
                new Uint256(amount),
                new Address(to),
                new Bytes32(tx_hash),
                new DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new DynamicArray<Bytes32>(
                        org.web3j.abi.Utils.typeMap(r, Bytes32.class)),
                new DynamicArray<Bytes32>(
                        org.web3j.abi.Utils.typeMap(s, Bytes32.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> addPeer(String new_address) {
        final Function function = new Function(
                FUNC_ADDPEER, 
                Arrays.<Type>asList(new Address(new_address)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Boolean> checkTokenAddress(String token_address) {
        final Function function = new Function(FUNC_CHECKTOKENADDRESS, 
                Arrays.<Type>asList(new Address(token_address)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<List> getTokens() {
        final Function function = new Function(FUNC_GETTOKENS, 
                Arrays.<Type>asList(), 
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

    public RemoteCall<TransactionReceipt> addToken(String new_token) {
        final Function function = new Function(
                FUNC_ADDTOKEN, 
                Arrays.<Type>asList(new Address(new_token)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
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

    public static Master load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Master load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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
