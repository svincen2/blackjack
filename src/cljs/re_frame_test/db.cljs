(ns re-frame-test.db
  (:require [re-frame-test.app.core :as core]
            [re-frame-test.app.blackjack :as blackjack]))

(def default-db
  {:deck    (blackjack/new-deck :num-decks 1 :shuffled false)
   :dealer  (blackjack/new-player {:name "dealer" :cash 0.00})
   :players (list)})
