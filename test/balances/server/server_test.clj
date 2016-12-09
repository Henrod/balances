(ns balances.server.server-test
  (:require [clojure.test :refer [is deftest testing are]]
            [balances.server.server :refer [ops app]]
            [balances.lib.core :refer [build build-ac]]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [balances.lib.util :as util]))

(defn- opp
  "Operation params: build a map more succinctly"
  [account description amount date]
  {:account account :description description :amount amount :date date})

(defn- http-resp
  "Default OK HTTP response as String"
  [body]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (if (map? body) (json/write-str body))})

(defn- date
  "Convert string to Date more succintly"
  [str]
  (util/str->date str))


(deftest invalid-address-test
  (is (= (app (mock/request :get "/invalid"))
         {:status 404
          :body "Not found"
          :headers {"Content-Type" "text/html; charset=utf-8"}}))
  (is (= (app (mock/request :post "/invalid"))
         {:status 404
          :body "Not found"
          :headers {"Content-Type" "text/html; charset=utf-8"}})))


;;;; NEW OPERATION TEST
(deftest one-new-operation-test
  (reset! ops {})
  (let [result {"1" (build-ac 100M (sorted-map
                                     (date "15/10") [(build "Credit" 100.00)]))}
        response (->> (opp 1 "Credit" 100.0 "15/10")
                      (mock/request :post "/new") app :body json/read-str)]
    (is (= result @ops))
    (is (= response {"operation" "Credit 100.00 at 15/10"}))))


(deftest four-new-operations-test
  (reset! ops {})
  (are [body op]
    (= body (-> (mock/request :post "/new" op) app :body json/read-str))
    {"operation" "Credit 100.00 at 15/10"} (opp 1 "Credit" 100.0 "15/10")
    {"operation" "Debit 124.00 at 16/10"}  (opp 1 "Debit" -124.0 "16/10")
    {"operation" "Debit 125.00 at 16/10"}  (opp 1 "Debit" -125.0 "16/10")
    {"operation" "Credit 126.00 at 16/10"} (opp 1 "Credit" 126.0 "16/10"))
  (testing "final result"
    (let [result {"1" (build-ac -23M (sorted-map
                                       (date "15/10") [(build "Credit" 100.00)]
                                       (date "16/10") [(build "Debit" -124.00)
                                                       (build "Debit" -125.00)
                                                       (build "Credit" 126.00)]))}]
      (is (= result @ops)))))


(deftest new-operation-missing-parameters-test
  (let [m (opp 1 "Credit" 100.0 "15/10")]
    (testing "Missing account parameter"
      (let [result {:status 400, :body "Missing parameter: account", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :account)))]
        (is (= result response))))

    (testing "Missing description parameter"
      (let [result {:status 400, :body "Missing parameter: description", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :description)))]
        (is (= result response))))

    (testing "Missing date parameter"
      (let [result {:status 400, :body "Missing parameter: date", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :date)))]
        (is (= result response))))

    (testing "Missing amount parameter"
      (let [result {:status 400, :body "Missing parameter: amount", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :amount)))]
        (is (= result response))))

    (testing "Missing two parameters"
      (let [result1 {:status 400, :body "Missing parameter: amount", :headers {}}
            result2 {:status 400, :body "Missing parameter: date", :headers {}}
            response (app (mock/request :post "/new" (dissoc m :amount :date)))]
        (is (or (= result1 response) (= result2 response)))))))

(deftest new-operation-empty-parameters-test
  (reset! ops {})
  (let [m (opp 1 "Credit" 100.0 "15/10")]
    (testing "empty account"
      (let [result {:status 400, :body "Empty parameter: account", :headers {}}
            response (app (mock/request :post "/new" (assoc m :account "")))]
        (is (= result response))))

    (testing "empty description"
      (let [result {:status 400, :body "Empty parameter: description", :headers {}}
            response (app (mock/request :post "/new" (assoc m :description "")))]
        (is (= result response))))

    (testing "empty amount"
      (let [result {:status 400, :body "Empty parameter: amount", :headers {}}
            response (app (mock/request :post "/new" (assoc m :amount "")))]
        (is (= result response))))

    (testing "empty date"
      (let [result {:status 400, :body "Empty parameter: date", :headers {}}
            response (app (mock/request :post "/new" (assoc m :date "")))]
        (is (= result response))))))

(deftest new-operation-invalid-dates-test
  (are [op] (= 400 (:status (app (mock/request :post "/new" op))))
            (opp 1 "Credit" 100.0 "15/13")
            (opp 1 "Credit" 100.0 "30/02")
            (opp 1 "Credit" 100.0 "31/11")
            (opp 1 "Credit" 100.0 "40/11")
            (opp 1 "Credit" 100.0 "21/11/16")
            (opp 1 "Credit" 100.0 "21-11")))

(deftest invalid-amount-test
  (testing "A word passed as amount"
    (are [op] (= 400 (:status (app (mock/request :post "/new" op))))
              (opp 1 "Credit" "Cheese" "15/13")
              (opp 1 "Credit" "" "15/13")
              (opp 1 "Credit" "." "15/13")
              (opp 1 "Credit" "-" "15/13")
              (opp 1 "Credit" "10.ab" "15/13")
              (opp 1 "Credit" "ab.10" "15/13"))))

(deftest valid-parameters-then-invalid
  (testing "this test show that invalid parameters don't invalidate nor reset
  the previous state variable"
    (reset! ops {})
    (let [result {"1" (build-ac 100M (sorted-map
                                       (date "15/10") [(build "Credit" 100.00)]))
                  "2" (build-ac 2000M (sorted-map
                                        (date "11/10") [(build "Credit" 2000)]))}]
      (app (mock/request :post "/new" (opp 1 "Credit" 100.0 "15/10"))) ; valid
      (app (mock/request :post "/new" (opp 1 "" 100.0 "15/10"))) ; invalid
      (app (mock/request :post "/new" (opp 2 "Credit" 2000 "11/10"))) ; valid
      (is (= result @ops)))))

;;;; CURRENT BALANCE TEST
(deftest current-balance-from-one-credit-operation-test
  (reset! ops {})
  (let [result (http-resp {:balance "100.00"})
        _ (app (mock/request :post "/new" (opp 1 "Credit" 100.0 "15/10")))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))


(deftest current-balance-from-one-debit-operation-test
  (reset! ops {})
  (let [result (http-resp {:balance "-120.00"})
        _ (app (mock/request :post "/new" (opp 1 "Credit" -120.0 "15/10")))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))


(deftest current-balance-from-multiple-operations-test
  (reset! ops {})
  (let [result (http-resp {:balance "1201.00"})
        operations [(opp 1 "Credit"  100.50  "15/10")  ;  100.50
                    (opp 1 "Debit"  -120.00  "16/10")  ; -19.50
                    (opp 1 "Deposit" 220.50  "17/10")  ;  201.00
                    (opp 1 "Salary"  1000.00 "18/10")]];  1201.00
    (is (every? #(= 200 (:status %))
                (for [op operations] (app (mock/request :post "/new" op)))))
    (is (= result (app (mock/request :post "/balance" {:account 1}))))))


(deftest current-balance-multiple-accounts-test
  (reset! ops {})
  (let [result1 (http-resp {:balance "1100.50"})
        result2 (http-resp {:balance "-319.99"})
        result3 (http-resp {:balance "5000.75"})
        result4 (http-resp {:balance nil})
        operations [(opp 1 "Credit"    100.50    "15/10")    ; 1: 100.50
                    (opp 2 "Debit"   "-120.0"    "15/10")    ; 2: -120.0
                    (opp 2 "Purchase" -199.99    "20/10")    ; 2: -319.99
                    (opp 3 "Salary"    5000.75 "12/10")      ; 3: 5000.75
                    (opp 1 "Salary"    1000.0    "18/10")]]  ; 1: 1100.50
    (is (every? #(= 200 (:status %))
                (for [op operations] (app (mock/request :post "/new" op)))))
    (are [result op] (= result (app (mock/request :post "/balance" op)))
                     result1 {:account 1}
                     result2 {:account 2}
                     result3 {:account 3}
                     result4 {:account 4})))


(deftest current-balance-missing-parameters-test
  (reset! ops {})
  (app (mock/request :post "/new" (opp 1 "Salary" 1000.0 "18/10")))
  (let [result {:status 400 :headers {} :body "Missing parameter: account"}
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
    (is (every? #(= 200 (:status %))
                (for [op operations] (app (mock/request :post "/new" op))))))

  (testing "Bank statement for account 1 from 06/08 to 16/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 1 :start "06/08" :end "16/09"}))
          result {"statement" [{"date" "06/08"
                               "balance" "-999.99"
                               "operations" ["Debit 999.99"]}
                              {"date" "16/09"
                               "balance" "178.02"
                               "operations" ["Deposit from Ann 1200.00"
                                             "Purchase a burger 21.99"]}]}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 1 from 06/08 to 15/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 1 :start "06/08" :end "15/09"}))
          result {"statement" [{"date" "06/08"
                               "balance" "-999.99"
                               "operations" ["Debit 999.99"]}]}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 1 from 07/08 to 16/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 1 :start "07/08" :end "16/09"}))
          result {"statement" [{"date" "16/09"
                               "balance" "178.02"
                               "operations" ["Deposit from Ann 1200.00"
                                             "Purchase a burger 21.99"]}]}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 1 from 07/08 to 15/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 1 :start "07/08" :end "15/09"}))
          result {"statement" []}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 4 from 02/08 to 01/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 4 :start "02/08" :end "01/09"}))
          result {"statement" [{"date" "02/08"
                               "balance" "1250.20"
                               "operations" ["Deposit 1250.20"]}
                              {"date" "25/08"
                               "balance" "1750.20"
                               "operations" ["Credit 500.00"]}
                              {"date" "01/09"
                               "balance" "1740.20"
                               "operations" ["Withdrawal 10.00"]}]}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 4 from 10/08 to 29/08"
    (let [response (app (mock/request :post "/statement"
                                      {:account 4 :start "10/08" :end "29/08"}))
          result {"statement" [{"date" "25/08"
                               "balance" "1750.20"
                               "operations" ["Credit 500.00"]}]}
          body (json/read-str (:body response))]
      (is (= body result))))

  (testing "Bank statement for account 4 from 20/08 to 10/09"
    (let [response (app (mock/request :post "/statement"
                                      {:account 4 :start "20/08" :end "10/09"}))
          result {"statement" [{"date" "25/08"
                               "balance" "1750.20"
                               "operations" ["Credit 500.00"]}
                              {"date" "01/09"
                               "balance" "1740.20"
                               "operations" ["Withdrawal 10.00"]}]}
          body (json/read-str (:body response))]
      (is (= body result)))))


(deftest bank-statement-missing-parameters-test
  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {:start "10/10" :end "24/12"}))
           {:status 400 :headers {} :body "Missing parameter: account"})))

  (testing "Missing parameter start"
    (is (= (app (mock/request :post "/statement" {:account 1 :end "24/12"}))
           {:status 400 :headers {} :body "Missing parameter: start"})))

  (testing "Missing parameter end"
    (is (= (app (mock/request :post "/statement" {:start "10/10" :account 1}))
           {:status 400 :headers {} :body "Missing parameter: end"})))

  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/statement" {}))
           {:status 400 :headers {} :body "Missing parameter: account"}))))

(deftest bank-statement-invalid-dates-test
  (testing "End date before Start date"
    (is (= (app (mock/request :post "/statement" {:start "20/10"
                                                  :end "10/10"
                                                  :account 1}))
           {:status 400
            :headers {}
            :body "Error: End date must be after Start date"}))))


;;;; DEBT PERIODS TEST
(deftest debt-periods-server-test
  (reset! ops {})
  (let [operations [(opp 1 "Credit"    175.34   "01/01")
                    (opp 1 "Debit"    -200.00   "03/01")
                    (opp 1 "Salary"    50173.48 "10/01")
                    (opp 2 "Salary"    6837.56  "10/01")
                    (opp 2 "Purchase" -200.23   "14/01")
                    (opp 2 "Purchase" -31.23    "14/01")
                    (opp 2 "Debit"    -1123.00  "02/02")
                    (opp 3 "Debit"    -1520.00  "05/01")
                    (opp 3 "Purchase" -154.00   "19/01")
                    (opp 3 "Debit"    -140.33   "22/02")]]
    (is (every?
          #(= 200 (:status %))
          (for [op operations] (app (mock/request :post "/new" op))))))

  (testing "Account number 1"
    (let [response (app (mock/request :post "/debt" {:account 1}))
          result {"debts"
                  [{"start" "03/01" "end" "09/01" "principal" "-24.66"}]}
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
                  [{"start" "05/01" "end" "18/01" "principal" "-1520.00"}
                   {"start" "19/01" "end" "21/02" "principal" "-1674.00"}
                   {"start" "22/02" "principal" "-1814.33"}]}
          body (json/read-str (:body response))]
      (is (= body result)))))

(deftest debt-periods-missing-parameters-test
  (testing "Missing parameter account"
    (is (= (app (mock/request :post "/debt" {}))
           {:status 400 :headers {} :body "Missing parameter: account"})))
  (testing "Empty parameter account"
    (is (= (app (mock/request :post "/debt" {:account ""}))
           {:status 400 :headers {} :body "Empty parameter: account"}))))