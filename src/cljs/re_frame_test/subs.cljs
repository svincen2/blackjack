(ns re-frame-test.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [clojure.pprint :refer [pprint]]))


(re-frame/reg-sub
  :players
  (fn [db _]
    (:players db)))

(re-frame/reg-sub
  :player
  (fn [db [_ id]]
    (let [dealer (:dealer db)
          players (:players db)]
      (if (= (:id dealer) id)
        dealer
        (first (filter #(= id (:id %)) players))))))

(re-frame/reg-sub
  :player-cards
  (fn [db [_ player-id]]
    (let [deck (:deck db)
          cards (filter #(= player-id (:owner %)) deck)]
      cards)))

(re-frame/reg-sub
  :dealer
  (fn [db _]
    (:dealer db)))

(re-frame/reg-sub
  :deck
  (fn [db _]
    (:deck db)))

(re-frame/reg-sub
  :turn
  (fn [db _]
    (:turn db)))

(re-frame/reg-sub
  :button
  (fn [db _]
    (:button db)))
