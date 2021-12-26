(ns devenv.core
  (:require [clojure.test :refer [are run-tests]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [integrant.repl.state :as ig-state]
            [integrant.repl :as ig-repl]
            [duct.core :as duct]
            [potemkin :as potemkin]))

;; re-export (system & config) intergrant state
(declare system config start)
(potemkin/import-vars [ig-state system config])

;; Stop this namespace from being reloaded.
(tn/disable-reload! (find-ns 'devenv.core))

;; Set Initial State
(def ^:private state
  (atom
   {:integrant/profiles nil
    :integrant/file-path nil
    :integrant/with-duct? false

    :watch/active? false
    :watch/paths nil
    :watch/handle nil
    :watch/pattern #"[^.].*(\.clj|\.edn)$"
    :watch/timestamp "[hh:mm:ss]"
    :watch/formatter nil

    :env/paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
    :env/start-on-init? false
    :env/load-runtime? false
    :env/duct? false
    :env/integrant? false
    :env/local-clj false
    :env/started? false}))

(def ^:private merge-state!
  (partial swap! state merge))

(defn ^:private get-time-formatter [timestamp]
  (when timestamp (java.text.SimpleDateFormat. timestamp)))

(defn ^:private notify [key]
  (when-let [formatter (@state :watch/formatter)]
    (->> (.format formatter (java.util.Date.))
         (format "\n%s %s" key)
         (println))))

(defn ^:private ig-prep-fn []
  (let [{:integrant/keys [file-path profiles] :env/keys [duct?]} @state
        resource (io/resource file-path)]
    (if duct?
      (-> resource
          (duct/read-config)
          (duct/prep-config profiles))
      (slurp resource))))

(defn init
  "Process user options and prepare dev environment:
  if no configuration is given, then defaults will be used."
  {:test
   #(are [lhs rhs] [lhs (some? (get (init rhs) (first lhs)))]
      [:dev/integrant? true] {:integrant/file-path "..."}
      [:dev/duct? true] {:integrant/with-duct? true :integrant/file-path "...."}
      [:dev/duct? false] {:integrant/with-duct? true}
      [:watch/paths true] nil)}
  ([] (init nil))
  ([config]
   (let [{:integrant/keys [file-path with-duct?]
          :env/keys [start-on-init?]
          :watch/keys [timestamp] :as s} (merge-state! config)]
     (merge-state!
      {:dev/integrant? (some? file-path)
       :dev/duct? (and with-duct? file-path)
       :dev/local-clj (io/resource "local.clj")
       :watch/paths #(or (:watch/paths s) (:env/paths s))
       :watch/formatter (get-time-formatter timestamp)})
     (when start-on-init?
       (start)))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - duct?, read configuration and prepare with profiles
  - when local.clj execute it "
  []
  (when-not (:env/started? @state)
    (let [{:env/keys [paths duct? integrant? local-clj load-runtime?]} @state]
      (notify :environment/starting)
      (apply tn/set-refresh-dirs paths)
      (when duct?
        (duct/load-hierarchy))
      (when load-runtime?
        (tn/refresh-all))
      (when local-clj
        (load local-clj))
      (when integrant?
        (ig-repl/set-prep! ig-prep-fn)
        (ig-repl/init)
        (ig-repl/go))
      (swap! state assoc :env/started? true)
      (notify :environment/started!))))

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
