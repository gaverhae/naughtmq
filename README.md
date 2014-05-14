# naughtmq

NaughtMQ is a zero-config, zero-hassle way to use ZeroMQ with the JVM on the
three major platforms (Linux, OS X, Windows).

## Disclaimer

As the version number suggest, this is a very early, alpha-level release.
Expect things to break. At the moment, I have been able to successfully run the
example programs below (in a `lein uberjar`) on OS X, Linux x86  and Linux
x86_64 (the same virtual machines as used for compiling, though on a clean
slate), and on Windows XP x86. I do not have access to a 64-bit version of
Windows for testing, though the package should work (Visual Studio allows for
the compilation of 64bit binaries on a 32bit machine).

## Usage

### From Clojure

To include this library in a Leiningen-based project, add this line to your
dependencies:

```clojure
[naughtmq "0.0.1"]
```

Then, in your main namespace, require or use the `naughtmq.core` namespace
**before** you import `org.zeromq.ZMQ`. As Clojure loads Java classes on
import, you have to structure your `ns` declaration such that `naughtmq` comes
first. Here is a minimal example of a Clojure namespace using `naughtmq`:

```clojure
(ns zmq-test.core
  (:require [naughtmq.core :as nc])
  (:import  [org.zeromq ZMQ]))

(defn -main
  [& args]
  (println (ZMQ/getFullVersion)))
```

To allow for `org.zeromq.ZMQ` to be imported in the `ns` declaration, I have
set `naughtmq.core` to load the native libraries upon compilation. This means
that there is nothing else to do than `require`ing the namespace.

### From Java

The jar is on [clojars](https://clojars.org/), so you will have to add:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

to your `pom.xml`, in addition to the dependency to this library:

```xml
<dependency>
  <groupId>naughtmq</groupId>
  <artifactId>naughtmq</artifactId>
  <version>0.0.1</version>
</dependency>
```

Java, in contrast with Clojure, does *not* actually load classes upon import.
The JVM actually waits for the first use of a classe to load it, which means
that the usage for `naughtmq` is quite natural. Here is a minimal example:

```java
package test.zmq.with.naught;

import org.zeromq.ZMQ; // order does not matter here

public class Main {
    // this line must appear before any ZMQ call
    static { naughtmq.Loader.load(); }

    public static void main(String[] args) {
        System.out.println(ZMQ.getFullVersion()); // ZMQ is loaded here
        // since the native libs have already been loaded by this point,
        // everything works.
    }
}
```

## How to build

If you want to build the binaries yourself (for example because you need a
different version of ZeroMQ), the process is, for the most part, easily
reproducible. To build the OS X binaries on OS X, just execute the Rakefile in
`resources/native/x86_64/mac`. To build the Linux binaries, any UNIX with
[Rake](http://rake.rubyforge.org/) and [Vagrant](http://www.vagrantup.com/)
will do (I have not made any extensive testing with other versions; I have used
Vagrant 1.5.4; I believe 1.5 is mandatory for the [new boxes
system](https://vagrantcloud.com/)).

On Windows, I have no idea how to automate things. The build process is [quite
straightforward](http://zeromq.org/bindings:java) if you have access to Visual
Studio.

## License

Copyright © 2014 Gary Verhaegen

Distributed under the Eclipse Public License, the same as Clojure.
