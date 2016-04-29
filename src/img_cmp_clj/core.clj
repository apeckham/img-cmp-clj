(ns img-cmp-clj.core
  (:require [me.raynes.conch :refer [with-programs]]
            [hiccup.page :refer [html5 include-css include-js]])
  (:gen-class))

(def missing-png "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASAAAAEgAQMAAAAuTTzDAAAABlBMVEXs7Oz///+10pB6AAADh0lEQVR4AYzQy5HcRhgGwc+TMWVNWxPGLBgCC3DbF1k6UlIGA9PHP6pfubdu188e99HHnvfRteM+OnfeR8eu++i5j/vosZ/3u+b3234DBdP7AgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVNLKFTGSKbWX6E+MnrKZHTIZHTKZHTJZPQBExFQ/zbZv4YwEQH1EZFQl5FQp5FQh5FQTyOhHkRC/Vtk/x7DZNRDJqOnTEaHTEanTEaXTEYff2Fq/lqP6afsvMKr52P9xPy2HBNQ2AUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMHUAgqmFlAwtYCCqQUUTC2gYGoBBVMLKJhaQMFE9JTJ6JDJ6JTJ6JLJ6EOmWkDB1AIKphZQMBk9ZTI6ZDI6ZTK6ZDL6kKkFFEwtoGBqAQVTC6jrheg4X4jO44XogqkFFEwtoGB6PXr9Oh/+DyN2TAAACMNA0L+yCqmRCPjldqYH2uQdgcPka8kF+1PxR+fP1z9Cv5R/Tv/mPjB89HSI+Tj0weojupgKytdGF5CvMl+Kvl6LqaB85Tc8eAzxQOPRKJgKyuOaBz+PkJfTHms9IHvUbmj3+O9FIpgCSstNMBUUFq5gCigtgcVUUFhMveIW4Hvt9gLvKsClQvWEiw5XJi5fXONUCLFacknlusvFWRUcyTzTgiQYSVWS9HR96iK2mALF5HAxBRQK62IKKJTowRRQKvbXqB0LAAAAAAjztw5gTwp7SyahXmyQSagXQGQS6kUZmYR6oUgmoV68kkmoF9RkEupFPpmEeuFRJqFeDJVJqBdoZRLqRWOZhHohWyahXlyXSagX/GEC6k4IMAF1xwiYgLqzBkxA3YEEJqDu1AITUHe0gQmoO//AJNQckmASak5SMAk1xy2YhJozmUxCvcFNJqHedCeTUG8ElEmoNyfKJNQbJmUSKoZVhKZF1BisAAAAAElFTkSuQmCC")

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
    [:img.img-responsive {:src missing-png}]))

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