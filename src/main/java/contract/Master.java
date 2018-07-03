package contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
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
    private static final String BINARY = "608060405234801561001057600080fd5b5060018054600160a060020a03191633179055610a0b806100326000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166322f86655811461004d5780632e9b50bd14610134575b005b34801561005957600080fd5b50604080516084356004818101356020818102858101820190965281855261004b95600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a9989019892975090820195509350839250850190849080828437509497506101559650505050505050565b34801561014057600080fd5b5061004b600160a060020a03600435166106ad565b6000848152600460205260408120548190606090829060ff161561017857600080fd5b6003546001111561018857600080fd5b6003805460001901049350836003540392506000805160206109c0833981519152836040518082815260200191505060405180910390a185518751146101ca57fe5b84518651146101d557fe5b84518311156101e057fe5b845160408051918252516000805160206109c08339815191529181900360200190a1845160405190808252806020026020018201604052801561022d578160200160208202803883390190505b509150600090505b84518110156103ae5760408051600160a060020a038d81166c01000000000000000000000000908102602080850191909152603484018f9052918d1602605483015260688083018c905283518084039091018152608890920192839052815161033c93918291908401908083835b602083106102c25780518252601f1990920191602091820191016102a3565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902088838151811015156102fd57fe5b90602001906020020151888481518110151561031557fe5b90602001906020020151888581518110151561032d57fe5b9060200190602002015161077a565b828281518110151561034a57fe5b600160a060020a03909216602092830290910190910152815160029060009084908490811061037557fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001146103a657fe5b600101610235565b6103b782610932565b15156103bf57fe5b600160a060020a038b16151561043857604080513031815290516000805160206109c08339815191529181900360200190a130318a11156103fc57fe5b604051600160a060020a038a16908b156108fc02908c906000818181858888f19350505050158015610432573d6000803e3d6000fd5b50610688565b60008054600160a060020a038d1673ffffffffffffffffffffffffffffffffffffffff19909116811790915560408051918252517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a160008054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290516000805160206109c083398151915293600160a060020a03909316926370a0823192602480820193602093909283900390910190829087803b15801561050e57600080fd5b505af1158015610522573d6000803e3d6000fd5b505050506040513d602081101561053857600080fd5b505160408051918252519081900360200190a160008054604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518d93600160a060020a03909316926370a0823192602480820193602093909283900390910190829087803b1580156105b557600080fd5b505af11580156105c9573d6000803e3d6000fd5b505050506040513d60208110156105df57600080fd5b505110156105e957fe5b60008054604080517fa9059cbb000000000000000000000000000000000000000000000000000000008152600160a060020a038d81166004830152602482018f90529151919092169263a9059cbb92604480820193602093909283900390910190829087803b15801561065b57600080fd5b505af115801561066f573d6000803e3d6000fd5b505050506040513d602081101561068557600080fd5b50505b505050600094855250506004602052505060409020805460ff19166001179055505050565b600154600160a060020a031633141561077757600160a060020a03811660009081526002602052604090205460ff16156106e657600080fd5b600160a060020a038116600090815260026020908152604091829020805460ff191660019081179091556003805490910190819055825190815291516000805160206109c08339815191529281900390910190a160408051600160a060020a038316815290517f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df469181900360200190a15b50565b604080518581529051600091829182917f648222d6a0d6f71c03a679bd0e65a7d5b2c10f2aa6efc1b3f66fa46854164533919081900360200190a1604080517f19457468657265756d205369676e6564204d6573736167653a0a333200000000602080830191909152603c8083018b905283518084039091018152605c90920192839052815191929182918401908083835b6020831061082b5780518252601f19909201916020918201910161080c565b51815160209384036101000a60001901801990921691161790526040805192909401829003822080835293519397507f648222d6a0d6f71c03a679bd0e65a7d5b2c10f2aa6efc1b3f66fa4685416453395509083900301925050a1604080516000808252602080830180855286905260ff8a16838501526060830189905260808301889052925160019360a0808501949193601f19840193928390039091019190865af11580156108e0573d6000803e3d6000fd5b505060408051601f19810151600160a060020a038116825291519193507f4fa24eeabbb33428278439846bf665517eaaa99ebda8d54001b32aad49e9df46925081900360200190a19695505050505050565b600080805b83518210156109b35750600181015b83518110156109a857838181518110151561095d57fe5b90602001906020020151600160a060020a0316848381518110151561097e57fe5b90602001906020020151600160a060020a031614156109a057600092506109b8565b600101610946565b816001019150610937565b600192505b5050919050560027f8b624baf37a6fa6509de46e0bfccb6228384271c48bdea3f8eba1aa7d8343a165627a7a72305820dbe407be7b79c7188acd4bad968d87828c55333994b80f3957d3b1f1bd73e3000029";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_ADDPEER = "addPeer";

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

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
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
