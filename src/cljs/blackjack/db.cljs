(ns blackjack.db
  (:require [blackjack.app.core :as core]
            [blackjack.app.blackjack :as blackjack]))

(def default-db
  {:deck    (core/new-deck :num-decks 1 :shuffled true)
   :dealer  (blackjack/new-player {:name "loyd" :cash 0.00})
   :players []
   :phase (first blackjack/phases-of-play)
   :turn nil
   :button nil
   })
