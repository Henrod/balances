(ns balances.lib.core
  "Core functions for the bank operations.
  All functions are pure."
  (:require [balances.lib.util :as u]))

;;;; Record definition
(defrecord ^:private Operation [description amount]
  Object
  (toString [_]
    (str description " " (u/to-format (u/abs amount)))))

(defn build
  "Constructs an Operation instance from description and amount.
  Description is a String describing this operation.
  Amount is a number or a string of a number that is converted to double with
  two decimal places to represent an amount of money."
  [description amount]
  {:pre [(u/validate-description description) (u/validate-amount amount)]}
  (Operation. description (bigdec amount)))

(defrecord ^:private Account [current operations])

(defn build-ac
  "Constructs an Account instance from current balance and operations.
  Current is the current balance of this an account. It is a BigDecimal.
  Operations is a sorted-map from date to vector of Operations."
  [current operations]
  {:pre [(map? operations) (sorted? operations) (decimal? current)]}
  (Account. current operations))


;;;; Helper function
(defn- compute-balances
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
                sum-ams (fn [ve v] (conj ve (+ v (last ve))))]
            (zipmap (keys operations) (reduce sum-ams [amount] ams)))
          {})))


;;;; Available functions
(defn new-operation
  "Takes the map ops and a map with account, description, amount and date.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Description is a string different than nil and not empty.
  Amount can be a number or a string of a number and must be different than
   zero. Actually, its absolute value must be greater or equal than 0.01.
  Date is a string representing a date in the format dd/MM. There can only be
   valid months and valid days for the specific month.
  Returns ops updated."
  [ops {:keys [account description amount date]}]
  {:pre [(u/validate-description description) (u/validate-account account)
         (u/validate-date date) (u/validate-amount amount) (map? ops)]
   :post [(map? %)]}
  (let [date# (u/str->date date) amount# (bigdec amount)]
    (if (contains? ops account)
      (-> (update-in ops [account :current] #(+ % amount#))
          (update-in [account :operations date#]
                     (comp vec conj) (Operation. description amount#)))
      (assoc ops
        account
        (Account. amount# (sorted-map date# [(Operation. description amount#)]))))))

(defn current-balance
  "Current balance of an account.
  It's the sum of the amounts of all operations since the beginning.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Returns a map from :balance to the string current balance
  Returns {:balance nil} if account does not exist."
  [ops account]
  {:pre [(u/validate-account account) (map? ops)]
   :post [(map? %)]}
  {:balance (if (contains? ops account)
              (-> (ops account) :current u/to-format))})

(defn bank-statement
  "Log of operations of an account between two dates.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Start-date and end-date are both strings of dates in the format dd/MM. They
   must be valid dates and end-date must be after start-date.
  Returns a map from :statement to vector ordered by date of maps. These maps
   have :date, :balance and :operations.
  The vector is empty if account doesn't exist."
  [ops account start-date end-date]
  {:pre [(u/validate-account account) (u/validate-date start-date end-date)
         (map? ops)]
   :post [(map? %)]}
  {:statement
   (let [within?      (u/make-within start-date end-date)
         operations   (get-in ops [account :operations])
         dated-ops    (select-keys operations (filter within? (keys operations)))
         each-balance (compute-balances operations)]
     (reduce
       (fn [a [k v]]
         (conj a {:date       (u/date->str k)
                  :operations (map str v)
                  :balance    (u/to-format (each-balance k))}))
       [] dated-ops))})

(defn debt-periods
  "Map with a vector of periods of negative balance of an account.
  Each element of the vector is a map with start date, negative balance and
   end date if the current-balance is non negative.
  Ops is the map of operations.
  Account is an identifier different than nil and not empty.
  Returns a map from :debts to a vector of maps. These maps have :start,
  :principal and, if current-balance is non negative, :end.
  The vector is empty if account doesn't exist."
  [ops account]
  {:pre [(u/validate-account account) (map? ops)]
   :post [(map? %)]}
  {:debts
   (let
     [operations (get-in ops [account :operations])
      every-2 (partition 2 1 (repeat nil) (compute-balances operations))
      sel (fn [a [[curr-date curr-bal] [next-date  ]]]
            (let [l (last a)
                  massoc #(if next-date (assoc % :end (u/previous-day next-date))
                                        (dissoc % :end))
                  plateau? (and (u/equal-decs curr-bal (:principal l))
                                (= (:end l) (u/previous-day curr-date)))]
              (cond
                plateau?        (conj (pop a) (massoc l))
                (neg? curr-bal) (conj a (massoc
                                          {:start     (u/date->str curr-date)
                                           :principal (u/to-format curr-bal)}))
                :else a)))]
     (reduce sel [] every-2))})