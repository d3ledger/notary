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

#include "generator/generator.hpp"

namespace generator {

  int64_t random_number(int64_t min, int64_t max) {
    uint32_t SEED_ = 1337;
    return min + (rand_r(&SEED_) % (max - min));
  }

  std::string randomString(size_t len) {
    std::string str(len, 0);
    std::generate_n(
        str.begin(), len, []() { return 'a' + std::rand() % ('z' - 'a' + 1); });
    return str;
  }

}  // namespace generator
