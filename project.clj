(defproject balances "0.1.0"
  :description "A bank simulation"
  :url "https://github.com/Henrod/balances"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :main balances.server
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.12.2"]
                 [ring "1.5.0"]
                 [compojure "1.4.0"]
                 [ring-json-response "0.2.0"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]
                                  [org.clojure/data.json "0.2.6"]]}})
