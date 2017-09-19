(ns re-frame-test.events
  (:require [re-frame.core :as re-frame]
            [re-frame-test.db :as db]
            [re-frame-test.app.core :as core]
            [re-frame-test.app.blackjack :as blackjack]
            [clojure.pprint :refer [pprint]]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
  :add-player
  (fn [db [_ player]]
    (update db :players conj player)))

(re-frame/reg-event-db
  :remove-player
  (fn [db [_ id]]
    (update db :players (partial filter #(not= id (:id %))))))


(re-frame/reg-event-db
  :deal-new-game
  (fn [db _]
    (let [deck (:deck db)
          dealer (:dealer db)
          players (:players db)
          new-deck (apply blackjack/deal-new-round deck dealer players)]
      (-> db
          (assoc :deck new-deck)
          (assoc :turn (first players))
          (assoc :button (first players))))))

(re-frame/reg-event-db
  :shuffle
  (fn [db _]
    (update db :deck shuffle)))
