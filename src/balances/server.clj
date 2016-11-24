(ns balances.server
  (:require
    [compojure.route :as route]
    [ring.middleware.reload :as reload]
    [ring.util.response :as res]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.json-response :refer [wrap-json-response]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.adapter.jetty :refer [run-jetty]]
    [compojure.core :refer [defroutes GET POST]]
    [balances.core :refer [new-operation current-balance bank-statement debt-periods]]))

;; Atom: synchronous and no need for coordinated (refs are coordinated)
;; TODO: transformar isso no doc da variavel ops
(def ops (atom {}))

(defn- validate
  [m type]
  (let [fields {:balance [:account], :new [:account :amount :description :date],
                :debt [:account],    :statement [:account :start :end]}]
    (->> type fields (drop-while m) first)))

(defn- handler
  [func params]
  (if-let [miss (validate params (keyword func))]
    (res/status (res/response (str "Missing parameter: " (name miss))) 422)
    (let [{:keys [account start end]} params]
      (res/response
        (case func
          "balance"   (str (current-balance @ops account))
          "statement" (bank-statement @ops account start end)
          "debt"      {:debts (debt-periods @ops account)}
          "new"       (do (swap! ops new-operation params) "ok")
          (res/not-found "Not found"))))))

; Hypothesis: client is authenticated in and using HTTPS
(defroutes app-routes
           (POST "/:func" [func & params] (handler func params))
           (route/not-found "Not found"))

(def app
  (-> app-routes
      (reload/wrap-reload)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-response)))

(defn -main []
  (run-jetty app {:port 3000}))