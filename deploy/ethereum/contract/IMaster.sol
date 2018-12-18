pragma solidity ^0.5.0;

/**
 * Subset of master contract interface
 */
contract IMaster {
    function withdraw(
        address tokenAddress,
        uint256 amount,
        address to,
        bytes32 txHash,
        uint8[] memory v,
        bytes32[] memory r,
        bytes32[] memory s,
        address from
    )
    public;
    function checkTokenAddress(address token) public view returns (bool);
}
