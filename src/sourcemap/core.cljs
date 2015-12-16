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

(println (= (parse-vlq "AWESOMEITWORKS")
            [0,11,2,9,7,6,2,4,-9,11,7,-8,5,9]))

(println (= (parse-vlq "g1orCikxoTupgI")
            [1233232, 10101313, 131223]))

(defn to-entry [sourcemap vlq]
  (let [[col src-file src-row src-col src-name] vlq]
    {:col col
     :src-file (if src-file
                 (nth (:sources sourcemap) src-file)
                 nil)
     :src-col src-col
     :src-row src-row
     :src-name (if src-name
                 (nth (:names sourcemap) src-name)
                 nil)}))

(defn parse-line [sourcemap base line]
  (loop [entries []
         base base
         entry-codes (cstr/split line #",")]
    (if (empty? entry-codes)
      [entries base]
      (let [[head & tail] entry-codes
            cur (parse-vlq head)
            entry (to-entry sourcemap (map + base cur))]
        (recur (conj entries entry)
               (mapv + base (concat cur (repeat 0)))
               tail)))))

(defn parse-entries [sourcemap]
  (loop [entries []
         ind 0
         base [0 0 0 0 0]
         lines (cstr/split (:mappings sourcemap) #";")]
    (if (empty? lines)
      entries
      (let [[head & tail] lines
            [new-entries new-base] (parse-line sourcemap base head)]
        (recur (concat entries (map #(assoc % :row ind) new-entries))
               (inc ind)
               (assoc new-base 0 0)
               tail)))))

(defn highlite-entries [entries row-fn col-fn code-el]
  (doseq [entry entries
          :let [col (col-fn entry)
                row (row-fn entry)]
          :when (and col row)]
    (println col row (.-id code-el))
    (-> code-el
        (d/sel1 (str ".row-" row " .col-" col))
        (d/add-class! "highlite"))))

(get-file
 "/source.min.js.map"
 (fn [text]
   (let [sm (js->clj (.parse js/JSON text)
                     :keywordize-keys true)
         entries (parse-entries sm)]
     (println entries)
     (highlite-entries entries :row :col (d/sel1 "#minified-code"))
     (highlite-entries entries :src-row :src-col (d/sel1 "#source-code")))))

