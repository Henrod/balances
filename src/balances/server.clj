(ns balances.server
  "Server functions.
  A state variable is made necessary to hold all past operations."
  (:require
    [compojure.route :as route]
    [ring.middleware.reload :as reload]
    [ring.util.response :as res]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.json-response :refer [wrap-json-response]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.adapter.jetty :refer [run-jetty]]
    [compojure.core :refer [defroutes GET POST]]
    [balances.core :refer [new-operation current-balance bank-statement
                           debt-periods]]))

(def ops
  "State variable.
  ops is defined as an atom because it needs to be synchronous and there is
  no need to be coordinated"
  (atom {}))

(defn- handler
  [func params]
  (try
    (res/response
      (let [{:keys [account start end]} params]
        (case func
          :new       (do (swap! ops new-operation params) "ok") ; side-effect: update ops
          :balance   (current-balance @ops account)
          :statement (bank-statement @ops account start end)
          :debt      (debt-periods @ops account))))
    (catch Exception e
      (res/status (-> e .getMessage res/response) 422))))

(defroutes app-routes
           (POST "/new"       [& params] (handler :new       params))
           (POST "/balance"   [& params] (handler :balance   params))
           (POST "/statement" [& params] (handler :statement params))
           (POST "/debt"      [& params] (handler :debt      params))
           (route/not-found "Not found"))

(def app
  (-> app-routes
      (reload/wrap-reload)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-response)))

(defn -main []
  (run-jetty app {:port 3000}))