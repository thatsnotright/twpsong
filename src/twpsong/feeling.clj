(ns twpsong.feeling
   (:use
	[clojure.set]
	[clojure.java.io]
  )
)

(defn get-lines [fname]
  (with-open [r (reader fname)]
    (doall (line-seq r))))

(def happywords (get-lines "happy.txt"))

(def sadwords (get-lines "sad.txt"))

(defn word-split [str] 
  (re-seq #"\w{1,}" str))

(defn combine [lst1 lst2]
  (mapcat (fn [x] (map #(list % x) lst1)) lst2))

(defn disjoint-words [list1 list2]
  (difference (set list1) (set list2))
)

(defn compute-emotion [list1]
   (def happy (count (disjoint-words list1 happywords)))
   (def sad (count (disjoint-words list1 sadwords)))
   (def wordcount (count list1))
   (if (<= happy sad) :sad :happy)
)


