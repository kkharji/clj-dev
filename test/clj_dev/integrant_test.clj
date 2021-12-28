(ns clj-dev.integrant-test
  #_{:clj-kondo/ignore [:unused-referred-var]}
  (:require [clojure.test :refer [is are testing deftest]]
            [clj-dev.integrant :as SUT]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [clj-dev.state :as s]))

(deftest integrant-integration
  (testing "Notify user on Configuration not found"
    (s/alt s/config {:integrant-file-path "integrant_config.edn"})
    (->> (SUT/init)
         (with-out-str)
         (= ":error/resource-not-found Got: \"integrant_config.edn\"\n")
         (is)))

  (testing "integrant initialization"

    (s/alt s/config {:integrant-file-path "clj_dev/integrant_config.edn"})

    (defmethod ig/init-key :main/hello   [_ name] (str "Hello " name))
    (defmethod ig/init-key :main/handler [_ opts] #(opts :message))

    (is (= ":environment/initialized-integrant\n"

           (with-out-str (SUT/init))))
    (is (= SUT/config
           (ig/read-string (slurp (io/resource (s/config :integrant-file-path))))))

    (is (= "Hello Tokyo"
           (:main/hello SUT/system)))

    (is (= "Hello Tokyo"
           ((:main/handler SUT/system))))))
