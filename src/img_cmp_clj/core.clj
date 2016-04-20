(ns img-cmp-clj.core
  (:gen-class))

(programs identify compare)

(defn size
  (re-find #"(\d+)x(\d+)" (identify "expected/mug-3.png")))

(-> (with-programs [compare] (compare "-metric" "AE" "expected/mug-1.png" "actual/mug-1.png" "x.png" {:throw false :verbose true})) :exit-code deref)
(with-programs [compare] (compare "-metric" "AE" "expected/mug-2.png" "actual/mug-2.png" "x.png" {:throw false :verbose true}))
(Integer/parseInt (first (:err (:proc (with-programs [compare] (compare "-metric" "AE" "expected/mug-3.png" "actual/mug-3.png" "x.png" {:throw false :verbose true}))))))

(pprint (file-seq (clojure.java.io/file ".")))

(subs (.getPath (first (filter #(.isFile %) (file-seq (clojure.java.io/file "././././actual"))))) (count "././././actual"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
