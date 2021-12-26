(ns devenv.state
  (:require [clojure.tools.namespace.repl :as tn]
            [hawk.core :as hawk]))

(tn/disable-reload! (find-ns 'devenv.state))

;; predicts
(def start-on-init? false)
(def duct? false)
(def integrant? false)
(def initialized? false)
(def started? false)
(def paused? false)
(def watching? nil)

(defmacro alt [root-var fun]
  `(let [fun# (if (fn? ~fun) ~fun (constantly ~fun))]
     (alter-var-root (var ~root-var) fun#)))

(def watch-handle nil)

(def config
  {:integrant/profiles nil
   :integrant/file-path nil
   :integrant/with-duct? false

   :watch/paths nil
   :watch/pattern #"[^.].*(\.clj|\.edn)$"
   :watch/timestamp "[hh:mm:ss]"

   :env/paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
   :env/start-on-init? false})

(defn set-state! [changs]
  (let [{:env/keys [paths]
         :integrant/keys [file-path with-duct?]
         :as c}
        (merge config changs)]
    (alt config (update c :watch/paths #(or % paths)))
    (alt integrant? (some? file-path))
    (alt duct? (and integrant? with-duct?))
    (alt initialized? true)
    (alt start-on-init? (c :start-on-init?))
    config))

(defn toggle-start! []
  (alt started? (fn [var] (if (true? var) false true))))

(defn set-watch-handle! [on-change]
  (let [set-handle! #(do (alt watch-handle %) (alt watching? (some? %)))]
    (if-not on-change
      (set-handle! nil)
      (letfn [(filter [_ {:keys [file]}]
                (re-matches (config :pattern) (.getName file)))
              (handler [ctx _]
                (binding [*ns* *ns*] (on-change) ctx))]
        (set-handle!
         (hawk/watch!
          [{:paths (config :paths)
            :filter filter
            :handler handler}]))))))

