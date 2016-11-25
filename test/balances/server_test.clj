(ns balances.server-test
  (:require [clojure.test :refer [is deftest testing]]
            [balances.server :refer [ops app]]
            [balances.core :refer [build]]
            [ring.mock.request :as mock]))

(deftest invalid-address-test
  (is (= (app (mock/request :get "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}}))
  (is (= (app (mock/request :port "/invalid"))
         {:status 404, :body "Not found", :headers {"Content-Type" "text/html; charset=utf-8"}})))

;;;; NEW OPERATION TEST
(deftest one-new-operation-test
  (reset! ops {})
  (let [result {"1" [(build "Credit" "100.0" "15/10")]}
        response (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount 100.0}))]
    (is (= result @ops))
    (is (= response {:status 200, :headers {}, :body "ok"}))))

(deftest two-new-operations-test
  (reset! ops {})
  (let [result {"1" [(build "Debit"  "-120.0" "16/10") (build "Credit" "100.0"  "15/10")]}
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
  (let [result {:status 200, :headers {}, :body "100.0"}
        _ (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount 100.0}))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))

(deftest current-balance-from-one-debit-operation-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body "-120.0"}
        _ (app (mock/request :post "/new" {:account 1, :description "Credit", :date "15/10", :amount -120.0}))
        response (app (mock/request :post "/balance" {:account 1}))]
    (is (= result response))))

(deftest current-balance-from-multiple-operations-test
  (reset! ops {})
  (let [result {:status 200, :headers {}, :body "1201.0"}
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
             {:account 2 :description "Debit"    :date "15 /10" :amount -120.0}   ; 2: -120.0
             {:account 2 :description "Purchase" :date "20/10"  :amount -199.99}  ; 2: -319.99
             {:account 3 :description "Salary"   :date "12/10"  :amount 5000.7565}; 3: 5000.75
             {:account 1 :description "Salary"   :date "18/10"  :amount 1000.0}]] ; 1: 1100.50
    (doseq [op ops] (app (mock/request :post "/new" op)))
    (is (= result1 (app (mock/request :post "/balance" {:account 1}))))
    (is (= result2 (app (mock/request :post "/balance" {:account 2}))))
    (is (= result3 (app (mock/request :post "/balance" {:account 3}))))))

;;;; BANK STATEMENT TESTS