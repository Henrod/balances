(ns balances.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [balances.core :refer [new-operation current-balance build
                                   bank-statement debt-periods]])
  (:import (balances.core Operation)))

(defn- opp
  "Operation params: build a map more succinctly"
  [account description amount date]
  {:account account :description description :amount amount :date date})

;;;; HELPER FUNCTIONS TESTS
(deftest all-dates->string-test
  (let [operations [(build "Debit" -146.32 "06/12")
                    (build "Credit" 370.05 "12/12")
                    (build "Purchase" -130.44 "01/12")]
        str-dates (#'balances.core/all-date->string operations)
        result [(Operation. "Debit" -146.32 "06/12")
                (Operation. "Credit" 370.05 "12/12")
                (Operation. "Purchase" -130.44 "01/12")]]
    (is (= result str-dates))))

(deftest compute-balances-test
  (let [ops {1 [(build "Credit" 1000.00 "07/12")
                (build "Debit" -146.32 "06/12")
                (build "Purchase" -32.20 "06/12")]
             2 [(build "Deposit" 500.00 "06/12")
                (build "Credit" 1000.00 "07/12")
                (build "Salary" 4267.34 "06/12")]}
        balances-1 (#'balances.core/compute-balances ops 1)
        balances-2 (#'balances.core/compute-balances ops 2)
        result-1 [{:date "06/12" :balance -178.52}
                  {:date "07/12" :balance 821.48}]
        result-2 [{:date "06/12" :balance 4767.34}
                  {:date "07/12" :balance 5767.34}]]
    (is (= balances-1 result-1))
    (is (= balances-2 result-2))))

(deftest compute-each-balance-test
  (let [ops {1 [(build "Credit" 1000.00 "07/12")
                (build "Debit" -146.32 "06/12")
                (build "Purchase" -32.20 "06/12")]
             2 [(build "Deposit" 500.00 "06/12")
                (build "Credit" 1000.00 "07/12")
                (build "Salary" 4267.34 "06/12")]}
        balances-1 (#'balances.core/compute-each-balance ops 1)
        balances-2 (#'balances.core/compute-each-balance ops 2)
        result-1 {"06/12" -178.52
                  "07/12" 821.48}
        result-2 {"06/12" 4767.34
                  "07/12" 5767.34}]
    (is (= balances-1 result-1))
    (is (= balances-2 result-2))))


;;;; NEW OPERATION TESTS
(deftest new-operation-test
  (let [opr-1 (opp 1 "Deposit" 1000.00 "15/10")
        opr-2 (opp 1 "Purchase on Amazon" -800.00 "17/10")
        opr-3 (opp 2 "Debit" -199.43 "17/11")
        opr-4 (opp 3 "Salary" 8123.30 "10/10")]

    (testing "Initial case"
      (let [ops (new-operation {} opr-1)
            res {1 [(build "Deposit" 1000.00 "15/10")]}]
        (is (= ops res))))

    (testing "Add new operation"
      (let [ops (-> (new-operation {} opr-1) (new-operation opr-2))
            res {1 [(build "Purchase on Amazon" -800.00 "17/10")
                    (build "Deposit" 1000.00 "15/10")]}]
        (is (= ops res))))

    (testing "Add multiple accounts"
      (let [ops (reduce new-operation {} [opr-1 opr-2 opr-3 opr-4])
            res {1 [(build "Purchase on Amazon" -800.00 "17/10")
                    (build "Deposit" 1000.00 "15/10")]
                 2 [(build "Debit" -199.43 "17/11")]
                 3 [(build "Salary" 8123.30 "10/10")]}]
        (is (= ops res))))))


;;;; CURRENT BALANCE TESTS
(deftest current-balance-test
  (let [operations [(opp 1 "Deposit" 1423.34 "15/10")
                    (opp 1 "Purchase on Amazon" -898.76 "17/10")
                    (opp 2 "Salary" 8115.40 "12/10")
                    (opp 2 "Debit to Mary" -100.00 "14/10")]
        ops (reduce new-operation {} operations)]

    (testing "Current balance of account number 1"
      (is (= (current-balance ops 1) "524.58")))

    (testing "Current balance of account number 2"
      (is (= (current-balance ops 2) "8015.40")))

    (testing "Current balance of absent account"
      (is (nil? (current-balance ops 3))))))


;;;; BANK STATEMENT TESTS
(deftest bank-statement-test
  (let [operations [(opp 1 "Deposit" 1000.00 "15/10")
                    (opp 1 "Purchase on Amazon" -800.00 "17/10")
                    (opp 2 "Salary" 8000.00 "15/10")
                    (opp 2 "Debit to Mary" -100.00 "17/10")]]

    (testing "statement with one operation for account 1"
      (let [sta (bank-statement
                  (new-operation {} (operations 0)) 1 "15/10" "17/10")
            res {"15/10" {:operations ["- Deposit 1000.00"]
                          :balance 1000.00}}]
        (is (= sta res))))

    (testing "statement with two operation for account 1"
      (let [sta (bank-statement
                  (reduce new-operation {} (take 2 operations)) 1 "15/10" "17/10")
            res {"15/10" {:operations ["- Deposit 1000.00"]
                          :balance 1000.00}
                  "17/10" {:operations ["- Purchase on Amazon 800.00"]
                           :balance 200.00}}]
        (is (= sta res))))

    (testing "statement with three operation for account 2"
      (let [sta (bank-statement
                  (reduce new-operation {} (take 3 operations)) 2 "15/10" "17/10")
            res {"15/10" {:operations ["- Salary 8000.00"]
                          :balance 8000.00}}]
        (is (= sta res))))

    (testing "statement with four operation for account 2"
      (let [sta (bank-statement
                  (reduce new-operation {} operations) 2 "15/10" "17/10")
            res {"15/10" {:operations ["- Salary 8000.00"]
                          :balance 8000.00}
                  "17/10" {:operations ["- Debit to Mary 100.00"]
                           :balance 7900.00}}]
        (is (= sta res))))

    (testing "statement for absent account"
      (let [sta (bank-statement
                  (reduce new-operation {} operations) 3 "15/10" "17/10")
            res {}]
        (is (= sta res))))))


(deftest bank-statement-between-dates-test
  (let [operations [(opp 1 "Deposit" 1000.00 "15/10")
                    (opp 1 "Purchase" -1200.00 "16/10")
                    (opp 1 "Salary" 4000.00 "19/10")
                    (opp 1 "Debit" -20000.00 "21/10")]
        res {"15/10" {:balance 1000.00   :operations ["- Deposit 1000.00"]}
             "16/10" {:balance -200.00   :operations ["- Purchase 1200.00"]}
             "19/10" {:balance 3800.00   :operations ["- Salary 4000.00"]}
             "21/10" {:balance -16200.00 :operations ["- Debit 20000.00"]}}
        ops (reduce new-operation {} operations)]

    (testing "Statement within dates"
      (let [sta (bank-statement ops 1 "15/10" "21/10")
            res# res]
        (is (= sta res#))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement ops 1 "10/10" "16/10")
            res# (select-keys res ["15/10" "16/10"])]
        (is (= sta res#))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement ops 1 "16/10" "29/10")
            res# (dissoc res "15/10")]
        (is (= sta res#))))))


(deftest bank-statement-multiple-operations-test
  (let [operations [(opp 1 "Deposit" 1000.00 "15/10")
                    (opp 1 "Purchase" -1200.00 "16/10")
                    (opp 1 "Salary" 4000.00 "16/10")
                    (opp 1 "Debit" -2000.00 "16/10")
                    (opp 1 "Credit" 5000.00 "19/10")
                    (opp 1 "Withdrawal" -3800.00 "19/10")]
        res {"15/10" {:balance 1000.00 :operations ["- Deposit 1000.00"]}
             "19/10" {:balance 3000.00 :operations ["- Withdrawal 3800.00"
                                                    "- Credit 5000.00"]}
             "16/10" {:balance 1800.00 :operations ["- Debit 2000.00"
                                                    "- Salary 4000.00"
                                                    "- Purchase 1200.00"]}}
        ops (reduce new-operation {} operations)]

    (testing "Multiple operations on 16/10 and 19/10"
      (let [sta (bank-statement ops 1 "15/10" "19/10")]
        (is (= sta res))))))


;;;; DEBT PERIODS TEST
(deftest debt-periods-test
  (let [ops [(opp 1 "Deposit"    1000.00 "15/10")  ; 1000
             (opp 1 "Purchase"  -1200.00 "16/10")
             (opp 1 "Salary"     4000.00 "16/10")  ; 3800
             (opp 1 "Withdrawal"-5000.00 "19/10")  ; -1200
             (opp 1 "Credit"     3000.00 "21/10")]]; 1800

    (testing "One period of debt with end"
      (let [debts (debt-periods (reduce new-operation {} ops) 1)
            res {:debts [{:start "19/10" :end "20/10" :principal -1200.00}]}]
        (is (= debts res))))

    (testing "Two periods of debt with ends"
      (let [ops# (conj ops (opp 1 "Debit" -2000.00 "23/10") ; -200
                           (opp 1 "Credit" 3000.00 "26/10")); 2800
            res {:debts [{:start "23/10" :end "25/10" :principal -200.00}
                         {:start "19/10" :end "20/10" :principal -1200.00}]}
            debts (debt-periods (reduce new-operation {} ops#) 1)]
        (is (= debts res))))

    (testing "Period of debt without end"
      (let [debts (debt-periods (reduce new-operation {} (drop-last ops)) 1)
            res {:debts [{:start "19/10" :principal -1200.00}]}]
        (is (= debts res))))))


(deftest debt-periods-same-debt-test
  (testing "Periods with debt when principal doesn't change"
    (let [ops [(opp 1 "Debit"    -100.00  "12/01")
               (opp 1 "Credit"    200.00  "15/01")
               (opp 1 "Purchase" -200.00  "15/01")
               (opp 1 "Salary"    1100.00 "20/01")]
          res {:debts [{:start "12/01" :principal -100.00 :end "19/01"}]}
          debts (debt-periods (reduce new-operation {} ops) 1)]
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