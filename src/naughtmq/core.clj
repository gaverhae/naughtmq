(ns naughtmq.core
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io]))

(defn os-specific-path
  "Finds the OS specific path for the given library name."
  [s]
  (let [os-arch (. (System/getProperty "os.arch") toLowerCase)
        os-name (. (System/getProperty "os.name") toLowerCase)]
    (cond (and (.startsWith os-name "win") (= "x86" os-arch))
          (str "win-x86/" s ".dll")

          (and (.startsWith os-name "linux") (= "i386" os-arch))
          (str "linux-x86/" s ".so")

          (and (.startsWith os-name "linux") (= "amd64" os-arch))
          (str "linux-x86_64/" s ".so")

          :else (throw (UnsupportedOperationException.
                         (str "Unsupported platform: " os-name ", " os-arch
                              " for library " s))))))

(defn copy-stream
  "Copies in to out."
  [^java.io.InputStream in ^java.io.OutputStream out]
  (with-local-vars [buf (byte-array (* 16 1024))]
    (loop []
      (let [cnt (.read in buf)]
        (when (>= cnt -1)
          (.write out buf 0 cnt)
          (recur))))))

(defn save-library
  [s v]
  (let [lib-name (os-specific-path s)
        lib-path (str v "/lib/" lib-name)
        tmp-path (System/getProperty "java.io.tmpdir")]
    (with-open [in (-> lib-path io/resource io/input-stream)
                tmp-dir (java.io.File. (str tmp-path "/naughtmq/" v))]
      (if (not (.exists tmp-dir)) (.mkdirs tmp-dir))
      (with-open [file (java.io.File/createTempFile (str s "-") ".tmp" tmp-dir)
                  out (-> file io/output-stream)]
        (.deleteOnExit file)
        (copy-stream in out)
        (let [path (.getAbsolutePath file)]
          (log/info (str "Saved lib to: " path))
          path)))))

(defn load-library
  "Loads the given path (string) as a native library."
  [s v]
  (let [file_path (save-library s v)]
    (try (System/load file_path)
      (catch java.io.IOException e
        (log/error (str "Could not load native file: " s))
        (throw e)))))
