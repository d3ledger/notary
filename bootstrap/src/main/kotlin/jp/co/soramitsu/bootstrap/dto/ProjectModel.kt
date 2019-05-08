/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.dto

data class ProjectEnv(val project: String = "D3", val environment: String = "test")

data class ProjectInfo(
    val project: String = "D3",
    val environments: MutableList<String> = mutableListOf("test")
)

data class Projects(val projects: Collection<ProjectInfo> = listOf()) : Conflictable()
