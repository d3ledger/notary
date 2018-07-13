pragma solidity ^0.4.23;

/**
 * Subset of ERC-20 token interface
 */
contract ICoin {
    function transfer(address to, uint256 value) public returns (bool);
    function approve(address spender, uint256 value) public returns (bool);
    function balanceOf(address who) public constant returns (uint256);
}

/**
 * Provides functionality of master contract
 */
contract Master {
    ICoin private ic_;
    address private owner_;
    mapping(address => bool) private peers_;
    uint private peers_count_;
    mapping(bytes32 => bool) private used_;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    /**
     * Constructor. Sets contract owner to contract creator.
     */
    constructor() public {
        owner_ = msg.sender;
    }

    /**
     * A special function-like stub to allow ether accepting
     */
    function() external payable { }

    /**
     * Recovers address from a given single signature
     * @param hash unsigned data
     * @param v v-component of signature from hash
     * @param r r-component of signature from hash
     * @param s s-component of signature from hash
     * @return address recovered from signature
     */
    function recoverAddress(bytes32 hash, uint8 v, bytes32 r, bytes32 s) private returns(address) {
        emit bytes_event(hash);
        bytes32 simple_hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", hash));
        address res = ecrecover(simple_hash, v, r, s);
        emit address_event(res);
        return res;
    }

    /**
     * Adds new peer to list of signature verifiers. Can be called only by contract owner.
     * @param new_address address of new peer
     */
    function addPeer(address new_address) public {
        if (msg.sender == owner_) {
            require(peers_[new_address] == false);
            peers_[new_address] = true;
            ++peers_count_;
            emit number_event(peers_count_);
            emit address_event(new_address);
        }
    }

    /**
     * Checks given addresses for duplicates
     * @param addresses addresses array to check
     * @return true if all given addresses are unique or false otherwise
     */
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

    /**
     * Withdraws specified amount of ether or one of ERC-20 tokens to provided address
     * @param coin_address address of token to withdraw (0 for ether)
     * @param amount amount of tokens or ether to withdraw
     * @param to target account address
     * @param tx_hash hash of transaction from Iroha
     * @param v array of signatures of tx_hash (v-component)
     * @param r array of signatures of tx_hash (r-component)
     * @param s array of signatures of tx_hash (s-component)
     */
    function withdraw(address coin_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        // TODO luckychess 26.06.2018 D3-103 fix whitelist checks inconsistency
        // TODO luckychess 26.06.2018 D3-101 improve require checks (copy-paste) (use modifiers)
        require(used_[tx_hash] == false);
        emit number_event(amount);
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
            recovered_addresses[i] = recoverAddress(keccak256(abi.encodePacked(coin_address, amount, to, tx_hash)), v[i], r[i], s[i]);
            // recovered address should be in peers_
            assert(peers_[recovered_addresses[i]] == true);
        }
        assert(checkForUniqueness(recovered_addresses));

        if (coin_address == 0) {
            emit number_event(address(this).balance);
            assert(address(this).balance >= amount);
            to.transfer(amount);
            emit number_event(address(this).balance);
        } else {
            ic_ = ICoin(coin_address);
            emit address_event(coin_address);
            emit number_event(ic_.balanceOf(this));
            assert(ic_.balanceOf(this) >= amount);
            ic_.transfer(to, amount);
        }
        used_[tx_hash] = true;
    }
}
