(ns balances.server
  (:require
    [compojure.route :as route]
    [ring.middleware.reload :as reload]
    [ring.util.response :as res]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.json-response :refer [wrap-json-response]]
    [ring.adapter.jetty :refer [run-jetty]]
    [compojure.core :refer [defroutes GET POST]]
    [balances.core :refer [new-operation current-balance bank-statement debt-periods]]))

;; Atom: synchronous and no need for coordinated (refs are coordinated)
;; TODO: transformar isso no doc da variavel ops
(def ops (atom {}))

(defn- validate
  [params fields]
  (first (drop-while params fields)))

(defn- handler-create
  [{params :params}]
  (if-let [miss (validate params ["account" "amount" "description" "date"])]
    (res/status (res/response (str "Missing parameter: " miss)) 422)
    (do (swap! ops new-operation params)
        (res/response "ok"))))

(defn- handler-access
  [func {:strs [account start end]}]
  (try (res/response
         (case func
           "balance"   (str (current-balance @ops account))
           "statement" (bank-statement @ops account start end)
           "debt"      {:debts (debt-periods @ops account)}
           (res/not-found "Not found")))
       (catch AssertionError e (-> (str "Missing parameter: " (.getSuppressed e))
                                   (res/response)
                                   (res/status 400)))))

; Hypothesis: client is authenticated in and using HTTPS
(defroutes app-routes
           (POST "/new" request (handler-create request))
           (POST "/:func" [func & params] (handler-access func params))
           (route/not-found "Not found"))

(def app
  (-> app-routes
      (reload/wrap-reload)
      (wrap-params)
      (wrap-json-response)))

(defn -main []
  (run-jetty app {:port 3000}))