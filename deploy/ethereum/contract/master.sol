pragma solidity 0.4.24;

/**
 * Subset of ERC-20 token interface
 */
contract ICoin {
    function transfer(address to, uint256 value) public returns (bool);
    function balanceOf(address who) public view returns (uint256);
}

/**
 * Provides functionality of master contract
 */
contract Master {
    address private owner_;
    mapping(address => bool) private peers_;
    uint private peers_count_;
    mapping(bytes32 => bool) private used_;
    mapping(address => bool) private unique_addresses_;

    address[] public tokens;

    event address_event(address input);
    event string_event(string input);
    event bytes_event(bytes32 input);
    event number_event(uint256 input);

    /**
     * Constructor. Sets contract owner to contract creator.
     * @param tokens_list whitelist of supported ERC-20 tokens
     */
    constructor(address[] tokens_list) public {
        owner_ = msg.sender;
        tokens = tokens_list;
    }

    /**
     * A special function-like stub to allow ether accepting
     */
    function() external payable {
        require(msg.data.length == 0);
        emit address_event(msg.sender);
    }

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
        require(msg.sender == owner_);
        require(peers_[new_address] == false);
        peers_[new_address] = true;
        ++peers_count_;
        emit number_event(peers_count_);
        emit address_event(new_address);
    }

    /**
     * Checks given addresses for duplicates
     * @param addresses addresses array to check
     * @return true if all given addresses are unique or false otherwise
     */
    function checkForUniqueness(address[] memory addresses) internal returns(bool) {
        uint i;
        bool isUnique = true;
        for (i = 0; i < addresses.length; ++i) {
            if (unique_addresses_[addresses[i]] == true) {
                isUnique = false;
                break;
            }
            unique_addresses_[addresses[i]] = true;
        }
        
        // restore state for future usages
        for (i = 0; i < addresses.length; ++i) {
            unique_addresses_[addresses[i]] = false;
        }
        
        return isUnique;
    }

    /**
     * Checks is given token inside a whitelist or not
     * @param token_address address of token to check
     * @return true if token inside whitelist or false otherwise
     */
    function checkTokenAddress(address token_address) public view returns (bool) {
        // 0 means ether which is definitely in whitelist
        if (token_address == 0) {
            return true;
        }
        bool token_found = false;
        for (uint i = 0; i < tokens.length; ++i) {
            if (tokens[i] == token_address) {
                token_found = true;
                break;
            }
        }
        return token_found;
    }

    /**
     * Withdraws specified amount of ether or one of ERC-20 tokens to provided address
     * @param token_address address of token to withdraw (0 for ether)
     * @param amount amount of tokens or ether to withdraw
     * @param to target account address
     * @param tx_hash hash of transaction from Iroha
     * @param v array of signatures of tx_hash (v-component)
     * @param r array of signatures of tx_hash (r-component)
     * @param s array of signatures of tx_hash (s-component)
     */
    function withdraw(address token_address, uint256 amount, address to, bytes32 tx_hash, uint8 []v, bytes32 []r, bytes32 []s) public {
        require(checkTokenAddress(token_address));
        // TODO luckychess 26.06.2018 D3-101 improve require checks (copy-paste) (use modifiers)
        require(used_[tx_hash] == false);
        require(peers_count_ >= 1);
        require(v.length == r.length);
        require(r.length == s.length);

        // sigs - at least 2f+1 from 3f+1
        // e. g. len(peers)==12 -> 3f+1=12; f=3; 12-3=9 -> at least 9 sigs we need
        // if we've got more sigs than we need all will be validated
        uint f = (peers_count_ - 1) / 3;
        uint need_sigs = peers_count_ - f;
        require(s.length >= need_sigs);

        used_[tx_hash] = true;

        emit number_event(amount);
        emit number_event(need_sigs);
        emit number_event(s.length);

        address[] memory recovered_addresses = new address[](s.length);
        for (uint i = 0; i < s.length; ++i) {
            recovered_addresses[i] = recoverAddress(keccak256(abi.encodePacked(token_address, amount, to, tx_hash)), v[i], r[i], s[i]);
            // recovered address should be in peers_
            require(peers_[recovered_addresses[i]] == true);
        }
        require(checkForUniqueness(recovered_addresses));

        if (token_address == 0) {
            require(address(this).balance >= amount);
            // untrusted transfer, relies on provided cryptographic proof
            to.transfer(amount);
        } else {
            ICoin coin = ICoin(token_address);
            // untrusted call, relies on token whitelist check
            require(coin.balanceOf(this) >= amount);
            // untrusted call, relies on provided cryptographic proof
            coin.transfer(to, amount);
        }
    }
}
