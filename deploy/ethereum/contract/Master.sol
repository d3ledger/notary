pragma solidity ^0.5.0;

import "./IRelayRegistry.sol";
import "./IERC20.sol";

/**
 * Provides functionality of master contract
 */
contract Master {
    address private owner;
    mapping(address => bool) private peers;
    uint public peersCount;
    mapping(bytes32 => bool) private used;
    mapping(address => bool) private uniqueAddresses;

    address private relayRegistryAddress;
    IRelayRegistry private relayRegistryInstance;

    address[] private tokens;

    // TODO: For development purpose only, https://soramitsu.atlassian.net/browse/D3-418
    bool public isLockAddPeer = false;

    /**
     * Emit event when master contract does not have enough assets to proceed withdraw
     */
    event InsufficientFundsForWithdrawal(address asset, address recipient);

    /**
     * Constructor. Sets contract owner to contract creator.
     */
    constructor(address relayRegistry) public {
        owner = msg.sender;
        relayRegistryAddress = relayRegistry;
        relayRegistryInstance = IRelayRegistry(relayRegistryAddress);
    }

    /**
     * @dev Throws if called by any account other than the owner.
     */
    modifier onlyOwner() {
        require(isOwner());
        _;
    }

    /**
     * @return true if `msg.sender` is the owner of the contract.
     */
    function isOwner() public view returns(bool) {
        return msg.sender == owner;
    }

    /**
     * A special function-like stub to allow ether accepting
     */
    function() external payable {
        require(msg.data.length == 0);
    }

    /**
     * Return array of listed tokens
     * @return array of tokens
     */
    function getTokens() public view returns (address[] memory) {
        return tokens;
    }


    /**
     * Adds new peers to list of signature verifiers. Can be called only by contract owner.
     * @param newAddresses addresses of new peers
     */
    function addPeers(address[] memory newAddresses) public onlyOwner returns (uint){
        for (uint i = 0; i < newAddresses.length; i++) {
            addPeer(newAddresses[i]);
        }
        return peersCount;
    }

    /**
     * Adds new peer to list of signature verifiers. Can be called only by contract owner.
     * @param newAddress address of new peer
     */
    function addPeer(address newAddress) public onlyOwner returns(uint){
        // TODO: For development purpose only, https://soramitsu.atlassian.net/browse/D3-418
        require(!isLockAddPeer);
        require(peers[newAddress] == false);
        peers[newAddress] = true;
        ++peersCount;
        return peersCount;
    }

    /**
     * Disable adding the new peers
     */
    function disableAddingNewPeers() public onlyOwner {
        isLockAddPeer = true;
    }

    /**
     * Adds new token to whitelist. Token should not been already added.
     * @param newToken token to add
     */
    function addToken(address newToken) public onlyOwner {
        uint i;
        for (i = 0; i < tokens.length; ++i) {
            require(tokens[i] != newToken);
        }
        tokens.push(newToken);
    }

    /**
     * Checks is given token inside a whitelist or not
     * @param tokenAddress address of token to check
     * @return true if token inside whitelist or false otherwise
     */
    function checkTokenAddress(address tokenAddress) public view returns (bool) {
        // 0 means ether which is definitely in whitelist
        if (tokenAddress == address (0)) {
            return true;
        }
        bool token_found = false;
        for (uint i = 0; i < tokens.length; ++i) {
            if (tokens[i] == tokenAddress) {
                token_found = true;
                break;
            }
        }
        return token_found;
    }

    /**
     * Withdraws specified amount of ether or one of ERC-20 tokens to provided address
     * @param tokenAddress address of token to withdraw (0 for ether)
     * @param amount amount of tokens or ether to withdraw
     * @param to target account address
     * @param txHash hash of transaction from Iroha
     * @param v array of signatures of tx_hash (v-component)
     * @param r array of signatures of tx_hash (r-component)
     * @param s array of signatures of tx_hash (s-component)
     * @param from relay contract address
     */
    function withdraw(
        address tokenAddress,
        uint256 amount,
        address payable to,
        bytes32 txHash,
        uint8[] memory v,
        bytes32[] memory r,
        bytes32[] memory s,
        address from
    )
    public
    {
        require(isLockAddPeer);
        require(checkTokenAddress(tokenAddress));
        require(relayRegistryInstance.isWhiteListed(from, to));
        // TODO luckychess 26.06.2018 D3-101 improve require checks (copy-paste) (use modifiers)
        require(used[txHash] == false);
        require(peersCount >= 1);
        require(v.length == r.length);
        require(r.length == s.length);

        // sigs - at least 2f+1 from 3f+1
        // e. g. len(peers)==12 -> 3f+1=12; f=3; 12-3=9 -> at least 9 sigs we need
        // if we've got more sigs than we need all will be validated
        uint f = (peersCount - 1) / 3;
        uint needSigs = peersCount - f;
        require(s.length >= needSigs);

        address[] memory recoveredAddresses = new address[](s.length);
        for (uint i = 0; i < s.length; ++i) {
            recoveredAddresses[i] = recoverAddress(
                keccak256(abi.encodePacked(tokenAddress, amount, to, txHash, from)),
                v[i],
                r[i],
                s[i]
            );
            // recovered address should be in peers_
            require(peers[recoveredAddresses[i]] == true);
        }
        require(checkForUniqueness(recoveredAddresses));

        if (tokenAddress == address (0)) {
            if (address(this).balance < amount) {
                emit InsufficientFundsForWithdrawal(tokenAddress, to);
            } else {
                used[txHash] = true;
                // untrusted transfer, relies on provided cryptographic proof
                to.transfer(amount);
            }
        } else {
            IERC20 coin = IERC20(tokenAddress);
            if (coin.balanceOf(address (this)) < amount) {
                emit InsufficientFundsForWithdrawal(tokenAddress, to);
            } else {
                used[txHash] = true;
                // untrusted call, relies on provided cryptographic proof
                coin.transfer(to, amount);
            }
        }
    }

    /**
     * Checks given addresses for duplicates
     * @param addresses addresses array to check
     * @return true if all given addresses are unique or false otherwise
     */
    function checkForUniqueness(address[] memory addresses) private returns (bool) {
        uint i;
        bool isUnique = true;
        for (i = 0; i < addresses.length; ++i) {
            if (uniqueAddresses[addresses[i]] == true) {
                isUnique = false;
                break;
            }
            uniqueAddresses[addresses[i]] = true;
        }

        // restore state for future usages
        for (i = 0; i < addresses.length; ++i) {
            uniqueAddresses[addresses[i]] = false;
        }

        return isUnique;
    }

    /**
     * Recovers address from a given single signature
     * @param hash unsigned data
     * @param v v-component of signature from hash
     * @param r r-component of signature from hash
     * @param s s-component of signature from hash
     * @return address recovered from signature
     */
    function recoverAddress(bytes32 hash, uint8 v, bytes32 r, bytes32 s) private pure returns (address) {
        bytes32 simple_hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", hash));
        address res = ecrecover(simple_hash, v, r, s);
        return res;
    }
}
