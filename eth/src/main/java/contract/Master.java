package contract;

import io.reactivex.Flowable;
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
import org.web3j.tx.gas.ContractGasProvider;

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
 * <p>Generated with web3j version 4.1.1.
 */
public class Master extends Contract {
    public static final String FUNC_XORTOKENINSTANCE = "xorTokenInstance";

    public static final String FUNC_MINTTOKENSBYPEERS = "mintTokensByPeers";
    public static final String FUNC_SETXORTOKEN = "setXorToken";

    public static final String FUNC_ADDPEERS = "addPeers";

    public static final String FUNC_ISLOCKADDPEER = "isLockAddPeer";

    public static final String FUNC_ADDPEER = "addPeer";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";

    public static final String FUNC_DISABLEADDINGNEWPEERS = "disableAddingNewPeers";

    public static final String FUNC_ADDTOKEN = "addToken";
    private static final String BINARY = "60806040526009805460ff1916905534801561001a57600080fd5b506040516020806114948339810180604052602081101561003a57600080fd5b50516000805433600160a060020a0319918216178255600580548216600160a060020a039485161790819055600680549190941691161790915561141090819061008490396000f3fe6080604052600436106100df576000357c0100000000000000000000000000000000000000000000000000000000900480639f1a156c1161009c578063d486885a11610076578063d486885a146104d1578063d48bfca7146104e6578063e8e659a514610519578063eea29e3e1461054c576100df565b80639f1a156c14610424578063aa6ca80814610457578063ae6664e0146104bc576100df565b806301f62bd7146100ec5780631b042ef9146102c05780631e7e2064146102f157806324bbce96146103b35780632e9b50bd146103dc5780638f32d59b1461040f575b36156100ea57600080fd5b005b3480156100f857600080fd5b506100ea600480360360c081101561010f57600080fd5b600160a060020a03823516916020810135916040820135919081019060808101606082013564010000000081111561014657600080fd5b82018360208201111561015857600080fd5b8035906020019184602083028401116401000000008311171561017a57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156101ca57600080fd5b8201836020820111156101dc57600080fd5b803590602001918460208302840111640100000000831117156101fe57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561024e57600080fd5b82018360208201111561026057600080fd5b8035906020019184602083028401116401000000008311171561028257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610736945050505050565b3480156102cc57600080fd5b506102d56109b2565b60408051600160a060020a039092168252519081900360200190f35b3480156102fd57600080fd5b506103a16004803603602081101561031457600080fd5b81019060208101813564010000000081111561032f57600080fd5b82018360208201111561034157600080fd5b8035906020019184602083028401116401000000008311171561036357600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506109c1945050505050565b60408051918252519081900360200190f35b3480156103bf57600080fd5b506103c8610a16565b604080519115158252519081900360200190f35b3480156103e857600080fd5b506103a1600480360360208110156103ff57600080fd5b5035600160a060020a0316610a1f565b34801561041b57600080fd5b506103c8610a9d565b34801561043057600080fd5b506103c86004803603602081101561044757600080fd5b5035600160a060020a0316610aae565b34801561046357600080fd5b5061046c610b22565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156104a8578181015183820152602001610490565b505050509050019250505060405180910390f35b3480156104c857600080fd5b506103a1610b84565b3480156104dd57600080fd5b506100ea610b8a565b3480156104f257600080fd5b506100ea6004803603602081101561050957600080fd5b5035600160a060020a0316610bac565b34801561052557600080fd5b506100ea6004803603602081101561053c57600080fd5b5035600160a060020a0316610c6d565b34801561055857600080fd5b506100ea600480360361010081101561057057600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a0810160808201356401000000008111156105b157600080fd5b8201836020820111156105c357600080fd5b803590602001918460208302840111640100000000831117156105e557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561063557600080fd5b82018360208201111561064757600080fd5b8035906020019184602083028401116401000000008311171561066957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156106b957600080fd5b8201836020820111156106cb57600080fd5b803590602001918460208302840111640100000000831117156106ed57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a03169150610caf9050565b600754600160a060020a0316151561074d57600080fd5b60008481526003602052604090205460ff161561076957600080fd5b6002546001111561077957600080fd5b815183511461078757600080fd5b805182511461079557600080fd5b6002548151600360001983010491829003908111156107b357600080fd5b606083516040519080825280602002602001820160405280156107e0578160200160208202803883390190505b50905060005b845181101561090c576108978a8a8a6040516020018084600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401838152602001828152602001935050505060405160208183030381529060405280519060200120888381518110151561085857fe5b90602001906020020151888481518110151561087057fe5b90602001906020020151888581518110151561088857fe5b90602001906020020151611209565b82828151811015156108a557fe5b600160a060020a0390921660209283029091019091015281516001906000908490849081106108d057fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461090457600080fd5b6001016107e6565b50610916816112d2565b151561092157600080fd5b600754604080517ff0dda65c000000000000000000000000000000000000000000000000000000008152600160a060020a038c81166004830152602482018c90529151919092169163f0dda65c91604480830192600092919082900301818387803b15801561098f57600080fd5b505af11580156109a3573d6000803e3d6000fd5b50505050505050505050505050565b600754600160a060020a031681565b60006109cb610a9d565b15156109d657600080fd5b60005b8251811015610a0b57610a0283828151811015156109f357fe5b90602001906020020151610a1f565b506001016109d9565b50506002545b919050565b60095460ff1681565b6000610a29610a9d565b1515610a3457600080fd5b60095460ff1615610a4457600080fd5b600160a060020a03821660009081526001602052604090205460ff1615610a6a57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600054600160a060020a0316331490565b6000600160a060020a0382161515610ac857506001610a11565b6000805b600854811015610b1b5783600160a060020a0316600882815481101515610aef57fe5b600091825260209091200154600160a060020a03161415610b135760019150610b1b565b600101610acc565b5092915050565b60606008805480602002602001604051908101604052809291908181526020018280548015610b7a57602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610b5c575b5050505050905090565b60025481565b610b92610a9d565b1515610b9d57600080fd5b6009805460ff19166001179055565b610bb4610a9d565b1515610bbf57600080fd5b60005b600854811015610c0d5781600160a060020a0316600882815481101515610be557fe5b600091825260209091200154600160a060020a03161415610c0557600080fd5b600101610bc2565b50600880546001810182556000919091527ff3f7a9fe364faab93b216da50a3214154f22a0a2b415b23a84c8169e8b636ee301805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b610c75610a9d565b1515610c8057600080fd5b6007805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60095460ff161515610cc057600080fd5b610cc988610aae565b1515610cd457600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b158015610d4257600080fd5b505afa158015610d56573d6000803e3d6000fd5b505050506040513d6020811015610d6c57600080fd5b50511515610d7957600080fd5b60008581526003602052604090205460ff1615610d9557600080fd5b60025460011115610da557600080fd5b8251845114610db357600080fd5b8151835114610dc157600080fd5b600254825160036000198301049182900390811115610ddf57600080fd5b60608451604051908082528060200260200182016040528015610e0c578160200160208202803883390190505b50905060005b8551811015610f7b57610f068c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610ed657fe5b906020019060200201518984815181101515610eee57fe5b90602001906020020151898581518110151561088857fe5b8282815181101515610f1457fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610f3f57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610f7357600080fd5b600101610e12565b50610f85816112d2565b1515610f9057600080fd5b600160a060020a038b1615156110465730318a1115610ff35760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1611041565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f1935050505015801561103f573d6000803e3d6000fd5b505b6111fc565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b1580156110a957600080fd5b505afa1580156110bd573d6000803e3d6000fd5b505050506040513d60208110156110d357600080fd5b505110156111255760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a16111fa565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b1580156111cd57600080fd5b505af11580156111e1573d6000803e3d6000fd5b505050506040513d60208110156111f757600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa1580156112bc573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b835182101561137d576004600085848151811015156112f357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001141561132b5750600061137d565b600160046000868581518110151561133f57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff1916911515919091179055600191909101906112d8565b600091505b83518210156113dd57600060046000868581518110151561139f57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190611382565b939250505056fea165627a7a72305820bdaa56b958169b3bce1e80ed3575bf88204ff4d7fb48b498e514f3527123217e0029";

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

    public RemoteCall<TransactionReceipt> mintTokensByPeers(String beneficiary, BigInteger amount, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_MINTTOKENSBYPEERS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(beneficiary),
                        new org.web3j.abi.datatypes.generated.Uint256(amount),
                        new org.web3j.abi.datatypes.generated.Bytes32(txHash),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                                org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                                org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                                org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> xorTokenInstance() {
        final Function function = new Function(FUNC_XORTOKENINSTANCE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public RemoteCall<TransactionReceipt> setXorToken(String tokenAddress) {
        final Function function = new Function(
                FUNC_SETXORTOKEN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress)), 
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
