(ns re-frame-test.views
  (:require [re-frame.core :as re-frame]
            [re-frame-test.app.core :as core]
            [re-frame-test.app.blackjack :as bj]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as reagent]
            [re-frame-test.app.blackjack :as blackjack]))



(def suit->color
  {:clubs :blue
   :diamonds :magenta
   :hearts :red
   :spades :black})

(defn card-view
  [[id {:keys [rank suit facedown?] :as card}]]
  (let [suit-color (suit->color suit)
        class (str "card" (if facedown? " facedown"))]
    (println "id" id)
    (fn []
      [:div {:class class}
       [:div.rank {:style {:color suit-color}}
        (core/rank->str rank)]
       [:div.suit {:style {:color suit-color}}
        (core/suit->char suit)]])))

(defn cards-view
  [player-id]
  (let [cards (re-frame/subscribe [:player-cards player-id])]
    (fn []
      (let [score (blackjack/score (map #(get % 1) @cards))]
        [:div
         (doall
           (for [card @cards]
              #^{:key (core/new-id)} [card-view card]))
         [:div (str "score " score)]]))))

(defn dealer-panel
  []
  (let [dealer @(re-frame/subscribe [:dealer])
        ]
    (fn []
      [:div.dealer-panel
       [:div (str "dealer, id: " (:id dealer))]
       [cards-view (:id dealer)]
       ])))

(defn player-panel
  [{:keys [id name cash]}]
  (let []
    (fn []
      [:div.player-panel
       [:div (str "name: " name ", id: " id)]
       [cards-view id]
       [:div (str "cash $" cash)]
       [:div
        [:button {:on-click #(re-frame/dispatch [:remove-player id])}
         "leave table"]]])))

(defn players-panel
  []
  (let [players @(re-frame/subscribe [:players])]
    [:div.players-panel
     (doall
       (for [player players]
         #^{:key (:id player)} [player-panel player]))]))

(defn control-panel
  []
  (let [name (reagent/atom "")
        cash (reagent/atom "")]
    (fn []
      [:div
       [:div {:style {:padding "6px"
                      :width "180px"
                      :border "1px solid black"}}
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
         [:button {:on-click #(re-frame/dispatch
                                [:add-player
                                 (blackjack/new-player {:name @name
                                                        :cash (js/parseFloat @cash)})])}
          "add player"]]]

       [:div {:style {:padding "6px"
                      :width "180px"
                      :border "1px solid black"}}
        [:button {:on-click #(re-frame/dispatch [:deal-new-game])} "deal"]
        [:button {:on-click #(re-frame/dispatch [:initialize-db])} "reset"]]])))

(defn main-panel []
  (let []
    (fn []
      [:div
       [dealer-panel]
       [players-panel]
       [:hr]
       [control-panel]
       ])))
