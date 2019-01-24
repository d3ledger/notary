pragma solidity 0.4.25;

import "./ERC20.sol";
import "./ERC20Detailed.sol";

contract AbstractToken is ERC20, ERC20Detailed {

    uint256 public constant INITIAL_SUPPLY = 0;

    constructor(string tokenName, string tokenSymbol, uint8 decimals) public ERC20Detailed(tokenName, tokenSymbol, decimals) {
    }

    //TODO: Implement minting function, with secure timestamp checking or other secure checking approach
    //TODO: Implement burn function, with secure checking approach

}
