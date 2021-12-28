(ns clj-dev.utils
  (:require [clj-dev.state :as s]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [clojure.tools.namespace.repl :as repl]
            [integrant.repl :as igr]
            [clojure.pprint :as pprint]))

(defn- get-timestamp []
  (some-> :watch-timestamp
          (s/config)
          (java.text.SimpleDateFormat.)
          (.format (java.util.Date.))))

(defn log
  ([key] (log key true))
  ([key with-timestamp?]
   (let [leading (when with-timestamp?
                   (str (get-timestamp)
                        " ===============================\n"))]
     (when key
       (println (str leading (keyword "environment" (name key))))))))

(defn set-integrant-prep! []
  (igr/set-prep!
   #(if-let [resource (println (io/resource (s/config :integrant-file-path)))]
      (if-not s/duct?
        (slurp resource)
        (-> (duct/read-config resource)
            (duct/prep-config (s/config :integrant-profiles)))))))

(defn resume-integrant []
  (try (igr/resume)
       (catch java.lang.AssertionError _)))

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
