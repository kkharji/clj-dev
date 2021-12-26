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
    :watch/initialized? false

    :env/first-start? true
    :env/paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
    :env/start-on-init? false
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
            (pause :watch/reload!)
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
      [:env/integrant? true] {:integrant/file-path "..."}
      [:env/duct? true] {:integrant/with-duct? true :integrant/file-path "...."}
      [:env/duct? false] {:integrant/with-duct? true}
      [:watch/paths true] nil)}
  ([] (init nil))
  ([config]
   (let [{:integrant/keys [file-path with-duct?]
          :env/keys [start-on-init?] :as s} (merge-state! config)
         integrant? (some? file-path)]
     (merge-state!
      {:env/integrant? integrant?
       :env/duct? (and with-duct? integrant?)
       :env/initialized? true
       :env/local-clj (io/resource "local.clj")
       :watch/paths #(or (:watch/paths s) (:env/paths s))
       :watch/formatter (get-time-formatter (s :watch/timestamp))})
     (when integrant? (ig-repl/set-prep! ig-prep-fn))
     (apply tn/set-refresh-dirs (:env/paths s))
     (notify :environment/initialized!)
     (when start-on-init? (start)))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - duct?, read configuration and prepare with :integrant/profiles
  - when local.clj exists, execute it
  - start-watch?, start watching proceess as well"
  ([]
   (start :environment/started! false))
  ([start-watch?]
   (start :environment/started! start-watch?))
  ([msg-key start-watch?]
   (when-not (:env/started? @state)
     (when-not (:env/initialized? @state)
       (init))
     (let [{:env/keys [duct? integrant? local-clj]} @state]
       (when duct? (duct/load-hierarchy))
       (tn/refresh-all)
       (when integrant? (ig-repl/init) (ig-repl/go))
       (when local-clj (load local-clj))
       (swap! state assoc {:env/started? true})
       (when start-watch? (watch :start))
       (notify msg-key)))))

(defn stop
  "Stop development environment. clear state"
  ([]
   (stop :environment/stoped! true))
  ([stop-watch?]
   (stop :environment/stoped! stop-watch?))
  ([msg-key stop-watch?]
   (let [{:keys [env/integrant? env/started? watch/handle]} @state]
     (if started?
       (do (when integrant?  (ig-repl/halt))
           (when (and stop-watch? handle) (watch :stop))
           (swap! state assoc :env/started? false)
           (tn/clear)
           (when msg-key (notify msg-key)))
       (when msg-key (notify :environment/already-stopped!))))))

(defn restart
  "Restart development environment."
  ([]
   (restart false))
  ([restart-watch?]
   (stop nil restart-watch?)
   (start :environment/restarted! restart-watch?)))

(defn pause
  "Pause development environment"
  ([]
   (pause :environment/paused!))
  ([msg-key]
   (let [{:env/keys [integrant? started?]} @state]
     (if started?
       (do (when integrant? (ig-repl/suspend))
           (swap! state assoc :env/paused? true)
           (when msg-key (notify msg-key)))
       (notify :environment/run-start-first)))))

(defn resume
  "Resume after pausing environment."
  ([]
   (resume :environment/resumed!))
  ([msg-key]
   (let [{:keys [env/integrant? env/paused?]} @state]
     (if paused?
       (do (when integrant? (ig-repl/resume))
           (when msg-key (notify msg-key)))
       (notify :environment/run-pause-first)))))

(defn refresh
  "Runs pause and then resume, while in the same running refreshing files with
  clojrue.tools.namespace/refresh"
  []
  (if (@state :env/started?)
    (do (pause nil)
        (tn/refresh)
        (resume :environment/refreshed!))
    (notify :environment/run-start-first)))

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
