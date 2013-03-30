package main.java.com.eweware.badging.mgr;

import main.java.com.eweware.badging.base.SystemErrorException;

import javax.xml.ws.WebServiceException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private SecureRandom randomizer;

    public SystemManager() {
        devMode = (System.getenv("BLAHGUA_DEV_MODE") != null);
        final String randomProvider = "SHA1PRNG";
        try {
            this.randomizer = SecureRandom.getInstance(randomProvider);
            randomizer.generateSeed(20);
        } catch (NoSuchAlgorithmException e) {
            throw new WebServiceException("Failed to initialized SystemManager due to unavailable secure random provider '"+randomProvider+"'", e);
        }
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

    public byte[] setSecureRandomBytes(byte[] rand) throws UnsupportedEncodingException {
        // TODO reseed this once in a while
        randomizer.nextBytes(rand);
        return rand;
    }
}
