(ns clj-dev.integrant
  (:require [integrant.repl :as repl]
            [duct.core :as duct]
            [clojure.java.io :as io]
            [clj-dev.utils :as u]
            [potemkin :as p]
            [integrant.core :as ig]
            [clj-dev.state :as s]))

(p/import-vars
 [integrant.repl.state system config])

(defn- config-reader [duct?]
  (if duct? duct/read-config #(ig/read-string (slurp %))))

(defn- config-prepper [duct? profiles]
  (let [prep (if duct? duct/prep-config ig/prep)]
    #(prep % profiles)))

(defn- prep
  "Set integrant preparer and prep."
  [duct? file-path profiles]
  (let [read-config   (config-reader duct?)
        prep-config   (config-prepper duct? profiles)
        prep-function #(-> file-path io/resource read-config prep-config)]
    (repl/set-prep! prep-function)))

(defn init
  "Try to initialized integrant with the given arugments. If no
  integrant-file-path found. Ignore"
  []
  (when s/duct? (duct/load-hierarchy))
  (let [{:keys [integrant-file-path integrant-profiles]} s/config]
    (if (io/resource integrant-file-path)
      (do (prep s/duct? integrant-file-path integrant-profiles)
          (when-not s/duct? (repl/prep) (repl/init))
          (u/log :environment/initialized-integrant false))
      (u/error :resource-not-found integrant-file-path))))

(defn start []
  (when s/duct? (duct/load-hierarchy))
  (repl/go))

(def stop repl/halt)

(def pause repl/suspend)

(def resume repl/resume)

; (try (ig.repl/resume)
;      (catch java.lang.AssertionError _)))

