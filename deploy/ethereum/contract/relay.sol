pragma solidity ^0.4.23;

/**
 * Subset of ERC-20 token interface
 */
contract ICoin {
    function transfer(address to, uint256 value) public returns (bool);
    function balanceOf(address who) public constant returns (uint256);
}

/**
 * Subset of master contract interface
 */
contract IMaster {
    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public;
}

/**
 * Provides functionality of relay contract
 */
contract Relay {
    address private master_;
    address[] private tokens_;
    IMaster private in_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    /**
     * Relay constructor
     * @param master address of master contract
     * @param tokens whitelist of supported ERC-20 tokens
     */
    constructor(address master, address[] tokens) public {
        master_ = master;
        for (uint i = 0; i < tokens.length; ++i) {
            tokens_.push(tokens[i]);
        }
        in_ = IMaster(master_);
    }

    /**
     * A special function-like stub to allow ether accepting
     */
    function() external payable { }

    /**
     * Sends ether and all tokens from this contract to master
     */
    function sendAllToMaster() public {
        master_.transfer(address(this).balance);
        // loop through all token addresses and transfer all tokens to master address
        for (uint i = 0; i < tokens_.length; ++i) {
            ICoin ic = ICoin(tokens_[i]);
            ic.transfer(master_, ic.balanceOf(address(this)));
        }
    }

    /**
     * Checks is given token inside a whitelist or not
     * @param token address of token to check
     * @return true if token inside whitelist or false otherwise
     */
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

    /**
     * Withdraws specified amount of ether or one of ERC-20 tokens to provided address
     * @param coin_address address of token to withdraw (0 for ether)
     * @param amount amount of tokens or ether to withdraw
     * @param to target account address
     * @param tx_hash hash of transaction from Iroha
     * @param v array of signatures of tx_hash (v-component)
     * @param r array of signatures of tx_hash (r-component)
     * @param s array of signatures of tx_hash (s-component)
     */
    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        // TODO: remove if statement
        if (coin_address != 0) {
            assert(checkTokenAddress(coin_address));
            emit address_event(coin_address);
        }
        emit address_event(master_);
        in_.withdraw(coin_address, amount, to, tx_hash, v, r, s);
    }
}
