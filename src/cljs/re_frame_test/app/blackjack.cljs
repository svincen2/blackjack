(ns re-frame-test.app.blackjack
  (:require [re-frame-test.app.core :as core]
            [clojure.pprint :refer [pprint]]))

(defn new-deck
  [& opts]
  (let [deck (apply core/new-deck opts)]
    (reduce (fn [acc card]
              (into acc {(core/new-id) card}))
            {}
            deck)))

(defrecord Player [id name cash]
  Object
  (toString [x] (str name " cash: $" cash)))

(defn new-player
  [{:keys [name cash]}]
  (map->Player {:id (core/new-id)
                :name name
                :cash cash}))

(defn rank->score
  [rank]
  (or ({:ace 11 :jack 10 :queen 10 :king 10} rank)
      (js/parseInt (name rank))))

(defn filter-showing
  [cards]
  (filter #(not (:facedown? %)) cards))

; Scored in the following manner:
; 1. Calculate raw score by treating each ace as 11
; 2. Count the number of aces
; 3. While the score is > 21, and we have at least one ace,
;    subtract 10 from the score, and decrement the number of aces.
(defn score
  [cards]
  (println "score - cards")
  (pprint cards)
  (let [showing (filter-showing cards)
        raw-score (->> showing (map :rank) (map rank->score) (reduce +))
        num-aces (core/count-rank :ace cards)]
    (println "showing" showing)
    (println "raw-score" raw-score)
    (println "num-aces" num-aces)
    (loop [score raw-score
           aces  num-aces]
      (if (or (<= score 21) (<= aces 0))
        score
        (recur (- score 10) (dec aces))))))

(defn deal
  [player pattern bj-deck]
  (let [player-id (:id player)
        n (count pattern)
        remaining (drop-while #(not (nil? (get-in % [1 :owner]))) bj-deck)
        dealt-cards (take n remaining)
        dealt-ids (map first dealt-cards)
        ids-and-pattern (zipmap dealt-ids pattern)]
    (reduce
      (fn [deck [id p]]
        (update deck id merge {:owner player-id :facedown? (= :down p)}))
      bj-deck
      ids-and-pattern)))

(defn deal-new-round
  [deck dealer & players]
  (let [deck (deal dealer [:down :up] deck)]
    (reduce (fn [deck player]
              (deal player [:up :up] deck))
            deck
            players)))

