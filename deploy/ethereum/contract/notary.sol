pragma solidity ^0.4.23;

contract BasicCoin {
    function transfer(address _to, uint256 _value) public returns (bool);
    function approve(address _spender, uint256 _value) public returns (bool);
    function balanceOf(address _who) public constant returns (uint256);
}
    
contract Notary {
    struct Peer {
        bytes32 public_key_;
    }

    address private owner_;
    bytes32[] private peers_;
    
    BasicCoin private bc_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    constructor() public {
        owner_ = msg.sender;
    }

    function() public payable { }

    function verify(bytes32 hash, uint8 v, bytes32 r, bytes32 s) public returns(address) {
        // Note: this only verifies that signer is correct.
        // You'll also need to verify that the hash of the data
        // is also correct.
        emit bytes_event(hash);
        bytes32 simple_hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", hash));
        emit bytes_event(simple_hash);

        address res = ecrecover(simple_hash, v, r, s);
        emit address_event(res);
        return res;
    }
    
    function addPeer(bytes32 public_key) public {
        if (msg.sender == owner_) {
            peers_.push(public_key);
        }
    }
    
    function getPeers() constant public returns (bytes32[]) {
        return peers_;
    }

    function receiveTokens(/*string metadata*/) public pure {
        // TODO: call method from ERC-20 smart contract
        // or don't call
        // depends on what we will decide to do - registration, double tx or whatever
    }

    function withdrawTokens(address coin_address, uint256 amount, address to, bytes32 hash, uint8 v, bytes32 r, bytes32 s) public {
        // TODO: sigs - at least 2f+1 from 3f+1
        // len(peers)==12 -> 3f+1=12; f=3; 2f+1=7 -> at least 7 sigs we need
        // type (address of token for now)
        // nonce (think)
        address res = verify(hash, v, r, s);
        // for now only requests signed by owner can be satisfied
        assert(res == owner_);
        bc_ = BasicCoin(coin_address);
        emit address_event(coin_address);
        emit address_event(this);
        emit number_event(bc_.balanceOf(this));
        assert(bc_.balanceOf(this) >= amount);
        bc_.transfer(to, amount);
    }
}