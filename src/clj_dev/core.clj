(ns clj-dev.core
  (:require [clj-dev.state :as s :refer [alt]]
            [clj-dev.utils :as u :refer [log]]
            [clj-dev.watch :as watch]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]
            [duct.core :as duct]
            [integrant.repl :as igr]
            [integrant.repl.state]
            [potemkin :as potemkin]))

(repl/disable-reload! (find-ns 'clj-dev.core))
(declare system start watch pause)
(potemkin/import-vars [integrant.repl.state system config])

(defn init
  "Process user options and prepare dev environment:
  If no configuration is given, then use defaults."
  ([] (init nil))
  ([config]
   (when-not s/initialized?
     (let [paths (:paths (s/set-state! config))]
       (when s/integrant? (u/set-integrant-prep!))
       (u/set-refresh-dirs paths)
       (log :initialized!)
       (when s/start-on-init? (start))))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - duct?, read configuration and prepare with :integrant/profiles
  - when local.clj exists, execute it
  - start-watch?, start watching proceess as well"
  ([] (start :started! false))
  ([start-watch?] (start :started! start-watch?))
  ([msg start-watch?]
   (if-not s/started?
     (do (when (not s/initialized?) (init))
         (repl/refresh-all)
         (when s/duct? (duct/load-hierarchy))
         (when s/integrant? (igr/init) (igr/go))
         (some-> (io/resource "local.clj") (load))
         (alt s/started? true)
         (when start-watch? (watch :start))
         (log msg))
     (log :already-started!))))

(defn stop
  "Stop development environment. clear state"
  ([] (stop :stoped! true))
  ([stop-watch] (stop :stoped! stop-watch))
  ([msg stop-watch]
   (let [should-watch (and stop-watch s/watching?)]
     (if s/started?
       (do (when s/integrant? (igr/halt))
           (when should-watch (watch :stop))
           (log msg)
           (repl/clear)
           (alt s/started? false)
           nil)
       (log :no-env-running)))))

(defn restart
  "Restart development environment."
  ([] (restart false))
  ([restart-watch?]
   (stop nil restart-watch?)
   (start :restarted! restart-watch?)))

(defn pause
  "Pause development environment"
  ([] (pause :paused!))
  ([msg]
   (if s/started?
     (do (when s/integrant? (igr/suspend))
         (alt s/paused? true)
         (log msg))
     (log :run-start-first))))

(defn resume
  "Resume after pausing environment."
  ([] (resume :resumed!))
  ([msg]
   (if s/paused?
     (do (when s/integrant? (u/resume-integrant))
         (alt s/paused? false)
         (log msg))
     (log :run-pause-first))))

(defn refresh
  "Pause, refresh changed files and resume"
  []
  (if s/started?
    (do (pause nil)
        (repl/refresh)
        (resume :refreshed!))
    (log :run-start-first)))

(defn on-change []
  (pause :reload!)
  (repl/refresh :after 'clj-dev.core/resume))

(defn watch
  "Start/Stop hot-reloading. To stop, pass :stop to watch"
  ([] (watch :start))
  ([operation]
   (when-not s/started? (start))
   (let [[start? stop?] [(= operation :start) (= operation :stop)]
         valid? (if start? (not s/watching?) s/watching?)]
     (cond (and start? valid?) (watch/start on-change)
           (and stop? valid?)  (watch/stop)
           :else (case operation
                   :start (log :already-watching!!)
                   :stop (log :no-watching-process))))))

(def go start)
(def halt stop)
(def halt! stop)
(def suspend pause)
(def reset-all restart)
(def reset refresh)
