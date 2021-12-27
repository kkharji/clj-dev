(ns clj-dev.state
  (:require [clojure.tools.namespace.repl :as tn]))

(defmacro alt [root-var fun]
  `(let [fun# (if (fn? ~fun) ~fun (constantly ~fun))]
     (alter-var-root (var ~root-var) fun#)))

(tn/disable-reload! (find-ns 'clj-dev.state))

(def start-on-init? false)
(def duct? false)
(def integrant? false)
(def initialized? false)
(def paused? false)
(def started? false)
(def watching? false)

(def config
  {:paths ["src" "test" "dev" "resources" "dev/src" "dev/resources"]
   :start-on-init? false

   :watch-paths nil
   :watch-pattern #"[^.].*(\.clj|\.edn)$"
   :watch-timestamp "[hh:mm:ss]"

   :integrant-profiles nil
   :integrant-file-path nil
   :integrant-with-duct? false})

(defn set-state! [changs]
  (let [{:keys [paths integrant-file-path integrant-with-duct?]
         :as c} (merge config changs)
        fix-paths #(mapv (fn [p] (str p "/")) (or % paths))]
    (alt config (update c :watch-paths fix-paths))
    (alt integrant? (some? integrant-file-path))
    (alt duct? (and integrant? integrant-with-duct?))
    (alt initialized? true)
    (alt start-on-init? (c :start-on-init?))
    config))

(def watch-handle nil)

(defn set-watch-handle! [callback]
  (alt watch-handle callback)
  (alt watching? (some? callback)))

