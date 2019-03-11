(ns re-find.fx
  (:gen-class)
  (:require [cljfx.api :as fx]
            [speculative.instrument]
            [re-find.core :as re-find :refer [match]]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn read-args [s]
  (try
    (eval (read-string (format "[%s]" s)))
    (catch Exception e
      ::invalid))
  )

(defn read-ret [s]
  (try
    (eval (read-string s))
    (catch Exception e
      ::invalid)))

(def init-state {:args             "inc [1 2 3]"
                 :ret              "[2 3 4]"
                 :exact-ret-match? true
                 :help?            false
                 :more?            false})

(def *state (atom (fx/create-context init-state)))

(defn mapply
  "Applies a function f to the argument list formed by concatenating
  everything but the last element of args with the last element of
  args.  This is useful for applying a function that accepts keyword
  arguments to a map."
  [f & args]
  (apply f (apply concat (butlast args) (last args))))

(defn type-score [v1 v2]
  (cond (= (type v1) (type v2)) 1
        (and (coll? v1) (coll? v2)) 0.8
        :else 0))

(defn table-view [{:keys [fx/context]}]
  (binding [*print-length* 10]
    (let [
          args (fx/sub context :args)
          ret (fx/sub context :ret)
          exact-ret-match? (fx/sub context :exact-ret-match?)
          permutations? (fx/sub context :permutations?)
          no-args? (fx/sub context :no-args?)
          from-example? false
          ;from-example? (and (empty? args)
          ;                   (empty? ret))
          [args* ret*] (if from-example?
                         ;[(eval-str (:args @example-state))
                         ; (eval-str (:ret @example-state))]
                         []
                         [(when-not (and (str/blank? args) no-args?)
                            (read-args args))
                          (when-not (str/blank? ret)
                            (read-ret ret))])]
      (if (and (not= ::invalid args*)
               (not= ::invalid ret*))
        (let [printable-args (if from-example?
                               (read-string (:args
                                              nil
                                              ;@example-state
                                              ))
                               (read-string args))
              args? (and args*
                         (not= args* ::invalid)
                         (some? args*))
              ret? (and ret* (not= ret* ::invalid))
              more? (if from-example?
                      (:more? nil
                        ;@example-state
                        )
                      false #_(:more? @delayed-state))
              ret-val (when ret? ret*)
              ret-pred (and ret?
                            (cond (fn? ret-val)
                                  ret-val))
              match-args (cond-> {:printable-args printable-args
                                  :finitize?      true}
                                 more? (assoc :permutations? true
                                              :sequential? true
                                              :splice-last-arg? true)
                                 args?
                                 (assoc :args args*)
                                 ret?
                                 (assoc :ret (or ret-pred ret-val))
                                 (and (not ret-pred)
                                      args*
                                      ret*
                                      (not more?))
                                 (assoc :exact-ret-match? exact-ret-match?))
              results (try (mapply re-find/match match-args)
                           (catch Exception e
                             (println e)
                             nil))
              results (map (fn [m]
                             (cond-> m
                                     ret*
                                     (assoc :type-score (type-score ret-val (:ret-val m)))))
                           results)]
          (let [results (if more? (take 50 results) results)
                ;; from here on, results can be fully realized
                no-perm-syms (set (keep #(when (not (:permutation? %))
                                           (:sym %)) results))
                results (map #(if (:permutation? %)
                                (assoc % :duplicate? (contains? no-perm-syms (:sym %)))
                                %) results)]
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
             :items   results}))
        ;; default table when invalid entry
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
         :items   []}))))

(defn text-area-input [{:keys [fx/context label key]}]
  {:fx/type  :v-box
   :children [{:fx/type :label
               :text    (name key)}
              {:fx/type         :text-field
               :on-text-changed #(swap! *state fx/swap-context assoc key %)
               :text            (fx/sub context key)}]})

(defn root-view [_]
  {:fx/type :stage
   :showing true
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
                                      :v-box/margin 5}
                                     ]}}})

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
