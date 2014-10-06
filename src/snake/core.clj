;;; -*- Mode: CLOJURE; -*-

;;; Classic snake game, it uses Swing for GUI.

;;; Copyright (c) 2014 Mark Karpov

;;; This program is free software: you can redistribute it and/or
;;; modify it under the terms of the GNU General Public License as
;;; published by the Free Software Foundation, either version 3 of the
;;; License, or (at your option) any later version.

;;; This program is distributed in the hope that it will be useful,
;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
;;; General Public License for more details.

(ns snake.core
  (:import (java.awt Color Dimension Font)
           (javax.swing JPanel JFrame Timer JOptionPane)
           (java.awt.event ActionListener KeyListener KeyEvent))
  (:gen-class))

;;; constants

(def c-width   "number of horizontal elements on the court" 20)
(def c-height  "number of vertical elements on the court"   20)
(def e-size    "size of an element in pixels"               14)
(def i-quantum "initial duration of repainting period"      100)
(def d-quantum "change of the duration for two succ levels" -5)
(def m-quantum "limit for the duration"                     50)
(def i-length  "initial length for the snake to win"        5)
(def d-length  "change of the length for two succ levels"   3)
(def p-width   "width of the game panel"   (* c-width e-size))
(def p-height  "height of the game panel" (* c-height e-size))
(def dirs      "mapping from even code to direction"
  {KeyEvent/VK_LEFT  [-1  0]
   KeyEvent/VK_RIGHT [ 1  0]
   KeyEvent/VK_UP    [ 0 -1]
   KeyEvent/VK_DOWN  [ 0  1]})

;;; colors

(def color-variation  35)
(def bright-sum       350)
(def bright-diff      250)
(def background-color (Color/WHITE))
(def text-color       (Color/DARK_GRAY))

;;; pure section

(defn quantum [level]
  "Evaluates period of repainting based on level."
  (max (+ i-quantum (* level d-quantum)) m-quantum))

(defn length [level]
  "Evaluates length of the snake that will cause win."
  (+ i-length (* level d-length)))

(def length (memoize length)) ; the function is called every period

(defn add-points [[x0 y0] [x1 y1]]
  "Adds two points, used to shift head to the snake."
  [(+ x0 x1) (+ y0 y1)])

(defn move [{:keys [body dir] :as snake} grows]
  "Evaluates snake after one move."
  (assoc snake :body
         (cons (add-points (first body) dir)
               (if grows body (butlast body)))))

(defn turn [snake dir]
  (assoc snake :dir dir))

(defn win? [{body :body} level]
  "Snake wins if it is long enough for actual level."
  (>= (count body) (length level)))

(defn eats-self? [[head & tail]]
  "Evaluates true if snake do some cannibalism."
  (contains? (set tail) head))

(defn eats-border? [[[x y]]]
  "Evaluates true when snake starts to eat border!"
  (or (>= x c-width)
      (>= y c-height)
      (< x 0)
      (< y 0)))

(defn lose? [{body :body}]
  "Snake loses when it eats something but the apple!"
  (or (eats-self?   body)
      (eats-border? body)))

(defn eats-apple? [{[head] :body} {[apple] :body}]
  "Evaluates true when snake eats the apple."
  (= head apple))

(defn screen-rect [[x y]]
  "Converts a pair of coordinates into x, y, width, and height of a
  rectangle on the screen."
  (map (fn [x] (* e-size x))
       [x y 1 1]))

(def screen-rect (memoize screen-rect)) ; creating 'table' of relations

;;; random values

(defn ->color [[r g b]]
  (Color. r g b))

(defn bright-color []
  "Generates random bright color."
  (->> (repeatedly #(rand-int 256))
       (partition 3 1)
       (some #(when (= (apply + %) bright-sum) %))))

(defn contrast? [x y]
  "Returns second vector if two vectors are contrast enough."
  (letfn [(diff [x y] (Math/abs (- x y)))]
    (if (>= (apply + (map diff x y))
            bright-diff)
      y nil)))

(defn contrast-color [color]
  "Generates random contrast color based on given one."
  (->> (repeatedly bright-color)
       (some (partial contrast? color))))

(defn vary-component [x]
  "Varies component of color."
  (letfn [(pm [x] [(rand-int x) (rand-int (- x))])]
    (let [x (apply + x (pm color-variation))]
      (cond (> x 255) 255
            (< x 0)   0
            :else     x))))

(defn vary-color [color]
  "Varies given color."
  (->color (map vary-component color)))

(defn new-snake []
  "Creates brand new snake."
  {:body  (list [1 1])
   :dir   [1 0]
   :color (bright-color)})

(defn new-apple-for [{color :color}]
  "Creates brand new apple."
  {:body  [[(rand-int c-width)
            (rand-int c-height)]]
   :color (contrast-color color)})

;;; non-pure section

(defn reset-game [snake apple pause]
  "Resets game."
  (dosync
   (ref-set snake (new-snake))
   (ref-set apple (new-apple-for @snake))
   (ref-set pause true))
  nil)

(defn update-dir [snake dir]
  "Updates direction of snake."
  (when dir
    (dosync (alter snake turn dir))))

(defn update-pos [snake apple]
  "Updates positions of snake and apple."
  (dosync
   (if (eats-apple? @snake @apple)
     (do (ref-set apple (new-apple-for @snake))
         (alter   snake move true))
     (alter snake move false)))
  nil)

;;; GUI

(defn paint [g {:keys [body color]}]
  "Paints constructions like snake or apple."
  (doseq [[x y w h] (map screen-rect body)]
    (doto g
      (.setColor (vary-color color))
      (.fillRect x y w h))))

(defn show-text [g title subtitle]
  "Shows some text: title and subtitle."
  (doto g
    (.setColor text-color)
    (.setFont (Font. "Tahoma" Font/TRUETYPE_FONT 30))
    (.drawString title 80 120)
    (.setFont (Font. "Tahoma" Font/TRUETYPE_FONT 12))
    (.drawString subtitle 60 150)))

(defn game-panel [snake apple level pause timer]
  "Creates game panel."
  (proxy [JPanel ActionListener KeyListener] []
    (paintComponent [g]
      (proxy-super paintComponent g)
      (if @pause
        (show-text g (str "Level " @level)
                   "Press any key to continue...")
        (do (paint g @snake)
            (paint g @apple))))
    (actionPerformed [e]
      (when-not @pause
        (update-pos snake apple))
      (when (lose? @snake)
        (reset-game snake apple pause))
      (when (win? @snake @level)
        (swap! level inc)
        (reset-game snake apple pause)
        (.setDelay timer (quantum @level)))
      (.repaint this))
    (keyPressed [e]
      (if @pause
        (dosync (ref-set pause false))
        (update-dir snake (dirs (.getKeyCode e)))))
    (windowClosed []
      (System/exit 0))
    (keyReleased [e])
    (keyTyped    [e])))

(defn game []
  "Creates some stuff and starts the game."
  (let [snake  (ref (new-snake))
        apple  (ref (new-apple-for @snake))
        level  (atom 0)
        pause  (ref true)
        frame  (JFrame. "Snake")
        timer  (Timer. (quantum @level) nil)
        panel  (game-panel snake apple level pause timer)]
    (doto panel
      (.setFocusable   true)
      (.addKeyListener panel)
      (.setBackground  background-color)
      (.setPreferredSize (Dimension. p-width p-height)))
    (doto frame
      (.add panel)
      (.pack)
      (.setVisible true)
      (.setResizable false)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setLocationRelativeTo nil))
    (doto timer
      (.addActionListener panel)
      (.start))
    [snake apple level timer]))

(defn -main [& args]
  "The main function."
  (game))
