(ns clj-kondo-test
  (:require [malli.core :as m]
            [malli.clj-kondo :as mc]))

(defn add [x y]
  (str (+ x y)))

(m/=> add [:=> [:cat number? number?] string?])

(comment (mc/emit!))

(comment (+ 1 (add 1 2)))
