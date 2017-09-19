(ns re-frame-test.db
  (:require [re-frame-test.app.core :as core]
            [re-frame-test.app.blackjack :as blackjack]))

(def default-db
  {:deck    (core/new-deck :num-decks 1 :shuffled false)
   :dealer  (blackjack/new-player {:name "loyd" :cash 0.00})
   :players []
   :turn nil
   :button nil})
