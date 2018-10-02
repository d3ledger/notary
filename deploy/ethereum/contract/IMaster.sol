pragma solidity 0.4.25;

/**
 * Subset of master contract interface
 */
contract IMaster {
    function withdraw(
        address tokenAddress,
        uint256 amount,
        address to,
        bytes32 txHash,
        uint8[] v,
        bytes32[] r,
        bytes32[] s,
        address from
    )
    public;
    function checkTokenAddress(address token) public view returns (bool);
}