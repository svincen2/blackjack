(ns re-frame-test.views
  (:require [re-frame.core :as re-frame]
            [re-frame-test.app.core :as core]
            [re-frame-test.app.blackjack :as bj]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as reagent]
            [re-frame-test.app.blackjack :as blackjack]))

(def wait-limit-for-more-bets 30)
(def view-results-delay 15)

; TODO - We don't actually use this, but it's a neat little component
(defn timer
  ([]
   (timer :asc))
  ([direction]
   (timer direction 0))
  ([direction start-time]
   (let [t (reagent/atom start-time)
         dir (or (get {:asc inc :desc dec} direction) inc)]
     (fn []
       (js/setTimeout #(swap! t dir) 1000)
       [:div "Timer: " @t]))))

(defn countdown-timer
  "Countdown (in seconds) from start-time to 0, then execute an event"
  [start-time event]
  (let [t (reagent/atom start-time)]
    (fn []
      (if (> @t 0)
        (do
          (js/setTimeout #(swap! t dec) 1000)
          [:div "time remaining: " @t])
        (re-frame/dispatch event)))))

(def suit->color
  {:clubs :blue
   :diamonds :magenta
   :hearts :red
   :spades :black})

(defn card-view
  ([card]
   (card-view card nil))
  ([{:keys [rank suit facedown? owner] :as card} & opts]
   (let [player @(re-frame/subscribe [:player owner])
         {show-owner? :show-owner
          :or {show-owner? false}} opts
         suit-color (suit->color suit)
         class (str "card"
                    (if facedown? " facedown"))]
     (fn []
       [:div {:class class}
        [:div.rank {:style {:color suit-color}}
         (core/rank->str rank)]
        [:div.suit {:style {:color suit-color}}
         (core/suit->char suit)]
        (if show-owner?
          [:div.owner (:name player)])]))))

(defn cards-view
  [player]
  (let [cards (re-frame/subscribe [:player-cards (:id player)])]
    (fn []
      (let [score (blackjack/score @cards)]
        [:div.cards-view
         (doall
           (for [card @cards]
              #^{:key (core/new-id)} [card-view card]))
         [:div (str "score " score)]]))))

(defn dealer-panel
  []
  (let [dealer (re-frame/subscribe [:dealer])
        players (re-frame/subscribe [:players])
        button (re-frame/subscribe [:button])
        turn (re-frame/subscribe [:turn])
        phase (re-frame/subscribe [:phase])
        button-cycle (cycle players)]
    (fn []
      ; TODO - All this logic shouldn't be in the view.
      (cond
        (= :waiting-for-first-bet @phase)
        (let [players-in (filter #(> (:bet %) 0) @players)]
          (when (and (> (count @players) 0)
                     (= (count players-in) (count @players)))
            (re-frame/dispatch [:next-phase])))
        (= :waiting-for-more-bets @phase)
        (let [players-in (filter #(> (:bet %) 0) @players)]
          (when (= (count players-in) (count @players))
            (re-frame/dispatch [:next-phase])))
        (= :deal-new-round @phase)
        (do
          (re-frame/dispatch [:deal-new-round])
          (re-frame/dispatch [:next-turn])
          (re-frame/dispatch [:next-phase]))
        (= :player-turns @phase)
        (if (nil? @turn)
          (re-frame/dispatch [:next-phase]))
        (= :dealer-turn @phase)
        (do
          (re-frame/dispatch [:dealer-turn])
          (re-frame/dispatch [:next-phase]))
        (= :player-payouts @phase)
        (do
          (re-frame/dispatch [:player-payouts])
          (re-frame/dispatch [:next-phase])
          )
        (= :finalize @phase)
        (do
          (re-frame/dispatch [:next-button])
          (re-frame/dispatch [:reset-deck])
          (re-frame/dispatch [:next-phase]))
        )
      [:div.dealer-panel
       [:div (str (:name @dealer) " (dealer)" )]
       [:div (str \( (name @phase) \))]
       (if (= :waiting-for-more-bets @phase)
         [countdown-timer wait-limit-for-more-bets [:next-phase]])
       (if (= :view-results @phase)
         [countdown-timer view-results-delay [:next-phase]])
       [:div (str "button: " @button)]
       [:div (str "turn: " @turn)]
       [cards-view @dealer]])))

(defn player-panel
  [id]
  (let [player (re-frame/subscribe [:player id])
        curr-bet (reagent/atom (:bet @player))
        turn (re-frame/subscribe [:turn])]
    (fn []
      (let [phase (re-frame/subscribe [:phase])
            playing? (blackjack/playing? @phase)
            turn? (blackjack/player= @turn @player)]
      [:div {:class (str "player-panel"
                         (if playing? " playing")
                         (if turn? " turn"))}
       [:div (:name @player)]
       [cards-view @player]
       [:div (str "bet $" (:bet @player))]
       [:div (str "cash $" (:cash @player))]
       [:div "bet amount "
        [:input {:on-change #(reset! curr-bet (-> % .-target .-value js/parseInt))
                 :disabled playing?}]
        [:button {:on-click #(re-frame/dispatch [:add-player-bet id @curr-bet])
                  :disabled playing?} "place bet"]]
       [:button {:on-click #(re-frame/dispatch [:clear-player-bet id])
                 :disabled playing?} "clear bet"]
       [:button {:on-click #(re-frame/dispatch [:hit-player id])
                 :disabled (not turn?)} "hit"]
       [:button {:on-click #(re-frame/dispatch [:next-turn])
                 :disabled (not turn?)} "stay"]
       [:button {:on-click #(re-frame/dispatch [:remove-player id])
                 :disabled playing?} "leave table"]]))))
         

(defn players-panel
  []
  (let [players @(re-frame/subscribe [:players])]
    [:div.players-panel
     (doall
       (for [player players]
         #^{:key (:id player)} [player-panel (:id player)]))]))

(defn control-panel
  []
  (let [name (reagent/atom "")
        cash (reagent/atom "")]
    (fn []
      [:div

       ; Add player
       [:div {:style {:padding "6px" :width "180px" :border "1px solid black"}}
        [:div {:style {:padding "4px 0"}}
         "name"
         [:input {:style {:float :right}
                  :value @name
                  :on-change #(reset! name (-> % .-target .-value))}]]
        [:div {:style {:padding "4px 0"}}
         "cash"
         [:input {:style {:float :right}
                  :value @cash
                  :on-change #(reset! cash (-> % .-target .-value))}]]
        [:div
         [:button
          {:on-click #(re-frame/dispatch
                        [:add-player
                         (blackjack/new-player {:name @name :cash (js/parseFloat @cash)})])}
          "add player"]]]

       ; Buttons
       [:div {:style {:padding "6px"
                      :width "180px"
                      :border "1px solid black"}}
        [:button {:on-click #(re-frame/dispatch [:deal-new-round])} "deal"]
        [:button {:on-click #(re-frame/dispatch [:shuffle])} "shuffle"]
        [:button {:on-click #(re-frame/dispatch [:initialize-db])} "reset"]
        [:button {:on-click #(re-frame/dispatch [:next-button])} "next button"]
        [:button {:on-click #(re-frame/dispatch [:reset-deck])} "reset deck"]
        [:button {:on-click #(re-frame/dispatch [:next-phase])} "next phase"]
        [:button {:on-click #(re-frame/dispatch [:next-turn])} "next turn"]
        ;[:button {:on-click #(re-frame/dispatch [:dealer-turn])} "dealer turn"]
        ;[:button {:on-click #(re-frame/dispatch [:player-payouts])} "player payouts"]
        ]])))

(defn deck-view
  []
  (let [deck (re-frame/subscribe [:deck])]
    [:div
     [:div "deck"]
     [:div
      (for [card @deck]
        #^{:key (core/new-id)} [card-view card :show-owner true])]]))

(defn main-panel []
  (let [turn (re-frame/subscribe [:turn])
        players (re-frame/subscribe [:players])]
    (fn []
      [:div
       [dealer-panel]
       [players-panel]
       [:hr]
       [control-panel]
       ;[deck-view]
       ;[:div (str "turn: " @turn)]
       ;[:div (str "players: " @players)]
       ])))
