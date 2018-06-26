pragma solidity ^0.4.23;

contract BasicCoin {
    function transfer(address to, uint256 value) public returns (bool);
    function approve(address spender, uint256 value) public returns (bool);
    function balanceOf(address who) public constant returns (uint256);
}
    
contract Master {
    BasicCoin private bc_;
    address private owner_;
    mapping(address => bool) private peers_;
    uint private peers_count_;
    mapping(bytes32 => bool) private used_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    constructor() public {
        owner_ = msg.sender;
    }

    function() public payable { }

    function verify(bytes32 hash, uint8 v, bytes32 r, bytes32 s) private returns(address) {
        emit bytes_event(hash);
        bytes32 simple_hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", hash));
        emit bytes_event(simple_hash);

        address res = ecrecover(simple_hash, v, r, s);
        emit address_event(res);
        return res;
    }
    
    function addPeer(address new_address) public {
        if (msg.sender == owner_) {
            peers_[new_address] = true;
            ++peers_count_;
            emit number_event(peers_count_);
            emit address_event(new_address);
        }
    }

    function checkForUniqueness(address[] memory addresses) pure internal returns(bool) {
        for (uint i = 0; i < addresses.length; ++i) {
            for (uint j = i+1; j < addresses.length; ++j) {
                if (addresses[i] == addresses[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        // TODO luckychess 26.06.2018 D3-103 fix whitelist checks inconsistency
        // TODO luckychess 26.06.2018 D3-101 improve require checks (copy-paste) (use modifiers)
        require(used_[tx_hash] == false);
        require(peers_count_ >= 1);
        // sigs - at least 2f+1 from 3f+1
        // e. g. len(peers)==12 -> 3f+1=12; f=3; 12-3=9 -> at least 9 sigs we need
        // if we've got more sigs than we need all will be validated
        uint f = (peers_count_ - 1) / 3;
        uint need_sigs = peers_count_ - f;
        emit number_event(need_sigs);

        assert(v.length == r.length);
        assert(r.length == s.length);
        assert(s.length >= need_sigs);
        emit number_event(s.length);

        address[] memory recovered_addresses = new address[](s.length);
        for (uint i = 0; i < s.length; ++i) {
            if (coin_address == 0) {
                recovered_addresses[i] = verify(keccak256(abi.encodePacked(amount, to, tx_hash)), v[i], r[i], s[i]);
            } else {
                recovered_addresses[i] = verify(keccak256(abi.encodePacked(coin_address, amount, to, tx_hash)), v[i], r[i], s[i]);
            }
            // recovered address should be in peers_
            assert(peers_[recovered_addresses[i]] == true);
        }
        assert(checkForUniqueness(recovered_addresses));

        if (coin_address == 0) {
            emit number_event(address(this).balance);
            assert(address(this).balance >= amount);
            to.transfer(amount);
        } else {
            bc_ = BasicCoin(coin_address);
            emit address_event(coin_address);
            emit number_event(bc_.balanceOf(this));
            assert(bc_.balanceOf(this) >= amount);
            bc_.transfer(to, amount);
        }
        used_[tx_hash] = true;
    }
}
