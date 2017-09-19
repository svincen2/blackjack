(ns re-frame-test.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [clojure.pprint :refer [pprint]]))


(re-frame/reg-sub
  :players
  (fn [db _]
    (:players db)))

(re-frame/reg-sub
  :player-cards
  (fn [db [_ player-id]]
    (let [deck (:deck db)
          cards (filter #(= player-id (get-in % [1 :owner])) deck)]
      ;(println "player-id" player-id)
      ;(println "cards")
      ;(pprint cards)
      cards)))

(re-frame/reg-sub
  :dealer
  (fn [db _]
    (:dealer db)))
