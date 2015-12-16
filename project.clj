(defproject sourcemap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]
                 [prismatic/dommy "1.1.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "none"
                        :source-paths ["src"]
                        :compiler {
                                   ;; Outputs main file as none.js in current directory
                                   ;; This file mainly consists of code loading other files
                                   :output-to "main.js"
                                   ;; Where all the other files are stored. This folder must
                                   ;; be accessible from your web page, as it will be loaded
                                   ;; from JavaScript
                                   :output-dir "out"
                                   :main "sourcemap.core"

                                   ;; The :none option is much faster than the other ones,
                                   ;; and is the only one to provide correct srouce-maps.
                                   :optimizations :none
                                   ;; source-maps are used by the browser to show the
                                   ;; ClojureScript code in the debugger
                                        ;                :source-map true
                                   }}]})
