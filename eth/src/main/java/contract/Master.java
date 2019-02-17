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
    public static final String FUNC_MINTTOKENSBYPEERS = "mintTokensByPeers";
    public static final String FUNC_XORTOKENADDRESS = "xorTokenAddress";

    public static final String FUNC_ADDPEERS = "addPeers";

    public static final String FUNC_ISLOCKADDPEER = "isLockAddPeer";

    public static final String FUNC_ADDPEER = "addPeer";
    public static final String FUNC_SETXORTOKEN = "setXorToken";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";

    public static final String FUNC_DISABLEADDINGNEWPEERS = "disableAddingNewPeers";

    public static final String FUNC_ADDTOKEN = "addToken";
    private static final String BINARY = "6080604052600a805460ff1916905534801561001a57600080fd5b506040516020806114a18339810180604052602081101561003a57600080fd5b50516000805433600160a060020a0319918216178255600580548216600160a060020a039485161790819055600680549190941691161790915561141d90819061008490396000f3fe6080604052600436106100df576000357c0100000000000000000000000000000000000000000000000000000000900480639f1a156c1161009c578063d486885a11610076578063d486885a146104d1578063d48bfca7146104e6578063e8e659a514610519578063eea29e3e1461054c576100df565b80639f1a156c14610424578063aa6ca80814610457578063ae6664e0146104bc576100df565b806301f62bd7146100ec5780631e7e2064146102c057806324bbce96146103825780632e9b50bd146103ab5780633855802d146103de5780638f32d59b1461040f575b36156100ea57600080fd5b005b3480156100f857600080fd5b506100ea600480360360c081101561010f57600080fd5b600160a060020a03823516916020810135916040820135919081019060808101606082013564010000000081111561014657600080fd5b82018360208201111561015857600080fd5b8035906020019184602083028401116401000000008311171561017a57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156101ca57600080fd5b8201836020820111156101dc57600080fd5b803590602001918460208302840111640100000000831117156101fe57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561024e57600080fd5b82018360208201111561026057600080fd5b8035906020019184602083028401116401000000008311171561028257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610736945050505050565b3480156102cc57600080fd5b50610370600480360360208110156102e357600080fd5b8101906020810181356401000000008111156102fe57600080fd5b82018360208201111561031057600080fd5b8035906020019184602083028401116401000000008311171561033257600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506109b2945050505050565b60408051918252519081900360200190f35b34801561038e57600080fd5b50610397610a07565b604080519115158252519081900360200190f35b3480156103b757600080fd5b50610370600480360360208110156103ce57600080fd5b5035600160a060020a0316610a10565b3480156103ea57600080fd5b506103f3610a8e565b60408051600160a060020a039092168252519081900360200190f35b34801561041b57600080fd5b50610397610a9d565b34801561043057600080fd5b506103976004803603602081101561044757600080fd5b5035600160a060020a0316610aae565b34801561046357600080fd5b5061046c610b22565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156104a8578181015183820152602001610490565b505050509050019250505060405180910390f35b3480156104c857600080fd5b50610370610b84565b3480156104dd57600080fd5b506100ea610b8a565b3480156104f257600080fd5b506100ea6004803603602081101561050957600080fd5b5035600160a060020a0316610bac565b34801561052557600080fd5b506100ea6004803603602081101561053c57600080fd5b5035600160a060020a0316610c6d565b34801561055857600080fd5b506100ea600480360361010081101561057057600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a0810160808201356401000000008111156105b157600080fd5b8201836020820111156105c357600080fd5b803590602001918460208302840111640100000000831117156105e557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561063557600080fd5b82018360208201111561064757600080fd5b8035906020019184602083028401116401000000008311171561066957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156106b957600080fd5b8201836020820111156106cb57600080fd5b803590602001918460208302840111640100000000831117156106ed57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a03169150610cbc9050565b600754600160a060020a0316151561074d57600080fd5b60008481526003602052604090205460ff161561076957600080fd5b6002546001111561077957600080fd5b815183511461078757600080fd5b805182511461079557600080fd5b6002548151600360001983010491829003908111156107b357600080fd5b606083516040519080825280602002602001820160405280156107e0578160200160208202803883390190505b50905060005b845181101561090c576108978a8a8a6040516020018084600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401838152602001828152602001935050505060405160208183030381529060405280519060200120888381518110151561085857fe5b90602001906020020151888481518110151561087057fe5b90602001906020020151888581518110151561088857fe5b90602001906020020151611216565b82828151811015156108a557fe5b600160a060020a0390921660209283029091019091015281516001906000908490849081106108d057fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461090457600080fd5b6001016107e6565b50610916816112df565b151561092157600080fd5b600854604080517ff0dda65c000000000000000000000000000000000000000000000000000000008152600160a060020a038c81166004830152602482018c90529151919092169163f0dda65c91604480830192600092919082900301818387803b15801561098f57600080fd5b505af11580156109a3573d6000803e3d6000fd5b50505050505050505050505050565b60006109bc610a9d565b15156109c757600080fd5b60005b82518110156109fc576109f383828151811015156109e457fe5b90602001906020020151610a10565b506001016109ca565b50506002545b919050565b600a5460ff1681565b6000610a1a610a9d565b1515610a2557600080fd5b600a5460ff1615610a3557600080fd5b600160a060020a03821660009081526001602052604090205460ff1615610a5b57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600754600160a060020a031681565b600054600160a060020a0316331490565b6000600160a060020a0382161515610ac857506001610a02565b6000805b600954811015610b1b5783600160a060020a0316600982815481101515610aef57fe5b600091825260209091200154600160a060020a03161415610b135760019150610b1b565b600101610acc565b5092915050565b60606009805480602002602001604051908101604052809291908181526020018280548015610b7a57602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610b5c575b5050505050905090565b60025481565b610b92610a9d565b1515610b9d57600080fd5b600a805460ff19166001179055565b610bb4610a9d565b1515610bbf57600080fd5b60005b600954811015610c0d5781600160a060020a0316600982815481101515610be557fe5b600091825260209091200154600160a060020a03161415610c0557600080fd5b600101610bc2565b50600980546001810182556000919091527f6e1540171b6c0c960b71a7020d9f60077f6af931a8bbf590da0223dacf75c7af01805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b610c75610a9d565b1515610c8057600080fd5b6007805473ffffffffffffffffffffffffffffffffffffffff19908116600160a060020a03938416179182905560088054929093169116179055565b600a5460ff161515610ccd57600080fd5b610cd688610aae565b1515610ce157600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b158015610d4f57600080fd5b505afa158015610d63573d6000803e3d6000fd5b505050506040513d6020811015610d7957600080fd5b50511515610d8657600080fd5b60008581526003602052604090205460ff1615610da257600080fd5b60025460011115610db257600080fd5b8251845114610dc057600080fd5b8151835114610dce57600080fd5b600254825160036000198301049182900390811115610dec57600080fd5b60608451604051908082528060200260200182016040528015610e19578160200160208202803883390190505b50905060005b8551811015610f8857610f138c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610ee357fe5b906020019060200201518984815181101515610efb57fe5b90602001906020020151898581518110151561088857fe5b8282815181101515610f2157fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610f4c57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610f8057600080fd5b600101610e1f565b50610f92816112df565b1515610f9d57600080fd5b600160a060020a038b1615156110535730318a11156110005760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a161104e565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f1935050505015801561104c573d6000803e3d6000fd5b505b611209565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b1580156110b657600080fd5b505afa1580156110ca573d6000803e3d6000fd5b505050506040513d60208110156110e057600080fd5b505110156111325760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1611207565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b1580156111da57600080fd5b505af11580156111ee573d6000803e3d6000fd5b505050506040513d602081101561120457600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa1580156112c9573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b835182101561138a5760046000858481518110151561130057fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114156113385750600061138a565b600160046000868581518110151561134c57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff1916911515919091179055600191909101906112e5565b600091505b83518210156113ea5760006004600086858151811015156113ac57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff19169115159190911790556001919091019061138f565b939250505056fea165627a7a723058203704ed51eb03ee67bbb01ec6ec5a90e0f282c19abb5a0e43750793c63881eb7e0029";

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

    public RemoteCall<String> xorTokenAddress() {
        final Function function = new Function(FUNC_XORTOKENADDRESS,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
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
