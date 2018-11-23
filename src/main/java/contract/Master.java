package contract;

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
import rx.Observable;
import rx.functions.Func1;

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
    private static final String BINARY = "60806040526008805460ff1916905534801561001a57600080fd5b50604051602080610ec083398101604052516000805433600160a060020a0319918216178255600580548216600160a060020a0394851617908190556006805491909416911617909155610e4c90819061007490396000f3006080604052600436106100a35763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416631e7e206481146100b057806324bbce96146101175780632e9b50bd146101405780638f32d59b146101615780639f1a156c14610176578063aa6ca80814610197578063ae6664e0146101fc578063d486885a14610211578063d48bfca714610226578063eea29e3e14610247575b36156100ae57600080fd5b005b3480156100bc57600080fd5b5060408051602060048035808201358381028086018501909652808552610105953695939460249493850192918291850190849080828437509497506103399650505050505050565b60408051918252519081900360200190f35b34801561012357600080fd5b5061012c61038f565b604080519115158252519081900360200190f35b34801561014c57600080fd5b50610105600160a060020a0360043516610398565b34801561016d57600080fd5b5061012c610416565b34801561018257600080fd5b5061012c600160a060020a0360043516610427565b3480156101a357600080fd5b506101ac6104a5565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156101e85781810151838201526020016101d0565b505050509050019250505060405180910390f35b34801561020857600080fd5b50610105610507565b34801561021d57600080fd5b506100ae61050d565b34801561023257600080fd5b506100ae600160a060020a036004351661052f565b34801561025357600080fd5b5060408051608435600481810135602081810285810182019096528185526100ae95600160a060020a038435811696602480359760443590931696606435963696919560a49590930192909182919085019084908082843750506040805187358901803560208181028481018201909552818452989b9a998901989297509082019550935083925085019084908082843750506040805187358901803560208181028481018201909552818452989b9a99890198929750908201955093508392508501908490808284375094975050509235600160a060020a031693506105f392505050565b600080610344610416565b151561034f57600080fd5b5060005b82518110156103855761037c838281518110151561036d57fe5b90602001906020020151610398565b50600101610353565b5050600254919050565b60085460ff1681565b60006103a2610416565b15156103ad57600080fd5b60085460ff16156103bd57600080fd5b600160a060020a03821660009081526001602052604090205460ff16156103e357600080fd5b50600160a060020a03166000908152600160208190526040909120805460ff191682179055600280549091019081905590565b600054600160a060020a0316331490565b60008080600160a060020a0384161515610444576001925061049e565b5060009050805b60075481101561049a5783600160a060020a031660078281548110151561046e57fe5b600091825260209091200154600160a060020a03161415610492576001915061049a565b60010161044b565b8192505b5050919050565b606060078054806020026020016040519081016040528092919081815260200182805480156104fd57602002820191906000526020600020905b8154600160a060020a031681526001909101906020018083116104df575b5050505050905090565b60025481565b610515610416565b151561052057600080fd5b6008805460ff19166001179055565b6000610539610416565b151561054457600080fd5b5060005b6007548110156105935781600160a060020a031660078281548110151561056b57fe5b600091825260209091200154600160a060020a0316141561058b57600080fd5b600101610548565b50600780546001810182556000919091527fa66cc928b5edb82af9bd49922954155ab7b0942694bea4ce44661d9a8736c68801805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60085460009081906060908290819060ff16151561061057600080fd5b6106198d610427565b151561062457600080fd5b600654604080517fa10cda99000000000000000000000000000000000000000000000000000000008152600160a060020a0389811660048301528e811660248301529151919092169163a10cda999160448083019260209291908290030181600087803b15801561069457600080fd5b505af11580156106a8573d6000803e3d6000fd5b505050506040513d60208110156106be57600080fd5b505115156106cb57600080fd5b60008a81526003602052604090205460ff16156106e757600080fd5b600254600111156106f757600080fd5b875189511461070557600080fd5b865188511461071357600080fd5b60025487516003600019830104965090869003945084111561073457600080fd5b865160405190808252806020026020018201604052801561075f578160200160208202803883390190505b509250600091505b865182101561093f576108c78d8d8d8d8a6040516020018086600160a060020a0316600160a060020a03166c0100000000000000000000000002815260140185815260200184600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401836000191660001916815260200182600160a060020a0316600160a060020a03166c01000000000000000000000000028152601401955050505050506040516020818303038152906040526040518082805190602001908083835b6020831061084d5780518252601f19909201916020918201910161082e565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390208a8481518110151561088857fe5b906020019060200201518a858151811015156108a057fe5b906020019060200201518a868151811015156108b857fe5b90602001906020020151610bf4565b83838151811015156108d557fe5b600160a060020a03909216602092830290910190910152825160019060009085908590811061090057fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011461093457600080fd5b816001019150610767565b61094883610d0e565b151561095357600080fd5b600160a060020a038d161515610a085730318c11156109b5576040805160008152600160a060020a038d16602082015281517f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293929181900390910190a1610a03565b60008a815260036020526040808220805460ff1916600117905551600160a060020a038d16918e156108fc02918f91818181858888f19350505050158015610a01573d6000803e3d6000fd5b505b610be5565b50604080517f70a0823100000000000000000000000000000000000000000000000000000000815230600482015290518d918d91600160a060020a038416916370a082319160248083019260209291908290030181600087803b158015610a6e57600080fd5b505af1158015610a82573d6000803e3d6000fd5b505050506040513d6020811015610a9857600080fd5b50511015610b08577f33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef2938d8c6040518083600160a060020a0316600160a060020a0316815260200182600160a060020a0316600160a060020a031681526020019250505060405180910390a1610be5565b6001600360008c6000191660001916815260200190815260200160002060006101000a81548160ff02191690831515021790555080600160a060020a031663a9059cbb8c8e6040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083600160a060020a0316600160a060020a0316815260200182815260200192505050602060405180830381600087803b158015610bb857600080fd5b505af1158015610bcc573d6000803e3d6000fd5b505050506040513d6020811015610be257600080fd5b50505b50505050505050505050505050565b60008060008660405160200180807f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250601c0182600019166000191681526020019150506040516020818303038152906040526040518082805190602001908083835b60208310610c785780518252601f199092019160209182019101610c59565b51815160209384036101000a600019018019909216911617905260408051929094018290038220600080845283830180875282905260ff8e1684870152606084018d9052608084018c905294519098506001965060a080840196509194601f19820194509281900390910191865af1158015610cf8573d6000803e3d6000fd5b5050604051601f19015198975050505050505050565b60008060015b8351821015610db957600460008584815181101515610d2f57fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff16151560011415610d6757506000610db9565b6001600460008685815181101515610d7b57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610d14565b600091505b8351821015610e19576000600460008685815181101515610ddb57fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff191691151591909117905560019190910190610dbe565b93925050505600a165627a7a72305820c588d913906f60fc75804b369eedc6970fbdfda4c97a197787708c7509130c5d0029";

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

    protected Master(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Master(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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

    public static RemoteCall<Master> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RemoteCall<Master> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String relayRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relayRegistry)));
        return deployRemoteCall(Master.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
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

    public Observable<InsufficientFundsForWithdrawalEventResponse> insufficientFundsForWithdrawalEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, InsufficientFundsForWithdrawalEventResponse>() {
            @Override
            public InsufficientFundsForWithdrawalEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(INSUFFICIENTFUNDSFORWITHDRAWAL_EVENT, log);
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
