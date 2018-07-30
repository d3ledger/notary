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
 * <p>Generated with web3j version 3.4.0.
 */
public class Master extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50604051610b37380380610b3783398101604052805160008054600160a060020a0319163317905501805161004c906004906020840190610053565b50506100df565b8280548282559060005260206000209081019282156100a8579160200282015b828111156100a85782518254600160a060020a031916600160a060020a03909116178255602090920191600190910190610073565b506100b49291506100b8565b5090565b6100dc91905b808211156100b4578054600160a060020a03191681556001016100be565b90565b610a49806100ee6000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166322f8665581146100965780632e9b50bd1461017f5780634f64b2be146101a0575b361561006157600080fd5b6040805133815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a1005b3480156100a257600080fd5b50604080516084356004818101356020818102858101820190965281855261017d95600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a9989019892975090820195509350839250850190849080828437509497506101d49650505050505050565b005b34801561018b57600080fd5b5061017d600160a060020a036004351661067c565b3480156101ac57600080fd5b506101b8600435610749565b60408051600160a060020a039092168252519081900360200190f35b60008060606000806101e58c610771565b15156101f057600080fd5b60008981526003602052604090205460ff161561020c57600080fd5b6002546001111561021c57600080fd5b865188511461022a57600080fd5b855187511461023857600080fd5b60025486516003600019830104965090869003945084111561025957600080fd5b600089815260036020908152604091829020805460ff1916600117905581518d815291516000805160206109fe8339815191529281900390910190a16040805185815290516000805160206109fe8339815191529181900360200190a1855160408051918252516000805160206109fe8339815191529181900360200190a18551604051908082528060200260200182016040528015610303578160200160208202803883390190505b509250600091505b85518210156104ba576104428c8c8c8c6040516020018085600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140184815260200183600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182600019166000191681526020019450505050506040516020818303038152906040526040518082805190602001908083835b602083106103c85780518252601f1990920191602091820191016103a9565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040518091039020898481518110151561040357fe5b90602001906020020151898581518110151561041b57fe5b90602001906020020151898681518110151561043357fe5b906020019060200201516107ef565b838381518110151561045057fe5b600160a060020a03909216602092830290910190910152825160019060009085908590811061047b57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001146104af57600080fd5b81600101915061030b565b6104c383610972565b15156104ce57600080fd5b600160a060020a038c1615156105285730318b11156104ec57600080fd5b604051600160a060020a038b16908c156108fc02908d906000818181858888f19350505050158015610522573d6000803e3d6000fd5b5061066e565b50604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a082319160248083019260209291908290030181600087803b15801561058e57600080fd5b505af11580156105a2573d6000803e3d6000fd5b505050506040513d60208110156105b857600080fd5b505110156105c557600080fd5b80600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b15801561064157600080fd5b505af1158015610655573d6000803e3d6000fd5b505050506040513d602081101561066b57600080fd5b50505b505050505050505050505050565b600054600160a060020a0316331461069357600080fd5b600160a060020a03811660009081526001602052604090205460ff16156106b957600080fd5b600160a060020a038116600090815260016020818152604092839020805460ff1916831790556002805490920191829055825191825291516000805160206109fe833981519152929181900390910190a160408051600160a060020a038316815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a150565b600480548290811061075757fe5b600091825260209091200154600160a060020a0316905081565b60008080600160a060020a038416151561078e57600192506107e8565b5060009050805b6004548110156107e45783600160a060020a03166004828154811015156107b857fe5b600091825260209091200154600160a060020a031614156107dc57600191506107e4565b600101610795565b8192505b5050919050565b604080518581529051600091829182917f648222d6a0d6f71c03a679bd0e65a7d5b2c10f2aa6efc1b3f66fa46854164533919081900360200190a160408051600160a060020a038316815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a1604080517f19457468657265756d205369676e6564204d6573736167653a0a333200000000602080830191909152603c8083018b905283518084039091018152605c90920192839052815191929182918401908083835b602083106108dc5780518252601f1990920191602091820191016108bd565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af115801561095c573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b600080805b83518210156109f35750600181015b83518110156109e857838181518110151561099d57fe5b90602001906020020151600160a060020a031684838151811015156109be57fe5b90602001906020020151600160a060020a031614156109e057600092506107e8565b600101610986565b816001019150610977565b5060019392505050560027f8b624baf37a6fa6509de46e0bfccb6228384271c48bdea3f8eba1aa7d8343a165627a7a72305820252adebac0ee415702af8ac718237ebfaa2adbd60af051aa79e4a28790a73daf0029";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_TOKENS = "tokens";

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

    public RemoteCall<String> tokens(BigInteger param0) {
        final Function function = new Function(FUNC_TOKENS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, List<String> tokens_list) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(tokens_list, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, List<String> tokens_list) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(tokens_list, org.web3j.abi.datatypes.Address.class))));
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
