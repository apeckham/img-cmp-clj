(ns img-cmp-clj.core
  (:require [me.raynes.conch :refer [with-programs]]
            [hiccup.page :refer [html5 include-css include-js]])
  (:gen-class))

(defn image-size
  [path]
  (with-programs [identify]
                 (re-find #"\d+x\d+" (identify path {:throw false}))))

(defn change-prefix
  [expected prefix]
  (clojure.string/replace expected "expected/" prefix))

(defn compare-files
  [expected]
  (let [diff (change-prefix expected "diff/")
        actual (change-prefix expected "actual/")
        comparison (with-programs [compare]
                                  (compare "-metric" "AE" expected actual diff {:throw false :verbose true}))
        exit-code (-> comparison :exit-code deref)]
    {:expected      expected
     :actual        actual
     :diff          diff
     :match         (= 0 exit-code)
     :expected-size (image-size expected)
     :actual-size   (image-size actual)
     :message       (-> comparison :proc :err first)}))

(defn expected-seq
  []
  (->> (clojure.java.io/file "expected")
       file-seq
       (filter #(.isFile %))
       (map str)))

(defn compare-all
  []
  (->> (expected-seq)
       (pmap compare-files)
       (sort-by :match)))

(defn img
  [src]
  [:img.img-responsive {:src (if (.exists (clojure.java.io/as-file src)) src "missing.png")}])

(defn render-item
  [item]
  [:div
   [:h1
    (:expected item)
    ": "
    (if (:match item) "Matched" "Did not match")]
   [:p
    [:code
     (:message item)]]
   [:table.table
    [:tr
     [:td.text-center
      (img (:expected item))
      [:div.size
       (:expected-size item)]]
     [:td.text-center
      (img (:actual item))
      [:div.size
       (:actual-size item)]]
     [:td.text-center
      (img (:diff item))]]]])

(defn render
  [items]
  (html5
    [:head
     (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
     (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js")
     [:style "
        td {
          width: 33%;
        }
        .size {
          color: lightgrey;
        }
      "]]
    [:div.container-fluid
     (map render-item items)]))

(defn -main
  [& args]
  (->> (compare-all) render (spit "out.html")))