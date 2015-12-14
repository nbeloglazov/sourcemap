(ns sourcemap.core
  (:require [goog.dom :as gdom]
            [om.core :as om]
            [om.dom :as dom]
            [dommy.core :as d]
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
       #(.appendChild js/document.body (build-text-element %)))

(def app-state (atom {:counter 0}))

(defn Counter [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil (:counter data)
               (dom/button #js {:onClick #(om/transact! data :counter inc)}
                           "+1")))))

(om/root Counter app-state
         {:target (.querySelector js/document "#app")})
