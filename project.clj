(defproject naughtmq "0.0.1-SNAPSHOT"
  :description "A high-level Clojure wrapper for ZeroMQ"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 [org.zeromq/jzmq "2.2.2"]]
  :java-source-paths ["src"])
