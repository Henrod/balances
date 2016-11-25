(ns balances.server-test
  (:require [clojure.test :refer [is deftest testing]]
            [balances.server :refer [ops app]]
            [balances.core :refer [build]]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]))

(deftest invalid-address-test
  (is (= (app (mock/request :get "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}}))
  (is (= (app (mock/request :post "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}})))


;;;; NEW OPERATION TEST
(deftest one-new-operation-test
  (reset! ops {})
  (let [result {"1" [(build "Credit" "100.00" "15/10")]}
        response (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount 100.0}))]
    (is (= result @ops))
    (is (= response {:status 200, :headers {}, :body "ok"}))))

(deftest two-new-operations-test
  (reset! ops {})
  (let [result {"1" [(build "Debit"  "-120.00" "16/10") (build "Credit" "100.00"  "15/10")]}
        response1 (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount 100.0}))
        response2 (app (mock/request :post "/new" {:account 1, :description "Debit",  :date "16/10", :amount -120.0}))]
    (is (= @ops result))
    (is (= response1 response2 {:status 200, :headers {}, :body "ok"}))))

(deftest new-operation-missing-parameters-test
  (let [m {:account 1, :description "Credit", :date "15/10", :amount 100.0}]
    (testing "Missing account parameter"
      (let [result {:status 422, :body "Missing parameter: account", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :account)))]
        (is (= result response))))

    (testing "Missing description parameter"
      (let [result {:status 422, :body "Missing parameter: description", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :description)))]
        (is (= result response))))

    (testing "Missing date parameter"
      (let [result {:status 422, :body "Missing parameter: date", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :date)))]
        (is (= result response))))

    (testing "Missing amount parameter"
      (let [result {:status 422, :body "Missing parameter: amount", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :amount)))]
        (is (= result response))))

    (testing "Missing two parameters"
      (let [result1 {:status 422, :body "Missing parameter: amount", :headers {}}
            result2 {:status 422, :body "Missing parameter: date", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :amount :date)))]
        (is (or (= result1 response) (= result2 response)))))))


;;;; CURRENT BALANCE TEST
(deftest current-balance-from-one-credit-operation-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body "100.00"}
        _ (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount 100.0}))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))

(deftest current-balance-from-one-debit-operation-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body "-120.00"}
        _ (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount -120.0}))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))

(deftest current-balance-from-multiple-operations-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body "1201.00"}
        ops [{:account 1, :description "Credit", :date "15/10", :amount 100.50}   ; 100.50
             {:account 1, :description "Debit", :date "16/10", :amount -120.0}    ; -19.50
             {:account 1, :description "Deposit", :date "17/10", :amount 220.50}  ; 201.00
             {:account 1, :description "Salary", :date "18/10", :amount 1000.0}]] ; 1201.00
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (is (= result (app (mock/request :post "/balance" {:account 1}))))))

(deftest current-balance-multiple-accounts-test
  (reset! ops {})
  (let [result1 {:status 200, :headers {}, :body "1100.50"}
        result2 {:status 200, :headers {}, :body "-319.99"}
        result3 {:status 200, :headers {}, :body "5000.75"}
        ops [{:account 1 :description "Credit"   :date "15/10"  :amount 100.50}   ; 1: 100.50
             {:account 2 :description "Debit"    :date "15/10" :amount -120.0}   ; 2: -120.0
             {:account 2 :description "Purchase" :date "20/10"  :amount -199.99}  ; 2: -319.99
             {:account 3 :description "Salary"   :date "12/10"  :amount 5000.7565}; 3: 5000.75
             {:account 1 :description "Salary"   :date "18/10"  :amount 1000.0}]] ; 1: 1100.50
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (is (= result1 (app (mock/request :post "/balance" {:account 1}))))
    (is (= result2 (app (mock/request :post "/balance" {:account 2}))))
    (is (= result3 (app (mock/request :post "/balance" {:account 3}))))))

(deftest current-balance-missing-parameters-test
  (reset! ops {})
  (app (mock/request :post "/new" {:account 1 :description "Salary"   :date "18/10"  :amount 1000.0}))
  (let [result {:status 422 :headers {} :body "Missing parameter: account"}
        response (app (mock/request :post "/balance" {}))]
    (is (= result response))))


;;;; BANK STATEMENT TESTS
(deftest bank-statement-server-test
  (reset! ops {})
  (let [ops [{:account 2 :description "Salary"           :date "05/08" :amount 5000.00}
             {:account 4 :description "Deposit"          :date "02/08" :amount 1250.20}
             {:account 1 :description "Debit"            :date "06/08" :amount -999.99}
             {:account 2 :description "Purchase on eBay" :date "01/09" :amount -30.50}
             {:account 4 :description "Withdrawal"       :date "01/09" :amount -10.00}
             {:account 4 :description "Credit"           :date "25/08" :amount 500.00}
             {:account 1 :description "Deposit from Ann" :date "16/09" :amount 1200.00}
             {:account 1 :description "Purchase on Moe's":date "16/09" :amount -21.99}
             {:account 2 :description "Salary"           :date "05/09" :amount 5000.00}]]
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (testing "Bank statement for account 1 from 06/08 to 16/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "06/08", :end "16/09"}))
            result {"06/08" {"operations" ["- Debit 999.99"]
                             "balance" -999.99}
                    "16/09" {"operations" ["- Purchase on Moe's 21.99"
                                          "- Deposit from Ann 1200.00"]
                             "balance" 178.02}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 06/08 to 15/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "06/08", :end "15/09"}))
            result {"06/08" {"operations" ["- Debit 999.99"]
                             "balance" -999.99}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 07/08 to 16/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "07/08",
                                         :end "16/09"}))
            result {"16/09" {"operations" ["- Purchase on Moe's 21.99"
                                           "- Deposit from Ann 1200.00"]
                             "balance" 178.02}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 07/08 to 15/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "07/08",
                                         :end "15/09"}))
            result {}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 02/08 to 01/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4, :start "02/08",
                                         :end "01/09"}))
            result {"02/08" {"operations" ["- Deposit 1250.20"]
                             "balance" 1250.20}
                    "25/08" {"operations" ["- Credit 500.00"]
                             "balance" 1750.20}
                    "01/09" {"operations" ["- Withdrawal 10.00"]
                             "balance" 1740.20}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 10/08 to 29/08"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4, :start "10/08",
                                         :end "29/08"}))
            result {"25/08" {"operations" ["- Credit 500.00"]
                             "balance" 1750.20}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 20/08 to 10/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4, :start "20/08",
                                         :end "10/09"}))
            result {"25/08" {"operations" ["- Credit 500.00"]
                             "balance" 1750.20}
                    "01/09" {"operations" ["- Withdrawal 10.00"]
                             "balance" 1740.20}}
            body (json/read-str (:body response))]
        (is (= body result))))))

(deftest bank-statement-missing-parameters-test
  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {:start "10/10"
                                                  :end "24/12"}))
           {:status 422 :headers {} :body "Missing parameter: account"})))

  (testing "Missing parameter start"
    (is (= (app (mock/request :post "/statement" {:account 1
                                                  :end "24/12"}))
           {:status 422 :headers {} :body "Missing parameter: start"})))

  (testing "Missing parameter end"
    (is (= (app (mock/request :post "/statement" {:start "10/10"
                                                  :account 1}))
           {:status 422 :headers {} :body "Missing parameter: end"})))

  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {}))
           {:status 422 :headers {} :body "Missing parameter: account"}))))


;;;; DEBT PERIODS TEST
(deftest debt-periods-server-test
  (reset! ops {})
  (let [ops [{:account 1 :description "Credit" :amount 175.34 :date "01/01"}
             {:account 1 :description "Debit" :amount -200.00 :date "03/01"}
             {:account 1 :description "Salary" :amount 50173.48 :date "10/01"}

             {:account 2 :description "Salary" :amount 6837.56 :date "10/01"}
             {:account 2 :description "Purchase" :amount -200.23 :date "14/01"}
             {:account 2 :description "Purchase" :amount -31.23 :date "14/01"}
             {:account 2 :description "Debit" :amount -1123.00 :date "02/02"}

             {:account 3 :description "Debit" :amount -1520.00 :date "05/01"}
             {:account 3 :description "Purchase" :amount -154.00 :date "19/01"}
             {:account 3 :description "Debit" :amount -140.33 :date "22/02"}]]
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (testing "Account number 1"
      (let [response (app (mock/request :post "/debt" {:account 1}))
            result {"debts"
                    [{"start" "03/01", "end" "09/01", "principal" -24.65}]}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Account number 2"
      (let [response (app (mock/request :post "/debt" {:account 2}))
            result {"debts" []}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Account number 3"
      (let [response (app (mock/request :post "/debt" {:account 3}))
            result {"debts"
                    [{"start" "22/02", "principal" -1814.33}
                     {"start" "19/01", "end" "21/02", "principal" -1674.00}
                     {"start" "05/01", "end" "18/01", "principal" -1520.00}]}
            body (json/read-str (:body response))]
        (is (= body result))))))

(deftest debt-periods-missing-parameters-test
  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/debt" {}))
           {:status 422 :headers {} :body "Missing parameter: account"}))))
