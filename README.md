# libtam/devenv

Library for setting up development environment (e.g. watching source code, running tests ..).
Mostly used to quickly setup my development environment with/without integrant/duct.

## Usage

### Options

```clojure
{:repl/refresh-dirs ["src" "test" "dev" "resources" "dev/src" "dev/resources"] ;; directories to refresh.
 :integrant/prep nil ;; A function that would be executed within integrant.repl/set-prep!
 :integrant/config-path nil ;; required to add custom workarounds for duct based system
 :duct/profiles nil ;; set [:dev :local] to enable duct framework.
 :watch/dirs nil ;; directories to watch for changes. default to :repl/refresh-dirs
 :watch/pattern #"[^.].*(\.clj|\.edn)$"  ;; file patterns to trigger reload on write
 :watch/timestamp "[hh:mm:ss]"} ;; Set to nil if you don't want have timestamp with each devenv action.
```

### Quick Start

#### user.clj

```clojure
(ns user
 (:require [libtam.devenv :as devenv]
           [potemkin :as potemkin]))

(potemkin/import-vars ;; make it accessible in user.clj namespace
 [devenv init start pause resume stop restart watch])

;; Initializd with defaults (only refresh-dirs and watch can be used.)
(init {})

(comment
  (start)        ;; start the development environment
  (pause)        ;; pause, only effective with integrant/duct
  (resume)       ;; resume after pausing, only effective
  (stop)         ;; stop the development environment
  (restart)      ;; restart by running stop and then start.
  (watch)        ;; start hot-reload
  (watch         ;; stop hot-reload
    :stop))
```

## License

Copyright © 2021 tami5

Distributed under the Eclipse Public License version 1.0.
