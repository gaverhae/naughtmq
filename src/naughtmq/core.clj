(ns naughtmq.core
  (:require [taoensso.timbre :as log]
            [pandect.core :as p]
            [clojure.java.io :as io]))

(defn platform
  "Returns a keyword representing the current platform, one of :win32, :win64,
  :linux32, :linux64, or :mac."
  []
  (let [os-arch (. (System/getProperty "os.arch") toLowerCase)
        os-name (. (System/getProperty "os.name") toLowerCase)]
    (cond (and (.startsWith os-name "win") (= "x86" os-arch))      :win32
          (and (.startsWith os-name "win") (= "x64" os-arch))      :win64
          (and (.startsWith os-name "mac"))                        :mac
          (and (.startsWith os-name "linux") (= "i386" os-arch))   :linux32
          (and (.startsWith os-name "linux") (= "amd64" os-arch))  :linux64
          :else
          (throw (UnsupportedOperationException.
                   (str "Unsupported platform: " os-name ", " os-arch))))))

(defn- os-specific-path
  "Finds the OS specific path for the given library name."
  [s]
  (get {:win32   (str "win-x86/" s ".dll")
        :win64   (str "win-x86_64/" s ".dll")
        :mac     (str "macosx/" s ".dylib")
        :linux32 (str "linux-x86/" s ".so")
        :linux64 (str "linux-x86_64/" s ".so")}
       (platform)))

(defn- save-library
  [s]
  (let [lib-name (os-specific-path s)
        lib-path (str "/native/" lib-name)
        tmp-dir  (io/file (str (System/getProperty "java.io.tmpdir")
                            "/naughtmq/" (p/sha1-file lib-path) "/"))
        tmp-path (-> (str tmp-dir "/" s) io/file .getAbsolutePath)]
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
    tmp-path))

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
