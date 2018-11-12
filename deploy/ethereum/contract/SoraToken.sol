pragma solidity 0.4.25;

import "./ERC20.sol";
import "./ERC20Detailed.sol";

contract SoraToken is ERC20, ERC20Detailed {

    uint256 public constant INITIAL_SUPPLY = 1618033988 * (10 ** uint256(decimals()));

    /**
     * @dev Constructor that gives msg.sender all of existing tokens.
     */
    constructor() public ERC20Detailed("Sora", "XOR", 18) {
        _mint(msg.sender, INITIAL_SUPPLY);
    }

    //TODO: Implement minting function, with secure timestamp checking or other secure checking approach
    //TODO: Implement burn function, with secure checking approach

}