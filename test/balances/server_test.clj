(ns balances.server-test
  (:require [clojure.test :refer [is deftest testing]]
            [balances.server :refer [ops app]]
            [balances.core :refer [build]]
            [ring.mock.request :as mock]))

(deftest new-operation-test
  (reset! ops {})

  (testing "Add first operation of Credit"
    (let [result {"1" [(build "Credit" "100.0" "15/10")]}
          response (app (mock/request :post "/new" {"account" 1, "description" "Credit",
                                                    "date" "15/10", "amount" 100.0}))]
      (is (= @ops result))
      (is (:status response) 200)))

  (testing "Get balance from this operation"
    (let [result 100.0
          response (app (mock/request :get "/balance" {"account" 1}))]
      (is (= response
             {:status 200
              :headers {}
              :body result})))))