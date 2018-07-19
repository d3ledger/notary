/**
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include "interfaces/iroha_internal/transaction_sequence.hpp"

#include "interfaces/iroha_internal/transaction_batch.hpp"
#include "validators/field_validator.hpp"
#include "validators/transaction_validator.hpp"
#include "validators/transactions_collection/batch_order_validator.hpp"

namespace shared_model {
  namespace interface {

    template <typename TransactionValidator, typename OrderValidator>
    iroha::expected::Result<TransactionSequence, std::string>
    TransactionSequence::createTransactionSequence(
        const types::SharedTxsCollectionType &transactions,
        const validation::TransactionsCollectionValidator<TransactionValidator,
                                                          OrderValidator>
            &validator) {
      std::unordered_map<interface::types::HashType,
                         std::vector<std::shared_ptr<Transaction>>,
                         interface::types::HashType::Hasher>
          extracted_batches;
      std::vector<TransactionBatch> batches;

      auto transaction_validator = validator.getTransactionValidator();

      auto insert_batch =
          [&batches](const iroha::expected::Value<TransactionBatch> &value) {
            batches.push_back(value.value);
          };

      validation::Answer result;
      for (const auto &tx : transactions) {
        if (auto meta = tx->batchMeta()) {
          auto hashes = meta.get()->transactionHashes();
          auto batch_hash = TransactionBatch::calculateReducedBatchHash(hashes);
          extracted_batches[batch_hash].push_back(tx);
        } else {
          TransactionBatch::createTransactionBatch(tx, transaction_validator)
              .match(insert_batch, [&tx, &result](const auto &err) {
                result.addReason(std::make_pair(
                    std::string("Error in transaction with reduced hash: ")
                        + tx->reducedHash().hex(),
                    std::vector<std::string>{err.error}));
              });
        }
      }

      for (const auto &it : extracted_batches) {
        TransactionBatch::createTransactionBatch(it.second, validator)
            .match(insert_batch, [&it, &result](const auto &err) {
              result.addReason(std::make_pair(
                  it.first.toString(), std::vector<std::string>{err.error}));
            });
      }

      if (result.hasErrors()) {
        return iroha::expected::makeError(result.reason());
      }

      return iroha::expected::makeValue(TransactionSequence(batches));
    }

    template iroha::expected::Result<TransactionSequence, std::string>
    TransactionSequence::createTransactionSequence(
        const types::SharedTxsCollectionType &transactions,
        const validation::TransactionsCollectionValidator<
            validation::TransactionValidator<
                validation::FieldValidator,
                validation::CommandValidatorVisitor<
                    validation::FieldValidator>>,
            validation::AnyOrderValidator> &validator);

    template iroha::expected::Result<TransactionSequence, std::string>
    TransactionSequence::createTransactionSequence(
        const types::SharedTxsCollectionType &transactions,
        const validation::TransactionsCollectionValidator<
            validation::TransactionValidator<
                validation::FieldValidator,
                validation::CommandValidatorVisitor<
                    validation::FieldValidator>>,
            validation::BatchOrderValidator> &validator);

    const types::SharedTxsCollectionType &TransactionSequence::transactions()
        const {
      if (not transactions_) {
        types::SharedTxsCollectionType result;
        auto transactions_amount = 0u;
        for (const auto &batch : batches_) {
          transactions_amount += batch.transactions().size();
        }
        result.reserve(transactions_amount);
        for (const auto &batch : batches_) {
          auto &transactions = batch.transactions();
          std::copy(transactions.begin(),
                    transactions.end(),
                    std::back_inserter(result));
        }
        transactions_.emplace(std::move(result));
      }
      return transactions_.value();
    }

    const types::BatchesCollectionType &TransactionSequence::batches() const {
      return batches_;
    }

    TransactionSequence::TransactionSequence(
        const types::BatchesCollectionType &batches)
        : batches_(batches) {}

  }  // namespace interface
}  // namespace shared_model
