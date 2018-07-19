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

#ifndef IROHA_IROHA_INSTANCE_HPP
#define IROHA_IROHA_INSTANCE_HPP

#include <boost/optional.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <chrono>
#include <memory>
#include <string>
#include "ametsuchi/impl/postgres_options.hpp"

namespace shared_model {
  namespace interface {
    class Block;
  }  // namespace interface
  namespace crypto {
    class Keypair;
  }  // namespace crypto
}  // namespace shared_model

namespace integration_framework {
  class TestIrohad;

  class IrohaInstance {
   public:
    /**
     * @param mst_support enables multisignature tx support
     * @param block_store_path
     * @param dbname is a name of postgres database
     */
    IrohaInstance(bool mst_support,
                  const std::string &block_store_path,
                  const boost::optional<std::string> &dbname = boost::none);

    void makeGenesis(const shared_model::interface::Block &block);

    void rawInsertBlock(const shared_model::interface::Block &block);
    void initPipeline(const shared_model::crypto::Keypair &key_pair,
                      size_t max_proposal_size = 10);

    void run();

    std::shared_ptr<TestIrohad> &getIrohaInstance();

    std::string getPostgreCredsOrDefault(
        const boost::optional<std::string> &dbname);

    std::shared_ptr<TestIrohad> instance_;

    // config area
    const std::string block_store_dir_;
    const std::string pg_conn_;
    const size_t torii_port_;
    const size_t internal_port_;
    const std::chrono::milliseconds proposal_delay_;
    const std::chrono::milliseconds vote_delay_;
    const std::chrono::milliseconds load_delay_;
    const bool is_mst_supported_;
  };
}  // namespace integration_framework
#endif  // IROHA_IROHA_INSTANCE_HPP
