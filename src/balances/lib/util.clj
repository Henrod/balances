(ns balances.lib.util
  "Date functions and other simple and generic helper functions.
  All functions are pure"
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

;;;; Date functions
(def ^:private formatter (f/formatter "dd/MM"))

(defn str->date
  "Constructs a Date from a String. Its format must be dd/MM"
  [str]
  (f/parse formatter str))

(defn date->str
  "Constructs a String from a Date. The string's format is dd/MM"
  [date]
  (f/unparse formatter date))

(defn previous-day
  "Gets a date and returns a string for the previous day"
  [date]
  (date->str (t/minus date (t/days 1))))

(defn next-day
  "Gets a string date and returns a Date for the next day"
  [str]
  (t/plus (str->date str) (t/days 1)))

(defn make-within
  "Constructs a predicate that returns true if a Date is between start-date
  and end-date"
  [start-date end-date]
  (let [start-date# (str->date start-date) end-date# (next-day end-date)]
    (fn [current-date]
      (t/within? (t/interval start-date# end-date#) current-date))))


;; Number functions
(defn abs
  "Absolute value of a number"
  [n]
  (if (number? n)
    (max n (- n))
    (throw (IllegalArgumentException.
             "Only numbers can have an absolute value"))))

(defn to-format
  "Converts a number or the string of a number to number with two decimal
  places"
  [number]
  (format "%.2f" number))

(defn equal-decs
  "Compares big decimal to string"
  [a b]
  {:pre [(decimal? a) (or (nil? b) (string? b))]}
  (if b
    (= (to-format a) b)))


;; Validation
(defn- invalid
  [^String msg]
  (throw (IllegalArgumentException. msg)))

(defn validate-amount
  [amount]
  (let [msg1 "Missing parameter: amount"
        msg2 "Empty parameter: amount"
        msg3 "Error: only numbers are acceptable in amount"
        msg4 "Error: only two decimal places allowed"
        msg5 "Error: transaction must be different than zero"
        amount# (str amount)
        pat-decimal #"^\-?\d*\.?\d{0,2}$"
        pat-number #"^\-?\d+\.?\d*|\-?\d*\.?\d+$"]
    (cond
      (nil? amount) (invalid msg1)
      (and (string? amount) (empty? amount)) (invalid msg2)
      (not= amount# (re-find pat-number amount#)) (invalid msg3)
      (not= amount# (re-find pat-decimal amount#)) (invalid msg4)
      (= 0M (bigdec amount)) (invalid msg5)
      :else true)))

(defn validate-date
  ([str]
   (cond
     (nil? str)   (invalid "Missing parameter: date")
     (empty? str) (invalid "Empty parameter: date")
     :else (str->date str)))
  ([str-start str-end]
   (cond
     (nil? str-start) (invalid "Missing parameter: start")
     (nil? str-end)   (invalid "Missing parameter: end")
     (empty? str-start) (invalid "Empty parameter: start")
     (empty? str-end)   (invalid "Empty parameter: end")
     (t/after? (str->date str-start) (str->date str-end))
      (invalid "Error: End date must be after Start date")
     :else true)))

(defn validate-account
  [account]
  (cond
    (nil? account) (invalid "Missing parameter: account")
    (and (string? account) (empty? account)) (invalid "Empty parameter: account")
    :else true))

(defn validate-description
  [description]
  (cond
    (nil? description)   (invalid "Missing parameter: description")
    (empty? description) (invalid "Empty parameter: description")
    :else true))