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
import rx.Observable;
import rx.functions.Func1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

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
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b50604051602080610e7883398101604052516000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610e0490819061007490396000f30060806040526004361061008d5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166324bbce9681146100cd5780632e9b50bd146100f65780638f32d59b146101195780639f1a156c1461012e578063aa6ca8081461014f578063d486885a146101b4578063d48bfca7146101c9578063eea29e3e146101ea575b361561009857600080fd5b6040805133815290517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a1005b3480156100d957600080fd5b506100e26102dc565b604080519115158252519081900360200190f35b34801561010257600080fd5b50610117600160a060020a03600435166102e5565b005b34801561012557600080fd5b506100e26103be565b34801561013a57600080fd5b506100e2600160a060020a03600435166103cf565b34801561015b57600080fd5b5061016461044d565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156101a0578181015183820152602001610188565b505050509050019250505060405180910390f35b3480156101c057600080fd5b506101176104af565b3480156101d557600080fd5b50610117600160a060020a03600435166104d1565b3480156101f657600080fd5b50604080516084356004818101356020818102858101820190965281855261011795600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a99890198929750908201955093508392508501908490808284375094975050509235600160a060020a0316935061059592505050565b60085460ff1681565b6102ed6103be565b15156102f857600080fd5b60085460ff161561030857600080fd5b600160a060020a03811660009081526001602052604090205460ff161561032e57600080fd5b600160a060020a038116600090815260016020818152604092839020805460ff191683179055600280549092019182905582519182529151600080516020610db9833981519152929181900390910190a160408051600160a060020a038316815290517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a150565b600054600160a060020a0316331490565b60008080600160a060020a03841615156103ec5760019250610446565b5060009050805b6007548110156104425783600160a060020a031660078281548110151561041657fe5b600091825260209091200154600160a060020a0316141561043a5760019150610442565b6001016103f3565b8192505b5050919050565b606060078054806020026020016040519081016040528092919081815260200182805480156104a557602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610487575b5050505050905090565b6104b76103be565b15156104c257600080fd5b6008805460ff19166001179055565b60006104db6103be565b15156104e657600080fd5b5060005b6007548110156105355781600160a060020a031660078281548110151561050d57fe5b600091825260209091200154600160a060020a0316141561052d57600080fd5b6001016104ea565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460009081906060908290819060ff1615156105b257600080fd5b6105bb8d6103cf565b15156105c657600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a0389811660048301528e811660248301529151919092169163a10cda999160448083019260209291908290030181600087803b15801561063657600080fd5b505af115801561064a573d6000803e3d6000fd5b505050506040513d602081101561066057600080fd5b5051151561066d57600080fd5b60008a81526003602052604090205460ff161561068957600080fd5b6002546001111561069957600080fd5b87518951146106a757600080fd5b86518851146106b557600080fd5b6002548751600360001983010496509086900394508411156106d657600080fd5b60008a815260036020908152604091829020805460ff1916600117905581518e81529151600080516020610db98339815191529281900390910190a1604080518581529051600080516020610db98339815191529181900360200190a186516040805191825251600080516020610db98339815191529181900360200190a18651604051908082528060200260200182016040528015610780578160200160208202803883390190505b509250600091505b8651821015610960576108e88d8d8d8d8a6040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401836000191660001916815260200182600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401955050505050506040516020818303038152906040526040518082805190602001908083835b6020831061086e5780518252601f19909201916020918201910161084f565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390208a848151811015156108a957fe5b906020019060200201518a858151811015156108c157fe5b906020019060200201518a868151811015156108d957fe5b90602001906020020151610b23565b83838151811015156108f657fe5b600160a060020a03909216602092830290910190910152825160019060009085908590811061092157fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461095557600080fd5b816001019150610788565b61096983610ca6565b151561097457600080fd5b600160a060020a038d1615156109ce5730318c111561099257600080fd5b604051600160a060020a038c16908d156108fc02908e906000818181858888f193505050501580156109c8573d6000803e3d6000fd5b50610b14565b50604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518d918d91600160a060020a038416916370a082319160248083019260209291908290030181600087803b158015610a3457600080fd5b505af1158015610a48573d6000803e3d6000fd5b505050506040513d6020811015610a5e57600080fd5b50511015610a6b57600080fd5b80600160a060020a031663a9059cbb8c8e6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610ae757600080fd5b505af1158015610afb573d6000803e3d6000fd5b505050506040513d6020811015610b1157600080fd5b50505b50505050505050505050505050565b604080518581529051600091829182917ff63569c5f7f35b1da5f0aa7910eae9cb48007b79dfe69fd658efee47b700575e919081900360200190a1604080517f19457468657265756d205369676e6564204d6573736167653a0a333200000000602080830191909152603c8083018b905283518084039091018152605c90920192839052815191929182918401908083835b60208310610bd45780518252601f199092019160209182019101610bb5565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af1158015610c54573d6000803e3d6000fd5b505060408051601f19810151600160a060020a038116825291519193507fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a49925081900360200190a19695505050505050565b60008060015b8351821015610d5157600460008584815181101515610cc757fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610cff57506000610d51565b6001600460008685815181101515610d1357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610cac565b600091505b8351821015610db1576000600460008685815181101515610d7357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610d56565b93925050505600e450ead2824f007ba5fe0defff15b2706ac9d12b679c1cea274df5f4dbf7cc47a165627a7a7230582068ee9152f0d74a4946156c7b9fa98853b6786686b190efbe18f4c0e2e1bbf3340029";

    public static final String FUNC_ISLOCKADDPEER = "isLockAddPeer";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_DISABLEADDINGNEWPEERS = "disableAddingNewPeers";

    public static final String FUNC_ADDTOKEN = "addToken";

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

    protected Master(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<Boolean> isLockAddPeer() {
        final Function function = new Function(FUNC_ISLOCKADDPEER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> addPeer(String newAddress) {
        final Function function = new Function(
                FUNC_ADDPEER, 
                Arrays.<Type>asList(new Address(newAddress)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> checkTokenAddress(String tokenAddress) {
        final Function function = new Function(FUNC_CHECKTOKENADDRESS, 
                Arrays.<Type>asList(new Address(tokenAddress)),
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

    public RemoteCall<TransactionReceipt> disableAddingNewPeers() {
        final Function function = new Function(
                FUNC_DISABLEADDINGNEWPEERS, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> addToken(String newToken) {
        final Function function = new Function(
                FUNC_ADDTOKEN, 
                Arrays.<Type>asList(new Address(newToken)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdraw(String tokenAddress, BigInteger amount, String to, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s, String from) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new Address(tokenAddress),
                new Uint256(amount),
                new Address(to),
                new Bytes32(txHash),
                new DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new DynamicArray<Bytes32>(
                        org.web3j.abi.Utils.typeMap(r, Bytes32.class)),
                new DynamicArray<Bytes32>(
                        org.web3j.abi.Utils.typeMap(s, Bytes32.class)),
                new Address(from)),
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

    public List<AddressEventEventResponse> getAddressEventEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(ADDRESSEVENT_EVENT, transactionReceipt);
        ArrayList<AddressEventEventResponse> responses = new ArrayList<AddressEventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
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
                EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESSEVENT_EVENT, log);
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
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(STRINGEVENT_EVENT, transactionReceipt);
        ArrayList<StringEventEventResponse> responses = new ArrayList<StringEventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
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
                EventValuesWithLog eventValues = extractEventParametersWithLog(STRINGEVENT_EVENT, log);
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
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(BYTESEVENT_EVENT, transactionReceipt);
        ArrayList<BytesEventEventResponse> responses = new ArrayList<BytesEventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
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
                EventValuesWithLog eventValues = extractEventParametersWithLog(BYTESEVENT_EVENT, log);
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
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(NUMBEREVENT_EVENT, transactionReceipt);
        ArrayList<NumberEventEventResponse> responses = new ArrayList<NumberEventEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
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
                EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBEREVENT_EVENT, log);
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

    public static Master load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Master load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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
