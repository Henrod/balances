(ns balances.core
  "Core functions for the bank operations.
  All functions are pure."
  (:require [balances.util :as util]))

;;;; Record definition
(defrecord Operation [description amount date]
  Object
  (toString [this]
    (str description " " (format "%.2f" (util/abs amount)))))

(defn build
  "Builds a Operation instance from description, amount and date"
  [description amount date]
  {:pre [(instance? String description)]}
  (Operation. description
              (util/to-format amount)
              (util/string->date date)))


;;;; Helper function
(defn- all-date->string
  "Convert the dates of each element from a seq of Operations from Date to
  String"
  [coll]
  (map #(update % :date util/date->string) coll))

(defn- compute-balances
  "Compute the balance for each date and sort the vector by date"
  [ops account]
  {:pos [vector?]}
  (let [sum-last (fn [val arr]
                   (util/to-format
                     (+ val (if (empty? arr) 0 (-> arr last :balance)))))]
    (->> (group-by :date (ops account))
         (reduce (fn [a [k v]] (conj a {:date k :amount (apply + (map :amount v))})) [])
         (sort-by :date)
         (reduce (fn [a {:keys [date amount]}]
                   (conj a {:date (util/date->string date)
                            :balance (sum-last amount a)}))
                 []))))

(defn- compute-each-balance
  "Compute the balance for each date and return a map from date to balance"
  [ops account]
  {:pos [map?]}
  (reduce (fn [m {:keys [balance date]}]
            (assoc m date balance))
          {} (compute-balances ops account)))


;;;; Available functions
(defn new-operation
  "Returns a new map with a new Operation"
  [ops {:keys [account description amount date]}]
  {:pre [ops account description amount date]
   :pos [map?]}
  (update ops
          account
          #(conj % (build description amount date))))

(defn current-balance
  "Current balance of an account calculated from the sum of all previous
  operations"
  [ops account]
  {:pre [ops account]
   :pos [float?]}
  (if (contains? ops account)
    (->> (ops account) (map :amount) (apply +) util/to-format)))

(defn bank-statement
  "Log of operations of an account between two dates"
  [ops account start-date end-date]
  {:pre [ops account start-date end-date]
   :pos [map?]}
  (let [within? (comp (util/within? start-date end-date) :date)
        each-balance (compute-each-balance ops account)]
    (reduce
      (fn [m [k v]] (assoc m k {:operations (map str (repeat "- ") v)
                                :balance (each-balance k)}))
      {}
      (->> (ops account) (filter within?) (all-date->string) (group-by :date)))))

(defn debt-periods
  "Map with a vector of periods when the account had a negative balance. Each
   element of the vector is a map with start date, negative balance and end
   date if the current balance is non negative"
  [ops account]
  {:pre [ops account]
   :pos [map?]}
  {:debts (let [[head & tail] (drop-while (comp not neg? :balance) (compute-balances ops account))
                mconj (fn [coll & xs] (apply conj coll (filter map? xs)))]
            (if head
              (reduce
                (fn [[head# & tail#] {:keys [balance date]}]
                  (if (= balance (:principal head#))
                    (conj tail# head#)
                    (mconj tail#
                           (if (contains? head# :end) head# (assoc head# :end (util/previous-day date)))
                           (if (neg? balance) {:start date :principal balance}))))
                [{:start (:date head) :principal (:balance head)}]
                tail)
              []))})