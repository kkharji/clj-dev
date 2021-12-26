(ns tami5.devenv
  (:require [clojure.test :refer [are run-tests]]
            [clojure.java.io :as io]))

(def ^:private state
  (atom
   {:integrant/profiles nil
    :integrant/file-path nil
    :integrant/prep nil
    :integrant/with-duct? false

    :watch/active? false
    :watch/paths nil
    :watch/handle nil
    :watch/pattern #"[^.].*(\.clj|\.edn)$"
    :watch/timestamp "[hh:mm:ss]"

    :env/paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
    :env/duct? false
    :env/integrant? false
    :env/local-clj? false
    :env/started? false}))

(def ^:private merge-state! (partial swap! state merge))

(defn init
  "Process user options and prepare dev environment:
  if no configuration is given, then defaults will be used."
  {:test
   #(are [lhs rhs] [lhs (some? (get (init rhs) (first lhs)))]
      [:dev/integrant? true] {:integrant/file-path "..."}
      [:dev/duct? true] {:integrant/with-duct? true :integrant/file-path "...."}
      [:dev/duct? false] {:integrant/with-duct? true}
      [:integrant/prep true] {:integrant/prep (fn [] (println "prep integrant"))}
      [:watch/paths true] nil)}
  ([] (init nil))
  ([config]
   (let [s (merge-state! config)]
     (merge-state!
      {:dev/integrant? (some? (s :file-path))
       :dev/duct? (and (s :with-duct?) (s :file-path))
       :dev/local-clj? (io/resource "local.clj")
       :watch/paths #(or (:watch/paths s) (:env/paths s))}))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - integrant? and :integrant/prep, execute in integrant.core/set-prep!
  - duct?, read configuration and prepare with profiles
  - local-clj?, execute it "
  [])

(defn pause
  "Pause development environment
  - if watch is enabled, stop watching for changes
  - if integrant/duct then suspend system."
  [])

(defn resume
  "Resume after pausing environment. (if watch is enabled, then re-watch)"
  [])

(defn stop
  "Stop development environment."
  [])

(defn restart
  "Restart development environment."
  [])

(defn watch
  "Start/Stop hot-reloading"
  ([] (watch :start))
  ([op]))

(run-tests)
