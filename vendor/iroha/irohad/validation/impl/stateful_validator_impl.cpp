/**
 * Copyright Soramitsu Co., Ltd. 2017 All Rights Reserved.
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

#include "validation/impl/stateful_validator_impl.hpp"

#include <boost/range/adaptor/filtered.hpp>
#include <boost/range/adaptor/transformed.hpp>
#include "builders/protobuf/proposal.hpp"
#include "validation/utils.hpp"

namespace iroha {
  namespace validation {

    StatefulValidatorImpl::StatefulValidatorImpl() {
      log_ = logger::log("SFV");
    }

    std::shared_ptr<shared_model::interface::Proposal>
    StatefulValidatorImpl::validate(
        const shared_model::interface::Proposal &proposal,
        ametsuchi::TemporaryWsv &temporaryWsv) {
      log_->info("transactions in proposal: {}",
                 proposal.transactions().size());
      auto checking_transaction = [](const auto &tx, auto &queries) {
        return bool(queries.getAccount(tx.creatorAccountId()) |
                    [&](const auto &account) {
                      // Check if tx creator has account and has quorum to
                      // execute transaction
                      return boost::size(tx.signatures()) >= account->quorum()
                          ? queries.getSignatories(tx.creatorAccountId())
                          : boost::none;
                    }
                    |
                    [&](const auto &signatories) {
                      // Check if signatures in transaction are account
                      // signatory
                      return signaturesSubset(tx.signatures(), signatories)
                          ? boost::make_optional(signatories)
                          : boost::none;
                    });
      };

      // Filter only valid transactions
      auto filter = [&temporaryWsv, checking_transaction](auto &tx) {
        return temporaryWsv.apply(tx, checking_transaction);
      };

      // TODO: kamilsa IR-1010 20.02.2018 rework validation logic, so that this
      // cast is not needed and stateful validator does not know about the
      // transport
      auto valid_proto_txs =
          proposal.transactions() | boost::adaptors::filtered(filter)
          | boost::adaptors::transformed([](auto &tx) {
              return static_cast<const shared_model::proto::Transaction &>(tx);
            });
      auto validated_proposal = shared_model::proto::ProposalBuilder()
                                    .createdTime(proposal.createdTime())
                                    .height(proposal.height())
                                    .transactions(valid_proto_txs)
                                    .createdTime(proposal.createdTime())
                                    .build();

      log_->info("transactions in verified proposal: {}",
                 validated_proposal.transactions().size());
      return std::make_shared<decltype(validated_proposal)>(
          validated_proposal.getTransport());
    }
  }  // namespace validation
}  // namespace iroha
