(ns blackjack.app.blackjack
  (:require [blackjack.app.core :as core]
            [clojure.pprint :refer [pprint]]))

(defrecord Player [id name cash bet]
  Object
  (toString [x] (str name " cash: $" cash ", bet: " bet)))

(defn new-player
  [{:keys [name cash]}]
  (map->Player {:id (core/new-id)
                :name name
                :cash cash
                :bet 0}))

(defn player=
  [a b]
  (= (:id a) (:id b)))

(def not-player= (complement player=))

(defn place-bet
  [player amt]
  (-> player
      (update :bet + amt)
      (update :cash - amt)))

(defn clear-bet
  [player]
  (let [curr-bet (:bet player)]
    (-> player
        (update :bet - curr-bet)
        (update :cash + curr-bet))))

(defn collect-payout
  [player rate]
  (let [payout (* rate (:bet player))]
    (-> player
        (assoc :bet 0)
        (update :cash + payout))))


(defn lose-bet
  [player]
  (assoc player :bet 0))

(defn player-cards
  [deck {:keys [id] :as player}]
  (filter #(= id (:owner %)) deck))

(defn show-cards
  [deck {:keys [id] :as player}]
  (map (fn [card]
         (if (= id (:owner card))
           (assoc card :facedown? false)
           card))
       deck))

; TODO - should be :face #{:up :down}
(defn filter-showing
  [cards]
  (filter #(not (:facedown? %)) cards))

(defn players-in
  [players]
  (filter #(> (:bet %) 0) players))

(defn rank->score
  [rank]
  (or ({:ace 11 :jack 10 :queen 10 :king 10} rank)
      (js/parseInt (name rank))))

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

(defn player-payouts
  [players deck dealer]
  (let [dealer-score (->> dealer (player-cards deck) (score))
        player-scores (map #(->> % (player-cards deck) (score)) players)
        score-to-beat (if (> dealer-score 21) 0 dealer-score)]
    (mapv (fn [player score]
           (cond
             (> score 21) (lose-bet player)
             (= score 21) (collect-payout player 1.5)
             (= score score-to-beat) (collect-payout player 1)
             (> score score-to-beat) (collect-payout player 2)
             :else (lose-bet player)))
         players
         player-scores)))

(defn deal
  [deck {:keys [id] :as player} pattern]
  (let [num-to-deal (count pattern)
        {owned false
         remaining true} (group-by #(nil? (:owner %)) deck)
        cards (take num-to-deal remaining)
        dealt (map #(merge %1 {:owner id :facedown? (= :down %2)}) cards pattern)
        new-deck (concat owned dealt (drop num-to-deal remaining))]
    new-deck))

(defn hit
  [player deck]
  (deal player [:up] deck))

(defn next-button
  [curr-button players]
  (if (empty? players)
    nil
    (if (nil? curr-button)
      (first players)
      (let [button-cycle (cycle players)]
        (second (drop-while #(not-player= curr-button %) button-cycle))))))

(defn player-turn-list
  [players button]
  (let [players (players-in players)
        player-cycle (cycle players)]
    (take (count players)
          (drop 1 (drop-while #(not-player= button %) player-cycle)))))

(defn next-turn
  [turn button players]
  (let [turns (player-turn-list players button)]
    (if (nil? turn)
      (first turns)
      (second (drop-while #(not-player= turn %) turns)))))

(defn deal-new-round
  [deck dealer & players]
  (let [players (players-in players)
        deck (deal deck dealer [:down :up])]
    (reduce (fn [deck player]
              (deal deck player [:up :up]))
            deck
            players)))

(defn dealer-turn
  [deck dealer]
  (loop [d deck]
    (let [cards (player-cards d dealer)
          score (score cards)]
      (if (> score 16)
        d
        (recur (deal d dealer [:up]))))))

; TODO - Rename dealer-phases or something
(def phases-of-play
  [:waiting-for-first-bet
   :waiting-for-more-bets
   :deal-new-round
   :player-turns
   :dealer-turn
   :player-payouts
   :view-results
   :finalize])

(def playing?
  #{:deal-new-round
    :player-turns
    :dealer-turn
    :player-payouts})

(defn next-phase
  [curr-phase]
  (let [phase-cycle (cycle phases-of-play)]
    (second (drop-while #(not= curr-phase %) phase-cycle))))

