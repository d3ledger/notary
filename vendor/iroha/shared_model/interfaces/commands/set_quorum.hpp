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

#ifndef IROHA_SHARED_MODEL_SET_QUORUM_HPP
#define IROHA_SHARED_MODEL_SET_QUORUM_HPP

#include "interfaces/base/primitive.hpp"
#include "interfaces/common_objects/types.hpp"

#ifndef DISABLE_BACKWARD
#include "model/commands/set_quorum.hpp"
#endif

namespace shared_model {
  namespace interface {
    /**
     * Set quorum of the account
     */
    class SetQuorum : public PRIMITIVE(SetQuorum) {
     public:
      /**
       * @return Id of the account to set quorum
       */
      virtual const types::AccountIdType &accountId() const = 0;
      /**
       * @return value of a new quorum
       */
      virtual const types::QuorumType &newQuorum() const = 0;

      std::string toString() const override {
        return detail::PrettyStringBuilder()
            .init("SetQuorum")
            .append("account_id", accountId())
            .append("quorum", std::to_string(newQuorum()))
            .finalize();
      }

#ifndef DISABLE_BACKWARD
      OldModelType *makeOldModel() const override {
        auto oldModel = new iroha::model::SetQuorum;
        oldModel->account_id = accountId();
        oldModel->new_quorum = newQuorum();
        return oldModel;
      }
#endif

      bool operator==(const ModelType &rhs) const override {
        return accountId() == rhs.accountId()
            and newQuorum() == rhs.newQuorum();
      }
    };
  }  // namespace interface
}  // namespace shared_model

#endif  // IROHA_SHARED_MODEL_SET_QUORUM_HPP
