(ns balances.util-test
  (:require [balances.util :as util]
            [clojure.test :refer [deftest testing is]]))

(deftest previous-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (util/previous-day "17/10"))))

  (testing "day at the beginning of a month"
    (is (= "30/11" (util/previous-day "01/12"))))

  (testing "day at the beginning of a year"
    (is (= "31/12" (util/previous-day "01/01")))))

(deftest next-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (util/date->string (util/next-day "15/10")))))

  (testing "day in the end of a month"
    (is (= "01/02" (util/date->string (util/next-day "31/01")))))

  (testing "day in the end of a year"
    (is (= "01/01" (util/date->string (util/next-day "31/12"))))))

(deftest abs-test
  (testing "for integers"
    (is (= 10 (util/abs 10))
        (= 10 (util/abs -10))))

  (testing "for double"
    (is (= 3.14159 (util/abs 3.14159))
        (= 3.14159 (util/abs -3.14159))))

  (testing "for ratio"
    (is (= 20/7 (util/abs 20/7))
        (= 20/7 (util/abs -20/7))))

  (testing "for BigInt"
    (is (= 100N (util/abs 100N))
        (= 100N (util/abs -100N))))

  (testing "for BigDecimal"
    (is (= 2718.281M (util/abs 2718.281M))
        (= 2718.281M (util/abs -2718.281M))))

  (testing "for string"
    (is (thrown? Exception (util/abs "3")))))

(deftest to-float-test
  (testing "for integer"
    (is (= 1.00 (util/to-float 1))
        (= -1.00 (util/to-float -1))))

  (testing "for float"
    (is (= 100.15 (util/to-float 100.1515151))
        (= -100.15 (util/to-float -100.15131341))))

  (testing "for strings"
    (is (= 100.50 (util/to-float "100.50292929"))
        (= -100.50 (util/to-float "-100.50278723")))))