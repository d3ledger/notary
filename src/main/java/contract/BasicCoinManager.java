package contract;

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
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.4.0.
 */
public class BasicCoinManager extends Contract {
    private static final String BINARY = "608060405260008054600160a060020a0319163317905534801561002257600080fd5b50610e95806100326000396000f30060806040526004361061007f5763ffffffff60e060020a600035041663061ea8cc811461008457806306661abd146100b757806313af4035146100cc5780635001f3b5146100ef5780638da5cb5b146101045780639507d39a146101355780639890220b14610178578063acfdfd1c1461018d578063c00ca3831461023b575b600080fd5b34801561009057600080fd5b506100a5600160a060020a036004351661025f565b60408051918252519081900360200190f35b3480156100c357600080fd5b506100a561027a565b3480156100d857600080fd5b506100ed600160a060020a0360043516610281565b005b3480156100fb57600080fd5b506100a5610300565b34801561011057600080fd5b50610119610307565b60408051600160a060020a039092168252519081900360200190f35b34801561014157600080fd5b5061014d600435610316565b60408051600160a060020a039485168152928416602084015292168183015290519081900360600190f35b34801561018457600080fd5b506100ed610362565b60408051602060046024803582810135601f810185900485028601850190965285855261022795833595369560449491939091019190819084018382808284375050604080516020601f89358b018035918201839004830284018301909452808352979a99988101979196509182019450925082915084018382808284375094975050509235600160a060020a031693506103a292505050565b604080519115158252519081900360200190f35b34801561024757600080fd5b5061014d600160a060020a036004351660243561075e565b600160a060020a031660009081526002602052604090205490565b6001545b90565b600054600160a060020a0316331461029857600080fd5b60008054604051600160a060020a03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a36000805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b620f424081565b600054600160a060020a031681565b60008060008060018581548110151561032b57fe5b6000918252602090912060039091020180546001820154600290920154600160a060020a0391821698928216975016945092505050565b600054600160a060020a0316331461037957600080fd5b6040513390303180156108fc02916000818181858888f1935050505015156103a057600080fd5b565b60008181808088336103b26107a8565b918252600160a060020a03166020820152604080519182900301906000f0801580156103e2573d6000803e3d6000fd5b5092506103ee3361025f565b915083600160a060020a031663ddca3f436040518163ffffffff1660e060020a028152600401602060405180830381600087803b15801561042e57600080fd5b505af1158015610442573d6000803e3d6000fd5b505050506040513d602081101561045857600080fd5b5051336000908152600260205260409020909150600183019061047b90826107b8565b5060015433600090815260026020526040902080548490811061049a57fe5b9060005260206000200181905550600160606040519081016040528085600160a060020a0316815260200133600160a060020a0316815260200186600160a060020a03168152509080600181540180825580915050906001820390600052602060002090600302016000909192909190915060008201518160000160006101000a815481600160a060020a030219169083600160a060020a0316021790555060208201518160010160006101000a815481600160a060020a030219169083600160a060020a0316021790555060408201518160020160006101000a815481600160a060020a030219169083600160a060020a0316021790555050505083600160a060020a0316637b1a547c82858b620f42408c336040518763ffffffff1660e060020a0281526004018086600160a060020a0316600160a060020a03168152602001806020018581526020018060200184600160a060020a0316600160a060020a03168152602001838103835287818151815260200191508051906020019080838360005b8381101561063757818101518382015260200161061f565b50505050905090810190601f1680156106645780820380516001836020036101000a031916815260200191505b50838103825285518152855160209182019187019080838360005b8381101561069757818101518382015260200161067f565b50505050905090810190601f1680156106c45780820380516001836020036101000a031916815260200191505b509750505050505050506020604051808303818588803b1580156106e757600080fd5b505af11580156106fb573d6000803e3d6000fd5b50505050506040513d602081101561071257600080fd5b5050604051600160a060020a03808516919086169033907f454b0172f64812df0cd504c2bd7df7aab8ff328a54d946b4bd0fa7c527adf9cc90600090a450600198975050505050505050565b600160a060020a038216600090815260026020526040812080548291829161079b91908690811061078b57fe5b9060005260206000200154610316565b9250925092509250925092565b60405161066a8061080083390190565b8154818355818111156107dc576000838152602090206107dc9181019083016107e1565b505050565b61027e91905b808211156107fb57600081556001016107e7565b5090560060806040526000805460a060020a60ff0219600160a060020a0319909116331716905534801561002e57600080fd5b5060405160408061066a833981016040528051602090910151600034111561005557600080fd5b8180151561006257600080fd5b50600182905560008054600160a060020a03909216600160a060020a031990921682178155908152600260205260409020556105c7806100a36000396000f3006080604052600436106100a35763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663095ea7b381146100b557806313af4035146100ed57806318160ddd1461011057806323b872dd146101375780635001f3b51461016157806350f9b6cd1461017657806370a082311461018b5780638da5cb5b146101ac578063a9059cbb146101dd578063dd62ed3e14610201575b3480156100af57600080fd5b50600080fd5b3480156100c157600080fd5b506100d9600160a060020a0360043516602435610228565b604080519115158252519081900360200190f35b3480156100f957600080fd5b5061010e600160a060020a03600435166102ad565b005b34801561011c57600080fd5b5061012561032c565b60408051918252519081900360200190f35b34801561014357600080fd5b506100d9600160a060020a0360043581169060243516604435610332565b34801561016d57600080fd5b50610125610476565b34801561018257600080fd5b506100d961047d565b34801561019757600080fd5b50610125600160a060020a036004351661049e565b3480156101b857600080fd5b506101c16104b9565b60408051600160a060020a039092168252519081900360200190f35b3480156101e957600080fd5b506100d9600160a060020a03600435166024356104c8565b34801561020d57600080fd5b50610125600160a060020a036004358116906024351661056c565b60008034111561023757600080fd5b604080518381529051600160a060020a0385169133917f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9259181900360200190a350336000908152600260209081526040808320600160a060020a0386168452600190810190925290912080548301905592915050565b600054600160a060020a031633146102c457600080fd5b60008054604051600160a060020a03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a36000805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b60015481565b60008034111561034157600080fd5b600160a060020a0384166000908152600260205260409020548490839081111561036a57600080fd5b600160a060020a03861660009081526002602090815260408083203380855260019091019092529091205487919086908111156103a657600080fd5b6000805474ff0000000000000000000000000000000000000000191674010000000000000000000000000000000000000000179055604080518881529051600160a060020a03808b1692908c16917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a3600160a060020a03808a1660009081526002602081815260408084203385526001808201845282862080548f900390559390925281548c9003909155928b168252919020805489019055955050505050509392505050565b620f424081565b60005474010000000000000000000000000000000000000000900460ff1681565b600160a060020a031660009081526002602052604090205490565b600054600160a060020a031681565b6000803411156104d757600080fd5b3360008181526002602052604090205483908111156104f557600080fd5b604080518581529051600160a060020a0387169133917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a33360009081526002602052604080822080548790039055600160a060020a0387168252902080548501905560019250505092915050565b600160a060020a03918216600090815260026020908152604080832093909416825260019092019091522054905600a165627a7a723058207da736352c139e12338e75f9801a1b2a070a1ceee76f4ef3263e9b0a32fdf99f0029a165627a7a7230582067f3e83c6035df9a5b572eb7f0bd7f39b2725c1cbd460f9ca4bb59d217e2369a0029";

    public static final String FUNC_COUNTBYOWNER = "countByOwner";

    public static final String FUNC_COUNT = "count";

    public static final String FUNC_SETOWNER = "setOwner";

    public static final String FUNC_BASE = "base";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_GET = "get";

    public static final String FUNC_DRAIN = "drain";

    public static final String FUNC_GETBYOWNER = "getByOwner";

    public static final String FUNC_DEPLOY = "deploy";

    public static final Event CREATED_EVENT = new Event("Created",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }),
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event NEWOWNER_EVENT = new Event("NewOwner",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }),
            Arrays.<TypeReference<?>>asList());
    ;

    protected BasicCoinManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected BasicCoinManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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

    public Observable<CreatedEventResponse> createdEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, CreatedEventResponse>() {
            @Override
            public CreatedEventResponse call(Log log) {
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

    public Observable<CreatedEventResponse> createdEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CREATED_EVENT));
        return createdEventObservable(filter);
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

    public Observable<NewOwnerEventResponse> newOwnerEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, NewOwnerEventResponse>() {
            @Override
            public NewOwnerEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NEWOWNER_EVENT, log);
                NewOwnerEventResponse typedResponse = new NewOwnerEventResponse();
                typedResponse.log = log;
                typedResponse.old = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.current = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<NewOwnerEventResponse> newOwnerEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NEWOWNER_EVENT));
        return newOwnerEventObservable(filter);
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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
