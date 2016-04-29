(ns img-cmp-clj.core
  (:require [me.raynes.conch :refer [with-programs]]
            [hiccup.page :refer [html5 include-css include-js]])
  (:gen-class))

(defn image-size
  [path]
  (with-programs [identify]
                 (re-find #"\d+x\d+" (identify path {:throw false}))))

(defn change-prefix
  [path prefix]
  (clojure.string/replace path "expected/" prefix))

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
     :result        (case exit-code 0 :similar 1 :dissimilar 2 :error)
     :expected-size (image-size expected)
     :actual-size   (image-size actual)
     :message       (-> comparison :proc :err first)}))

(defn expected-files
  []
  (->> (clojure.java.io/file "expected")
       file-seq
       (filter #(.isFile %))
       (map str)))

(defn compare-all
  []
  (->> (expected-files)
       (pmap compare-files)
       (sort-by :result)))

(defn img
  [src]
  (if (.exists (clojure.java.io/as-file src))
    [:a {:href src}
     [:img.img-responsive {:src src}]]
    [:img.img-responsive {:src "missing.png"}]))

(defn render-item
  [item]
  [:div
   [:h1
    (change-prefix (:expected item) "")
    ": "
    (:result item)]
   [:p
    [:code
     (:message item)]]
   [:div.diff
    [:div
     (img (:expected item))
     [:div.size
      (:expected-size item)]]
    [:div
     (img (:actual item))
     [:div.size
      (:actual-size item)]]
    [:div
     (img (:diff item))]]])

(defn render
  [items]
  (html5
    [:head
     (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
     [:style "
        .diff {
          display: flex;
          align-items: center;
        }
        .diff > div {
          width: 33%;
          text-align: center;
        }
        .size {
          color: lightgrey;
        }
      "]]
    [:div.container-fluid
     (map render-item items)]))

(defn write
  [results]
  (->> results render (spit "out.html")))

(defn -main
  [& args]
  (let [results (compare-all)
        similar (every? #(= :similar %) (map :result results))]
    (write results)
    (System/exit (if similar 0 1))))