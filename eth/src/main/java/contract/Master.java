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
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b5060405160208061106f8339810180604052602081101561003a57600080fd5b505161004f3382640100000000610055810204565b506100bc565b60005460ff161561006557600080fd5b6000805460058054600160a060020a0319908116600160a060020a039586161791829055600680549286169290911691909117905561010060a860020a03191661010093909216929092021760ff19166001179055565b610fa4806100cb6000396000f3fe6080604052600436106100a8577c010000000000000000000000000000000000000000000000000000000060003504631e7e206481146100b557806324bbce96146101775780632e9b50bd146101a0578063485cc955146101d35780638f32d59b1461020e5780639f1a156c14610223578063aa6ca80814610256578063ae6664e0146102bb578063d486885a146102d0578063d48bfca7146102e5578063eea29e3e14610318575b36156100b357600080fd5b005b3480156100c157600080fd5b50610165600480360360208110156100d857600080fd5b8101906020810181356401000000008111156100f357600080fd5b82018360208201111561010557600080fd5b8035906020019184602083028401116401000000008311171561012757600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610502945050505050565b60408051918252519081900360200190f35b34801561018357600080fd5b5061018c610557565b604080519115158252519081900360200190f35b3480156101ac57600080fd5b50610165600480360360208110156101c357600080fd5b5035600160a060020a0316610560565b3480156101df57600080fd5b506100b3600480360360408110156101f657600080fd5b50600160a060020a03813581169160200135166105de565b34801561021a57600080fd5b5061018c61065f565b34801561022f57600080fd5b5061018c6004803603602081101561024657600080fd5b5035600160a060020a0316610675565b34801561026257600080fd5b5061026b6106e9565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156102a757818101518382015260200161028f565b505050509050019250505060405180910390f35b3480156102c757600080fd5b5061016561074b565b3480156102dc57600080fd5b506100b3610751565b3480156102f157600080fd5b506100b36004803603602081101561030857600080fd5b5035600160a060020a0316610773565b34801561032457600080fd5b506100b3600480360361010081101561033c57600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561037d57600080fd5b82018360208201111561038f57600080fd5b803590602001918460208302840111640100000000831117156103b157600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561040157600080fd5b82018360208201111561041357600080fd5b8035906020019184602083028401116401000000008311171561043557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561048557600080fd5b82018360208201111561049757600080fd5b803590602001918460208302840111640100000000831117156104b957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a031691506108349050565b600061050c61065f565b151561051757600080fd5b60005b825181101561054c57610543838281518110151561053457fe5b90602001906020020151610560565b5060010161051a565b50506002545b919050565b60085460ff1681565b600061056a61065f565b151561057557600080fd5b60085460ff161561058557600080fd5b600160a060020a03821660009081526001602052604090205460ff16156105ab57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b60005460ff16156105ee57600080fd5b600080546005805473ffffffffffffffffffffffffffffffffffffffff19908116600160a060020a039586161791829055600680549286169290911691909117905574ffffffffffffffffffffffffffffffffffffffff00191661010093909216929092021760ff19166001179055565b6000546101009004600160a060020a0316331490565b6000600160a060020a038216151561068f57506001610552565b6000805b6007548110156106e25783600160a060020a03166007828154811015156106b657fe5b600091825260209091200154600160a060020a031614156106da57600191506106e2565b600101610693565b5092915050565b6060600780548060200260200160405190810160405280929190818152602001828054801561074157602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610723575b5050505050905090565b60025481565b61075961065f565b151561076457600080fd5b6008805460ff19166001179055565b61077b61065f565b151561078657600080fd5b60005b6007548110156107d45781600160a060020a03166007828154811015156107ac57fe5b600091825260209091200154600160a060020a031614156107cc57600080fd5b600101610789565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460ff16151561084557600080fd5b61084e88610675565b151561085957600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b1580156108c757600080fd5b505afa1580156108db573d6000803e3d6000fd5b505050506040513d60208110156108f157600080fd5b505115156108fe57600080fd5b60008581526003602052604090205460ff161561091a57600080fd5b6002546001111561092a57600080fd5b825184511461093857600080fd5b815183511461094657600080fd5b60025482516003600019830104918290039081111561096457600080fd5b60608451604051908082528060200260200182016040528015610991578160200160208202803883390190505b50905060005b8551811015610b0f57610a9a8c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610a5b57fe5b906020019060200201518984815181101515610a7357fe5b906020019060200201518985815181101515610a8b57fe5b90602001906020020151610d9d565b8282815181101515610aa857fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610ad357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610b0757600080fd5b600101610997565b50610b1981610e66565b1515610b2457600080fd5b600160a060020a038b161515610bda5730318a1115610b875760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610bd5565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f19350505050158015610bd3573d6000803e3d6000fd5b505b610d90565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b158015610c3d57600080fd5b505afa158015610c51573d6000803e3d6000fd5b505050506040513d6020811015610c6757600080fd5b50511015610cb95760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610d8e565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610d6157600080fd5b505af1158015610d75573d6000803e3d6000fd5b505050506040513d6020811015610d8b57600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610e50573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015610f1157600460008584815181101515610e8757fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610ebf57506000610f11565b6001600460008685815181101515610ed357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610e6c565b600091505b8351821015610f71576000600460008685815181101515610f3357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610f16565b939250505056fea165627a7a7230582039188e27f8ef5b9a8b4187571587cb661d82d861e4dd729bfc105871db69eb580029";

    public static final String FUNC_ADDPEERS = "addPeers";

    public static final String FUNC_ISLOCKADDPEER = "isLockAddPeer";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_INITIALIZE = "initialize";

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

    public RemoteCall<TransactionReceipt> initialize(String owner, String relayRegistry) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner),
                        new org.web3j.abi.datatypes.Address(relayRegistry)),
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
