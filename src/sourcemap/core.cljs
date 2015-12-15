(ns sourcemap.core
  (:require [goog.dom :as gdom]
            [dommy.core :as d :include-macros true]
            [clojure.string :as cstr]
            [goog.labs.net.xhr :as xhr]))

(enable-console-print!)

(defn get-file [file done]
  (.then (xhr/get file) done))

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

(get-file "/source.js"
          #(d/append! (d/sel1 "#source-code") (build-text-element %)))

(get-file "/source.min.js"
          #(d/append! (d/sel1 "#minified-code") (build-text-element %)))

(defn b64-char-to-num [char]
  (.indexOf "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=" char))

(defn acc-to-num [acc]
  (let [res (reduce #(+ (* %1 32) %2) (reverse acc))]
    (if (even? res)
      (bit-shift-right res 1)
      (- (bit-shift-right res 1)))))

(defn parse-vlq [str]
  (loop [acc []
         results []
         left (map b64-char-to-num str)]
    (if (empty? left)
      results
      (let [[cur & left] left
            acc (conj acc (bit-and cur 31))]
        (if (< cur 32)
          (recur [] (conj results (acc-to-num acc)) left)
          (recur acc results left))))))

(println [0,11,2,9,7,6,2,4,-9,11,7,-8,5,9])
(println (parse-vlq "AWESOMEITWORKS"))

(println [1233232, 10101313, 131223])
(println (parse-vlq "g1orCikxoTupgI"))

(get-file
 "/source.min.js.map"
 (fn [text]
   (let [sm (js->clj (.parse js/JSON text)
                     :keywordize-keys true)]
     (println sm))))

