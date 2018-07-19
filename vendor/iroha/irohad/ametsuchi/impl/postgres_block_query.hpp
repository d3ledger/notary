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

#ifndef IROHA_POSTGRES_FLAT_BLOCK_QUERY_HPP
#define IROHA_POSTGRES_FLAT_BLOCK_QUERY_HPP

#include <boost/optional.hpp>

#include "ametsuchi/block_query.hpp"
#include "ametsuchi/impl/flat_file/flat_file.hpp"
#include "logger/logger.hpp"
#include "postgres_wsv_common.hpp"

namespace iroha {
  namespace ametsuchi {

    class FlatFile;

    /**
     * Class which implements BlockQuery with a Postgres backend.
     */
    class PostgresBlockQuery : public BlockQuery {
     public:
      explicit PostgresBlockQuery(soci::session &sql,
                                  KeyValueStorage &file_store);
      explicit PostgresBlockQuery(std::unique_ptr<soci::session> sql_ptr,
                                  KeyValueStorage &file_store);

      rxcpp::observable<wTransaction> getAccountTransactions(
          const shared_model::interface::types::AccountIdType &account_id)
          override;

      rxcpp::observable<wTransaction> getAccountAssetTransactions(
          const shared_model::interface::types::AccountIdType &account_id,
          const shared_model::interface::types::AssetIdType &asset_id) override;

      rxcpp::observable<boost::optional<wTransaction>> getTransactions(
          const std::vector<shared_model::crypto::Hash> &tx_hashes) override;

      boost::optional<wTransaction> getTxByHashSync(
          const shared_model::crypto::Hash &hash) override;

      rxcpp::observable<wBlock> getBlocks(
          shared_model::interface::types::HeightType height,
          uint32_t count) override;

      rxcpp::observable<wBlock> getBlocksFrom(
          shared_model::interface::types::HeightType height) override;

      rxcpp::observable<wBlock> getTopBlocks(uint32_t count) override;

      uint32_t getTopBlockHeight() override;

      bool hasTxWithHash(const shared_model::crypto::Hash &hash) override;

      expected::Result<wBlock, std::string> getTopBlock() override;

     private:
      /**
       * Returns all blocks' ids containing given account id
       * @param account_id
       * @return vector of block ids
       */
      std::vector<shared_model::interface::types::HeightType> getBlockIds(
          const shared_model::interface::types::AccountIdType &account_id);

      /**
       * Returns block id which contains transaction with a given hash
       * @param hash - hash of transaction
       * @return block id or boost::none
       */
      boost::optional<shared_model::interface::types::HeightType> getBlockId(
          const shared_model::crypto::Hash &hash);

      /**
       * creates callback to lrange query to Postgres to supply result to
       * subscriber s
       * @param s
       * @param block_id
       * @return
       */
      std::function<void(std::vector<std::string> &result)> callback(
          const rxcpp::subscriber<wTransaction> &s, uint64_t block_id);

      std::unique_ptr<soci::session> sql_ptr_;
      soci::session &sql_;

      KeyValueStorage &block_store_;
      logger::Logger log_;
    };
  }  // namespace ametsuchi
}  // namespace iroha

#endif  // IROHA_POSTGRES_FLAT_BLOCK_QUERY_HPP
