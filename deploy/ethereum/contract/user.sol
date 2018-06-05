pragma solidity ^0.4.23;

contract ICoin {
    function transfer(address to, uint256 value) public returns (bool);
    function balanceOf(address who) public constant returns (uint256);
}

contract INotary {
    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public;
}

contract User {
    address private master_;
    address[] private tokens_;
    INotary private in_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    constructor(address master, address[] tokens) public {
        master_ = master;
        for (uint i = 0; i < tokens.length; ++i) {
            tokens_.push(tokens[i]);
        }
        in_ = INotary(master_);
    }

    // can accept ether
    function() public payable { }

    function sendAllToMaster() private {
        master_.transfer(address(this).balance);
        // loop through all token addresses and transfer all tokens to master address
        for (uint i = 0; i < tokens_.length; ++i) {
            ICoin ic = ICoin(tokens_[i]);
            ic.transfer(master_, ic.balanceOf(address(this)));
        }
    }

    function checkTokenAddress(address token) private constant returns (bool) {
        bool token_found = false;
        for (uint i = 0; i < tokens_.length; ++i) {
            if (tokens_[i] == token) {
                token_found = true;
                break;
            }
        }
        return token_found;
    }

    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        if (coin_address != 0) {
            assert(checkTokenAddress(coin_address));
            emit address_event(coin_address);
        }
        sendAllToMaster();
        emit address_event(master_);
        in_.withdraw(coin_address, amount, to, tx_hash, v, r, s);
    }
}
