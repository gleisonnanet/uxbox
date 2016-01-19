(ns uxbox.shapes
  (:require [uxbox.util.matrix :as mtx]
            [uxbox.util.math :as mth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:static ^:private +hierarchy+
  (as-> (make-hierarchy) $
    (derive $ :builtin/icon ::shape)
    (derive $ :builtin/icon-svg ::shape)
    (derive $ :builtin/group ::shape)))

(defn shape?
  [type]
  {:pre [(keyword? type)]}
  (isa? +hierarchy+ type ::shape))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- dispatch-by-type
  [shape & params]
  (:type shape))

(defmulti -render
  dispatch-by-type
  :hierarchy #'+hierarchy+)

(defmulti -render-svg
  dispatch-by-type
  :hierarchy #'+hierarchy+)

(defmulti -move
  dispatch-by-type
  :hierarchy #'+hierarchy+)

(defmulti -resize
  dispatch-by-type
  :hierarchy #'+hierarchy+)

(defmulti -rotate
  dispatch-by-type
  :hierarchy #'+hierarchy+)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod -move ::shape
  [shape {:keys [dx dy] :as opts}]
  (assoc shape
         :x (+ (:x shape) dx)
         :y (+ (:y shape) dy)))

(defmethod -resize ::shape
  [shape {:keys [width height] :as opts}]
  (assoc shape
         :width width
         :height height))

(defmethod -rotate ::shape
  [shape rotation]
  (assoc shape :rotation rotation))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn container-rect
  [{:keys [x y width height rotation] :as shape}]
  (let [center-x (+ x (/ width 2))
        center-y (+ y (/ height 2))

        angle (mth/radians (or rotation 0))
        x1 (- x center-x)
        y1 (- y center-y)

        x2 (- (+ x width) center-x)
        y2 (- y center-y)

        rx1 (- (* x1 (mth/cos angle))
               (* y1 (mth/sin angle)))
        ry1 (+ (* x1 (mth/sin angle))
               (* y1 (mth/cos angle)))

        rx2 (- (* x2 (mth/cos angle))
               (* y2 (mth/sin angle)))
        ry2 (+ (* x2 (mth/sin angle))
               (* y2 (mth/cos angle)))

        [d1 d2] (cond
                  (and (>= rotation 0)
                       (< rotation 90))
                  [(mth/abs ry1)
                   (mth/abs rx2)]

                  (and (>= rotation 90)
                       (< rotation 180))
                  [(mth/abs ry2)
                   (mth/abs rx1)]

                  (and (>= rotation 180)
                       (< rotation 270))
                  [(mth/abs ry1)
                   (mth/abs rx2)]

                  (and (>= rotation 270)
                       (<= rotation 360))
                  [(mth/abs ry2)
                   (mth/abs rx1)])
        final-x (- center-x d2)
        final-y (- center-y d1)
        final-width (* d2 2)
        final-height (* d1 2)]
    {:x final-x
     :y final-y
     :width final-width
     :height final-height}))

(defn group-size-and-position
  "Given a collection of shapes, calculates the
  dimensions of possible envolving rect.

  Mainly used for calculate the selection
  rect or shapes grop size."
  [shapes]
  {:pre [(seq shapes)]}
  (let [shapes (map container-rect shapes)
        x (apply min (map :x shapes))
        y (apply min (map :y shapes))
        x' (apply max (map (fn [{:keys [x width]}] (+ x width)) shapes))
        y' (apply max (map (fn [{:keys [y height]}] (+ y height)) shapes))
        width (- x' x)
        height (- y' y)]
    {:width (- x' x)
     :height (- y' y)
     :x x
     :y y}))

(defn translate-coords
  "Given a shape and initial coords, transform
  it mapping its coords to new provided initial coords."
  [shape x y]
  (let [x' (:x shape)
        y' (:y shape)]
    (assoc shape :x (- x' x) :y (- y' y))))
