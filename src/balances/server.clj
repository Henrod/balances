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
  (let [fields {:balance [:account] :new       [:account :amount :description :date],
                :debt    [:account] :statement [:account :start :end]}]
    (->> type fields (drop-while m) first)))

(defn- handler
  [func params]
  (if-let [miss (validate params func)]
    (res/status (res/response (str "Missing parameter: " (name miss))) 422)
    (let [{:keys [account start end]} params]
      (res/response
        (case func
          :new       (do (swap! ops new-operation params) "ok") ; side-effect: update ops
          :balance   (current-balance @ops account)
          :statement (bank-statement @ops account start end)
          :debt      (debt-periods @ops account))))))

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