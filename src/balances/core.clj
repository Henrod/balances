(ns balances.core
  "Core functions for the bank operations.
  All functions are pure."
  (:require [balances.util :as u]))

;;;; Record definition
(defrecord Operation [description amount]
  Object
  (toString [this]
    (str description " " (format "%.2f" (u/abs amount)))))

(defn build
  "Builds a Operation instance from description, amount and date"
  [description amount]
  {:pre [(instance? String description)]}
  (Operation. description (u/to-format amount)))


;;;; Helper function
(defn compute-balances
  "Receive a sorted-map called operations as input.
  Compute the balance for each date."
  [operations]
  {:pos [(map? %)]}
  (letfn [(sum-last [v m] (u/to-format (+ v (if (empty? m) 0 (-> m last second)))))
          (compress [m [k v]] (assoc m k (apply + (map :amount v))))
          (build-sum [m [k v]] (assoc m k (sum-last v m)))]
    (->> (reduce compress  (sorted-map) operations)
         (reduce build-sum (sorted-map)))))


;;;; Available functions
(defn new-operation
  "Returns a new map with a new Operation"
  [ops {:keys [account description amount date]}]
  {:pre [ops (u/validate-description description) (u/validate-account account)
         (u/validate-date date) (u/validate-amount amount)]
   :pos [(map? %)]}
  (let [date# (u/str->date date)
        amount# (u/to-format amount)]
    (if (contains? ops account)
      (-> ops
          (update-in [account :current] #(+ % amount#))
          (update-in [account :operations date#] conj (build description amount)))
      (assoc ops
        account
        {:current amount#
         :operations (sorted-map date# [(build description amount#)])}))))

(defn current-balance
  "Current balance of an account calculated from the sum of all previous
  operations"
  [ops account]
  {:pre [ops (u/validate-account account)]
   :pos [(map? %)]}
  (if (contains? ops account)
    {:balance (-> (ops account) :current u/to-format)}))

(defn bank-statement
  "Log of operations of an account between two dates"
  [ops account start-date end-date]
  {:pre [ops (u/validate-account account) (u/validate-date start-date end-date)]
   :pos [(map? %)]}
  (let [within? (u/within? start-date end-date)
        operations (get-in ops [account :operations])
        dated-ops (select-keys operations (filter within? (keys operations)))
        each-balance (compute-balances operations)]
    (reduce (fn [m [k v]] (assoc m (u/date->str k) {:operations (map str v)
                                                    :balance (each-balance k)}))
      {} dated-ops)))

(defn debt-periods
  "Map with a vector of periods when the account had a negative balance.
  Each element of the vector is a map with start date, negative balance and
  end date if the current balance is non negative"
  [ops account]
  {:pre [ops (u/validate-account account)]
   :pos [(map? %)]}
  {:debts (let [mconj (fn [coll & xs] (apply conj coll (filter map? xs)))
                operations (get-in ops [account :operations])
                [head & tail] (drop-while (comp not neg? second)
                                          (compute-balances operations))]
            (if head
              (reduce
                (fn [[head# & tail#] [date balance]]
                  (if (= balance (:principal head#))
                    (conj tail# head#)
                    (mconj tail#
                           (if (contains? head# :end)
                             head#
                             (assoc head# :end (u/previous-day date)))
                           (if (neg? balance)
                             {:start (u/date->str date) :principal balance}))))
                [{:start (-> head first u/date->str) :principal (second head)}]
                tail)
              []))})