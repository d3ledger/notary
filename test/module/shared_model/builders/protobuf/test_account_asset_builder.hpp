/**
 * Copyright Soramitsu Co., Ltd. 2018 All Rights Reserved.
 * http://soramitsu.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "builders/protobuf/common_objects/proto_account_asset_builder.hpp"

#ifndef IROHA_TEST_ACCOUNT_BUILDER_HPP
#define IROHA_TEST_ACCOUNT_BUILDER_HPP

/**
 * Builder alias, to build shared model proto block object avoiding validation
 * and "required fields" check
 */
using TestAccountAssetBuilder = shared_model::proto::AccountAsset;

#endif //IROHA_TEST_ACCOUNT_BUILDER_HPP
