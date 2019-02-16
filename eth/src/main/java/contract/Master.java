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
    public static final String FUNC_PEERS = "peers";
    public static final String FUNC_UNIQUEADDRESSES = "uniqueAddresses";
    public static final String FUNC_TOKENS = "tokens";
    public static final String FUNC_RELAYREGISTRYADDRESS = "relayRegistryAddress";
    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_REMOVEPEERBYPEER = "removePeerByPeer";
    public static final String FUNC_USED = "used";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";
    public static final String FUNC_ADDPEERBYPEER = "addPeerByPeer";
    public static final String FUNC_RELAYREGISTRYINSTANCE = "relayRegistryInstance";

    public static final String FUNC_ADDTOKEN = "addToken";
    public static final Event TESTINFO_EVENT = new Event("TestInfo",
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
            }));

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT = new Event("InsufficientFundsForWithdrawal", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;
    private static final String BINARY = "60806040523480156200001157600080fd5b506040516200191038038062001910833981018060405260408110156200003757600080fd5b8151602083018051919392830192916401000000008111156200005957600080fd5b820160208101848111156200006d57600080fd5b81518560208202830111640100000000821117156200008b57600080fd5b505060008054600160a060020a03199081163317825560058054600160a060020a0389811691841691909117918290556006805490931691161790559093509150505b81518160ff1610156200011a5762000110828260ff16815181101515620000f157fe5b9060200190602002015162000123640100000000026401000000009004565b50600101620000ce565b5050506200017d565b600160a060020a03811660009081526001602052604081205460ff16156200014a57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b611783806200018d6000396000f3fe608060405260043610610105576000357c0100000000000000000000000000000000000000000000000000000000900480639f1a156c116100a7578063ca70cf6e11610076578063ca70cf6e146104c7578063d48bfca714610694578063e991232b146106c7578063eea29e3e146106dc57610105565b80639f1a156c146103de578063aa6ca80814610411578063ae6664e014610476578063b07c411f1461049d57610105565b8063658afed4116100e3578063658afed4146101d257806389c39baf146101e75780638da5cb5b146103b45780638f32d59b146103c957610105565b80631c8590ba146101125780631d345ebb146101595780634f64b2be1461018c575b361561011057600080fd5b005b34801561011e57600080fd5b506101456004803603602081101561013557600080fd5b5035600160a060020a03166108c6565b604080519115158252519081900360200190f35b34801561016557600080fd5b506101456004803603602081101561017c57600080fd5b5035600160a060020a03166108db565b34801561019857600080fd5b506101b6600480360360208110156101af57600080fd5b50356108f0565b60408051600160a060020a039092168252519081900360200190f35b3480156101de57600080fd5b506101b6610918565b3480156101f357600080fd5b50610145600480360360a081101561020a57600080fd5b600160a060020a038235169160208101359181019060608101604082013564010000000081111561023a57600080fd5b82018360208201111561024c57600080fd5b8035906020019184602083028401116401000000008311171561026e57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156102be57600080fd5b8201836020820111156102d057600080fd5b803590602001918460208302840111640100000000831117156102f257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561034257600080fd5b82018360208201111561035457600080fd5b8035906020019184602083028401116401000000008311171561037657600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610927945050505050565b3480156103c057600080fd5b506101b6610b0b565b3480156103d557600080fd5b50610145610b1a565b3480156103ea57600080fd5b506101456004803603602081101561040157600080fd5b5035600160a060020a0316610b2b565b34801561041d57600080fd5b50610426610ba1565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561046257818101518382015260200161044a565b505050509050019250505060405180910390f35b34801561048257600080fd5b5061048b610c03565b60408051918252519081900360200190f35b3480156104a957600080fd5b50610145600480360360208110156104c057600080fd5b5035610c09565b3480156104d357600080fd5b50610145600480360360a08110156104ea57600080fd5b600160a060020a038235169160208101359181019060608101604082013564010000000081111561051a57600080fd5b82018360208201111561052c57600080fd5b8035906020019184602083028401116401000000008311171561054e57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561059e57600080fd5b8201836020820111156105b057600080fd5b803590602001918460208302840111640100000000831117156105d257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561062257600080fd5b82018360208201111561063457600080fd5b8035906020019184602083028401116401000000008311171561065657600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610c1e945050505050565b3480156106a057600080fd5b50610110600480360360208110156106b757600080fd5b5035600160a060020a0316610dc4565b3480156106d357600080fd5b506101b6610e85565b3480156106e857600080fd5b50610110600480360361010081101561070057600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a08101608082013564010000000081111561074157600080fd5b82018360208201111561075357600080fd5b8035906020019184602083028401116401000000008311171561077557600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156107c557600080fd5b8201836020820111156107d757600080fd5b803590602001918460208302840111640100000000831117156107f957600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561084957600080fd5b82018360208201111561085b57600080fd5b8035906020019184602083028401116401000000008311171561087d57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a03169150610e949050565b60016020526000908152604090205460ff1681565b60046020526000908152604090205460ff1681565b60078054829081106108fe57fe5b600091825260209091200154600160a060020a0316905081565b600554600160a060020a031681565b60008481526003602052604081205460ff161561094357600080fd5b6002546001111561095357600080fd5b825184511461096157600080fd5b815183511461096f57600080fd5b60025482516003600019830104918290039081111561098d57600080fd5b606084516040519080825280602002602001820160405280156109ba578160200160208202803883390190505b50905060005b8551811015610ade57610a698a8a6040516020018083600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182815260200192505050604051602081830303815290604052805190602001208983815181101515610a2a57fe5b906020019060200201518984815181101515610a4257fe5b906020019060200201518985815181101515610a5a57fe5b906020019060200201516114cd565b8282815181101515610a7757fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610aa257fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610ad657600080fd5b6001016109c0565b50610ae881611596565b1515610af357600080fd5b610afc896116a8565b50600198975050505050505050565b600054600160a060020a031681565b600054600160a060020a0316331490565b6000600160a060020a0382161515610b4557506001610b9c565b6000805b600754811015610b985783600160a060020a0316600782815481101515610b6c57fe5b600091825260209091200154600160a060020a03161415610b905760019150610b98565b600101610b49565b5090505b919050565b60606007805480602002602001604051908101604052809291908181526020018280548015610bf957602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610bdb575b5050505050905090565b60025481565b60036020526000908152604090205460ff1681565b60008481526003602052604081205460ff1615610c3a57600080fd5b60025460011115610c4a57600080fd5b8251845114610c5857600080fd5b8151835114610c6657600080fd5b600254825160036000198301049182900390811115610c8457600080fd5b60608451604051908082528060200260200182016040528015610cb1578160200160208202803883390190505b50905060005b8551811015610d9657610d218a8a6040516020018083600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140182815260200192505050604051602081830303815290604052805190602001208983815181101515610a2a57fe5b8282815181101515610d2f57fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610d5a57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610d8e57600080fd5b600101610cb7565b50610da081611596565b1515610dab57600080fd5b610db4896116fe565b5060019998505050505050505050565b610dcc610b1a565b1515610dd757600080fd5b60005b600754811015610e255781600160a060020a0316600782815481101515610dfd57fe5b600091825260209091200154600160a060020a03161415610e1d57600080fd5b600101610dda565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b600654600160a060020a031681565b610e9d88610b2b565b1515610ea857600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b158015610f1657600080fd5b505afa158015610f2a573d6000803e3d6000fd5b505050506040513d6020811015610f4057600080fd5b50511515610f4d57600080fd5b60008581526003602052604090205460ff1615610f6957600080fd5b60025460011115610f7957600080fd5b8251845114610f8757600080fd5b8151835114610f9557600080fd5b600254825160036000198301049182900390811115610fb357600080fd5b6040805160208082526001908201527f31000000000000000000000000000000000000000000000000000000000000008183015290517f42d7f55ddad77363e0072cb2968db2509471e24abc05cb021d204a77dc98e28c9181900360600190a160608451604051908082528060200260200182016040528015611040578160200160208202803883390190505b50905060005b855181101561117f5761110a8c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610a2a57fe5b828281518110151561111857fe5b600160a060020a03909216602092830290910190910152815160019060009084908490811061114357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461117757600080fd5b600101611046565b506040805160208082526001908201527f32000000000000000000000000000000000000000000000000000000000000008183015290517f42d7f55ddad77363e0072cb2968db2509471e24abc05cb021d204a77dc98e28c9181900360600190a16111e981611596565b15156111f457600080fd5b600160a060020a038b16151561130a5730318a11156112575760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a16112a5565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f193505050501580156112a3573d6000803e3d6000fd5b505b6040805160208082526001908201527f33000000000000000000000000000000000000000000000000000000000000008183015290517f42d7f55ddad77363e0072cb2968db2509471e24abc05cb021d204a77dc98e28c9181900360600190a16114c0565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b15801561136d57600080fd5b505afa158015611381573d6000803e3d6000fd5b505050506040513d602081101561139757600080fd5b505110156113e95760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a16114be565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b15801561149157600080fd5b505af11580156114a5573d6000803e3d6000fd5b505050506040513d60208110156114bb57600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015611580573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015611641576004600085848151811015156115b757fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114156115ef57506000611641565b600160046000868581518110151561160357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff19169115159190911790556001919091019061159c565b600091505b83518210156116a157600060046000868581518110151561166357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190611646565b9392505050565b600160a060020a03811660009081526001602081905260409091205460ff161515146116d357600080fd5b600160a060020a03166000908152600160205260409020805460ff1916905560028054600019019055565b600160a060020a03811660009081526001602052604081205460ff161561172457600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff19168217905560028054909101908190559056fea165627a7a72305820221eda3f52a2847399b5a25cbf242acba194776050f8255b514774e0a9ccb2210029";
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

    public RemoteCall<TransactionReceipt> addToken(String newToken) {
        final Function function = new Function(
                FUNC_ADDTOKEN,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newToken)),
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

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public RemoteCall<String> relayRegistryInstance() {
        final Function function = new Function(FUNC_RELAYREGISTRYINSTANCE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public List<TestInfoEventResponse> getTestInfoEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TESTINFO_EVENT, transactionReceipt);
        ArrayList<TestInfoEventResponse> responses = new ArrayList<TestInfoEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TestInfoEventResponse typedResponse = new TestInfoEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TestInfoEventResponse> testInfoEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, TestInfoEventResponse>() {
            @Override
            public TestInfoEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TESTINFO_EVENT, log);
                TestInfoEventResponse typedResponse = new TestInfoEventResponse();
                typedResponse.log = log;
                typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TestInfoEventResponse> testInfoEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TESTINFO_EVENT));
        return testInfoEventFlowable(filter);
    }

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }

    public static class TestInfoEventResponse {
        public Log log;

        public String message;
    }
}
