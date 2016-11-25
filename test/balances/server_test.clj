(ns balances.server-test
  (:require [clojure.test :refer [is deftest testing]]
            [balances.server :refer [ops app]]
            [balances.core :refer [build]]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]))

(defn- opp
  "Operation params: build a map more succinctly"
  [account description amount date]
  {:account account :description description :amount amount :date date})


(deftest invalid-address-test
  (is (= (app (mock/request :get "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}}))
  (is (= (app (mock/request :post "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}})))


;;;; NEW OPERATION TEST
(deftest one-new-operation-test
  (reset! ops {})
  (let [result {"1" [(build "Credit" "100.00" "15/10")]}
        response (app (mock/request :post "/new" (opp 1 "Credit" 100.0 "15/10")))]
    (is (= result @ops))
    (is (= response {:status 200, :headers {}, :body "ok"}))))


(deftest two-new-operations-test
  (reset! ops {})
  (let [result {"1" [(build "Debit"  -120.00 "16/10")
                     (build "Credit" 100.00 "15/10")]}
        response1 (app (mock/request :post "/new" (opp 1 "Credit" 100.0 "15/10")))
        response2 (app (mock/request :post "/new" (opp 1 "Debit" -120.0 "16/10")))]
    (is (= @ops result))
    (is (= response1 response2 {:status 200, :headers {}, :body "ok"}))))


(deftest new-operation-missing-parameters-test
  (let [m (opp 1 "Credit" 100.0 "15/10")]
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
  (let [result {:status 200, :headers {}, :body 100.00}
        _ (app (mock/request :post "/new" (opp 1 "Credit" 100.0 "15/10" )))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))

(deftest current-balance-from-one-debit-operation-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body -120.00}
        _ (app (mock/request :post "/new" (opp 1 "Credit" -120.0 "15/10" )))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))


(deftest current-balance-from-multiple-operations-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body 1201.00}
        ops [(opp 1 "Credit"  100.50  "15/10")   ;  100.50
             (opp 1 "Debit"  -120.00  "16/10" )  ; -19.50
             (opp 1 "Deposit" 220.50  "17/10" )  ;  201.00
             (opp 1 "Salary"  1000.00 "18/10" )]];  1201.00
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (is (= result (app (mock/request :post "/balance" {:account 1}))))))


(deftest current-balance-multiple-accounts-test
  (reset! ops {})
  (let [result1 {:status 200, :headers {}, :body 1100.50}
        result2 {:status 200, :headers {}, :body -319.99}
        result3 {:status 200, :headers {}, :body 5000.75}
        result4 {:status 200, :headers {}, :body nil}
        ops [(opp 1 "Credit"    100.50    "15/10")    ; 1: 100.50
             (opp 2 "Debit"    -120.0     "15/10")     ; 2: -120.0
             (opp 2 "Purchase" -199.99    "20/10")    ; 2: -319.99
             (opp 3 "Salary"    5000.7565 "12/10") ; 3: 5000.75
             (opp 1 "Salary"    1000.0    "18/10")]]  ; 1: 1100.50
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (is (= result1 (app (mock/request :post "/balance" {:account 1}))))
    (is (= result2 (app (mock/request :post "/balance" {:account 2}))))
    (is (= result3 (app (mock/request :post "/balance" {:account 3}))))
    (is (= result4 (app (mock/request :post "/balance" {:account 4}))))))


(deftest current-balance-missing-parameters-test
  (reset! ops {})
  (app (mock/request :post "/new" (opp 1 "Salary" 1000.0 "18/10")))
  (let [result {:status 422 :headers {} :body "Missing parameter: account"}
        response (app (mock/request :post "/balance" {}))]
    (is (= result response))))


;;;; BANK STATEMENT TESTS
(deftest bank-statement-server-test
  (reset! ops {})
  (let [operations [(opp 2 "Salary"            5000.00 "05/08")
                    (opp 4 "Deposit"           1250.20 "02/08")
                    (opp 1 "Debit"            -999.99  "06/08")
                    (opp 2 "Purchase on eBay" -30.50   "01/09")
                    (opp 4 "Withdrawal"       -10.00   "01/09")
                    (opp 4 "Credit"            500.00  "25/08")
                    (opp 1 "Deposit from Ann"  1200.00 "16/09")
                    (opp 1 "Purchase a burger"-21.99   "16/09")
                    (opp 2 "Salary"            5000.00 "05/09")]]
    (doseq [op operations] (app (mock/request :post "/new" op)))

    (testing "Bank statement for account 1 from 06/08 to 16/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "06/08", :end "16/09"}))
            result {"06/08" {"balance" -999.99
                             "operations" ["- Debit 999.99"]}
                    "16/09" {"balance" 178.02
                             "operations" ["- Purchase a burger 21.99"
                                          "- Deposit from Ann 1200.00"]}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 06/08 to 15/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1, :start "06/08", :end "15/09"}))
            result {"06/08" {"balance" -999.99
                             "operations" ["- Debit 999.99"]}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 07/08 to 16/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1 :start "07/08" :end "16/09"}))
            result {"16/09" {"balance" 178.02
                             "operations" ["- Purchase a burger 21.99"
                                           "- Deposit from Ann 1200.00"]}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 1 from 07/08 to 15/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 1 :start "07/08" :end "15/09"}))
            result {}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 02/08 to 01/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4 :start "02/08" :end "01/09"}))
            result {"02/08" {"balance" 1250.20
                             "operations" ["- Deposit 1250.20"]}
                    "25/08" {"balance" 1750.20
                             "operations" ["- Credit 500.00"]}
                    "01/09" {"balance" 1740.20
                             "operations" ["- Withdrawal 10.00"]}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 10/08 to 29/08"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4 :start "10/08" :end "29/08"}))
            result {"25/08" {"balance" 1750.20
                             "operations" ["- Credit 500.00"]}}
            body (json/read-str (:body response))]
        (is (= body result))))

    (testing "Bank statement for account 4 from 20/08 to 10/09"
      (let [response (app (mock/request :post "/statement"
                                        {:account 4 :start "20/08" :end "10/09"}))
            result {"25/08" {"balance" 1750.20
                             "operations" ["- Credit 500.00"]}
                    "01/09" {"balance" 1740.20
                             "operations" ["- Withdrawal 10.00"]}}
            body (json/read-str (:body response))]
        (is (= body result))))))


(deftest bank-statement-missing-parameters-test
  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {:start "10/10" :end "24/12"}))
           {:status 422 :headers {} :body "Missing parameter: account"})))

  (testing "Missing parameter start"
    (is (= (app (mock/request :post "/statement" {:account 1 :end "24/12"}))
           {:status 422 :headers {} :body "Missing parameter: start"})))

  (testing "Missing parameter end"
    (is (= (app (mock/request :post "/statement" {:start "10/10" :account 1}))
           {:status 422 :headers {} :body "Missing parameter: end"})))

  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {}))
           {:status 422 :headers {} :body "Missing parameter: account"}))))


;;;; DEBT PERIODS TEST
(deftest debt-periods-server-test
  (reset! ops {})
  (let [ops [(opp 1 "Credit"    175.34   "01/01")
             (opp 1 "Debit"    -200.00   "03/01")
             (opp 1 "Salary"    50173.48 "10/01")
             (opp 2 "Salary"    6837.56  "10/01")
             (opp 2 "Purchase" -200.23   "14/01")
             (opp 2 "Purchase" -31.23    "14/01")
             (opp 2 "Debit"    -1123.00  "02/02")
             (opp 3 "Debit"    -1520.00  "05/01")
             (opp 3 "Purchase" -154.00   "19/01")
             (opp 3 "Debit"    -140.33   "22/02")]]
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
