(defproject balances "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main balances.server
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.12.2"]
                 [ring "1.5.0"]
                 [compojure "1.4.0"]
                 [ring-json-response "0.2.0"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]]}})
