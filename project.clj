;;; -*- Mode: CLOJURE; -*-

;;; Leiningen project for Snake.

;;; Copyright (c) 2014 Mark Karpov

;;; This program is free software: you can redistribute it and/or
;;; modify it under the terms of the GNU General Public License as
;;; published by the Free Software Foundation, either version 3 of the
;;; License, or (at your option) any later version.

;;; This program is distributed in the hope that it will be useful,
;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
;;; General Public License for more details.

(defproject snake "0.1.0"
  :description "classic snake game, it uses swing for gui"
  :url "https://github.com/mrkkrp/snake"
  :license {:name "GNU GPL"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot snake.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
