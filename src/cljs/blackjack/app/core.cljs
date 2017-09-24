(ns blackjack.app.core
  (:require [clojure.string :as str]))

; UID generation
(defn make-uid-generator
  []
  (let [id (atom 0)]
    (fn []
      (let [next-id (inc @id)]
        (swap! id inc)
        next-id))))

(def new-id (make-uid-generator))

; Ranks
(def ranks [:ace :2 :3 :4 :5 :6 :7 :8 :9 :10 :jack :queen :king])
(def rank-set (set ranks))
(defn rank? [r] (rank-set r))
(defn rank->str [rank] (or ({:ace "A" :jack "J" :queen "Q" :king "K"} rank) (name rank)))
(defn rand-rank [] (rand-nth ranks))

; Suits
(def suits [:clubs :diamonds :hearts :spades])
(def suit-set (set suits))
(defn suit? [s] (suit-set s))
(def suit->char {:clubs \u2663 :diamonds \u2666 :hearts \u2665 :spades \u2660})
(defn rand-suit [] (rand-nth suits))

; Cards
(defrecord Card [rank suit]
  Object
  (toString [c] (str (rank->str rank) " " (suit->char suit))))

(defn new-card
  [{:keys [rank suit] :as card}]
  (assert (rank? rank))
  (assert (suit? suit))
  (map->Card card))

; Decks
(def ^:private fresh-deck
  (flatten
    (for [suit suits]
      (for [rank ranks]
        (new-card {:rank rank :suit suit})))))

(defn new-deck
  ([]
   (new-deck :num-decks 1))
  ([& opts]
   (let [{num-decks :num-decks
          shuffled? :shuffled
          :or       {num-decks 1
                     shuffled? false}} opts
         deck (flatten (repeat num-decks fresh-deck))]
     (cond
       shuffled? (shuffle deck)
       :else deck))))

; Collection of cards
(defn count-rank
  [rank cards]
  (count (filter #(= rank (:rank %)) cards)))

(defn count-suit
  [suit cards]
  (count (filter #(= suit (:suit %)) cards)))
