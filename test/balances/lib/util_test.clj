(ns balances.lib.util-test
  (:require [balances.lib.util :as u]
            [clojure.test :refer [deftest testing is are]]))

(deftest previous-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (u/previous-day (u/str->date "17/10")))))

  (testing "day at the beginning of a month"
    (is (= "30/11" (u/previous-day (u/str->date "01/12")))))

  (testing "day at the beginning of a year"
    (is (= "31/12" (u/previous-day (u/str->date "01/01"))))))

(deftest next-day-test
  (testing "day in the middle of a month"
    (is (= "16/10" (u/date->str (u/next-day "15/10")))))

  (testing "day in the end of a month"
    (is (= "01/02" (u/date->str (u/next-day "31/01")))))

  (testing "day in the end of a year"
    (is (= "01/01" (u/date->str (u/next-day "31/12"))))))

(deftest within-test
  (let [within (u/make-within "9/3" "29/03")]
    (is (true? (within (u/str->date "9/03"))))
    (is (true? (within (u/str->date "15/03"))))
    (is (true? (within (u/str->date "29/03"))))

    (is (false? (within (u/str->date "1/3"))))
    (is (false? (within (u/str->date "30/3"))))
    (is (false? (within (u/str->date "1/02"))))))

(deftest abs-test
  (testing "for integers"
    (is (= 10 (u/abs 10) (u/abs -10))))

  (testing "for double"
    (is (= 3.14159 (u/abs 3.14159) (u/abs -3.14159))))

  (testing "for ratio"
    (is (= 20/7 (u/abs 20/7) (u/abs -20/7))))

  (testing "for BigInt"
    (is (= 100N (u/abs 100N) (u/abs -100N))))

  (testing "for BigDecimal"
    (is (= 2718.281M (u/abs 2718.281M) (u/abs -2718.281M))))

  (testing "for string"
    (is (thrown? Exception (u/abs "3")))))

(deftest to-float-test
  (is (= "1.00" (u/to-format 1M)))
  (is (= "-1.00" (u/to-format -1M)))
  (is (= "-1.99" (u/to-format -1.99M)))
  (is (= "-1.01" (u/to-format -1.0099999M))))

(deftest validate-amount-test
  (is (thrown? IllegalArgumentException (u/validate-amount nil)))
  (is (thrown? IllegalArgumentException (u/validate-amount "")))
  (is (thrown? IllegalArgumentException (u/validate-amount 0)))
  (is (thrown? IllegalArgumentException (u/validate-amount 0.0)))
  (is (thrown? IllegalArgumentException (u/validate-amount 0M)))
  (is (thrown? IllegalArgumentException (u/validate-amount "0")))
  (is (thrown? IllegalArgumentException (u/validate-amount ".")))
  (is (thrown? IllegalArgumentException (u/validate-amount "-")))
  (is (thrown? IllegalArgumentException (u/validate-amount "-.")))

  (is (u/validate-amount "10"))
  (is (u/validate-amount "10."))
  (is (u/validate-amount 10.1))
  (is (u/validate-amount "10.11"))
  (is (u/validate-amount "-10"))
  (is (u/validate-amount "-10."))
  (is (u/validate-amount "-10.1"))
  (is (u/validate-amount "-10.11"))
  (is (u/validate-amount 0.11))
  (is (u/validate-amount ".11"))

  (is (thrown? IllegalArgumentException (u/validate-amount "10.111")))
  (is (thrown? IllegalArgumentException (u/validate-amount "10.1111")))
  (is (thrown? IllegalArgumentException (u/validate-amount ".111")))
  (is (thrown? IllegalArgumentException (u/validate-amount "-10.111"))))