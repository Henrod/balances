(ns balances.util
  (:require [clj-time.core :as t]
            [clj-time.format :as f])
  (:import (org.joda.time DateTime)))

(def formatter (f/formatter "dd/MM"))

(defn string->date
  [str]
  (f/parse formatter str))

(defn date->string
  [date]
  (f/unparse formatter date))

(defn previous-day
  [str]
  (date->string (t/minus (string->date str) (t/days 1))))

(defn next-day
  [str]
  (t/plus (string->date str) (t/days 1)))

(defn within?
  [start-date end-date]
  (let [start-date# (string->date start-date) end-date# (next-day end-date)]
    (fn [current-date]
      (t/within? (t/interval start-date# end-date#) current-date))))

(defn after?
  [date]
  (cond
    (instance? String date) #(t/after? (string->date date) %)
    (instance? DateTime date) #(t/after? date %)))

(defn abs
  [n]
  (if (number? n)
    (max n (- n))
    (throw (Exception. "Only numbers can have an absolute value"))))

(defn to-float
  [number]
  (if (string? number)
    (Float. number)
    (float number)))