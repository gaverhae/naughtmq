(ns naughtmq.core
  (:require [taoensso.timbre :as log]
            [pandect.core :as p]
            [clojure.java.io :as io]))

(defn- os-specific-path
  "Finds the OS specific path for the given library name."
  [s]
  (let [os-arch (. (System/getProperty "os.arch") toLowerCase)
        os-name (. (System/getProperty "os.name") toLowerCase)]
    (cond (and (.startsWith os-name "win") (= "x86" os-arch))
          (str "win-x86/" s ".dll")

          (and (.startsWith os-name "win") (= "x64" os-arch))
          (str "win-x86_64/" s ".dll")

          (and (.startsWith os-name "mac")) ; assume all macs are 64bits
          (str "macosx/" s ".dylib")

          (and (.startsWith os-name "linux") (= "i386" os-arch))
          (str "linux-x86/" s ".so")

          (and (.startsWith os-name "linux") (= "amd64" os-arch))
          (str "linux-x86_64/" s ".so")

          :else (throw (UnsupportedOperationException.
                         (str "Unsupported platform: " os-name ", " os-arch
                              " for library " s))))))

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
    path))

(defn- load-library
  "Loads the given file as a native library. The file must be in the native
  folder in the CLASSPATH."
  [s]
  (let [file_path (save-library s)]
    (try (System/load file_path)
      (catch java.io.IOException e
        (log/error (str "Could not load native file: " s))
        (throw e)))))
