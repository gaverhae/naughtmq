package naughtmq;

import java.util.concurrent.atomic.AtomicBoolean;
import clojure.lang.IFn;
import clojure.java.api.Clojure;

public final class Loader {
    private Loader() {}
    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    public static void load() {
        if(loaded.compareAndSet(false, true)) {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("naughtmq.core"));
        }
    }
}
