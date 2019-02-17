pragma solidity 0.5.4;

contract InternalTxProducer {
    uint public balance;

    function() payable external {
        balance += msg.value;
    }

    function sendFunds(address payable receiver) public {
        balance -= address(this).balance;
        address(receiver).transfer(address(this).balance);
    }
}