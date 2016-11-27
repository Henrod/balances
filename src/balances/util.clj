(ns balances.util
  "Date functions and other simple and generic helper functions.
  All functions are pure"
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

;;;; Date functions
(def formatter (f/formatter "dd/MM"))

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

(defn within?
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
    (throw (Exception. "Only numbers can have an absolute value"))))

(defn to-format
  "Converts a number or the string of a number to double with two decimal
  places"
  [number]
  (-> (if (string? number) (Double. number) (double number))
      (* 100) long (/ 100) double))


;; Validation
(defn validate-amount
  [amount]
  (cond
    (nil? amount) (throw (Exception. "Missing parameter: amount"))
    (and (string? amount) (empty? amount))
      (throw (Exception. "Empty parameter: amount"))
    :else (let [msg "Error: transaction must be different than zero"]
            (if (= 0.0 (to-format amount)) (throw (Exception. msg)) true))))

(defn validate-date
  ([str]
   (cond
     (nil? str) (throw (Exception. "Missing parameter: date"))
     (empty? str) (throw (Exception. "Empty parameter: date"))
     :else (str->date str)))
  ([str-start str-end]
   (cond
     (nil? str-start) (throw (Exception. "Missing parameter: start"))
     (nil? str-end) (throw (Exception. "Missing parameter: end"))
     (t/after? (str->date str-start) (str->date str-end))
      (throw (Exception. "Error: End date must be after Start date"))
     :else true)))

(defn validate-account
  [account]
  (cond
    (nil? account) (throw (Exception. "Missing parameter: account"))
    (and (string? account) (empty? account))
      (throw (Exception. "Empty parameter: account"))
    :else true))

(defn validate-description
  [description]
  (cond
    (nil? description) (throw (Exception. "Missing parameter: description"))
    (empty? description) (throw (Exception. "Empty parameter: description"))
    :else true))