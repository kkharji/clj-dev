(ns clj-dev.core
  (:require [clj-dev.state :as s :refer [alt]]
            [clj-dev.utils :as u :refer [log]]
            [clj-dev.watch :as watch]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]
            [clj-dev.integrant :as integrant]
            [potemkin :as p]))

(repl/disable-reload!
 (find-ns 'clj-dev.core))

(p/import-vars
 [integrant system config])

(declare start watch pause)

(defn init
  "Process user options and prepare dev environment:
  If no configuration is given, then use defaults."
  ([] (init nil))
  ([config]
   (when-not s/initialized?
     (let [{:keys [start-on-init? paths]} (s/set-state! config)]
       (if s/integrant? (integrant/init))
       (u/set-refresh-dirs paths)
       (log :initialized!)
       (when start-on-init? (start))))))

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
   (if (or (not s/started?) s/initialized?)
     (do (repl/refresh-all)
         (when s/integrant? (integrant/start))
         (when (io/resource "local.clj") (load "local"))
         (alt s/started? true)
         (when start-watch? (watch))
         (log msg))
     (log (if s/started?
            :already-started!
            :run-init-first)))))

(defn stop
  "Stop development environment. clear state"
  ([] (stop :stoped! true))
  ([stop-watch] (stop :stoped! stop-watch))
  ([msg stop-watch]
   (let [should-watch (and stop-watch s/watching?)]
     (if s/started?
       (do (when s/integrant? (integrant/stop))
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
     (do (when s/integrant? (integrant/pause))
         (alt s/paused? true)
         (log msg))
     (log :run-start-first))))

(defn resume
  "Resume after pausing environment."
  ([] (resume :resumed!))
  ([msg]
   (if s/paused?
     (do (when s/integrant? (integrant/resume))
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
   (let [start? (= operation :start)
         stop? (not start?)
         valid? (if start? (not s/watching?) s/watching?)]
     (cond (and start? valid?) (watch/start on-change)
           (and stop? valid?)  (watch/stop)
           :else (case operation
                   :start (log :already-watching!!)
                   :stop  (log :no-watching-process))))))

(def go start)
(def halt stop)
(def halt! stop)
(def suspend pause)
(def reset-all restart)
(def reset refresh)
