(ns re-find.match
  (:require
    [speculative.instrument]
    [re-find.core :as re-find :refer [match]]
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

(defn compute [args ret no-args? exact-ret-match?]
  (let [from-example? false
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
    (if-not (and (not (str/blank? args))
                 (not= ::invalid args*)
                 (not= ::invalid ret*))
      []
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
                           nil))
            results (map (fn [m]
                           (cond-> m
                                   ret*
                                   (assoc :type-score (type-score ret-val (:ret-val m)))))
                         results)
            results (if more? (take 50 results) results)
            ;; from here on, results can be fully realized
            no-perm-syms (set (keep #(when (not (:permutation? %))
                                       (:sym %)) results))]
        (map #(if (:permutation? %)
                (assoc % :duplicate? (contains? no-perm-syms (:sym %)))
                %) results)))))
