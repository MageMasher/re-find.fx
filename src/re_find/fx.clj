(ns re-find.fx
  (:gen-class)
  (:require [cljfx.api :as fx]
            [re-find.match :as match]))

(def init-state {:args             "inc [1 2 3]"
                 :ret              "[2 3 4]"
                 :exact-ret-match? true
                 :help?            false
                 :more?            false})

(def *state (atom (fx/create-context init-state)))

(defn table-view [{:keys [fx/context]}]
  (binding [*print-length* 10]
    (let [result (match/compute
                   (fx/sub context :args)
                   (fx/sub context :ret)
                   (fx/sub context :no-args?)
                   (fx/sub context :exact-ret-match?))]
      {:fx/type :table-view
       :columns [{:fx/type            :table-column
                  :min-width          150
                  :text               "function"
                  :cell-value-factory identity
                  :cell-factory       (fn [x]
                                        {:text (pr-str (:sym x))})}
                 {:fx/type            :table-column
                  :min-width          150
                  :text               "arguments"
                  :cell-value-factory identity
                  :cell-factory       (fn [x]
                                        {:text (pr-str (:printable-args x))})}
                 {:fx/type            :table-column
                  :min-width          150
                  :text               "return value"
                  :cell-value-factory identity
                  :cell-factory       (fn [x]
                                        {:text (pr-str (:ret-val x))})}]
       :items   result})))

(defn text-area-input [{:keys [fx/context key]}]
  {:fx/type  :v-box
   :children [{:fx/type :label
               :text    (name key)}
              {:fx/type         :text-field
               :on-text-changed #(swap! *state fx/swap-context assoc key %)
               :text            (fx/sub context key)}]})

(defn root-view [_]
  {:fx/type :stage
   :showing true
   :always-on-top true
   :scene   {:fx/type :scene
             :root    {:fx/type     :v-box
                       :pref-width  600
                       :pref-height 400
                       :children    [{:fx/type      text-area-input
                                      :key          :args
                                      :label        "Arguments"
                                      :v-box/margin 5}

                                     {:fx/type      text-area-input
                                      :v-box/margin 5
                                      :label        "Return Value"
                                      :key          :ret}

                                     {:fx/type      table-view
                                      :v-box/margin 5}]}}})

(def renderer
  (fx/create-renderer
    :middleware (comp
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_]
                                      {:fx/type root-view})))
    :opts {:fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(defn ui []
  (fx/mount-renderer *state renderer))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ui))

(comment

  (ui)

  )

;
;1. Intro
;2. Survey of Clojure JavaFX Ecosystem
;3. Discuss OpenJFX 11 and explain why JavaFX was removed from JDK11
;4. Walk through some #cljfx examples, showing some different ways to interactively develop an app at the repl.
;5. Interlude to discuss re-find
;6. Build re-find.fx, importing the re-find logic from a different ns make it explicit how little code is needed to build useful tools and GUI’s
;7. Call to Action for building an ecosystem of simpler, non-monolithic clojure tools
;8. Outro & Questions
