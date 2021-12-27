(ns devenv.utils
  (:require [devenv.state :as s]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [clojure.tools.namespace.repl :as repl]
            [integrant.repl :as igr]))

(defn- get-timestamp []
  (some-> :watch-timestamp
          (s/config)
          (java.text.SimpleDateFormat.)
          (.format (java.util.Date.))))

(defn log [key]
  (when key
    (some->>
     (get-timestamp)
     (println (keyword "environment" (name key))))))

(defn set-integrant-prep! []
  (igr/set-prep!
   #(let [resource (io/resource (s/config :integrant-file-path))]
      (if-not s/duct?
        (slurp resource)
        (-> (duct/read-config resource)
            (duct/prep-config (s/config :integrant-profiles)))))))

(defn resume-integrant []
  (try (igr/resume)
       (catch java.lang.AssertionError _)))

(def set-refresh-dirs
  (partial apply repl/set-refresh-dirs))
