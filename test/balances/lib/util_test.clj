(ns balances.lib.util-test
  (:require [balances.lib.util :as util]
            [clojure.test :refer [deftest testing is]]))

(deftest previous-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (util/previous-day (util/str->date "17/10")))))

  (testing "day at the beginning of a month"
    (is (= "30/11" (util/previous-day (util/str->date "01/12")))))

  (testing "day at the beginning of a year"
    (is (= "31/12" (util/previous-day (util/str->date "01/01"))))))

(deftest next-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (util/date->str (util/next-day "15/10")))))

  (testing "day in the end of a month"
    (is (= "01/02" (util/date->str (util/next-day "31/01")))))

  (testing "day in the end of a year"
    (is (= "01/01" (util/date->str (util/next-day "31/12"))))))

(deftest within-test
  (let [within (util/within? "9/3" "29/03")]
    (is (true? (within (util/str->date "9/03"))))
    (is (true? (within (util/str->date "15/03"))))
    (is (true? (within (util/str->date "29/03"))))

    (is (false? (within (util/str->date "1/3"))))
    (is (false? (within (util/str->date "30/3"))))
    (is (false? (within (util/str->date "1/02"))))))

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
    (is (= 1.00 (util/to-format 1))
        (= -1.00 (util/to-format -1))))

  (testing "for float"
    (is (= 100.15 (util/to-format 100.1515151))
        (= -100.15 (util/to-format -100.15131341))))

  (testing "for strings"
    (is (= 100.50 (util/to-format "100.50292929"))
        (= -100.50 (util/to-format "-100.50278723")))))