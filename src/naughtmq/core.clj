(ns naughtmq.core
  (:require [taoensso.timbre :as log]
            [pandect.core :as p]
            [clojure.java.io :as io]))

(defn os
  "Returns a string representing the current operating system, one of win,
  linux, or mac."
  []
  (let [os-arch (. (System/getProperty "os.arch") toLowerCase)
        os-name (. (System/getProperty "os.name") toLowerCase)]
    (cond (.startsWith os-name "win")    "win"
          (.startsWith os-name "mac")    "mac"
          (.startsWith os-name "linux")  "linux"
          :else
          (throw (UnsupportedOperationException.
                   (str "Unsupported platform: " os-name ", " os-arch))))))

(defn arch
  "Returns a string representing the current architecture, one of x86 or
  x86_64."
  []
  (let [os-arch (. (System/getProperty "os.arch") toLowerCase)
        os-name (. (System/getProperty "os.name") toLowerCase)]
    (condp = os-arch
      "x86"    "x86"
      "i386"   "x86"
      "i686"   "x86"
      "x86_64" "x86_64"
      (throw (UnsupportedOperationException.
               (str "Unsupported platform: " os-name ", " os-arch))))))

(defn- save-library
  [lib-name]
  (let [lib-path (str "native/" (arch) "/" (os) "/" lib-name)
        tmp-dir  (io/file (str (System/getProperty "java.io.tmpdir")
                            "/naughtmq/"
                            (-> lib-path io/resource io/input-stream p/sha1)
                            "/"))
        tmp-path (-> (str tmp-dir "/" lib-name) io/file)]
    (if (not (.exists tmp-dir)) (.mkdirs tmp-dir))
    (if (not (.exists tmp-path))
      (with-open [in  (-> lib-path io/resource io/input-stream)
                  out (-> tmp-path io/output-stream)]
        (io/copy in out)
        (log/info (str "Saved lib to: " tmp-path)))
      (with-open [in  (-> lib-path io/resource io/input-stream)
                  out (-> tmp-path io/input-stream)]
        (let [to-byte-seq (fn [is] (let [os (java.io.ByteArrayOutputStream.)]
                                     (io/copy is os)
                                     (-> os .toByteArray seq)))]
          (if (not (= (to-byte-seq in)
                      (to-byte-seq out)))
            (throw
              (IllegalStateException.
                (str "File "
                     tmp-path
                     " already exists but has different content.")))
            (log/info (str "Lib was already there: " tmp-path))))))
    (.getAbsolutePath tmp-path)))

(defn- load-library
  "Loads the given file as a native library. The file must be in the native
  folder in the CLASSPATH."
  [s]
  (let [file_path (save-library s)]
    (try (System/load file_path)
      (catch java.io.IOException e
        (log/error (str "Could not load native file: " s))
        (throw e)))))

(defn load-zmq-native
  "Loads all required native libraries for using ZeroMQ on the current
  platform. This is done by first extracting the binaries from the JAR, then
  copying them to /tmp/naughtmq/ (or the equivalent on Windows), and then
  asking the JVM to load the native libraries there. This is done in the
  correct order, so that the JVM does not need to look at java.library.path."
  []
  (let [libs (get {:win32 ["jzmq.dll" "libzmq.dll" "msvcp100.dll" "msvcr100.dll"]
                   :win64 ["jzmq.dll" "libzmq.dll" "msvcp100.dll" "msvcr100.dll"]
                   :linux32 ["libjzmq.so" "libzmq.so"]
                   :linux64 ["libjzmq.so" "libzmq.so"]
                   :mac ["libjzmq.dylib" "libzmq.dylib"]}
                  (platform))]
    (doseq [l libs] (load-library l))))
