(ns balances.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [balances.core :refer [new-operation current-balance build
                                   bank-statement debt-periods]]))

;;;; NEW OPERATION TESTS
(deftest new-operation-test
  (let [opr-1 {:account 1 :description "Deposit" :amount 1000.00 :date "15/10"}
        opr-2 {:account 1 :description "Purchase on Amazon" :amount -800.00 :date "17/10"}]
    (testing "Initial case"
      (let [ops (new-operation {} opr-1)
            res {1 (list (build "Deposit" 1000.00 "15/10"))}]
        (is (= ops res))))

    (testing "Add new operation"
      (let [ops (-> (new-operation {} opr-1) (new-operation opr-2))
            res {1 (list (build "Purchase on Amazon" -800.00 "17/10")
                         (build "Deposit" 1000.00 "15/10"))}]
        (is (= ops res))))))


;;;; CURRENT BALANCE TESTS
(deftest current-balance-test
  (let [operations [{:account 1 :description "Deposit" :amount 1000.00 :date "15/10"}
                    {:account 1 :description "Purchase on Amazon" :amount -800.00 :date "17/10"}
                    {:account 2 :description "Salary" :amount 8000.00 :date "12/10"}
                    {:account 2 :description "Debit to Mary" :amount -100.00 :date "14/10"}]
        ops (reduce new-operation {} operations)]
    (testing "Current balance of account number 1"
      (is (= (current-balance ops 1) "200.00")))

    (testing "Current balance of account number 2"
      (is (= (current-balance ops 2) "7900.00")))

    (testing "Current balance of absent account"
      (is (nil? (current-balance ops 3))))))

;;;; BANK STATEMENT TESTS
(deftest bank-statement-test
  (let [opr-1 {:account 1 :description "Deposit" :amount 1000.00 :date "15/10"}
        opr-2 {:account 1 :description "Purchase on Amazon" :amount -800.00 :date "17/10"}
        opr-3 {:account 2 :description "Salary" :amount 8000.00 :date "15/10"}
        opr-4 {:account 2 :description "Debit to Mary" :amount -100.00 :date "17/10"}]
    (testing "statement with one operation for account 1"
      (let [sta (bank-statement (new-operation {} opr-1) 1 "15/10" "17/10")
            res {"15/10" {:operations ["- Deposit 1000.00"] :balance 1000.00}}]
        (is (= sta res))))

    (testing "statement with two operation for account 1"
      (let [sta (bank-statement (reduce new-operation {} [opr-1 opr-2])
                                1 "15/10" "17/10")
            res {"15/10" {:operations ["- Deposit 1000.00"] :balance 1000.00}
                 "17/10" {:operations ["- Purchase on Amazon 800.00"]
                          :balance 200.00}}]
        (is (= sta res))))

    (testing "statement with three operation for account 2"
      (let [sta (bank-statement (reduce new-operation {} [opr-1 opr-2 opr-3])
                                2 "15/10" "17/10")
            res {"15/10" {:operations ["- Salary 8000.00"] :balance 8000.00}}]
        (is (= sta res))))

    (testing "statement with four operation for account 2"
      (let [sta (bank-statement (reduce new-operation {} [opr-1 opr-2 opr-3 opr-4])
                                2 "15/10" "17/10")
            res {"15/10" {:operations ["- Salary 8000.00"] :balance 8000.00}
                 "17/10" {:operations ["- Debit to Mary 100.00"]
                          :balance 7900.00}}]
        (is (= sta res))))

    (testing "statement for absent account"
      (let [sta (bank-statement (reduce new-operation {} [opr-1 opr-2 opr-3 opr-4])
                                3 "15/10" "17/10")
            res {}]
        (is (= sta res))))))


(deftest bank-statement-between-dates-test
  (let [ops [{:account 1, :description "Deposit",  :amount 1000.00,   :date "15/10"}
             {:account 1, :description "Purchase", :amount -1200.00,  :date "16/10"}
             {:account 1, :description "Salary",   :amount 4000.00,   :date "19/10"}
             {:account 1, :description "Debit",    :amount -20000.00, :date "21/10"}]]
    (testing "Statement within dates"
      (let [sta (bank-statement (reduce new-operation {} ops) 1 "15/10" "21/10")
            res {"15/10" {:balance 1000.00 :operations ["- Deposit 1000.00"]}
                 "16/10" {:balance -200.00 :operations ["- Purchase 1200.00"]}
                 "19/10" {:balance 3800.00 :operations ["- Salary 4000.00"]}
                 "21/10" {:balance -16200.00 :operations ["- Debit 20000.00"]}}]
        (is (= sta res))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement (reduce new-operation {} ops) 1 "10/10" "16/10")
            res {"15/10" {:balance 1000.00 :operations ["- Deposit 1000.00"]}
                 "16/10" {:balance -200.00 :operations ["- Purchase 1200.00"]}}]
        (is (= sta res))))

    (testing "Statement that overlaps dates"
      (let [sta (bank-statement (reduce new-operation {} ops) 1 "16/10" "29/10")
            res {"16/10" {:balance -200.00 :operations ["- Purchase 1200.00"]}
                 "19/10" {:balance 3800.00 :operations ["- Salary 4000.00"]}
                 "21/10" {:balance -16200.00 :operations ["- Debit 20000.00"]}}]
        (is (= sta res))))))


(deftest bank-statement-multiple-operations-test
  (let [ops [{:account 1, :description "Deposit",  :amount 1000.00,   :date "15/10"}
             {:account 1, :description "Purchase", :amount -1200.00,  :date "16/10"}
             {:account 1, :description "Salary",   :amount 4000.00,   :date "16/10"}
             {:account 1, :description "Debit",    :amount -2000.00,  :date "16/10"}
             {:account 1, :description "Credit",   :amount 5000.00,   :date "19/10"}
             {:account 1, :description "Withdrawal",:amount -3800.00, :date "19/10"}]]
    (testing "Multiple operations onm 16/10"
      (let [sta (bank-statement (reduce new-operation {} ops) 1 "15/10" "19/10")
            res {"15/10" {:balance 1000.00
                          :operations ["- Deposit 1000.00"]}
                 "16/10" {:balance 1800.00
                          :operations ["- Debit 2000.00" "- Salary 4000.00"
                                       "- Purchase 1200.00"]}
                 "19/10" {:balance 3000.00
                          :operations ["- Withdrawal 3800.00"
                                       "- Credit 5000.00"]}}]
        (is (= sta res))))))


;;;; DEBT PERIODS TEST
(deftest debt-periods-test
  (let [ops [{:account 1, :description "Deposit",  :amount 1000.00,   :date "15/10"} ; 1000
             {:account 1, :description "Purchase", :amount -1200.00,  :date "16/10"}
             {:account 1, :description "Salary",   :amount 4000.00,   :date "16/10"} ; 3800
             {:account 1, :description "Withdrawal",:amount -5000.00, :date "19/10"} ; -1200
             {:account 1, :description "Credit",   :amount 3000.00, :date "21/10"}]] ; 1800
    (testing "One period of debt with end"
      (let [debts (debt-periods (reduce new-operation {} ops) 1)
            res {:debts [{:start "19/10" :end "20/10" :principal -1200.00}]}]
        (is (= debts res))))

    (testing "Two periods of debt with ends"
      (let [ops# (conj ops
                       {:account 1 :description "Debit"  :amount -2000.00 :date "23/10"} ;-200
                       {:account 1 :description "Credit" :amount 3000.00, :date "26/10"}) ;2800
            debts (debt-periods (reduce new-operation {} ops#) 1)
            res {:debts [{:start "23/10" :end "25/10" :principal -200.00}
                         {:start "19/10" :end "20/10" :principal -1200.00}]}]
        (is (= debts res))))

    (testing "Period of debt without end"
      (let [debts (debt-periods (reduce new-operation {} (take 4 ops)) 1)
            res {:debts [{:start "19/10" :principal -1200.00}]}]
        (is (= debts res))))))


(deftest debt-periods-same-debt-test
  (testing "Periods with debt when principal doesn't change"
    (let [ops# [{:account 1 :description "Debit"    :amount -100.00 :date "12/01"}
                {:account 1 :description "Credit"   :amount 200.00  :date "15/01"}
                {:account 1 :description "Purchase" :amount -200.00 :date "15/01"}
                {:account 1 :description "Salary"   :amount 1100.00 :date "20/01"}]
          debts (debt-periods (reduce new-operation {} ops#) 1)
          res {:debts [{:start "12/01" :principal -100.00 :end "19/01"}]}]
      (is (= debts res)))))


(deftest debt-periods-multiple-accounts-test
  (let [ops [{:account 1, :description "Deposit",  :amount 1000.00,   :date "15/10"}
             {:account 1, :description "Purchase", :amount -1200.00,  :date "16/10"}
             {:account 2, :description "Salary",   :amount 4000.00,   :date "16/10"}
             {:account 2, :description "Withdrawal",:amount -5000.00, :date "19/10"}
             {:account 1, :description "Credit",   :amount 3000.00, :date "21/10"}]]
    (testing "Debt periods of account 1"
      (let [debts (debt-periods (reduce new-operation {} ops) 1)
            res {:debts [{:start "16/10" :end "20/10" :principal -200.00}]}]
        (is (= debts res))))

    (testing "Debt periods of account 2"
      (let [debts (debt-periods (reduce new-operation {} ops) 2)
            res {:debts [{:start "19/10" :principal -1000.00}]}]
        (is (= debts res))))))

(deftest debt-periods-no-debt
  (let [ops [{:account 1, :description "Deposit",  :amount 1000.00,   :date "15/10"}
             {:account 1, :description "Purchase", :amount 1200.00,  :date "16/10"}
             {:account 2, :description "Salary",   :amount 4000.00,   :date "16/10"}
             {:account 2, :description "Withdrawal",:amount 5000.00, :date "19/10"}
             {:account 1, :description "Credit",   :amount 3000.00, :date "21/10"}]]
    (testing "Account 1 with no debt periods"
      (let [debts (debt-periods (reduce new-operation {} ops) 1)
            res {:debts []}]
        (is (= debts res))))

    (testing "Account 2 with no debt periods"
      (let [debts (debt-periods (reduce new-operation {} ops) 2)
            res {:debts []}]
        (is (= debts res))))))