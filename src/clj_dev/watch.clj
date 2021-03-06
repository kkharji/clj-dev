(ns clj-dev.watch
  (:require [clj-dev.state :as s :refer [config]]
            [clj-dev.utils :as u :refer [log]]
            [hawk.core :as hawk]))

(defn- filter-fn [_ {:keys [file]}]
  (-> (config :watch-pattern)
      (re-matches (.getName file))))

(defn- handler-fn [on-change]
  (fn [ctx _]
    (binding [*ns* *ns*]
      (on-change)
      ctx)))

(defn start [callback]
  (let [paths (config :watch-paths)
        handler (handler-fn callback)
        arguments {:paths paths :filter filter-fn :handler handler}]
    (s/set-watch-handle! (hawk/watch! [arguments]))
    :watch/started!))

(defn stop []
  (hawk/stop! s/watch-handle)
  (s/set-watch-handle! nil)
  :watch/stopped!)
