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

    private static MongoStoreManager singleton;

    private final String hostname;
    private final Integer port;
    private final Integer connectionsPerHost;
    private Mongo mongo;
    private String badgeDBName;
    private String badgeCollectionName;
    private String transactionCollectionName;
    private String applicationCollectionName;
    private DBCollection badgesCollection;
    private DBCollection transactionCollection;
    private DBCollection applicationCollection;
    private boolean usingReplica;

    public MongoStoreManager(
        String hostname,
        Integer port,
        Integer connectionsPerHost
    ) {
        this.hostname = hostname;
        this.port = port;
        this.connectionsPerHost = connectionsPerHost;
        singleton = this;
    }

    public static MongoStoreManager getInstance() {
        return singleton;
    }

    public void start() {
        try {
            final boolean devMode = SystemManager.getInstance().isDevMode();

            final MongoClientOptions.Builder builder = new MongoClientOptions.Builder().connectionsPerHost(connectionsPerHost);
            if (getUsingReplica()) {
                builder
                        .readPreference(ReadPreference.primaryPreferred()) // tries to read from primary
                        .writeConcern(WriteConcern.MAJORITY);      // Writes to secondaries before returning
                logger.info("*** Connecting to replica set ***");
            }
            final ServerAddress serverAddress = new ServerAddress(devMode ? "localhost" : hostname, port);
            this.mongo = new MongoClient(serverAddress, builder.build());

            final DB db = mongo.getDB(getBadgeDBName());
            badgesCollection = db.getCollection(getBadgeCollectionName());
            transactionCollection = db.getCollection(getTransactionCollectionName());
            applicationCollection = db.getCollection(getApplicationCollectionName());
//            logger.info("MongoDB Status: " + db.command(new BasicDBObject("serverStatus", 1)));

            // Add a shutdown hook to keep it independent of Spring
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mongo != null) {
                        mongo.close();
                        logger.info("*** Closed MongoDB Driver ***");
                    }
                }
            }));
            logger.info("*** StoreManager Started ***");
        } catch (Exception e) {
            throw new WebServiceException("Failed to start", e);
        }
    }

    public void shutdown() {
        if (mongo != null) {
            mongo.close();
            logger.info("*** Closed MongoDB Driver via Spring shutdown ***");
        }
        logger.info("*** StoreManager Shutdown ***");
    }

    public boolean getUsingReplica() {
        return usingReplica;
    }

    public void setUsingReplica(boolean usingReplica) {
        this.usingReplica = usingReplica;
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

    public void setBadgeCollectionName(String name) {
        this.badgeCollectionName = name;
    }

    public DBCollection getBadgesCollection() {
        return badgesCollection;
    }

    public String getTransactionCollectionName() {
        return transactionCollectionName;
    }

    public void setTransactionCollectionName(String name) {
        transactionCollectionName = name;
    }

    public DBCollection getTransactionCollection() {
        return transactionCollection;
    }

    public String getApplicationCollectionName() {
        return applicationCollectionName;
    }

    public void setApplicationCollectionName(String name) {
        applicationCollectionName = name;
    }

    public DBCollection getAppCollection() {
        return applicationCollection;
    }
}
