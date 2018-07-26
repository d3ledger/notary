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
 * <p>Generated with web3j version 3.4.0.
 */
public class Master extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50604051610c3b380380610c3b83398101604052805160018054600160a060020a031916331790550160005b815181101561009a576005828281518110151561005557fe5b6020908102919091018101518254600180820185556000948552929093209092018054600160a060020a031916600160a060020a03909316929092179091550161003c565b5050610b90806100ab6000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166322f8665581146100585780632e9b50bd1461013f578063466bca0214610160575b005b34801561006457600080fd5b50604080516084356004818101356020818102858101820190965281855261005695600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a9989019892975090820195509350839250850190849080828437509497506101c59650505050505050565b34801561014b57600080fd5b50610056600160a060020a0360043516610789565b34801561016c57600080fd5b50610175610856565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156101b1578181015183820152602001610199565b505050509050019250505060405180910390f35b600080606060006101d58b6108b8565b15156101e057600080fd5b60008881526004602052604090205460ff16156101fc57600080fd5b604080518b81529051600080516020610b458339815191529181900360200190a16003546001111561022d57600080fd5b600380546000190104935083600354039250600080516020610b45833981519152836040518082815260200191505060405180910390a1855187511461027257600080fd5b845186511461028057600080fd5b845183111561028e57600080fd5b84516040805191825251600080516020610b458339815191529181900360200190a184516040519080825280602002602001820160405280156102db578160200160208202803883390190505b509150600090505b845181101561045f5760408051600160a060020a038d81166c01000000000000000000000000908102602080850191909152603484018f9052918d1602605483015260688083018c90528351808403909101815260889092019283905281516103ea93918291908401908083835b602083106103705780518252601f199092019160209182019101610351565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902088838151811015156103ab57fe5b9060200190602002015188848151811015156103c357fe5b9060200190602002015188858151811015156103db57fe5b90602001906020020151610936565b82828151811015156103f857fe5b600160a060020a03909216602092830290910190910152815160029060009084908490811061042357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461045757600080fd5b6001016102e3565b61046882610ab9565b151561047357600080fd5b600160a060020a038b1615156105115760408051303181529051600080516020610b458339815191529181900360200190a130318a11156104b357600080fd5b604051600160a060020a038a16908b156108fc02908c906000818181858888f193505050501580156104e9573d6000803e3d6000fd5b5060408051303181529051600080516020610b458339815191529181900360200190a1610764565b60008054600160a060020a038d1673ffffffffffffffffffffffffffffffffffffffff19909116811790915560408051918252517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a160008054604080517f70a082310000000000000000000000000000000000000000000000000000000081523060048201529051600080516020610b4583398151915293600160a060020a03909316926370a0823192602480820193602093909283900390910190829087803b1580156105e757600080fd5b505af11580156105fb573d6000803e3d6000fd5b505050506040513d602081101561061157600080fd5b505160408051918252519081900360200190a160008054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518d93600160a060020a03909316926370a0823192602480820193602093909283900390910190829087803b15801561068e57600080fd5b505af11580156106a2573d6000803e3d6000fd5b505050506040513d60208110156106b857600080fd5b505110156106c557600080fd5b60008054604080517fa9059cbb000000000000000000000000000000000000000000000000000000008152600160a060020a038d81166004830152602482018f90529151919092169263a9059cbb92604480820193602093909283900390910190829087803b15801561073757600080fd5b505af115801561074b573d6000803e3d6000fd5b505050506040513d602081101561076157600080fd5b50505b505050600094855250506004602052505060409020805460ff19166001179055505050565b600154600160a060020a031633141561085357600160a060020a03811660009081526002602052604090205460ff16156107c257600080fd5b600160a060020a038116600090815260026020908152604091829020805460ff19166001908117909155600380549091019081905582519081529151600080516020610b458339815191529281900390910190a160408051600160a060020a038316815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a15b50565b606060058054806020026020016040519081016040528092919081815260200182805480156108ae57602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610890575b5050505050905090565b60008080600160a060020a03841615156108d5576001925061092f565b5060009050805b60055481101561092b5783600160a060020a03166005828154811015156108ff57fe5b600091825260209091200154600160a060020a03161415610923576001915061092b565b6001016108dc565b8192505b5050919050565b604080518581529051600091829182917f648222d6a0d6f71c03a679bd0e65a7d5b2c10f2aa6efc1b3f66fa46854164533919081900360200190a1604080517f19457468657265756d205369676e6564204d6573736167653a0a333200000000602080830191909152603c8083018b905283518084039091018152605c90920192839052815191929182918401908083835b602083106109e75780518252601f1990920191602091820191016109c8565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af1158015610a67573d6000803e3d6000fd5b505060408051601f19810151600160a060020a038116825291519193507f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df46925081900360200190a19695505050505050565b600080805b8351821015610b3a5750600181015b8351811015610b2f578381815181101515610ae457fe5b90602001906020020151600160a060020a03168483815181101515610b0557fe5b90602001906020020151600160a060020a03161415610b27576000925061092f565b600101610acd565b816001019150610abe565b5060019392505050560027f8b624baf37a6fa6509de46e0bfccb6228384271c48bdea3f8eba1aa7d8343a165627a7a7230582046a8889060ed385dcd1b459be1fd120a5d3d03265e749f6ccd38c746427b30dd0029";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_GETTOKENSLIST = "getTokensList";

    public static final Event ADDRESS_EVENT_EVENT = new Event("address_event", 
            Arrays.<TypeReference<?>>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event STRING_EVENT_EVENT = new Event("string_event", 
            Arrays.<TypeReference<?>>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    public static final Event BYTES_EVENT_EVENT = new Event("bytes_event", 
            Arrays.<TypeReference<?>>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final Event NUMBER_EVENT_EVENT = new Event("number_event", 
            Arrays.<TypeReference<?>>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    protected Master(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<TransactionReceipt> withdraw(String coin_address, BigInteger amount, String to, byte[] tx_hash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(coin_address), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(to), 
                new org.web3j.abi.datatypes.generated.Bytes32(tx_hash), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> addPeer(String new_address) {
        final Function function = new Function(
                FUNC_ADDPEER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(new_address)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<List> getTokensList() {
        final Function function = new Function(FUNC_GETTOKENSLIST, 
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

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, List<String> tokens) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(tokens, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, List<String> tokens) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(tokens, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<Address_eventEventResponse> getAddress_eventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDRESS_EVENT_EVENT, transactionReceipt);
        ArrayList<Address_eventEventResponse> responses = new ArrayList<Address_eventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESS_EVENT_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(STRING_EVENT_EVENT, transactionReceipt);
        ArrayList<String_eventEventResponse> responses = new ArrayList<String_eventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(STRING_EVENT_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BYTES_EVENT_EVENT, transactionReceipt);
        ArrayList<Bytes_eventEventResponse> responses = new ArrayList<Bytes_eventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BYTES_EVENT_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(NUMBER_EVENT_EVENT, transactionReceipt);
        ArrayList<Number_eventEventResponse> responses = new ArrayList<Number_eventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBER_EVENT_EVENT, log);
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
