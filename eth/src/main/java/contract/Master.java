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
    public static final String FUNC_REMOVEPEERBYPEER = "removePeerByPeer";
    public static final String FUNC_ADDPEERBYPEER = "addPeerByPeer";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHECKTOKENADDRESS = "checkTokenAddress";

    public static final String FUNC_GETTOKENS = "getTokens";

    public static final String FUNC_PEERSCOUNT = "peersCount";
    private static final String BINARY = "60806040523480156200001157600080fd5b506040516200160038038062001600833981018060405260408110156200003757600080fd5b8151602083018051919392830192916401000000008111156200005957600080fd5b820160208101848111156200006d57600080fd5b81518560208202830111640100000000821117156200008b57600080fd5b505060008054600160a060020a03199081163317825560058054600160a060020a0389811691841691909117918290556006805490931691161790559093509150505b81518160ff1610156200011a5762000110828260ff16815181101515620000f157fe5b9060200190602002015162000123640100000000026401000000009004565b50600101620000ce565b5050506200017d565b600160a060020a03811660009081526001602052604081205460ff16156200014a57600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b611473806200018d6000396000f3fe608060405260043610610098576000357c010000000000000000000000000000000000000000000000000000000090048063ae6664e01161006b578063ae6664e014610333578063ca70cf6e1461035a578063d48bfca714610527578063eea29e3e1461055a57610098565b806389c39baf146100a55780638f32d59b146102725780639f1a156c1461029b578063aa6ca808146102ce575b36156100a357600080fd5b005b3480156100b157600080fd5b506100a3600480360360a08110156100c857600080fd5b600160a060020a03823516916020810135918101906060810160408201356401000000008111156100f857600080fd5b82018360208201111561010a57600080fd5b8035906020019184602083028401116401000000008311171561012c57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561017c57600080fd5b82018360208201111561018e57600080fd5b803590602001918460208302840111640100000000831117156101b057600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561020057600080fd5b82018360208201111561021257600080fd5b8035906020019184602083028401116401000000008311171561023457600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610744945050505050565b34801561027e57600080fd5b50610287610923565b604080519115158252519081900360200190f35b3480156102a757600080fd5b50610287600480360360208110156102be57600080fd5b5035600160a060020a0316610934565b3480156102da57600080fd5b506102e36109aa565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561031f578181015183820152602001610307565b505050509050019250505060405180910390f35b34801561033f57600080fd5b50610348610a0c565b60408051918252519081900360200190f35b34801561036657600080fd5b506100a3600480360360a081101561037d57600080fd5b600160a060020a03823516916020810135918101906060810160408201356401000000008111156103ad57600080fd5b8201836020820111156103bf57600080fd5b803590602001918460208302840111640100000000831117156103e157600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561043157600080fd5b82018360208201111561044357600080fd5b8035906020019184602083028401116401000000008311171561046557600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156104b557600080fd5b8201836020820111156104c757600080fd5b803590602001918460208302840111640100000000831117156104e957600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610a12945050505050565b34801561053357600080fd5b506100a36004803603602081101561054a57600080fd5b5035600160a060020a0316610bb3565b34801561056657600080fd5b506100a3600480360361010081101561057e57600080fd5b600160a060020a0382358116926020810135926040820135909216916060820135919081019060a0810160808201356401000000008111156105bf57600080fd5b8201836020820111156105d157600080fd5b803590602001918460208302840111640100000000831117156105f357600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929594936020810193503591505064010000000081111561064357600080fd5b82018360208201111561065557600080fd5b8035906020019184602083028401116401000000008311171561067757600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156106c757600080fd5b8201836020820111156106d957600080fd5b803590602001918460208302840111640100000000831117156106fb57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955050509035600160a060020a03169150610c749050565b60008481526003602052604090205460ff161561076057600080fd5b6002546001111561077057600080fd5b815183511461077e57600080fd5b805182511461078c57600080fd5b6002548151600360001983010491829003908111156107aa57600080fd5b606083516040519080825280602002602001820160405280156107d7578160200160208202803883390190505b50905060005b84518110156108fb5761088689896040516020018083600160a060020a0316600160a060020a03166c010000000000000000000000000281526014018281526020019250505060405160208183030381529060405280519060200120888381518110151561084757fe5b90602001906020020151888481518110151561085f57fe5b90602001906020020151888581518110151561087757fe5b906020019060200201516111bd565b828281518110151561089457fe5b600160a060020a0390921660209283029091019091015281516001906000908490849081106108bf57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1615156001146108f357600080fd5b6001016107dd565b5061090581611286565b151561091057600080fd5b61091988611398565b5050505050505050565b600054600160a060020a0316331490565b6000600160a060020a038216151561094e575060016109a5565b6000805b6007548110156109a15783600160a060020a031660078281548110151561097557fe5b600091825260209091200154600160a060020a0316141561099957600191506109a1565b600101610952565b5090505b919050565b60606007805480602002602001604051908101604052809291908181526020018280548015610a0257602002820191906000526020600020905b8154600160a060020a031681526001909101906020018083116109e4575b5050505050905090565b60025481565b60008481526003602052604090205460ff1615610a2e57600080fd5b60025460011115610a3e57600080fd5b8151835114610a4c57600080fd5b8051825114610a5a57600080fd5b600254815160036000198301049182900390811115610a7857600080fd5b60608351604051908082528060200260200182016040528015610aa5578160200160208202803883390190505b50905060005b8451811015610b8a57610b1589896040516020018083600160a060020a0316600160a060020a03166c010000000000000000000000000281526014018281526020019250505060405160208183030381529060405280519060200120888381518110151561084757fe5b8282815181101515610b2357fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610b4e57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610b8257600080fd5b600101610aab565b50610b9481611286565b1515610b9f57600080fd5b610ba8886113ee565b505050505050505050565b610bbb610923565b1515610bc657600080fd5b60005b600754811015610c145781600160a060020a0316600782815481101515610bec57fe5b600091825260209091200154600160a060020a03161415610c0c57600080fd5b600101610bc9565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b610c7d88610934565b1515610c8857600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a03848116600483015289811660248301529151919092169163a10cda99916044808301926020929190829003018186803b158015610cf657600080fd5b505afa158015610d0a573d6000803e3d6000fd5b505050506040513d6020811015610d2057600080fd5b50511515610d2d57600080fd5b60008581526003602052604090205460ff1615610d4957600080fd5b60025460011115610d5957600080fd5b8251845114610d6757600080fd5b8151835114610d7557600080fd5b600254825160036000198301049182900390811115610d9357600080fd5b60608451604051908082528060200260200182016040528015610dc0578160200160208202803883390190505b50905060005b8551811015610f2f57610eba8c8c8c8c896040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140183815260200182600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140195505050505050604051602081830303815290604052805190602001208983815181101515610e8a57fe5b906020019060200201518984815181101515610ea257fe5b90602001906020020151898581518110151561087757fe5b8282815181101515610ec857fe5b600160a060020a039092166020928302909101909101528151600190600090849084908110610ef357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114610f2757600080fd5b600101610dc6565b50610f3981611286565b1515610f4457600080fd5b600160a060020a038b161515610ffa5730318a1115610fa75760408051600160a060020a03808e1682528b16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610ff5565b600088815260036020526040808220805460ff1916600117905551600160a060020a038b16918c156108fc02918d91818181858888f19350505050158015610ff3573d6000803e3d6000fd5b505b6111b0565b604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518c918c91600160a060020a038416916370a08231916024808301926020929190829003018186803b15801561105d57600080fd5b505afa158015611071573d6000803e3d6000fd5b505050506040513d602081101561108757600080fd5b505110156110d95760408051600160a060020a03808f1682528c16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a16111ae565b6001600360008b815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8b8d6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b15801561118157600080fd5b505af1158015611195573d6000803e3d6000fd5b505050506040513d60208110156111ab57600080fd5b50505b505b5050505050505050505050565b6000808560405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c01828152602001915050604051602081830303815290604052805190602001209050600060018287878760405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015611270573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015611331576004600085848151811015156112a757fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff161515600114156112df57506000611331565b60016004600086858151811015156112f357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff19169115159190911790556001919091019061128c565b600091505b835182101561139157600060046000868581518110151561135357fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190611336565b9392505050565b600160a060020a03811660009081526001602081905260409091205460ff161515146113c357600080fd5b600160a060020a03166000908152600160205260409020805460ff1916905560028054600019019055565b600160a060020a03811660009081526001602052604081205460ff161561141457600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff19168217905560028054909101908190559056fea165627a7a72305820d681a62d2d13fb8e4ba3f94c9aec379c7a38b2f62fefd8fe264bcaee797e9bcd0029";

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

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
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

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    public RemoteCall<TransactionReceipt> addToken(String newToken) {
        final Function function = new Function(
                FUNC_ADDTOKEN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newToken)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
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

    @Deprecated
    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry, List<String> initialPeers) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.Utils.typeMap(initialPeers, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
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

    public static class InsufficientFundsForWithdrawalEventResponse {
        public Log log;

        public String asset;

        public String recipient;
    }
}
