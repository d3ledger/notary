/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package d3.report.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration that holds all currently used test folders
 */
public enum TestFolder {
    BTC_TESTS("notary-btc-integration-test"),
    ETH_TESTS("notary-eth-integration-test"),
    IROHA_TESTS("notary-iroha-integration-test"),
    SORA_TESTS("notary-sora-integration-test"),
    NOTIFICATIONS_TEST("notifications-integration-test");
    private final String path;

    TestFolder(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Test folder path cannot be null");
        }
        this.path = path;
    }

    //Returns list full of test folders
    public static List<String> getAllFolders() {
        List<String> folders = new ArrayList<>();
        for (TestFolder folder : TestFolder.values()) {
            folders.add(folder.path);
        }
        return folders;
    }
}
