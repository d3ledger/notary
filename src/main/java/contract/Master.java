package contract;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
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
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b50604051602080610ddb83398101604052516000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610d6790819061007490396000f3006080604052600436106100985763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166324bbce9681146100a55780632e9b50bd146100ce5780638f32d59b146101015780639f1a156c14610116578063aa6ca80814610137578063ae6664e01461019c578063d486885a146101b1578063d48bfca7146101c6578063eea29e3e146101e7575b36156100a357600080fd5b005b3480156100b157600080fd5b506100ba6102d9565b604080519115158252519081900360200190f35b3480156100da57600080fd5b506100ef600160a060020a03600435166102e2565b60408051918252519081900360200190f35b34801561010d57600080fd5b506100ba610360565b34801561012257600080fd5b506100ba600160a060020a0360043516610371565b34801561014357600080fd5b5061014c6103ef565b60408051602080825283518183015283519192839290830191858101910280838360005b83811015610188578181015183820152602001610170565b505050509050019250505060405180910390f35b3480156101a857600080fd5b506100ef610451565b3480156101bd57600080fd5b506100a3610457565b3480156101d257600080fd5b506100a3600160a060020a0360043516610479565b3480156101f357600080fd5b5060408051608435600481810135602081810285810182019096528185526100a395600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a99890198929750908201955093508392508501908490808284375094975050509235600160a060020a0316935061053d92505050565b60085460ff1681565b60006102ec610360565b15156102f757600080fd5b60085460ff161561030757600080fd5b600160a060020a03821660009081526001602052604090205460ff161561032d57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600054600160a060020a0316331490565b60008080600160a060020a038416151561038e57600192506103e8565b5060009050805b6007548110156103e45783600160a060020a03166007828154811015156103b857fe5b600091825260209091200154600160a060020a031614156103dc57600191506103e4565b600101610395565b8192505b5050919050565b6060600780548060200260200160405190810160405280929190818152602001828054801561044757602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610429575b5050505050905090565b60025481565b61045f610360565b151561046a57600080fd5b6008805460ff19166001179055565b6000610483610360565b151561048e57600080fd5b5060005b6007548110156104dd5781600160a060020a03166007828154811015156104b557fe5b600091825260209091200154600160a060020a031614156104d557600080fd5b600101610492565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460009081906060908290819060ff16151561055a57600080fd5b6105638d610371565b151561056e57600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a0389811660048301528e811660248301529151919092169163a10cda999160448083019260209291908290030181600087803b1580156105de57600080fd5b505af11580156105f2573d6000803e3d6000fd5b505050506040513d602081101561060857600080fd5b5051151561061557600080fd5b60008a81526003602052604090205460ff161561063157600080fd5b6002546001111561064157600080fd5b875189511461064f57600080fd5b865188511461065d57600080fd5b60025487516003600019830104965090869003945084111561067e57600080fd5b60008a815260036020908152604091829020805460ff191660011790558851825181815281830281019092019092529080156106c4578160200160208202803883390190505b509250600091505b86518210156108a45761082c8d8d8d8d8a6040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401836000191660001916815260200182600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401955050505050506040516020818303038152906040526040518082805190602001908083835b602083106107b25780518252601f199092019160209182019101610793565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390208a848151811015156107ed57fe5b906020019060200201518a8581518110151561080557fe5b906020019060200201518a8681518110151561081d57fe5b90602001906020020151610b0f565b838381518110151561083a57fe5b600160a060020a03909216602092830290910190910152825160019060009085908590811061086557fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461089957600080fd5b8160010191506106cc565b6108ad83610c29565b15156108b857600080fd5b600160a060020a038d1615156109575730318c111561091a576040805160008152600160a060020a038d16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610952565b604051600160a060020a038c16908d156108fc02908e906000818181858888f19350505050158015610950573d6000803e3d6000fd5b505b610b00565b50604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518d918d91600160a060020a038416916370a082319160248083019260209291908290030181600087803b1580156109bd57600080fd5b505af11580156109d1573d6000803e3d6000fd5b505050506040513d60208110156109e757600080fd5b50511015610a57577f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef2938d8c6040518083600160a060020a0316600160a060020a0316815260200182600160a060020a0316600160a060020a031681526020019250505060405180910390a1610b00565b80600160a060020a031663a9059cbb8c8e6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610ad357600080fd5b505af1158015610ae7573d6000803e3d6000fd5b505050506040513d6020811015610afd57600080fd5b50505b50505050505050505050505050565b60008060008660405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c0182600019166000191681526020019150506040516020818303038152906040526040518082805190602001908083835b60208310610b935780518252601f199092019160209182019101610b74565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af1158015610c13573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015610cd457600460008584815181101515610c4a57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610c8257506000610cd4565b6001600460008685815181101515610c9657fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610c2f565b600091505b8351821015610d34576000600460008685815181101515610cf657fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610cd9565b93925050505600a165627a7a72305820a6fc518fab600945ce8f97f0c9a78b78bfd019d14e73d6bf4d0ec948464f6ffa0029";

    public static final String FUNC_ISLOCKADDPEER = "isLockAddPeer";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";

    public static final String FUNC_DISABLEADDINGNEWPEERS = "disableAddingNewPeers";

    public static final String FUNC_ADDTOKEN = "addToken";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT = new Event("InsufficientFundsForWithdrawal", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
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

    public RemoteCall<BigInteger> peersCount() {
        final Function function = new Function(FUNC_PEERSCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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
                new org.web3j.abi.datatypes.generated.Bytes32(txHash), 
                new DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class)), 
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

    public List<InsufficientFundsForWithdrawalEventResponse> getInsufficientFundsForWithdrawalEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, transactionReceipt);
        ArrayList<InsufficientFundsForWithdrawalEventResponse> responses = new ArrayList<InsufficientFundsForWithdrawalEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            InsufficientFundsForWithdrawalEventResponse typedResponse = new InsufficientFundsForWithdrawalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.asset = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, InsufficientFundsForWithdrawalEventResponse>() {
            @Override
            public InsufficientFundsForWithdrawalEventResponse call(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, log);
                InsufficientFundsForWithdrawalEventResponse typedResponse = new InsufficientFundsForWithdrawalEventResponse();
                typedResponse.log = log;
                typedResponse.asset = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT));
        return insufficientFundsForWithdrawalEventObservable(filter);
    }

    public static Master load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Master load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }
}
