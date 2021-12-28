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
(defn ret [msg]
  (when msg (keyword "env" (if (string? msg) msg (name msg)))))

(defn init
  "Process user options and prepare dev environment:
  If no configuration is given, then use defaults."
  ([] (init nil))
  ([config]
   (when-not s/initialized?
     (u/stamp "Initializing Dev environment")
     (let [{:keys [start-on-init? paths]} (s/set-state! config)]
       (if s/integrant? (integrant/init))
       (u/set-refresh-dirs paths)
       (when start-on-init? (start))
       (ret :initialized!)))))

(defn start
  "Start development environment:
  - set clojure.tools.namespace.repl/refresh-dirs.
  - integrant?, run integrant.core/set-prep!
  - integrant?, read configuration in integrant.core/set-prep!
  - duct?, read configuration and prepare with :integrant/profiles
  - when local.clj exists, execute it
  - start-watch?, start watching proceess as well"
  ([] (start "started!" false))
  ([start-watch?] (start :started! start-watch?))
  ([msg start-watch?]
   (when msg (u/stamp "Starting Dev Environment"))
   (if (and (not s/started?) s/initialized?)
     (do (repl/refresh-all)
         (when s/integrant? (integrant/start))
         (when (io/resource "local.clj") (load "local"))
         (alt s/started? true)
         (when start-watch? (watch))
         (ret msg))
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
       (do (when msg (u/stamp "Stopping Dev Environment"))
           (when s/integrant? (integrant/stop))
           (when should-watch (watch :stop))
           (repl/clear)
           (alt s/started? false)
           (ret msg))
       (ret :no-running-environment)))))

(defn restart
  "Restart development environment."
  ([] (restart false))
  ([restart-watch?]
   (u/stamp "Restarting Dev Environment")
   (stop nil restart-watch?)
   (start nil restart-watch?)
   (ret :restarted!)))

(defn pause
  "Pause development environment"
  ([] (pause :paused!))
  ([msg]
   (when msg (u/stamp "Pausing Dev Environment"))
   (if s/started?
     (do (when s/integrant? (integrant/pause))
         (alt s/paused? true)
         (ret msg))
     (ret :no-env-running))))

(defn resume
  "Resume after pausing environment."
  ([] (resume :resumed!))
  ([msg]
   (when msg (u/stamp "Resuming Dev Environment"))
   (if s/paused?
     (do (when s/integrant? (integrant/resume))
         (alt s/paused? false)
         (ret msg))
     (ret :no-env-paused))))

(defn -reload*
  "internal reloading"
  ([]
   (u/stamp "Reloading Dev Environment")
   (if s/paused?
     (do (when s/integrant? (integrant/resume))
         (alt s/paused? false))
     (ret :no-env-paused))))

(defn refresh
  "Pause, refresh changed files and resume"
  []
  (u/stamp "Refreshing Dev Environment")
  (if s/started?
    (do (pause nil)
        (repl/refresh)
        (resume nil)
        (ret :refreshed!))
    (ret :no-env-running)))

(defn on-change []
  (pause nil)
  (repl/refresh :after 'clj-dev.core/-reload*)
  (println (ret :reloaded!)))

(defn watch
  "Start/Stop hot-reloading. To stop, pass :stop to watch"
  ([] (watch :start))
  ([operation]
   (when-not s/started? (start))
   (u/stamp (if (= operation :start) "Watching Dev Environment" "Killing Watching process"))
   (let [start? (= operation :start)
         stop? (not start?)
         valid? (if start? (not s/watching?) s/watching?)]
     (cond (and start? valid?) (watch/start on-change)
           (and stop? valid?)  (watch/stop)
           :else (case operation
                   :start (ret :already-watching!!)
                   :stop  (ret :no-watching-process))))))

(def go start)
(def halt stop)
(def halt! stop)
(def suspend pause)
(def reset-all restart)
(def reset refresh)
