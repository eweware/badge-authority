package main.java.com.eweware.badging.mgr;

import com.mongodb.*;

import javax.xml.ws.WebServiceException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /**
     * MongoDB port
     */
    private final Integer port;

    private final Integer connectionsPerHost;

    /**
     * Hostname(s) for DB. This might be a set of instances in a replica set.
     */
    private List<String> hostnames = new ArrayList<String>();

    private MongoClient mongo;
    private String badgeDBName;
    private String badgeCollectionName;
    private String transactionCollectionName;
    private String applicationCollectionName;
    private String graphCollectionName;
    private DBCollection badgesCollection;
    private DBCollection transactionCollection;
    private DBCollection applicationCollection;
    private DBCollection graphCollection;

    /**
     * Are we using a replica set config?
     */
    private boolean usingReplica;

    public MongoStoreManager(
        String hostnames,
        Integer port,
        Integer connectionsPerHost
    ) {
        if (hostnames == null || hostnames.length() == 0) {
            throw new WebServiceException("Failed to start store manager: missing hostnames");
        }
        this.hostnames = Arrays.asList(hostnames.split("\\|"));
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
            List<ServerAddress> serverAddresses = new ArrayList<ServerAddress>();
            for (String hostname : hostnames) {
                serverAddresses.add(new ServerAddress(hostname, port));
            }
            if (getUsingReplica()) {
                builder
                        .readPreference(ReadPreference.primaryPreferred()) // tries to read from primary
                        .writeConcern(WriteConcern.MAJORITY);              // Writes to secondaries before returning
                logger.info("*** Connecting to hostname(s) in replica set: " + hostnames + " port=" + port + " ***");
            } else {
                serverAddresses.add(new ServerAddress(devMode ? "localhost" : hostnames.get(0), port));
                logger.info("*** Connecting to standalone DB instance: " + hostnames.get(0) + ":" + port);
            }
            this.mongo = new MongoClient(serverAddresses, builder.build());

            final DB db = mongo.getDB(getBadgeDBName());
            badgesCollection = db.getCollection(getBadgeCollectionName());
            transactionCollection = db.getCollection(getTransactionCollectionName());
            applicationCollection = db.getCollection(getApplicationCollectionName());
            graphCollection = db.getCollection(getGraphCollectionName());
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
        } catch (UnknownHostException e) {
            throw new WebServiceException("Failed to start store manager due to unknown DB hostname; hostname(s)=" + hostnames, e);
        } catch (Exception e) {
            throw new WebServiceException("Failed to start store manager", e);
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

    public String getGraphCollectionName() {
        return graphCollectionName;
    }

    public void setGraphCollectionName(String name) {
        graphCollectionName = name;
    }

    public DBCollection getGraphCollection() {
        return graphCollection;
    }
}
