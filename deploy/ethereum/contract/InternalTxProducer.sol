pragma solidity ^0.4.24;

contract InternalTxProducer {
    uint public balance;

    function () payable public {
        balance += msg.value;
    }

    function sendFunds(address to) public {
        balance -= address(this).balance;
        to.transfer(address(this).balance);
    }
}