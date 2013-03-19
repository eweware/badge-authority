package main.java.com.eweware.badging.mgr;

import java.util.logging.Logger;

/**
 * <p>Manages system-wide state.</p>
 *
 * @author rk@post.harvard.edu
 *         Date: 3/8/13 Time: 1:29 PM
 */
public final class SystemManager {

    private static final Logger logger = Logger.getLogger("SystemManager");

    private static SystemManager singleton;

    private final boolean devMode;

    public SystemManager() {
        devMode = (System.getenv("BLAHGUA_DEV_MODE") != null);
        singleton = this;
    }

    public static SystemManager getInstance() {
        return singleton;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void start() {
        System.out.println("*** SystemManager Started ***");
    }

    public void shutdown() {
        System.out.println("*** SystemManager Shutdown ***");
    }
}
