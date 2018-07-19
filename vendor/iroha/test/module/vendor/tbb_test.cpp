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

#include <gtest/gtest.h>
#include <tbb/concurrent_queue.h>
#include <iostream>

TEST(TBBTest, ConcurrentQueueUsage) {
  tbb::concurrent_queue<int> queue;
  for (int i = 0; i < 10; ++i) queue.push(i);
  typedef tbb::concurrent_queue<int>::iterator iter;
  for (iter i(queue.unsafe_begin()); i != queue.unsafe_end(); ++i)
    std::cout << *i << " ";
  std::cout << std::endl;
}
