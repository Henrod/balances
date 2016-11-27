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
  "Constructs an Operation instance from description and amount.
  Description is a String describing this operation.
  Amount is a number or a string of a number that is converted to double with
  two decimal places to represent an amount of money."
  [description amount]
  {:pre [(instance? String description)]}
  (Operation. description (u/to-format amount)))


;;;; Helper function
(defn compute-balances
  "Compute the balance for each date by adding the total amount of operations
   from the current date with the balance of the previous date.
  Operations is a sorted-map from date to vector of Operations at that date.
  Returns a sorted-map from date to balance ordered by date."
  [operations]
  {:pre [(or (nil? operations) (and (sorted? operations) (map? operations)))]
   :post [(sorted? %) (map? %)]}
  (into (sorted-map)
        (if operations
          (let [compress (fn [a] (apply + (pmap :amount a)))
                [amount & ams] (pmap compress (vals operations))
                sum-ams (fn [ve v] (conj ve (u/to-format (+ v (last ve)))))
                amount# (u/to-format amount)]
            (zipmap (keys operations) (reduce sum-ams [amount#] ams)))
          {})))


;;;; Available functions
(defn new-operation
  "Takes the map ops and a map with account, description, amount and date.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Description is a string that cannot be empty.
  Amount can be a number or a string of a number and must be different than
   zero. Actually, its absolute value must be greater or equal than 0.01.
  Date is a string representing a date in the format dd/MM. There can only be
   valid months and valid days for the specific month."
  [ops {:keys [account description amount date]}]
  {:pre [ops (u/validate-description description) (u/validate-account account)
         (u/validate-date date) (u/validate-amount amount)]
   :post [(map? %)]}
  (let [date# (u/str->date date) amount# (u/to-format amount)]
    (if (contains? ops account)
      (-> (update-in ops [account :current] #(+ % amount#))
          (update-in [account :operations date#] conj (build description amount)))
      (assoc ops
        account
        {:current amount#
         :operations (sorted-map date# [(build description amount#)])}))))

(defn current-balance
  "Current balance of an account.
  It's the sum of the amounts of all operations since the beginning.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty."
  [ops account]
  {:pre [ops (u/validate-account account)]
   :post [(map? %)]}
  {:balance (if (contains? ops account)
              (-> (ops account) :current u/to-format))})

(defn bank-statement
  "Log of operations of an account between two dates.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Start-date and end-date are both strings of dates in the format dd/MM. They
   must be valid dates and end-date must be after start-date."
  [ops account start-date end-date]
  {:pre [ops (u/validate-account account) (u/validate-date start-date end-date)]
   :post [(map? %)]}
  {:statement
   (let [within? (u/within? start-date end-date)
         operations (get-in ops [account :operations])
         dated-ops (select-keys operations (filter within? (keys operations)))
         each-balance (compute-balances operations)]
     (pmap (fn [[k v]] {:date       (u/date->str k)
                        :operations (map str v)
                        :balance    (each-balance k)}) dated-ops))})

(defn debt-periods
  "Map with a vector of periods when the account had a negative balance.
  Each element of the vector is a map with start date, negative balance and
   end date if the current balance is non negative
  Ops is the map of operations.
  Account is an identifier different than nil and not empty."
  [ops account]
  {:pre [ops (u/validate-account account)]
   :post [(map? %)]}
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