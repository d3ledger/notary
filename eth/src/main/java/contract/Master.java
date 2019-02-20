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
 * <p>Generated with web3j version 4.1.1.
 */
public class Master extends Contract {
    private static final String BINARY = "60806040523480156200001157600080fd5b50604051620019e1380380620019e1833981018060405260408110156200003757600080fd5b8151602083018051919392830192916401000000008111156200005957600080fd5b820160208101848111156200006d57600080fd5b81518560208202830111640100000000821117156200008b57600080fd5b5050929190505050620000af338383620000b7640100000000026401000000009004565b5050620001ce565b60005460ff1615620000c857600080fd5b60008054600160a060020a038086166101000261010060a860020a031990921691909117825560058054858316600160a060020a0319918216179182905560068054909116919092161790555b81518160ff161015620001615762000157828260ff168151811015156200013857fe5b9060200190602002015162000174640100000000026401000000009004565b5060010162000115565b50506000805460ff191660011790555050565b600160a060020a03811660009081526001602052604081205460ff16156200019b57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b61180380620001de6000396000f3fe608060405260043610610110576000357c010000000000000000000000000000000000000000000000000000000090048063aa6ca808116100a7578063d48bfca711610076578063d48bfca714610753578063e766307914610786578063e991232b1461079b578063eea29e3e146107b057610110565b8063aa6ca808146104d0578063ae6664e014610535578063b07c411f1461055c578063ca70cf6e1461058657610110565b806377a24f36116100e357806377a24f36146101f257806389c39baf146102bb5780638f32d59b146104885780639f1a156c1461049d57610110565b80631c8590ba1461011d5780631d345ebb146101645780634f64b2be14610197578063658afed4146101dd575b361561011b57600080fd5b005b34801561012957600080fd5b506101506004803603602081101561014057600080fd5b5035600160a060020a031661099a565b604080519115158252519081900360200190f35b34801561017057600080fd5b506101506004803603602081101561018757600080fd5b5035600160a060020a03166109af565b3480156101a357600080fd5b506101c1600480360360208110156101ba57600080fd5b50356109c4565b60408051600160a060020a039092168252519081900360200190f35b3480156101e957600080fd5b506101c16109ec565b3480156101fe57600080fd5b5061011b6004803603606081101561021557600080fd5b600160a060020a03823581169260208101359091169181019060608101604082013564010000000081111561024957600080fd5b82018360208201111561025b57600080fd5b8035906020019184602083028401116401000000008311171561027d57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506109fb945050505050565b3480156102c757600080fd5b50610150600480360360a08110156102de57600080fd5b600160a060020a038235169160208101359181019060608101604082013564010000000081111561030e57600080fd5b82018360208201111561032057600080fd5b8035906020019184602083028401116401000000008311171561034257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561039257600080fd5b8201836020820111156103a457600080fd5b803590602001918460208302840111640100000000831117156103c657600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561041657600080fd5b82018360208201111561042857600080fd5b8035906020019184602083028401116401000000008311171561044a57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610abd945050505050565b34801561049457600080fd5b50610150610ca1565b3480156104a957600080fd5b50610150600480360360208110156104c057600080fd5b5035600160a060020a0316610cb7565b3480156104dc57600080fd5b506104e5610d2d565b60408051602080825283518183015283519192839290830191858101910280838360005b83811015610521578181015183820152602001610509565b505050509050019250505060405180910390f35b34801561054157600080fd5b5061054a610d8f565b60408051918252519081900360200190f35b34801561056857600080fd5b506101506004803603602081101561057f57600080fd5b5035610d95565b34801561059257600080fd5b50610150600480360360a08110156105a957600080fd5b600160a060020a03823516916020810135918101906060810160408201356401000000008111156105d957600080fd5b8201836020820111156105eb57600080fd5b8035906020019184602083028401116401000000008311171561060d57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561065d57600080fd5b82018360208201111561066f57600080fd5b8035906020019184602083028401116401000000008311171561069157600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156106e157600080fd5b8201836020820111156106f357600080fd5b8035906020019184602083028401116401000000008311171561071557600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610daa945050505050565b34801561075f57600080fd5b5061011b6004803603602081101561077657600080fd5b5035600160a060020a0316610f50565b34801561079257600080fd5b506101c1611011565b3480156107a757600080fd5b506101c1611025565b3480156107bc57600080fd5b5061011b60048036036101008110156107d457600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561081557600080fd5b82018360208201111561082757600080fd5b8035906020019184602083028401116401000000008311171561084957600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561089957600080fd5b8201836020820111156108ab57600080fd5b803590602001918460208302840111640100000000831117156108cd57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561091d57600080fd5b82018360208201111561092f57600080fd5b8035906020019184602083028401116401000000008311171561095157600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a031691506110349050565b60016020526000908152604090205460ff1681565b60046020526000908152604090205460ff1681565b60078054829081106109d257fe5b600091825260209091200154600160a060020a0316905081565b600554600160a060020a031681565b60005460ff1615610a0b57600080fd5b60008054600160a060020a038086166101000274ffffffffffffffffffffffffffffffffffffffff00199092169190911782556005805485831673ffffffffffffffffffffffffffffffffffffffff19918216179182905560068054909116919092161790555b81518160ff161015610aaa57610aa1828260ff16815181101515610a9257fe5b9060200190602002015161154d565b50600101610a72565b50506000805460ff191660011790555050565b60008481526003602052604081205460ff1615610ad957600080fd5b60025460011115610ae957600080fd5b8251845114610af757600080fd5b8151835114610b0557600080fd5b600254825160036000198301049182900390811115610b2357600080fd5b60608451604051908082528060200260200182016040528015610b50578160200160208202803883390190505b50905060005b8551811015610c7457610bff8a8a6040516020018083600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182815260200192505050604051602081830303815290604052805190602001208983815181101515610bc057fe5b906020019060200201518984815181101515610bd857fe5b906020019060200201518985815181101515610bf057fe5b906020019060200201516115a6565b8282815181101515610c0d57fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610c3857fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610c6c57600080fd5b600101610b56565b50610c7e8161166f565b1515610c8957600080fd5b610c9289611781565b50600198975050505050505050565b6000546101009004600160a060020a0316331490565b6000600160a060020a0382161515610cd157506001610d28565b6000805b600754811015610d245783600160a060020a0316600782815481101515610cf857fe5b600091825260209091200154600160a060020a03161415610d1c5760019150610d24565b600101610cd5565b5090505b919050565b60606007805480602002602001604051908101604052809291908181526020018280548015610d8557602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610d67575b5050505050905090565b60025481565b60036020526000908152604090205460ff1681565b60008481526003602052604081205460ff1615610dc657600080fd5b60025460011115610dd657600080fd5b8251845114610de457600080fd5b8151835114610df257600080fd5b600254825160036000198301049182900390811115610e1057600080fd5b60608451604051908082528060200260200182016040528015610e3d578160200160208202803883390190505b50905060005b8551811015610f2257610ead8a8a6040516020018083600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182815260200192505050604051602081830303815290604052805190602001208983815181101515610bc057fe5b8282815181101515610ebb57fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610ee657fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610f1a57600080fd5b600101610e43565b50610f2c8161166f565b1515610f3757600080fd5b610f408961154d565b5060019998505050505050505050565b610f58610ca1565b1515610f6357600080fd5b60005b600754811015610fb15781600160a060020a0316600782815481101515610f8957fe5b600091825260209091200154600160a060020a03161415610fa957600080fd5b600101610f66565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b6000546101009004600160a060020a031681565b600654600160a060020a031681565b61103d88610cb7565b151561104857600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b1580156110b657600080fd5b505afa1580156110ca573d6000803e3d6000fd5b505050506040513d60208110156110e057600080fd5b505115156110ed57600080fd5b60008581526003602052604090205460ff161561110957600080fd5b6002546001111561111957600080fd5b825184511461112757600080fd5b815183511461113557600080fd5b60025482516003600019830104918290039081111561115357600080fd5b60608451604051908082528060200260200182016040528015611180578160200160208202803883390190505b50905060005b85518110156112bf5761124a8c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610bc057fe5b828281518110151561125857fe5b600160a060020a03909216602092830290910190910152815160019060009084908490811061128357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001146112b757600080fd5b600101611186565b506112c98161166f565b15156112d457600080fd5b600160a060020a038b16151561138a5730318a11156113375760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1611385565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f19350505050158015611383573d6000803e3d6000fd5b505b611540565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b1580156113ed57600080fd5b505afa158015611401573d6000803e3d6000fd5b505050506040513d602081101561141757600080fd5b505110156114695760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a161153e565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b15801561151157600080fd5b505af1158015611525573d6000803e3d6000fd5b505050506040513d602081101561153b57600080fd5b50505b505b5050505050505050505050565b600160a060020a03811660009081526001602052604081205460ff161561157357600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015611659573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b835182101561171a5760046000858481518110151561169057fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114156116c85750600061171a565b60016004600086858151811015156116dc57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190611675565b600091505b835182101561177a57600060046000868581518110151561173c57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff19169115159190911790556001919091019061171f565b9392505050565b600160a060020a03811660009081526001602081905260409091205460ff161515146117ac57600080fd5b600160a060020a03166000908152600160205260409020805460ff191690556002805460001901905556fea165627a7a72305820601bbf444a5e49ba2ac27031987871c8d0aeacc0f0b8a82dd824d9476026a35a0029";

    public static final String FUNC_PEERS = "peers";

    public static final String FUNC_UNIQUEADDRESSES = "uniqueAddresses";

    public static final String FUNC_TOKENS = "tokens";

    public static final String FUNC_RELAYREGISTRYADDRESS = "relayRegistryAddress";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_REMOVEPEERBYPEER = "removePeerByPeer";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";

    public static final String FUNC_USED = "used";

    public static final String FUNC_ADDPEERBYPEER = "addPeerByPeer";

    public static final String FUNC_ADDTOKEN = "addToken";

    public static final String FUNC_OWNER_ = "owner_";

    public static final String FUNC_RELAYREGISTRYINSTANCE = "relayRegistryInstance";

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

    public RemoteCall<Boolean> peers(String param0) {
        final Function function = new Function(FUNC_PEERS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> uniqueAddresses(String param0) {
        final Function function = new Function(FUNC_UNIQUEADDRESSES,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<String> tokens(BigInteger param0) {
        final Function function = new Function(FUNC_TOKENS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> relayRegistryAddress() {
        final Function function = new Function(FUNC_RELAYREGISTRYADDRESS,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> initialize(String owner, String relayRegistry, List<String> initialPeers) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner),
                        new org.web3j.abi.datatypes.Address(relayRegistry),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                                org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> removePeerByPeer(String peerAddress, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_REMOVEPEERBYPEER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(peerAddress),
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

    public RemoteCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> checkTokenAddress(String tokenAddress) {
        final Function function = new Function(FUNC_CHECKTOKENADDRESS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<List> getTokens() {
        final Function function = new Function(FUNC_GETTOKENS,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {
                }));
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
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<Boolean> used(byte[] param0) {
        final Function function = new Function(FUNC_USED,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> addPeerByPeer(String newPeerAddress, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_ADDPEERBYPEER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newPeerAddress),
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

    public RemoteCall<TransactionReceipt> addToken(String newToken) {
        final Function function = new Function(
                FUNC_ADDTOKEN,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newToken)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> owner_() {
        final Function function = new Function(FUNC_OWNER_,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> relayRegistryInstance() {
        final Function function = new Function(FUNC_RELAYREGISTRYINSTANCE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }
}
