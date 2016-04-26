(ns img-cmp-clj.core
  (:require [me.raynes.conch :refer [with-programs]])
  (:gen-class))

(defn compare-files
  [expected actual diff]
  (let [comparison (with-programs [compare]
                                  (compare "-metric" "AE" expected actual diff {:throw false :verbose true}))
        exit-code (-> comparison :exit-code deref)]
    {:match (= 0 exit-code)
     :message (-> comparison :proc :err first)}))

(defn change-prefix
  [expected prefix]
  (clojure.string/replace expected "expected/" prefix))

(defn expected-seq
  []
  (->> (clojure.java.io/file "expected")
       file-seq
       (filter #(.isFile %))
       (map str)
       (map #(hash-map :expected %
                       :diff (change-prefix % "diff/")
                       :actual (change-prefix % "actual/")))))

(defn -main
  [& args]
  (println "Hello, World!"))
