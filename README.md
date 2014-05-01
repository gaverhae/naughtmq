# naughtmq

NaughtMQ is a high-level wrapper around ZeroMQ for Clojure. It serves two distinct purposes:

* Provide higher-level abstractions around ZeroMQ. Traditionally, ZeroMQ bindings remain fairly close to the C bindings. This is good, in that it allows programmer knowledge about ZeroMQ to be transferable accross languages, and it allows the one true [guide](http://zguide.zeromq.org) to serve as a unique, language-independent reference. However, in Clojure land, there is a case to be made for a more data-oriented API.

* Provide native libraries as part of the JAR. This is somewhat tricky, as the JAR format is not meant to carry native libraries (the JVM cannot, as far as I know, load native libraries directly from a ZIP file), in addition to the usual trust questions raised by the distribution of binary files. In this respect, this library may be seen (and used!) as just a way to embed the native libraries, as both [JZMQ](https://github.com/zeromq/jzmq) and [cljzmq](https://github.com/zeromq/cljzmq) are declared as dependencies, and therefore accessible.

## Usage

FIXME

## License

Copyright © 2013 Gary Verhaegen

Distributed under the Eclipse Public License, the same as Clojure.
