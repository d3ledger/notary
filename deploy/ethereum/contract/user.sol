pragma solidity ^0.4.23;

contract User {
    address private owner_;
    address private master_;

    constructor(address master) public {
        owner_ = msg.sender;
        master_ = master;
    }

    // can accept ether
    function() public payable { }

    function sendAllToMaster() private {
        master_.transfer(address(this).balance);
        // loop by token addresses and transfer all of them to master address
    }

    function withdrawEther() public {
        // send everything to master account
    }

    function withdrawTokens() public {
        // send everything to master account
    }
}
