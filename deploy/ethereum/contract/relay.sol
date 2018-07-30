pragma solidity 0.4.24;

/**
 * Subset of ERC-20 token interface
 */
contract ICoin {
    function transfer(address to, uint256 value) public returns (bool);
    function balanceOf(address who) public view returns (uint256);
}

/**
 * Subset of master contract interface
 */
contract IMaster {
    function withdraw(address token_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public;
    function tokens() public view returns (address[]);
    function checkTokenAddress(address token) public view returns (bool);
}

/**
 * Provides functionality of relay contract
 */
contract Relay {
    address private master_address_;
    IMaster private master_instance_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    /**
     * Relay constructor
     * @param master address of master contract
     */
    constructor(address master) public {
        master_address_ = master;
        master_instance_ = IMaster(master_address_);
    }

    /**
     * A special function-like stub to allow ether accepting
     */
    function() external payable {
        require(msg.data.length == 0);
        emit address_event(msg.sender);
    }

    /**
     * Sends ether and all tokens from this contract to master
     * @param token_address address of sending token (0 for Ether)
     */
    function sendToMaster(address token_address) public {
        // trusted call
        require(master_instance_.checkTokenAddress(token_address));
        if (token_address == 0) {
            // trusted transfer
            master_address_.transfer(address(this).balance);
        } else {
            ICoin ic = ICoin(token_address);
            // untrusted call in general but coin addresses are received from trusted master contract
            // which contains and manages whitelist of them
            ic.transfer(master_address_, ic.balanceOf(address(this)));
        }
    }

    /**
     * Withdraws specified amount of ether or one of ERC-20 tokens to provided address
     * @param token_address address of token to withdraw (0 for ether)
     * @param amount amount of tokens or ether to withdraw
     * @param to target account address
     * @param tx_hash hash of transaction from Iroha
     * @param v array of signatures of tx_hash (v-component)
     * @param r array of signatures of tx_hash (r-component)
     * @param s array of signatures of tx_hash (s-component)
     */
    function withdraw(address token_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        emit address_event(master_address_);
        // trusted call
        master_instance_.withdraw(token_address, amount, to, tx_hash, v, r, s);
    }
}
