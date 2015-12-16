(ns sourcemap.core
  (:require [goog.dom :as gdom]
            [dommy.core :as d :include-macros true]
            [clojure.string :as cstr]
            [goog.labs.net.xhr :as xhr]
            [reagent.core :as r]))

(enable-console-print!)

(defonce state
  (r/atom {:source-code ""
           :minified-code ""}))

(defn key-by [entries fn]
  (into {} (for [entry entries]
             [(fn entry) entry])))

(defn get-file [file done]
  (.then (xhr/get file) done))

(defn line-component [row-ind row entries selected]
  [:p {:class (str "row row-" row-ind)}
   (map-indexed (fn [col char]
                  (let [entry (entries [row-ind col])
                        id (:id entry)]
                    ^{:key col}
                    [:span {:class (str "col col-" col
                                        (if entry " highlite" "")
                                        (if (= selected id) " selected" ""))
                            :data-entry id}
                     char]))
                row)])

(defn mouse-over [event]
  (let [el (.-target event)]
    (when-let [id (d/attr el :data-entry)]
      (println "setting selected" id)
      (swap! state assoc :selected-entry id))))

(defn code-component [text entries selected]
  [:pre.code {:on-mouseOver mouse-over}
   (map-indexed (fn [row-ind row]
                  ^{:key row-ind} [line-component row-ind row entries selected])
                (cstr/split-lines text))])

(defn source-code-component []
  [code-component
   (:source-code @state)
   (key-by (:entries @state) (juxt :src-row :src-col))
   (:selected-entry @state -1)])

(defn minified-code-component []
  [code-component
   (:minified-code @state)
   (key-by (:entries @state) (juxt :row :col))
   (:selected-entry @state -1)])

(r/render-component [source-code-component]
                    (d/sel1 "#source-code"))

(r/render-component [minified-code-component]
                    (d/sel1 "#minified-code"))

(get-file "/source.js"
          #(swap! state assoc :source-code %))

(get-file "/source.min.js"
          #(swap! state assoc :minified-code %))

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

(defn highlite-entry [entry source-el minified-el]
  (let [highlite (fn [row-fn col-fn code-el]
                   (-> code-el
                       (d/sel1 (str ".row-" (row-fn entry)
                                    " .col-" (col-fn entry)))
                       (d/add-class! "highlite")
                       (d/set-attr! :data-entry (:id entry))))]
    (highlite :row :col minified-el)
    (highlite :src-row :src-col source-el)))

(defn highlite-entries [entries]
  (let [source-el (d/sel1 "#source-code")
        minified-el (d/sel1 "#minified-code")]
   (doseq [entry entries
           :when (every? identity
                         (map #(% entry) [:row :col :src-row :src-col]))]
     (highlite-entry entry source-el minified-el))))

(defn index-entries [entries]
  (map-indexed #(assoc %2 :id (str %1)) entries))

(get-file
 "/source.min.js.map"
 (fn [text]
   (let [sm (js->clj (.parse js/JSON text)
                     :keywordize-keys true)
         entries (-> sm parse-entries index-entries)]
     (swap! state assoc
            :sourcemap sm
            :entries entries))))
