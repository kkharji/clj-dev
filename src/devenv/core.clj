(ns devenv.core
  (:require [clojure.test :refer [are run-tests]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]
            [integrant.repl.state]
            [integrant.repl :as igr]
            [duct.core :as duct]
            [potemkin :as potemkin]
            [hawk.core :as hawk]
            [devenv.state :as s :refer [alt]]))

;; re-export (system & config) intergrant state
(declare system config start watch pause)
(potemkin/import-vars
 [integrant.repl.state system config])

;; Stop this namespace from being reloaded.
(repl/disable-reload!
 (find-ns 'devenv.core))

(defn ^:private notify [key]
  (when key
    (when-let [t (some-> (s/config :watch/timestamp)
                         (java.text.SimpleDateFormat.)
                         (.format (java.util.Date.)))]
      (println key t))))

(defn ^:private ig-prep-fn []
  (let [resource (-> :integrant/file-path s/config io/resource)]
    (if s/duct?
      (-> resource
          (duct/read-config)
          (duct/prep-config (s/config :integrant/profiles)))
      (slurp resource))))

(defn init
  "Process user options and prepare dev environment:
  if no configuration is given, then defaults will be used."
  {:test #(are [lhs rhs] [lhs (some? (get (init rhs) (first lhs)))]
            [:env/integrant? true] {:integrant/file-path "..."}
            [:env/duct? true] {:integrant/with-duct? true :integrant/file-path "...."}
            [:env/duct? false] {:integrant/with-duct? true}
            [:watch/paths true] nil)}
  ([] (init nil))
  ([config]
   (let [{:keys [env/paths]} (s/set-state! config)]
     (when s/integrant? (igr/set-prep! ig-prep-fn))
     (apply repl/set-refresh-dirs paths)
     (notify :environment/initialized!)
     (when s/start-on-init? (start))
     s/config)))

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
   (if-not s/started?
     (do (when-not s/initialized? (init))
         (when s/duct? (duct/load-hierarchy))
         (repl/refresh-all)
         (when s/integrant? (igr/init) (igr/go))
         (some-> (io/resource "local.clj") (load))
         (s/toggle-start!)
         (when start-watch? (watch :start))
         (notify msg-key))
     (notify :environment/already-started!))))

(defn stop
  "Stop development environment. clear state"
  ([]
   (stop :environment/stoped! true))
  ([stop-watch?]
   (stop :environment/stoped! stop-watch?))
  ([msg-key stop-watch?]
   (let [watch? (and stop-watch? s/watching?)]
     (if s/started?
       (do (when s/integrant? (igr/halt))
           (when watch? (watch :stop))
           (s/toggle-start!)
           (repl/clear)
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
   (if s/started?
     (do (when s/integrant? (igr/suspend))
         (alt s/paused? true)
         (when msg-key (notify msg-key)))
     (notify :environment/run-start-first))))

(defn resume
  "Resume after pausing environment."
  ([]
   (resume :environment/resumed!))
  ([msg-key]
   (if s/paused?
     (do (when s/integrant? (igr/resume))
         (when msg-key (notify msg-key)))
     (notify :environment/run-pause-first))))

(defn refresh
  "Runs pause and then resume, while in the same running refreshing files with
  clojrue.tools.namespace/refresh."
  []
  (if s/started?
    (do (pause nil)
        (repl/refresh)
        (resume :environment/refreshed!))
    (notify :environment/run-start-first)))

(defn ^:private on-change []
  (pause :watch/reload!)
  (repl/refresh :after 'devenv.core/resume))

(defn watch
  "Start/Stop hot-reloading. To stop, pass :stop to watch"
  ([] (watch :start))
  ([op]
   (let [[start? stop?] [(= op :start) (= op :stop)]
         watching? s/watching?
         start (and start? (not watching?))
         stop  (and stop? watching?)]
     (cond
       start (do (when-not s/started? (start))
                 (s/set-watch-handle! on-change)
                 (notify :environment/watching!))
       stop  (do (hawk/stop! s/watch-handle)
                 (s/set-watch-handle! nil)
                 (notify :environment/stopped-watching!))
       :else (notify
              (if start?
                :environment/already-watching!!
                :environment/no-watching-process))))))

(run-tests)
