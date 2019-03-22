pragma solidity 0.5.4;

/**
 * Contract that sends Ether with internal transaction for testing purposes.
 */
contract TransferEthereum {

    function transfer(uint256 amount, address payable to) public {
        to.transfer(amount);
    }

}
