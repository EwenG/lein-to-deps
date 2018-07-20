(ns lein-to-deps.lein-to-deps
  (:require [clojure.java.io :as io]
            [leiningen.core.project :as project]))

(defn- format-exclusion [exclusion]
  (if (coll? exclusion)
    (first exclusion)
    exclusion))

(defn- format-exclusions [exclusions]
  (mapv format-exclusion exclusions))

(defn- deps-reducer [deps [lib version & {:keys [exclusions classifier extension]}]]
  (assoc deps lib `{:mvn/version ~version
                    ~@(when (seq exclusions)
                        [:exclusions (format-exclusions exclusions)])
                    ~@(when (seq classifier)
                        [:classifier classifier])
                    ~@(when (seq extension)
                        [:extension extension])
                    ~@[]}))

(defn format-dependencies [dependencies]
  {:deps (reduce deps-reducer {} dependencies)})

(defn format-repository [[name v]]
  (if (string? v)
    [name {:url v}]
    [name {:url (:url v)}]))

(defn format-repositories [repositories]
  {:mvn/repos `{~@(mapcat format-repository repositories) ~@[]}})

(defmacro ^{:private true} defproject
  "Custom defproject which only read the raw project map, without adding default values"
  [project-name version & args]
  (let [f (io/file *file*)]
    `(let [args# ~(#'leiningen.core.project/unquote-project
                   (#'leiningen.core.project/argument-list->argument-map args))
           root# ~(if f (.getParent f))]
       (def ~'project args#))))

(defn read-raw
  "Custom read-raw which only read the raw project map, without adding default values"
  [source]
  (try (with-redefs [leiningen.core.project/defproject @#'defproject]
        (alter-meta! #'leiningen.core.project/defproject assoc :macro true)
         (project/read-raw source))
       (finally
         (alter-meta! #'leiningen.core.project/defproject assoc :macro true))))


(comment

  (format-dependencies '[[org.clojure/clojure "1.3.0"]
                         [org.jclouds/jclouds "1.0" :classifier "jdk15"]
                         [net.sf.ehcache/ehcache "2.3.1" :extension "pom"]
                         [log4j "1.2.15" :exclusions [[javax.mail/mail :extension "jar"]
                                                      [javax.jms/jms :classifier "*"]
                                                      com.sun.jdmk/jmxtools
                                                      com.sun.jmx/jmxri]]
                         ["net.3scale/3scale-api" "3.0.2"]
                         [org.lwjgl.lwjgl/lwjgl "2.8.5"]
                         [org.lwjgl.lwjgl/lwjgl-platform "2.8.5"
                          :classifier "natives-osx"
                          ;; LWJGL stores natives in the root of the jar; this
                          ;; :native-prefix will extract them.
                          :native-prefix ""]])

  (format-repositories [["java.net" "https://download.java.net/maven/2"]
                        ["sonatype" {:url "https://oss.sonatype.org/content/repositories/releases"
                                     ;; If a repository contains releases only setting
                                     ;; :snapshots to false will speed up dependencies.
                                     :snapshots false
                                     ;; Disable signing releases deployed to this repo.
                                     ;; (Not recommended.)
                                     :sign-releases false
                                     ;; You can also set the policies for how to handle
                                     ;; :checksum failures to :fail, :warn, or :ignore.
                                     :checksum :fail
                                     ;; How often should this repository be checked for
                                     ;; snapshot updates? (:daily, :always, or :never)
                                     :update :always
                                     ;; You can also apply them to releases only:
                                     :releases {:checksum :fail :update :always}}]
                        ;; Repositories named "snapshots" and "releases" automatically
                        ;; have their :snapshots and :releases disabled as appropriate.
                        ;; Credentials for repositories should *not* be stored
                        ;; in project.clj but in ~/.lein/credentials.clj.gpg instead,
                        ;; see `lein help deploying` under "Authentication".
                        ["snapshots" "https://blueant.com/archiva/snapshots"]
                        ["releases" {:url "https://blueant.com/archiva/internal"
                                     ;; Using :env as a value here will cause an
                                     ;; environment variable to be used based on
                                     ;; the key; in this case LEIN_PASSWORD.
                                     :username "milgrim" :password :env}]])
  )
