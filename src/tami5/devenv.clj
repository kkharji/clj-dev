(ns tami5.devenv)

(def ^:private state
  (atom
   {:integrant/profiles nil
    :integrant/file-path nil
    :integrant/prep nil
    :integrant/with-duct? false

    :watch/active? false
    :watch/dirs nil
    :watch/handle nil
    :watch/pattern #"[^.].*(\.clj|\.edn)$"
    :watch/timestamp "[hh:mm:ss]"

    :env/paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
    :env/duct? false
    :env/integrant? false
    :env/local-clj? false
    :env/started? false}))

(defn init
  "Process user options and prepare dev environment:
  if no configuration is given, then defaults will be used."
  ([])
  ([config]))

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
