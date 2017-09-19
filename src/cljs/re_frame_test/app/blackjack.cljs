(ns re-frame-test.app.blackjack
  (:require [re-frame-test.app.core :as core]
            [clojure.pprint :refer [pprint]]))

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
; 1. Filter out any face-down cards.
; 2. Calculate raw score by treating each ace as 11
; 3. Count the number of aces
; 4. While the score is > 21, and we have at least one ace,
;    subtract 10 from the score, and decrement the number of aces.
(defn score
  [cards]
  (let [showing (filter-showing cards)
        raw-score (->> showing (map :rank) (map rank->score) (reduce +))
        num-aces (core/count-rank :ace cards)]
    (loop [score raw-score
           aces  num-aces]
      (if (or (<= score 21) (<= aces 0))
        score
        (recur (- score 10) (dec aces))))))

(defn deal
  [{:keys [id] :as player} pattern deck]
  (let [num-to-deal (count pattern)
        {owned false
         remaining true} (group-by #(nil? (:owner %)) deck)
        dealt (map #(merge %1 {:owner id :facedown? (= :down %2)}) (take num-to-deal remaining) pattern)
        new-deck (concat owned dealt (drop num-to-deal remaining))]
    new-deck))

(defn deal-new-round
  [deck dealer & players]
  (let [deck (deal dealer [:down :up] deck)]
    (reduce (fn [deck player]
              (deal player [:up :up] deck))
            deck
            players)))

