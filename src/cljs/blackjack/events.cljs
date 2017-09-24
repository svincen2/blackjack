(ns blackjack.events
  (:require [re-frame.core :as re-frame]
            [blackjack.db :as db]
            [blackjack.app.core :as core]
            [blackjack.app.blackjack :as blackjack]
            [clojure.pprint :refer [pprint]]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
  :next-button
  (fn [db _]
    (let [curr-button (:button db)
          players (:players db)]
      (update db :button blackjack/next-button players))))

(re-frame/reg-event-db
  :next-turn
  (fn [db _]
    (let [curr-turn (:turn db)
          button (:button db)
          players (:players db)]
      (update db :turn blackjack/next-turn button players))))

(re-frame/reg-event-db
  :add-player
  (fn [db [_ player]]
    (update db :players conj player)))

(defn find-player
  [players id]
  (first (filter #(= id (:id %)) players)))

(defn player-index
  [players id]
  (let [player (find-player players id)]
    (.indexOf players player)))

(re-frame/reg-event-db
  :remove-player
  (fn [db [_ id]]
    (let [players (:players db)
          player-to-remove (find-player players id)
          new-players (vec (filter #(blackjack/not-player= player-to-remove %) players))
          curr-button (:button db)]
      (println "players")
      (pprint players)
      (println "player-to-remove")
      (pprint player-to-remove)
      (println "new-players")
      (pprint new-players)
      (println "curr-button")
      (pprint curr-button)
      (if (blackjack/player= curr-button player-to-remove)
        (if (empty? new-players)
          (-> db
              (assoc :button nil)
              (assoc :players new-players))
          (-> db
              (update :button blackjack/next-button players)
              (assoc :players new-players)))
        (assoc db :players new-players)))))

(re-frame/reg-event-db
  :add-player-bet
  (fn [db [_ id amt]]
    (let [player-idx (player-index (:players db) id)
          phase (:phase db)]
      (if (= :waiting-for-first-bet phase)
        (-> db
            (update :phase blackjack/next-phase)
            (update-in [:players player-idx] blackjack/place-bet amt))
        (update-in db [:players player-idx] blackjack/place-bet amt)))))

(re-frame/reg-event-db
  :clear-player-bet
  (fn [db [_ id]]
    (let [player-idx (player-index (:players db) id)]
      (update-in db [:players player-idx] blackjack/clear-bet))))

(re-frame/reg-event-db
  :collect-payout
  (fn [db [_ id]]
    (let [player-idx (player-index (:players db) id)]
      (update-in db [:players player-idx] blackjack/collect-payout))))

(re-frame/reg-event-db
  :lose-bet
  (fn [db [_ id]]
    (let [player-idx (player-index (:players db) id)]
      (update-in db [:players player-idx] blackjack/lose-bet))))

(re-frame/reg-event-db
  :hit-player
  (fn [db [_ id]]
    (let [player (find-player (:players db) id)]
      (update db :deck blackjack/deal player [:up] (:deck db)))))
          

(re-frame/reg-event-db
  :deal-new-round
  (fn [db _]
    (let [deck (:deck db)
          dealer (:dealer db)
          players (:players db)
          button (:button db)
          new-deck (apply blackjack/deal-new-round deck dealer players)]
      (if (nil? button)
        (-> db
            (update :button blackjack/next-button players)
            (assoc :deck new-deck))
        (-> db
            (assoc :deck new-deck))))))

(re-frame/reg-event-db
  :dealer-turn
  (fn [db _]
    (let [dealer (:dealer db)
          deck (blackjack/show-cards (:deck db) dealer)] 
      (assoc db :deck (blackjack/dealer-turn deck dealer)))))

(re-frame/reg-event-db
  :player-payouts
  (fn [db _]
    (let [deck (:deck db)
          dealer (:dealer db)]
      (update db :players blackjack/player-payouts deck dealer))))

(re-frame/reg-event-db
  :shuffle
  (fn [db _]
    (update db :deck shuffle)))

(re-frame/reg-event-db
  :reset-deck
  (fn [db _]
    (assoc db :deck (core/new-deck :num-decks 1 :shuffled true))))

(re-frame/reg-event-db
  :next-phase
  (fn [db _]
    (update db :phase blackjack/next-phase)))

