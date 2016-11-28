(ns balances.core-test
  (:require [clojure.test :refer [deftest testing is are]]
            [balances.core :refer [new-operation current-balance build
                                   bank-statement debt-periods
                                   compute-balances]]
            [balances.util :as util]))

(defn- opp
  "Operation params: build a map more succinctly"
  [account description amount date]
  {:account account :description description :amount amount :date date})

(defn- date
  "Convert string to Date more succintly"
  [str]
  (util/str->date str))

;;;; HELPER FUNCTIONS TESTS
(deftest compute-balances-test
  (let [day1 (date "06/12")
        day2 (date "07/12")
        ops {1 {:current 821.48
                :operations (sorted-map day1 [(build "Debit" -146.32)
                                              (build "Purchase" -32.20)]
                                        day2 [(build "Credit" 1000.00)])}
             2 {:current 5767.34
                :operations (sorted-map day1 [(build "Salary" 4267.34)]
                                        day2 [(build "Deposit" 500.00)
                                              (build "Credit" 1000.00)])}}
        balances-1 (compute-balances (get-in ops [1 :operations]))
        balances-2 (compute-balances (get-in ops [2 :operations]))
        result-1 {day1 -178.51 day2 821.49}
        result-2 {day1 4267.34 day2 5767.34}]
    (is (= balances-1 result-1))
    (is (= balances-2 result-2))))


;;;; NEW OPERATION TESTS
(deftest new-operation-test
  (let [opr-1 (opp 1 "Deposit"   "1000"     "15/10")
        opr-2 (opp 1 "Purchase" -800.00     "17/10")
        opr-3 (opp 2 "Debit"    "-199.43"   "17/11")
        opr-4 (opp 3 "Salary"    8123.30999 "10/10")]

    (testing "Initial case"
      (let [ops (new-operation {} opr-1)
            res {1 {:current    1000.00
                    :operations (sorted-map
                                  (date "15/10") [(build "Deposit" 1000.00)])}}]
        (is (= ops res))))

    (testing "Add new operation"
      (let [ops (-> (new-operation {} opr-1) (new-operation opr-2))
            res {1 {:current 200.00
                    :operations (sorted-map
                                  (date "15/10") [(build "Deposit" 1000.00)]
                                  (date "17/10") [(build "Purchase" -800.00)])}}]
        (is (= ops res))))

    (testing "Add multiple accounts"
      (let [ops (reduce new-operation {} [opr-1 opr-2 opr-3 opr-4])
            res {1 {:current 200.00
                    :operations (sorted-map
                                  (date "15/10") [(build "Deposit" 1000.00)]
                                  (date "17/10") [(build "Purchase" -800.00)])}
                 2 {:current -199.43
                    :operations (sorted-map
                                  (date "17/11") [(build "Debit" -199.43)])}
                 3 {:current 8123.30
                    :operations (sorted-map
                                  (date "10/10") [(build "Salary" 8123.30)])}}]
        (is (= ops res))))))


(deftest new-operation-invalid-parameters-test
  (let [good {:account 1     :description "Credit"
              :amount 100.00 :date "5/8"}]
    (is (thrown? IllegalArgumentException
                 (new-operation {} (dissoc good :account))) "No account")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (dissoc good :description))) "No description")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (dissoc good :amount))) "No amount")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (dissoc good :date))) "No date")

    (is (thrown? IllegalArgumentException
                 (new-operation {} (assoc good :account ""))) "Empty account")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (assoc good :description ""))) "Empty description")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (assoc good :amount 0.00))) "Zero amount")
    (is (thrown? IllegalArgumentException
                 (new-operation {} (assoc good :date ""))) "Empty date")

    (is (thrown? AssertionError
                 (new-operation nil good)) "No ops")))

;;;; CURRENT BALANCE TESTS
(deftest current-balance-test
  (let [operations [(opp 1 "Deposit"             1423.34 "15/10")
                    (opp 1 "Purchase on Amazon" -898.76  "17/10")
                    (opp 2 "Debit to Mary"      -100.00  "14/10")
                    (opp 2 "Salary"              8115.40 "12/10")]
        ops (reduce new-operation {} operations)]

    (testing "Current balance of account number 1"
      (is (= (current-balance ops 1) {:balance 524.57})))

    (testing "Current balance of account number 2"
      (is (= (current-balance ops 2) {:balance 8015.40})))

    (testing "Current balance of absent account"
      (is (= (current-balance ops 3) {:balance nil})))))

(deftest current-balance-invalid-parameter-test
  (is (thrown? IllegalArgumentException
               (current-balance {} nil)) "No account")
  (is (thrown? IllegalArgumentException
               (current-balance {} "")) "Empty account")
  (is (thrown? AssertionError
               (current-balance nil 1)) "No ops"))


;;;; BANK STATEMENT TESTS
(deftest bank-statement-test
  (let [operations [(opp 1 "Deposit"             1000.00 "15/10")
                    (opp 1 "Purchase on Amazon" -800.00  "17/10")
                    (opp 2 "Salary"              8000.00 "15/10")
                    (opp 2 "Debit to Mary"      -100.00  "17/10")]]

    (testing "statement with one operation for account 1"
      (let [sta (-> (new-operation {} (operations 0))
                    (bank-statement 1 "15/10" "17/10"))
            res {"15/10" {:operations ["Deposit 1000.00"]
                          :balance 1000.00}}]
        (is (= sta res))))

    (testing "statement with two operation for account 1"
      (let [sta (-> (reduce new-operation {} (take 2 operations))
                    (bank-statement 1 "15/10" "17/10"))
            res {"15/10" {:operations ["Deposit 1000.00"]
                          :balance 1000.00}
                 "17/10" {:operations ["Purchase on Amazon 800.00"]
                          :balance 200.00}}]
        (is (= sta res))))

    (testing "statement with three operation for account 2"
      (let [sta (-> (reduce new-operation {} (take 3 operations))
                    (bank-statement 2 "15/10" "17/10"))
            res {"15/10" {:operations ["Salary 8000.00"]
                          :balance 8000.00}}]
        (is (= sta res))))

    (testing "statement with four operation for account 2"
      (let [sta (-> (reduce new-operation {} operations)
                    (bank-statement 2 "15/10" "17/10"))
            res {"15/10" {:operations ["Salary 8000.00"]
                          :balance 8000.00}
                 "17/10" {:operations ["Debit to Mary 100.00"]
                          :balance 7900.00}}]
        (is (= sta res))))

    (testing "statement for absent account"
      (let [sta (-> (reduce new-operation {} operations)
                    (bank-statement 3 "15/10" "17/10"))
            res {}]
        (is (= sta res))))))


(deftest bank-statement-between-dates-test
  (let [operations [(opp 1 "Deposit"   1000.00  "15/10")
                    (opp 1 "Purchase" -1200.00  "16/10")
                    (opp 1 "Salary"    4000.00  "19/10")
                    (opp 1 "Debit"    -20000.00 "21/10")]
        res {"15/10" {:balance  1000.00  :operations ["Deposit 1000.00"]}
             "16/10" {:balance -200.00   :operations ["Purchase 1200.00"]}
             "19/10" {:balance  3800.00  :operations ["Salary 4000.00"]}
             "21/10" {:balance -16200.00 :operations ["Debit 20000.00"]}}
        ops (reduce new-operation {} operations)]

    (testing "Statement within dates"
      (let [sta (bank-statement ops 1 "15/10" "21/10")]
        (is (= sta res))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement ops 1 "10/10" "16/10")
            res# (select-keys res ["15/10" "16/10"])]
        (is (= sta res#))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement ops 1 "16/10" "29/10")
            res# (dissoc res "15/10")]
        (is (= sta res#))))))


(deftest bank-statement-multiple-operations-test
  (let [operations [(opp 1 "Deposit"     1000.00 "15/10")
                    (opp 1 "Purchase"   -1200.00 "16/10")
                    (opp 1 "Salary"      4000.00 "16/10")
                    (opp 1 "Debit"      -2000.00 "16/10")
                    (opp 1 "Credit"      5000.00 "19/10")
                    (opp 1 "Withdrawal" -3800.00 "19/10")]
        res {"15/10" {:balance 1000.00 :operations ["Deposit 1000.00"]}
             "16/10" {:balance 1800.00 :operations ["Purchase 1200.00"
                                                    "Salary 4000.00"
                                                    "Debit 2000.00"]}
             "19/10" {:balance 3000.00 :operations ["Credit 5000.00"
                                                    "Withdrawal 3800.00"]}}
        ops (reduce new-operation {} operations)]

    (testing "Multiple operations on 16/10 and 19/10"
      (let [sta (bank-statement ops 1 "15/10" "19/10")]
        (is (= sta res))))))

(deftest bank-statement-invalid-parameters-test
  (is (thrown? IllegalArgumentException
               (bank-statement {} nil "1/3" "29/3")) "No account")
  (is (thrown? IllegalArgumentException
               (bank-statement {} "" "1/3" "29/3")) "Empty account")
  (is (thrown? IllegalArgumentException
               (bank-statement {} 1 nil "29/3")) "No start date")
  (is (thrown? IllegalArgumentException
               (bank-statement {} 1 "" "29/3")) "Empty start date")
  (is (thrown? IllegalArgumentException
               (bank-statement {} 1 "1/3" nil)) "No end date")
  (is (thrown? IllegalArgumentException
               (bank-statement {} 1 "1/3" "")) "Empty end date")
  (is (thrown? IllegalArgumentException
               (bank-statement {} "" nil nil)) "All wrong")
  (is (thrown? IllegalArgumentException
               (bank-statement {} 1 "29/3" "1/3")) "start after end")
  (is (thrown? AssertionError
               (bank-statement nil 1 "1/3" "29/3")) "No ops"))


;;;; DEBT PERIODS TEST
(deftest debt-periods-test
  (let [operations [(opp 1 "Deposit"    1000.00 "15/10")  ; 1000
                    (opp 1 "Purchase"  -1200.00 "16/10")
                    (opp 1 "Salary"     4000.00 "16/10")  ; 3800
                    (opp 1 "Withdrawal"-5000.00 "19/10")  ; -1200
                    (opp 1 "Credit"     3000.00 "21/10")]]; 1800

    (testing "One period of debt with end"
      (let [debts (debt-periods (reduce new-operation {} operations) 1)
            res {:debts [{:start "19/10" :end "20/10" :principal -1200.00}]}]
        (is (= debts res))))

    (testing "Two periods of debt with ends"
      (let [ops# (conj operations (opp 1 "Debit" -2000.00 "23/10") ; -200
                                  (opp 1 "Credit" 3000.00 "26/10")); 2800
            res {:debts [{:start "19/10" :end "20/10" :principal -1200.00}
                         {:start "23/10" :end "25/10" :principal -200.00}]}
            debts (debt-periods (reduce new-operation {} ops#) 1)]
        (is (= debts res))))

    (testing "Period of debt without end"
      (let [debts (-> (reduce new-operation {} (drop-last operations))
                      (debt-periods 1))
            res {:debts [{:start "19/10" :principal -1200.00}]}]
        (is (= debts res))))))


(deftest debt-periods-same-debt-test
  (testing "Periods with debt when principal doesn't change"
    (let [operations [(opp 1 "Debit"    -100.00  "12/01")
                      (opp 1 "Credit"    200.00  "15/01")
                      (opp 1 "Purchase" -200.00  "15/01")
                      (opp 1 "Salary"    1100.00 "20/01")]
          res {:debts [{:start "12/01" :principal -100.00 :end "19/01"}]}
          debts (debt-periods (reduce new-operation {} operations) 1)]
      (is (= debts res))))

  (testing "Seperate periods with same debt"
    (let [operations [(opp 1 "Debit"    -100.00  "12/01")
                      (opp 1 "Credit"    200.00  "15/01")
                      (opp 1 "Purchase" -200.00  "17/01")
                      (opp 1 "Salary"    1100.00 "20/01")]
          res {:debts [{:start "12/01" :principal -100.00 :end "14/01"}
                       {:start "17/01" :principal -100.00 :end "19/01"}]}
          debts (debt-periods (reduce new-operation {} operations) 1)]
      (is (= debts res)))))


(deftest debt-periods-multiple-accounts-test
  (let [operations [(opp 1 "Deposit"     1000.00 "15/10")
                    (opp 1 "Purchase"   -1200.00 "16/10")
                    (opp 2 "Salary"      4000.00 "16/10")
                    (opp 2 "Withdrawal" -5000.00 "19/10")
                    (opp 1 "Credit"      3000.00 "21/10")]
        ops (reduce new-operation {} operations)]

    (testing "Debt periods of account 1"
      (let [debts (debt-periods ops 1)
            res {:debts [{:start "16/10" :end "20/10" :principal -200.00}]}]
        (is (= debts res))))

    (testing "Debt periods of account 2"
      (let [debts (debt-periods ops 2)
            res {:debts [{:start "19/10" :principal -1000.00}]}]
        (is (= debts res))))))


(deftest debt-periods-no-debt
  (let [operations [(opp 1 "Deposit"    1000.00 "15/10")
                    (opp 1 "Purchase"   1200.00 "16/10")
                    (opp 2 "Salary"     4000.00 "16/10")
                    (opp 2 "Withdrawal" 5000.00 "19/10")
                    (opp 1 "Credit"     3000.00 "21/10")]
        ops (reduce new-operation {} operations)]

    (testing "Account 1 with no debt periods"
      (let [debts (debt-periods ops 1)
            res {:debts []}]
        (is (= debts res))))

    (testing "Account 2 with no debt periods"
      (let [debts (debt-periods ops 2)
            res {:debts []}]
        (is (= debts res))))))

(deftest debt-periods-invalid-parameters-test
  (is (thrown? IllegalArgumentException
               (debt-periods {} nil)) "No account")
  (is (thrown? IllegalArgumentException
               (debt-periods {} "")) "Empty account")
  (is (thrown? AssertionError
               (debt-periods nil 1)) "No ops"))

;TODO: construir teste com todos os casos de forma didatica e clara
(deftest complete-test
  (let []))