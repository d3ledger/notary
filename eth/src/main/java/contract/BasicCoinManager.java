package contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
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
import org.web3j.tuples.generated.Tuple3;
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
public class BasicCoinManager extends Contract {
    private static final String BINARY = "608060405260008054600160a060020a0319163317905534801561002257600080fd5b50610fb8806100326000396000f3fe60806040526004361061008a5760003560e060020a900480638da5cb5b1161005d5780638da5cb5b146101335780639507d39a146101645780639890220b146101b9578063acfdfd1c146101ce578063c00ca383146103215761008a565b8063061ea8cc1461008f57806306661abd146100d457806313af4035146100e95780635001f3b51461011e575b600080fd5b34801561009b57600080fd5b506100c2600480360360208110156100b257600080fd5b5035600160a060020a031661035a565b60408051918252519081900360200190f35b3480156100e057600080fd5b506100c2610375565b3480156100f557600080fd5b5061011c6004803603602081101561010c57600080fd5b5035600160a060020a031661037c565b005b34801561012a57600080fd5b506100c26103fb565b34801561013f57600080fd5b50610148610402565b60408051600160a060020a039092168252519081900360200190f35b34801561017057600080fd5b5061018e6004803603602081101561018757600080fd5b5035610411565b60408051600160a060020a039485168152928416602084015292168183015290519081900360600190f35b3480156101c557600080fd5b5061011c610480565b61030d600480360360808110156101e457600080fd5b8135919081019060408101602082013564010000000081111561020657600080fd5b82018360208201111561021857600080fd5b8035906020019184600183028401116401000000008311171561023a57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929594936020810193503591505064010000000081111561028d57600080fd5b82018360208201111561029f57600080fd5b803590602001918460018302840111640100000000831117156102c157600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092955050509035600160a060020a031691506104c09050565b604080519115158252519081900360200190f35b34801561032d57600080fd5b5061018e6004803603604081101561034457600080fd5b50600160a060020a038135169060200135610884565b600160a060020a031660009081526002602052604090205490565b6001545b90565b600054600160a060020a0316331461039357600080fd5b60008054604051600160a060020a03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a36000805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b620f424081565b600054600160a060020a031681565b600080600061041e6108ce565b600180548690811061042c57fe5b60009182526020918290206040805160608101825260039093029091018054600160a060020a0390811680855260018301548216958501869052600290920154169290910182905297919650945092505050565b600054600160a060020a0316331461049757600080fd5b6040513390303180156108fc02916000818181858888f1935050505015156104be57600080fd5b565b600080829050600086336040516104d6906108ee565b918252600160a060020a03166020820152604080519182900301906000f080158015610506573d6000803e3d6000fd5b50905060006105143361035a565b9050600083600160a060020a031663ddca3f436040518163ffffffff1660e060020a02815260040160206040518083038186803b15801561055457600080fd5b505afa158015610568573d6000803e3d6000fd5b505050506040513d602081101561057e57600080fd5b505133600090815260026020526040902090915060018301906105a190826108fb565b506001543360009081526002602052604090208054849081106105c057fe5b9060005260206000200181905550600160606040519081016040528085600160a060020a0316815260200133600160a060020a0316815260200186600160a060020a03168152509080600181540180825580915050906001820390600052602060002090600302016000909192909190915060008201518160000160006101000a815481600160a060020a030219169083600160a060020a0316021790555060208201518160010160006101000a815481600160a060020a030219169083600160a060020a0316021790555060408201518160020160006101000a815481600160a060020a030219169083600160a060020a0316021790555050505083600160a060020a0316637b1a547c82858b620f42408c336040518763ffffffff1660e060020a0281526004018086600160a060020a0316600160a060020a03168152602001806020018581526020018060200184600160a060020a0316600160a060020a03168152602001838103835287818151815260200191508051906020019080838360005b8381101561075d578181015183820152602001610745565b50505050905090810190601f16801561078a5780820380516001836020036101000a031916815260200191505b50838103825285518152855160209182019187019080838360005b838110156107bd5781810151838201526020016107a5565b50505050905090810190601f1680156107ea5780820380516001836020036101000a031916815260200191505b509750505050505050506020604051808303818588803b15801561080d57600080fd5b505af1158015610821573d6000803e3d6000fd5b50505050506040513d602081101561083857600080fd5b5050604051600160a060020a03808516919086169033907f454b0172f64812df0cd504c2bd7df7aab8ff328a54d946b4bd0fa7c527adf9cc90600090a450600198975050505050505050565b600160a060020a03821660009081526002602052604081208054829182916108c19190869081106108b157fe5b9060005260206000200154610411565b9250925092509250925092565b604080516060810182526000808252602082018190529181019190915290565b61064a8061094383390190565b81548183558181111561091f5760008381526020902061091f918101908301610924565b505050565b61037991905b8082111561093e576000815560010161092a565b509056fe60806040526000805460a060020a60ff0219600160a060020a0319909116331716905534801561002e57600080fd5b5060405160408061064a8339810180604052604081101561004e57600080fd5b5080516020909101518180151561006457600080fd5b50600182905560008054600160a060020a03909216600160a060020a031990921682178155908152600260205260409020556105a5806100a56000396000f3fe608060405234801561001057600080fd5b50600436106100bb576000357c01000000000000000000000000000000000000000000000000000000009004806350f9b6cd1161008357806350f9b6cd1461018057806370a08231146101885780638da5cb5b146101ae578063a9059cbb146101d2578063dd62ed3e146101fe576100bb565b8063095ea7b3146100c057806313af40351461010057806318160ddd1461012857806323b872dd146101425780635001f3b514610178575b600080fd5b6100ec600480360360408110156100d657600080fd5b50600160a060020a03813516906020013561022c565b604080519115158252519081900360200190f35b6101266004803603602081101561011657600080fd5b5035600160a060020a03166102a6565b005b610130610325565b60408051918252519081900360200190f35b6100ec6004803603606081101561015857600080fd5b50600160a060020a0381358116916020810135909116906040013561032b565b610130610460565b6100ec610467565b6101306004803603602081101561019e57600080fd5b5035600160a060020a0316610488565b6101b66104a3565b60408051600160a060020a039092168252519081900360200190f35b6100ec600480360360408110156101e857600080fd5b50600160a060020a0381351690602001356104b2565b6101306004803603604081101561021457600080fd5b50600160a060020a038135811691602001351661054a565b604080518281529051600091600160a060020a0385169133917f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925919081900360200190a350336000908152600260209081526040808320600160a060020a0386168452600190810190925290912080548301905592915050565b600054600160a060020a031633146102bd57600080fd5b60008054604051600160a060020a03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a36000805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60015481565b600160a060020a0383166000908152600260205260408120548490839081111561035457600080fd5b600160a060020a038616600090815260026020908152604080832033808552600190910190925290912054879190869081111561039057600080fd5b6000805474ff0000000000000000000000000000000000000000191674010000000000000000000000000000000000000000179055604080518881529051600160a060020a03808b1692908c16917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a3600160a060020a03808a1660009081526002602081815260408084203385526001808201845282862080548f900390559390925281548c9003909155928b168252919020805489019055955050505050509392505050565b620f424081565b60005474010000000000000000000000000000000000000000900460ff1681565b600160a060020a031660009081526002602052604090205490565b600054600160a060020a031681565b3360008181526002602052604081205490919083908111156104d357600080fd5b604080518581529051600160a060020a0387169133917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a33360009081526002602052604080822080548790039055600160a060020a0387168252902080548501905560019250505092915050565b600160a060020a039182166000908152600260209081526040808320939094168252600190920190915220549056fea165627a7a723058204dfa1f78e2e3f2e9b2f5203ffa41cc894d1a6f7fc774d2b71d08311e462493630029a165627a7a72305820aa0aa3dc787d479ee67a02e547cd65784ff59b89d181daec0ee59b9724d331d40029";

    public static final String FUNC_COUNTBYOWNER = "countByOwner";

    public static final String FUNC_COUNT = "count";

    public static final String FUNC_SETOWNER = "setOwner";

    public static final String FUNC_BASE = "base";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_GET = "get";

    public static final String FUNC_DRAIN = "drain";

    public static final String FUNC_GETBYOWNER = "getByOwner";

    public static final Event CREATED_EVENT = new Event("Created",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));
    ;

    public static final Event NEWOWNER_EVENT = new Event("NewOwner",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));
    ;

    @Deprecated
    protected BasicCoinManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected BasicCoinManager(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected BasicCoinManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected BasicCoinManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<BigInteger> countByOwner(String _owner) {
        final Function function = new Function(FUNC_COUNTBYOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> count() {
        final Function function = new Function(FUNC_COUNT,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> setOwner(String _new) {
        final Function function = new Function(
                FUNC_SETOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_new)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> base() {
        final Function function = new Function(FUNC_BASE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<Tuple3<String, String, String>> get(BigInteger _index) {
        final Function function = new Function(FUNC_GET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteCall<Tuple3<String, String, String>>(
                new Callable<Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, String>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue());
                    }
                });
    }

    public RemoteCall<TransactionReceipt> drain() {
        final Function function = new Function(
                FUNC_DRAIN, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deploy(BigInteger _totalSupply, String _tla, String _name, String _tokenreg, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPLOY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_totalSupply),
                        new org.web3j.abi.datatypes.Utf8String(_tla),
                        new org.web3j.abi.datatypes.Utf8String(_name),
                        new org.web3j.abi.datatypes.Address(_tokenreg)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<Tuple3<String, String, String>> getByOwner(String _owner, BigInteger _index) {
        final Function function = new Function(FUNC_GETBYOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner),
                        new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteCall<Tuple3<String, String, String>>(
                new Callable<Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, String>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue());
                    }
                });
    }

    public List<CreatedEventResponse> getCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CREATED_EVENT, transactionReceipt);
        ArrayList<CreatedEventResponse> responses = new ArrayList<CreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CreatedEventResponse typedResponse = new CreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.tokenreg = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.coin = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<CreatedEventResponse> createdEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, CreatedEventResponse>() {
            @Override
            public CreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CREATED_EVENT, log);
                CreatedEventResponse typedResponse = new CreatedEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.tokenreg = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.coin = (String) eventValues.getIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<CreatedEventResponse> createdEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CREATED_EVENT));
        return createdEventFlowable(filter);
    }

    public List<NewOwnerEventResponse> getNewOwnerEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(NEWOWNER_EVENT, transactionReceipt);
        ArrayList<NewOwnerEventResponse> responses = new ArrayList<NewOwnerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            NewOwnerEventResponse typedResponse = new NewOwnerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.old = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.current = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<NewOwnerEventResponse> newOwnerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, NewOwnerEventResponse>() {
            @Override
            public NewOwnerEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NEWOWNER_EVENT, log);
                NewOwnerEventResponse typedResponse = new NewOwnerEventResponse();
                typedResponse.log = log;
                typedResponse.old = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.current = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<NewOwnerEventResponse> newOwnerEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NEWOWNER_EVENT));
        return newOwnerEventFlowable(filter);
    }

    @Deprecated
    public static BasicCoinManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static BasicCoinManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new BasicCoinManager(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new BasicCoinManager(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(BasicCoinManager.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(BasicCoinManager.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class CreatedEventResponse {
        public Log log;

        public String owner;

        public String tokenreg;

        public String coin;
    }

    public static class NewOwnerEventResponse {
        public Log log;

        public String old;

        public String current;
    }
}
