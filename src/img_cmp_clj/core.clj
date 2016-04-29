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
    {:expected expected
     :actual   actual
     :diff     diff
     :result   (case exit-code
                 0 :similar
                 1 :dissimilar
                 2 :error)
     :message  (-> comparison :proc :err first)}))

(defn is-image
  [path]
  (-> path
      (clojure.string/split #"\.")
      last
      clojure.string/lower-case
      #{"png" "jpg"}))

(defn expected-files
  []
  (->> (clojure.java.io/file "expected")
       file-seq
       (map str)
       (filter is-image)))

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

(defn render-result
  [result]
  [:div
   [:h1 (change-prefix (:expected result) "") ": " (:result result)]
   [:p [:code (:message result)]]
   [:div.diff
    [:div
     (img (:expected result))
     [:div (-> result :expected image-size)]]
    [:div
     (img (:actual result))
     [:div (-> result :actual image-size)]]
    [:div
     (img (:diff result))]]])

(defn render
  [results]
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
          color: lightgrey;
        }
      "]]
    [:div.container-fluid
     (map render-result results)]))

(defn write
  [results]
  (->> results render (spit "out.html")))

(defn -main
  [& args]
  (let [results (compare-all)
        similar (every? #(= :similar %) (map :result results))]
    (write results)
    (System/exit (if similar 0 1))))