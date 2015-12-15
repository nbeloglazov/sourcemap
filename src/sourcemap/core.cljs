(ns sourcemap.core
  (:require [goog.dom :as gdom]
            [dommy.core :as d :include-macros true]
            [clojure.string :as cstr]
            [goog.labs.net.xhr :as xhr]))

(enable-console-print!)

(defn build-text-element [text]
  (letfn [(build-line [line]
            (let [element (d/create-element :p)
                  char-els (for [i (range (count line))]
                             (doto (d/create-element :span)
                                   (d/set-text! (nth line i))
                                   (d/add-class! "col" (str "col-" i))))]
              (if (empty? char-els)
                element
                (apply d/append! element char-els))))]
    (let [element (d/create-element :pre)
          lines (cstr/split-lines text)
          lines-els (for [i (range (count lines))
                          :let [line (nth lines i)
                                line-el (build-line line)]]
                      (d/add-class! (build-line (nth lines i))
                                    "row" (str "row-" i)))]
      (when-not (empty? lines-els)
        (apply d/append! element lines-els))
      (d/add-class! element "code"))))

(.then (xhr/get "/source.js")
       #(d/append! (d/sel1 "#source-code") (build-text-element %)))

(.then (xhr/get "/source.min.js")
       #(d/append! (d/sel1 "#minified-code") (build-text-element %)))

