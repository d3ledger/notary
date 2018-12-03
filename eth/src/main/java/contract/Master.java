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
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b50604051602080610f628339810180604052602081101561003a57600080fd5b50516000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610ede90819061008490396000f3fe6080604052600436106100a35763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416631e7e206481146100b057806324bbce96146101725780632e9b50bd1461019b5780638f32d59b146101ce5780639f1a156c146101e3578063aa6ca80814610216578063ae6664e01461027b578063d486885a14610290578063d48bfca7146102a5578063eea29e3e146102d8575b36156100ae57600080fd5b005b3480156100bc57600080fd5b50610160600480360360208110156100d357600080fd5b8101906020810181356401000000008111156100ee57600080fd5b82018360208201111561010057600080fd5b8035906020019184602083028401116401000000008311171561012257600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506104c2945050505050565b60408051918252519081900360200190f35b34801561017e57600080fd5b50610187610517565b604080519115158252519081900360200190f35b3480156101a757600080fd5b50610160600480360360208110156101be57600080fd5b5035600160a060020a0316610520565b3480156101da57600080fd5b5061018761059e565b3480156101ef57600080fd5b506101876004803603602081101561020657600080fd5b5035600160a060020a03166105af565b34801561022257600080fd5b5061022b610623565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561026757818101518382015260200161024f565b505050509050019250505060405180910390f35b34801561028757600080fd5b50610160610685565b34801561029c57600080fd5b506100ae61068b565b3480156102b157600080fd5b506100ae600480360360208110156102c857600080fd5b5035600160a060020a03166106ad565b3480156102e457600080fd5b506100ae60048036036101008110156102fc57600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561033d57600080fd5b82018360208201111561034f57600080fd5b8035906020019184602083028401116401000000008311171561037157600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156103c157600080fd5b8201836020820111156103d357600080fd5b803590602001918460208302840111640100000000831117156103f557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561044557600080fd5b82018360208201111561045757600080fd5b8035906020019184602083028401116401000000008311171561047957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a0316915061076e9050565b60006104cc61059e565b15156104d757600080fd5b60005b825181101561050c5761050383828151811015156104f457fe5b90602001906020020151610520565b506001016104da565b50506002545b919050565b60085460ff1681565b600061052a61059e565b151561053557600080fd5b60085460ff161561054557600080fd5b600160a060020a03821660009081526001602052604090205460ff161561056b57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600054600160a060020a0316331490565b6000600160a060020a03821615156105c957506001610512565b6000805b60075481101561061c5783600160a060020a03166007828154811015156105f057fe5b600091825260209091200154600160a060020a03161415610614576001915061061c565b6001016105cd565b5092915050565b6060600780548060200260200160405190810160405280929190818152602001828054801561067b57602002820191906000526020600020905b8154600160a060020a0316815260019091019060200180831161065d575b5050505050905090565b60025481565b61069361059e565b151561069e57600080fd5b6008805460ff19166001179055565b6106b561059e565b15156106c057600080fd5b60005b60075481101561070e5781600160a060020a03166007828154811015156106e657fe5b600091825260209091200154600160a060020a0316141561070657600080fd5b6001016106c3565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460ff16151561077f57600080fd5b610788886105af565b151561079357600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b15801561080157600080fd5b505afa158015610815573d6000803e3d6000fd5b505050506040513d602081101561082b57600080fd5b5051151561083857600080fd5b60008581526003602052604090205460ff161561085457600080fd5b6002546001111561086457600080fd5b825184511461087257600080fd5b815183511461088057600080fd5b60025482516003600019830104918290039081111561089e57600080fd5b606084516040519080825280602002602001820160405280156108cb578160200160208202803883390190505b50905060005b8551811015610a49576109d48c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c010000000000000000000000000281526014019550505050505060405160208183030381529060405280519060200120898381518110151561099557fe5b9060200190602002015189848151811015156109ad57fe5b9060200190602002015189858151811015156109c557fe5b90602001906020020151610cd7565b82828151811015156109e257fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610a0d57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610a4157600080fd5b6001016108d1565b50610a5381610da0565b1515610a5e57600080fd5b600160a060020a038b161515610b145730318a1115610ac15760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610b0f565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f19350505050158015610b0d573d6000803e3d6000fd5b505b610cca565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b158015610b7757600080fd5b505afa158015610b8b573d6000803e3d6000fd5b505050506040513d6020811015610ba157600080fd5b50511015610bf35760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610cc8565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610c9b57600080fd5b505af1158015610caf573d6000803e3d6000fd5b505050506040513d6020811015610cc557600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610d8a573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015610e4b57600460008584815181101515610dc157fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610df957506000610e4b565b6001600460008685815181101515610e0d57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610da6565b600091505b8351821015610eab576000600460008685815181101515610e6d57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610e50565b939250505056fea165627a7a72305820ff25b324b4a56595ad695e2fd5938177dc85ab39ce965bef32eb7b92859077b40029";

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
                Arrays.<Type>asList(new DynamicArray<Address>(
                        org.web3j.abi.Utils.typeMap(newAddresses, Address.class))),
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

    public Flowable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, InsufficientFundsForWithdrawalEventResponse>() {
            @Override
            public InsufficientFundsForWithdrawalEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, log);
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
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }
}
