# lein-to-deps

A library to help the creation of a deps.edn file (tools.deps) from a project.clj file (Leiningen).

# Usage

From a Leiningen project, create the following namespace:

```
(ns my-lein-project.lein-to-deps
  (:require [lein-to-deps.lein-to-deps :as to-deps]
            [clojure.pprint :as pprint])
  (:import [java.io FileNotFoundException]))

(defn -main []
  (let [;; Read the raw Leiningen project.clj file into a map
        {:keys [dependencies repositories]} (to-deps/read-raw "project.clj")
        ;; Read the deps.edn file into a map
        deps-map (try (read-string (slurp "deps.edn"))
                      (catch FileNotFoundException e {}))
        ;; Update the deps.edn map with the dependencies and repositories from the project.clj map
        ;; ALso set the :paths entry of the deps.edn map to a hard coded value
        deps-map (merge
                  deps-map
                  {:paths ["src" "target/classes"]}
                  ;; lein-to-deps has two helper functions to convert from the Leiningen
                  ;; dependencies and repositores format into the tools.deps format
                  (to-deps/format-dependencies dependencies)
                  (to-deps/format-repositories repositories))]
    ;; Pretty print the result into the deps.edn file
    (binding [*print-level* nil
              *print-length* nil]
      (spit "deps.edn" (with-out-str (pprint/pprint deps-map))))))
```

Create a script at the root of the project called `lein-to-deps.sh` and execute it:

```
#!/bin/sh

clojure -Sdeps "{:deps {lein-to-deps/lein-to-deps {:git/url \"git@github.com:EwenG/lein-to-deps.git\" :sha \"36571bcc08c66a91b85e4036d609ff5a0563fb61\" :tag \"1.0.0\"}}}" -m my-lein-project.lein-to-deps
```