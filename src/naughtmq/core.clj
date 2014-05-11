(ns naughtmq.core
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io])
  (:import [naughtmq PrivateLoader]
           [org.zeromq EmbeddedLibraryTools]
           [java.lang.reflect Field Modifier]))

(def version-for
  {"jzmq" "2.2.2"
   "zmq"  "4.0.4"})

(defn- os
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

(defn- arch
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
      "amd64"  "x86_64"
      (throw (UnsupportedOperationException.
               (str "Unsupported platform: " os-name ", " os-arch))))))

(defn- os-temp-dir
  "Returns an OS-specific temp dir. Mostly needed because java.io.tmpdir yields
  nondeterministic results on Mac OS X, which, combined with the rather rigid
  way in which Mac OS X resolves dynamic libraries, makes it unreliable."
  [lib-name]
  (io/file (str (cond (= "mac" (os)) "/tmp/"
                      :else          (System/getProperty "java.io.tmpdir"))
                "/naughtmq/" lib-name "/" (version-for lib-name) "/")))

(defn- os-name
  "Returns an os-specific name for the given library name. For example, zmq
  will become libzmq.dylib on OS X."
  [lib-name]
  (condp = (os)
    "mac"   (str "lib" lib-name ".dylib")
    "linux" (str "lib" lib-name ".so")
    "win"   (str lib-name ".dll")))

(defn- save-library
  [lib-name]
  (let [lib-path (str "native/" (arch) "/" (os) "/" (os-name lib-name))
        tmp-dir  (os-temp-dir lib-name)
        tmp-path (-> (str tmp-dir "/" (os-name lib-name)) io/file)]
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
    (try (PrivateLoader/load file_path)
      (catch java.io.IOException e
        (log/error (str "Could not load native file: " s))
        (throw e)))))

(defn- load-libraries
  "Loads the native libraries."
  []
  (let [libs (get {"win"   ["msvcr100" "msvcp100" "libzmq" "jzmq"]
                   "linux" ["zmq" "jzmq"]
                   "mac"   ["zmq" "jzmq"]}
                  (os))]
    (doseq [l libs] (load-library l))))

(defn- disable-jzmq-dynamic-loading
  "Prevents jzmq to look for the native library in java.library.path, as the
  whole point of this namespace is that the native libraries must already be
  loaded when the JVM loads the ZMQ class. This requires setting a final field
  of the org.zeromq.EmbeddedLibraryTools class."
  []
  (let [f (.getField EmbeddedLibraryTools "LOADED_EMBEDDED_LIBRARY")
        modif (.getDeclaredField Field "modifiers")]
    (.setAccessible f true)
    (.setAccessible modif true)
    (.setInt modif f (bit-and (.getModifiers f) (bit-not Modifier/FINAL)))
    (.set f nil true)
    (.setInt modif f (bit-and (.getModifiers f) Modifier/FINAL))))

(defn load-zmq-native
  "Loads all required native libraries for using ZeroMQ on the current
  platform. This is done by first extracting the binaries from the JAR, then
  copying them to /tmp/naughtmq/ (or the equivalent on Windows), and then
  asking the JVM to load the native libraries there. This is done in the
  correct order, so that the JVM does not need to look at java.library.path.
  Additionally, prevents JZMQ itself from asking the JVM to load the native
  libraries."
  []
  (load-libraries)
  (disable-jzmq-dynamic-loading))

(load-zmq-native)
