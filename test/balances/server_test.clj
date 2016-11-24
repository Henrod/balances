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

(deftest new-operation-server-test
  (reset! ops {})

  (testing "Add first operation of Credit"
    (let [result {"1" [(build "Credit" "100.0" "15/10")]}
          response (app (mock/request :post "/new" {:account 1, :description "Credit",
                                                    :date "15/10", :amount 100.0}))]
      (is (= @ops result))
      (is (:status response) 200)))

  (testing "Get balance from this operation"
    (let [result {:status 200 :headers {} :body "100.0"}
          response (app (mock/request :post "/balance" {:account 1}))]
      (is (= response result)))))