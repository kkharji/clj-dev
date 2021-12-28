(ns clj-dev.utils
  (:require [clj-dev.state :as s]
            [clojure.pprint :as pprint]
            [clojure.tools.namespace.repl :as repl]))

(defn- get-timestamp []
  (some-> :watch-timestamp
          (s/config)
          (java.text.SimpleDateFormat.)
          (.format (java.util.Date.))))

(defn stamp
  "Print status update"
  [msg]
  (let [msg (str (get-timestamp) ": " msg " ...")
        sep (apply str (repeat (count msg) "-"))]
    (print "\n")
    (println sep)
    (println msg)
    (println sep)
    (print "\n")))

(defn log
  ([key] (log key false))
  ([key with-timestamp?]
   (let [leading (when with-timestamp? (get-timestamp))]
     (when key
       (println (str (keyword "env" (name key)) leading))))))

(def set-refresh-dirs
  (partial apply repl/set-refresh-dirs))

(defmacro spy
  "A simpler version of Timbre's spy.
  credit: https://github.com/thiru
  Returns the eval'd expression."
  [expr]
  `(let [evaled# ~expr]
     (print (str '~expr " => "))
     (pprint/pprint evaled#)
     evaled#))

(defn- logger [ns code used]
  (println (str (keyword ns (name code)) " Got: " (pr-str used))))

(def debug (partial logger "debug"))
(def error (partial logger "error"))
