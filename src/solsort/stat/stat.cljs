(ns solsort.stat.stat
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]
   [solsort.toolbox.macros :refer [<?]]
   [reagent.ratom :as ratom :refer  [reaction]])
  (:require
   [cljs.reader]
   [solsort.toolbox.setup]
   [solsort.toolbox.appdb :refer [db db! db-async!]]
   [solsort.toolbox.ui :refer [input select]]
   [solsort.util
    :refer
    [<ajax <seq<! js-seq load-style! put!close!
     parse-json-or-nil log page-ready render dom->clj]]
   [reagent.core :as reagent :refer []]
   [clojure.string :as string :refer [replace split blank?]]
   [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))

(defn events-by-hits []
  (let [stats (->>
               (db [:stats])
               (map second)
               (remove empty?)
               (apply concat)
               (reduce
                (fn [acc [k v]]
                  (assoc acc k (+ v (get acc k 0))))
                {})
               (sort-by second)
               (reverse))
        ]
     (into
      [:div]
      (for [[p c] stats]
        [:div
         [:div {:style {:display :inline-block
                        :width "10%"
                        :vertical-align :top
                        :text-align :right}}
          (str c)]
         " "
         [:div {:style {:display :inline-block
                        :text-align :left}}
          (str p)]
         ])))
  )

(defn week-graph []
  (let [graph (->>
               (db [:stats])
               (sort-by first)
               (map second)
               (map #(apply + (map second %))))
        graph-max (apply max graph)]
    (into [:div {:style {:vertical-align :bottom}}]
          (map
           (fn [n]
             [:span
              {:style
               {:display :inline-block
                :vertical-align :bottom
                :height (+ 1 (* 200 (/ n graph-max)))
                :width "0.5%"
                :background-color :black
                :color :green
                }
               }])
           graph))
    ))
(render
 [:div
  [:h1 "Stats last week"]
  [:p "(very little amount of stats collected at the moment)"]
  [week-graph]
  [events-by-hits]
  ])
(doall
 (for [i (range (* 24 7))]
   (let [d (-> (js/Date. (- (js/Date.now) (* i 60 60 1000)))
               (.toISOString)
               (.slice 0 13)
               (.replace "T" "-"))
         f (str "https://incoming.solsort.com/" d ".json")]
     (go (db!
          [:stats d] (try
                (<? (<ajax f))
                (catch js/Error e {})))))))
