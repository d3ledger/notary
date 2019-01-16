package contract;

import io.reactivex.Flowable;
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
 * <p>Generated with web3j version 4.0.1.
 */
public class Master extends Contract {
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b50604051602080610f5c8339810180604052602081101561003a57600080fd5b50516000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610ed890819061008490396000f3fe60806040526004361061009d577c010000000000000000000000000000000000000000000000000000000060003504631e7e206481146100aa57806324bbce961461016c5780632e9b50bd146101955780638f32d59b146101c85780639f1a156c146101dd578063aa6ca80814610210578063ae6664e014610275578063d486885a1461028a578063d48bfca71461029f578063eea29e3e146102d2575b36156100a857600080fd5b005b3480156100b657600080fd5b5061015a600480360360208110156100cd57600080fd5b8101906020810181356401000000008111156100e857600080fd5b8201836020820111156100fa57600080fd5b8035906020019184602083028401116401000000008311171561011c57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506104bc945050505050565b60408051918252519081900360200190f35b34801561017857600080fd5b50610181610511565b604080519115158252519081900360200190f35b3480156101a157600080fd5b5061015a600480360360208110156101b857600080fd5b5035600160a060020a031661051a565b3480156101d457600080fd5b50610181610598565b3480156101e957600080fd5b506101816004803603602081101561020057600080fd5b5035600160a060020a03166105a9565b34801561021c57600080fd5b5061022561061d565b60408051602080825283518183015283519192839290830191858101910280838360005b83811015610261578181015183820152602001610249565b505050509050019250505060405180910390f35b34801561028157600080fd5b5061015a61067f565b34801561029657600080fd5b506100a8610685565b3480156102ab57600080fd5b506100a8600480360360208110156102c257600080fd5b5035600160a060020a03166106a7565b3480156102de57600080fd5b506100a860048036036101008110156102f657600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561033757600080fd5b82018360208201111561034957600080fd5b8035906020019184602083028401116401000000008311171561036b57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156103bb57600080fd5b8201836020820111156103cd57600080fd5b803590602001918460208302840111640100000000831117156103ef57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561043f57600080fd5b82018360208201111561045157600080fd5b8035906020019184602083028401116401000000008311171561047357600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a031691506107689050565b60006104c6610598565b15156104d157600080fd5b60005b8251811015610506576104fd83828151811015156104ee57fe5b9060200190602002015161051a565b506001016104d4565b50506002545b919050565b60085460ff1681565b6000610524610598565b151561052f57600080fd5b60085460ff161561053f57600080fd5b600160a060020a03821660009081526001602052604090205460ff161561056557600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600054600160a060020a0316331490565b6000600160a060020a03821615156105c35750600161050c565b6000805b6007548110156106165783600160a060020a03166007828154811015156105ea57fe5b600091825260209091200154600160a060020a0316141561060e5760019150610616565b6001016105c7565b5092915050565b6060600780548060200260200160405190810160405280929190818152602001828054801561067557602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610657575b5050505050905090565b60025481565b61068d610598565b151561069857600080fd5b6008805460ff19166001179055565b6106af610598565b15156106ba57600080fd5b60005b6007548110156107085781600160a060020a03166007828154811015156106e057fe5b600091825260209091200154600160a060020a0316141561070057600080fd5b6001016106bd565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460ff16151561077957600080fd5b610782886105a9565b151561078d57600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b1580156107fb57600080fd5b505afa15801561080f573d6000803e3d6000fd5b505050506040513d602081101561082557600080fd5b5051151561083257600080fd5b60008581526003602052604090205460ff161561084e57600080fd5b6002546001111561085e57600080fd5b825184511461086c57600080fd5b815183511461087a57600080fd5b60025482516003600019830104918290039081111561089857600080fd5b606084516040519080825280602002602001820160405280156108c5578160200160208202803883390190505b50905060005b8551811015610a43576109ce8c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c010000000000000000000000000281526014019550505050505060405160208183030381529060405280519060200120898381518110151561098f57fe5b9060200190602002015189848151811015156109a757fe5b9060200190602002015189858151811015156109bf57fe5b90602001906020020151610cd1565b82828151811015156109dc57fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610a0757fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610a3b57600080fd5b6001016108cb565b50610a4d81610d9a565b1515610a5857600080fd5b600160a060020a038b161515610b0e5730318a1115610abb5760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610b09565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f19350505050158015610b07573d6000803e3d6000fd5b505b610cc4565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b158015610b7157600080fd5b505afa158015610b85573d6000803e3d6000fd5b505050506040513d6020811015610b9b57600080fd5b50511015610bed5760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610cc2565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610c9557600080fd5b505af1158015610ca9573d6000803e3d6000fd5b505050506040513d6020811015610cbf57600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610d84573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015610e4557600460008584815181101515610dbb57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610df357506000610e45565b6001600460008685815181101515610e0757fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610da0565b600091505b8351821015610ea5576000600460008685815181101515610e6757fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610e4a565b939250505056fea165627a7a72305820aaaa3c87675cc8b42ba8810f74458e16699dbd9908e7c62e0126ec15bf4411f60029";

    public static final String FUNC_ADDPEERS = "addPeers";

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

    @Deprecated
    protected Master(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> addPeers(List<String> newAddresses) {
        final Function function = new Function(
                FUNC_ADDPEERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(newAddresses, org.web3j.abi.datatypes.Address.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newAddress)), 
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress)), 
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newToken)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdraw(String tokenAddress, BigInteger amount, String to, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s, String from) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(to), 
                new org.web3j.abi.datatypes.generated.Bytes32(txHash), 
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

    public List<InsufficientFundsForWithdrawalEventResponse> getInsufficientFundsForWithdrawalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, transactionReceipt);
        ArrayList<InsufficientFundsForWithdrawalEventResponse> responses = new ArrayList<InsufficientFundsForWithdrawalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InsufficientFundsForWithdrawalEventResponse typedResponse = new InsufficientFundsForWithdrawalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.asset = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, InsufficientFundsForWithdrawalEventResponse>() {
            @Override
            public InsufficientFundsForWithdrawalEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, log);
                InsufficientFundsForWithdrawalEventResponse typedResponse = new InsufficientFundsForWithdrawalEventResponse();
                typedResponse.log = log;
                typedResponse.asset = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT));
        return insufficientFundsForWithdrawalEventFlowable(filter);
    }

    @Deprecated
    public static Master load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Master load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Master(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Master load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Master(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Master load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Master(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }
}
