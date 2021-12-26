(ns devenv.core
  (:require [clojure.test :refer [are run-tests]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [integrant.repl.state]
            [integrant.repl :as ig-repl]
            [duct.core :as duct]
            [potemkin :as potemkin]
            [hawk.core :as hawk]))

;; re-export (system & config) intergrant state
(declare system config start watch pause)
(potemkin/import-vars
 [integrant.repl.state system config])

;; Stop this namespace from being reloaded.
(tn/disable-reload! (find-ns 'devenv.core))

;; Set Initial State
(def ^:private state
  (atom
   {:integrant/profiles nil
    :integrant/file-path nil
    :integrant/with-duct? false
    :integrant/initialized? false

    :watch/paths nil
    :watch/handle nil
    :watch/formatter nil
    :watch/pattern #"[^.].*(\.clj|\.edn)$"
    :watch/timestamp "[hh:mm:ss]"

    :env/first-start? true
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
    (println key (.format formatter (java.util.Date.)))))

(defn ^:private ig-prep-fn []
  (let [{:integrant/keys [file-path profiles] :env/keys [duct?]} @state
        resource (io/resource file-path)]
    (if duct?
      (-> resource
          (duct/read-config)
          (duct/prep-config profiles))
      (slurp resource))))

(defn ^:private get-watch-handler [pattern paths]
  (letfn [(filter [_ {:keys [file]}]
            (re-matches pattern (.getName file)))
          (on-change []
            (pause false :watch/reload!)
            (tn/refresh :after 'devenv.core/resume))
          (handler [ctx _]
            (binding [*ns* *ns*] (on-change) ctx))]
    (hawk/watch! [{:paths paths :filter filter :handler handler}])))

(def ^:private update-watch-handle
  (partial swap! state assoc :watch/handle))

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
          :env/keys [start-on-init?] :as s} (merge-state! config)
         integrant? (some? file-path)]
     (merge-state!
      {:dev/integrant? integrant?
       :dev/duct? (and with-duct? integrant?)
       :dev/local-clj (io/resource "local.clj")
       :watch/paths #(or (:watch/paths s) (:env/paths s))
       :watch/formatter (get-time-formatter (s :watch/timestamp))})
     (when integrant? (ig-repl/set-prep! ig-prep-fn))
     (when start-on-init? (start))
     (notify :environment/initialized)
     (apply tn/set-refresh-dirs (:env/paths s)))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - duct?, read configuration and prepare with profiles
  - when local.clj execute it "
  []
  (when-not (:env/started? @state)
    (let [{:env/keys [duct? integrant? local-clj load-runtime? first-start?]} @state]
      (when duct? (duct/load-hierarchy))
      (when (and load-runtime? first-start?) (tn/refresh-all))
      (when integrant? (ig-repl/init) (ig-repl/go))
      (when local-clj (load local-clj))
      (swap! state merge {:env/started? true :env/first-start? false})
      (notify :environment/started!))))

(defn pause
  "Pause development environment
  - if watch is enabled, stop watching for changes
  - if integrant/duct then suspend system."
  ([] (pause true :environment/paused!))
  ([keep-watching? msg-key]
   (let [{:env/keys [integrant? started?]} @state]
     (when started?
       (when-not keep-watching? (watch :stop))
       (when integrant? (ig-repl/suspend))
       (swap! state assoc :env/started? false)
       (notify msg-key)))))

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
  "Start/Stop hot-reloading. To stop, pass :stop to watch"
  ([] (watch :start))
  ([op]
   (let [{:watch/keys [handle pattern paths] :env/keys [started?]} @state
         [start? stop?] [(= op :start) (= op :stop)]
         case* (cond (and handle stop?) :stop
                     (and (not handle) start?) :start
                     (and (not handle) stop?) :environment/no-watching-process
                     (and handle start?) :environment/already-watching!!)]
     (case case*
       :start (do (when-not started? (start))
                  (update-watch-handle (get-watch-handler pattern paths))
                  (notify :environment/watching!))
       :stop  (do (hawk/stop! handle)
                  (update-watch-handle nil)
                  (notify :environment/stopped-watching!))
       (notify case*)))))

(run-tests)
