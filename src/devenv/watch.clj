(ns devenv.watch
   (:require [hawk.core :as hawk]
             [clojure.tools.namespace.repl :as tn]
             [integrant.repl :as ig-repl]))

(defn- get-watch-is-valid-file? [pattern]
  (fn [_ {:keys [file]}]
    (re-matches #"[^.].*(\.clj|\.edn)$" (.getName file))))

(defn start
  "Start hot-reloading"
  []
  (hawk/))
