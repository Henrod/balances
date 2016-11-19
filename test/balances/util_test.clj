(ns balances.util-test
  (:require [balances.util :as util]
            [clojure.test :refer [deftest testing is]]))

(deftest test-abs
  (testing "for integers"
    (is (= 10 (util/abs 10)))
    (is (= 10 (util/abs -10))))

  (testing "for double"
    (is (= 3.14159 (util/abs 3.14159)))
    (is (= 3.14159 (util/abs -3.14159))))

  (testing "for ratio"
    (is (= 20/7 (util/abs 20/7)))
    (is (= 20/7 (util/abs -20/7))))

  (testing "for BigInt"
    (is (= 100N (util/abs 100N)))
    (is (= 100N (util/abs -100N))))

  (testing "for BigDecimal"
    (is (= 2718.281M (util/abs 2718.281M)))
    (is (= 2718.281M (util/abs -2718.281M))))

  (testing "for string"
    (is (thrown? Exception (util/abs "3")))))
