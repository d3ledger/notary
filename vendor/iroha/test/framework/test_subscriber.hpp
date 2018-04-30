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

#ifndef IROHA_TEST_SUBSCRIBER_HPP
#define IROHA_TEST_SUBSCRIBER_HPP

#include <functional>
#include <memory>
#include <rxcpp/rx-observable.hpp>
#include <utility>

namespace framework {
  namespace test_subscriber {

    /**
     * Interface of testing verification uses in test subscriber;
     * @tparam T type of observed data
     *
     * For implement own strategies should:
     *  1) inherit from this class
     *  2) implement move constructor and move operator=
     *  3) deny copy constructor and operator=
     *  4) implement on_next_* handlers
     *  5) implement validate() method
     */
    template <typename T>
    class VerificationStrategy {
      template <typename K>
      friend class TestSubscriber;

     public:
      /**
       * Handler that called before providing value to target subscriber
       * @param next - new value
       */
      virtual void on_next_before(T next) {}

      /**
       * Handler that called after calling target subscriber
       * @param next - new value
       */
      virtual void on_next_after(T next) {}

      /**
       * Handler which is called after target subscriber call
       * @param ep - pointer to exception thrown in observable
       */
      virtual void on_error(std::exception_ptr ep) {}

      /**
       * Handler which is called after target subscriber call
       */
      virtual void on_completed() {}

      /**
       * Implement destructor for verify invariant
       */
      virtual ~VerificationStrategy() = default;

     protected:
      /**
       * validation function for invariant
       * @return true if invariant safe, otherwise false
       */
      virtual bool validate() = 0;

      /**
       * Exception reason
       */
      std::string invalidate_reason_ = "empty_reason";
    };

    /**
     * TestSubscriber class provide wrapper for observable
     * @tparam T type of data in wrapped observable
     */
    template <typename T>
    class TestSubscriber {
     public:
      /**
       * Constructor for wrapping observable for checking invariant
       * @param unwrapped_observable - object for wrapping
       * @param strategy - invariant for validation
       */
      TestSubscriber(rxcpp::observable<T> unwrapped_observable,
                     std::unique_ptr<VerificationStrategy<T>> strategy)
          : unwrapped_(unwrapped_observable), strategy_(std::move(strategy)) {}

      /**
       * Method provide subscription
       * for wrapped observable with checking invariant.
       * @param subscriber - business logic subscriber
       */
      TestSubscriber<T> &subscribe(
          std::function<void(T)> subscriber = [](T) {},
          std::function<void(std::exception_ptr)> error =
              [](std::exception_ptr) {},
          std::function<void()> completed = []() {}) {
        unwrapped_.subscribe(
            [this, subscriber](T val) {
              // verify before invariant
              this->strategy_->on_next_before(val);

              // invoke subscriber
              subscriber(val);

              // verify after invariant
              this->strategy_->on_next_after(val);
            },
            [this, error](std::exception_ptr ep) {
              // invoke subscriber
              error(ep);

              this->strategy_->on_error(ep);
            },
            [this, completed]() {
              // invoke subscriber
              completed();

              this->strategy_->on_completed();
            });

        return *this;
      }

      /**
       * Validate invariant
       * @return true if invariant correct
       */
      bool validate() {
        return strategy_->validate();
      }

     private:
      rxcpp::observable<T> unwrapped_;
      std::unique_ptr<VerificationStrategy<T>> strategy_;
    };

    /**
     * Create test subscriber with specified strategy for given observable and
     * strategy arguments
     * @tparam S - strategy type
     * @tparam O - observable type
     * @tparam T - type of elements emitted by observable
     * @tparam SourceOperator - internal rxcpp type
     * @tparam Args - types of strategy constructor arguments
     * @param unwrapped_observable - observable to test
     * @param args - strategy arguments
     * @return test subscriber described above
     *
     * Example:
     *   auto o = rxcpp::observable<>::just(1);
     *   auto sub = make_test_subscriber<CallExact>(o, 1);
     */
    template <template <typename K> class S,
              template <typename V, typename SO> class O,
              typename T,
              typename SourceOperator,
              typename... Args>
    TestSubscriber<T> make_test_subscriber(
        O<T, SourceOperator> unwrapped_observable, Args &&... args) {
      return TestSubscriber<T>(
          unwrapped_observable,
          std::make_unique<S<T>>(std::forward<Args>(args)...));
    }

    /**
     * CallExact check invariant that subscriber called exact number of times
     * @tparam T - observable parameter
     */
    template <typename T>
    class CallExact : public VerificationStrategy<T> {
     public:
      /**
       * @param expected_number_of_calls - number of calls
       * that required for call
       */
      explicit CallExact(uint64_t expected_number_of_calls)
          : expected_number_of_calls_(expected_number_of_calls) {}

      CallExact(CallExact<T> &&rhs) {
        number_of_calls_ = 0;
        expected_number_of_calls_ = 0;
        std::swap(expected_number_of_calls_, rhs.expected_number_of_calls_);
        std::swap(number_of_calls_, rhs.number_of_calls_);
      }

      CallExact<T> &operator=(CallExact<T> &&rhs) {
        number_of_calls_ = 0;
        expected_number_of_calls_ = 0;
        std::swap(expected_number_of_calls_, rhs.expected_number_of_calls_);
        std::swap(number_of_calls_, rhs.number_of_calls_);
        return *this;
      }

      /**
       * Remove copy operator=
       */
      CallExact<T> &operator=(CallExact<T> &rhs) = delete;

      /**
       * Remove copy constructor
       */
      CallExact(const CallExact<T> &rhs) = delete;

      void on_next_after(T next) override {
        ++number_of_calls_;
      }

     protected:
      bool validate() override {
        auto val = number_of_calls_ == expected_number_of_calls_;
        if (!val) {
          this->invalidate_reason_ =
              "Expected calls: " + std::to_string(expected_number_of_calls_)
              + ", but called " + std::to_string(number_of_calls_);
        }
        return val;
      }

     private:
      uint64_t expected_number_of_calls_;
      uint64_t number_of_calls_ = 0;
    };

    /**
     * Checks that on_completed was called by observable
     * @tparam T - observable parameter
     */
    template <typename T>
    class IsCompleted : public VerificationStrategy<T> {
     public:
      IsCompleted() = default;

      IsCompleted(IsCompleted<T> &&rhs)
          : on_complete_called(rhs.on_complete_called) {
        rhs.on_complete_called = false;
      }

      IsCompleted &operator=(IsCompleted<T> &&rhs) {
        on_complete_called = rhs.on_complete_called;
        rhs.on_complete_called = false;
        return *this;
      }

      IsCompleted(const IsCompleted &) = delete;

      IsCompleted &operator=(const IsCompleted &) = delete;

      void on_completed() override {
        on_complete_called = true;
      }

     protected:
      bool validate() override {
        if (not on_complete_called) {
          this->invalidate_reason_ =
              "on_completed expected to be called once, but never called";
        }
        return on_complete_called;
      }

     private:
      bool on_complete_called = false;
    };

  }  // namespace test_subscriber
}  // namespace framework
#endif  // IROHA_TEST_SUBSCRIBER_HPP
