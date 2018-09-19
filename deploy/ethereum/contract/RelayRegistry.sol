pragma solidity 0.4.25;
import "./IRelayRegistry.sol";

/**
 * @title Relay registry store data about white list and provide interface for master
 */
contract RelayRegistry is IRelayRegistry {
    address private owner_;

    mapping(address => address[]) private _relayWhiteList;
    mapping(address => bool) private _whiteList;

    constructor () public {
        owner_ = msg.sender;
    }

    /**
     * Store relay address and appropriate whitelist of addresses
     * @param relay contract address
     * @param newWhiteList white list
     * @return true if data was stored
     */
    function addNewRelayAddress(address relay, address[] newWhiteList) external returns (bool) {
        require(msg.sender == owner_);
        require(newWhiteList.length <= 100);
        require(relay != address(0));
        require(_relayWhiteList[relay].length == 0);

        for (uint i = 0; i < newWhiteList.length; i++) {
            require(newWhiteList[i] != (0x0));
            require(_whiteList[newWhiteList[i]] == false);
            _whiteList[newWhiteList[i]] = true;
            emit newWhiteListed(newWhiteList[i]);
        }

        _relayWhiteList[relay] = newWhiteList;

        emit AddNewRelay(relay, newWhiteList);
        return true;
    }

    /**
     * Check if some address is in the whitelist
     * @param relay contract address
     * @param who address in whitelist
     * @return true if address in the whitelist
     */
    function isWhiteListed(address relay, address who) external view returns (bool) {
        if (_relayWhiteList[relay].length == 0) {
            return true;
        }
        if (_relayWhiteList[relay].length > 0) {
            return _whiteList[who];
        }
        return false;
    }

    /**
     * Get entire whitelist by relay address
     * @param relay contract address
     * @return array of the whitelist
     */
    function getWhiteListByRelay(address relay) external view returns (address[]) {
        require(relay != address(0));
        require(_relayWhiteList[relay].length != 0);
        return _relayWhiteList[relay];
    }
}