pragma solidity 0.4.24;

/**
 * Contract for revert cases testing
 */
contract Failer {
    /**
     * A special function-like stub to allow ether accepting. Always fails.
     */
    function() external payable {
        revert("eth transfer revert");
    }
}
