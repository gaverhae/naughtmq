package naughtmq;

/**
 * This class is reserved for internal use.
 */
public final class PrivateLoader {
    public static void load(String path) {
        System.load(path);
    }
    private PrivateLoader() {};
}
