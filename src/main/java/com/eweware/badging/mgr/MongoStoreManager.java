package main.java.com.eweware.badging.mgr;

import com.mongodb.*;

import javax.xml.ws.WebServiceException;
import java.util.logging.Logger;

/**
 * <p>Manages mongo store</p>
 *
 * @author rk@post.harvard.edu
 *         Date: 3/8/13 Time: 1:30 PM
 */
public final class MongoStoreManager extends StoreManager {

    private static final Logger logger = Logger.getLogger("MongoStoreManager");

    private final String hostname;
    private final Integer port;
    private final Integer connectionsPerHost;
    private String badgeDBName;
    private String badgeCollectionName;
    private Mongo mongo;
    private DBCollection badgesCollection;

    public MongoStoreManager(
        String hostname,
        Integer port,
        Integer connectionsPerHost
    ) {
        this.hostname = hostname;
        this.port = port;
        this.connectionsPerHost = connectionsPerHost;
    }

    public void start() {
        try {
            final MongoOptions mongoOptions = new MongoOptions();
            mongoOptions.connectionsPerHost = connectionsPerHost;
            final ServerAddress serverAddress = new ServerAddress(SystemManager.getInstance().isDevMode() ? "localhost" : hostname, port);
            mongo = new Mongo(serverAddress, mongoOptions);
            final DB db = mongo.getDB(getBadgeDBName());
            badgesCollection = db.getCollection(getBadgeCollectionName());
            logger.info("MongoDB Status: " + db.command(new BasicDBObject("serverStatus", 1)));

            // Add a shutdown hook to keep it independent of Spring
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mongo != null) {
                        mongo.close();
                    }
                }
            }));
            logger.info("*** StoreManager Started ***");
        } catch (Exception e) {
            throw new WebServiceException("Failed to start", e);
        }
    }

    public void shutdown() {
        // mongo connections closed in shutdown hook
        logger.info("*** StoreManager Shutdown ***");
    }

    public String getBadgeDBName() {
        return badgeDBName;
    }

    public void setBadgeDBName(String badgeDBName) {
        this.badgeDBName = badgeDBName;
    }

    public String getBadgeCollectionName() {
        return badgeCollectionName;
    }

    public void setBadgeCollectionName(String badgeCollectionName) {
        this.badgeCollectionName = badgeCollectionName;
    }

    public DBCollection getBadgesCollection() {
        return badgesCollection;
    }
}
