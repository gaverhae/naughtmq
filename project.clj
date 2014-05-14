(defproject naughtmq "0.0.2-SNAPSHOT"
  :description "A native-embedding wrapper for jzmq"
  :url "https://github.com/gaverhae/naughtmq"
  :scm {:name "git"
        :url "https://github.com/gaverhae/naughtmq"}
  :signing {:gpg-key "gary.verhaegen@gmail.com"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :pom-addition [:developers [:developer
                              [:name "Gary Verhaegen"]
                              [:url "https://github.com/gaverhae"]
                              [:email "gary.verhaegen@gmail.com"]
                              [:timezone "+1"]]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 [org.zeromq/jzmq "2.2.2"]]
  :java-source-paths ["src"])
