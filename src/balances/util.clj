(ns balances.util
  "Date functions and other simple and generic helper functions.
  All functions are pure"
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

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
      (* 100) int (/ 100) double))