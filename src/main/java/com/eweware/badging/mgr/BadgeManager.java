package main.java.com.eweware.badging.mgr;

import com.mongodb.*;
import main.java.com.eweware.badging.base.SystemErrorException;
import main.java.com.eweware.badging.dao.ApplicationDAO;
import main.java.com.eweware.badging.dao.BadgeDAO;
import main.java.com.eweware.badging.dao.GraphDAOConstants;
import main.java.com.eweware.badging.dao.TransactionDAO;
import main.java.com.eweware.badging.payload.BadgingNotificationEntity;
import main.java.com.eweware.badging.util.DateUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.codehaus.jackson.map.ObjectMapper;

import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/21/13 Time: 10:41 AM
 */
public final class BadgeManager {

    private static final Logger logger = Logger.getLogger("BadgeManager");

    private static final String DEFAULT_SYSTEM_ERROR_MESSAGE = "<p>There was a technical difficulty in creating your badge. Please try later.</p>";
    private static final String VERIFICATION_TIMEOUT_MESSAGE = "<p>Sorry, your verification code has expired.</p><p>Once you receive your email, you have ten minutes to enter the code.</p>";
    private static final String APP_NOT_REGISTERED_ERROR_MESSAGE = "<p>Your badge request couldn't be handled because your sponsor is no longer registered in this badge authority</p>";
    private static final String BADGE_GRANTED_BUT_NOT_ACCEPTED_BY_SPONSOR_APP = "<p>Your badge request has been granted, but due to a technical problem, your sponsor failed to accept the badge.</p><p>You may try to create the badge again.</p>";
    private static final String BADGE_GRANTED_BUT_SPONSOR_APP_FAILED_ACK = "<p>Your badge request has been granted. However, your sponsor has not been notified due to a network problem.</p>";
    private static final String BADGE_SUCCESSFULLY_GRANTED_AND_ACCEPTED_BY_SPONSOR_MESSAGE = "<p>Congratulations! Your badge request has been granted.</p>";
    private static final String BADGE_ALREADY_GRANTED_AND_ACTIVE = "<p>Your badge was granted in the past and is still active.</p>";

    private static BadgeManager singleton;

    private static final long TEN_MINUTES_IN_MS = (1000l * 60 * 10);
    private static final long FIFTEEN_MINUTES_IN_MS = (1000l * 60 * 15);
    private static final long ONE_YEAR_IN_MILLIS = (1000l * 60 * 60 * 24 * 365);

    private static final int BADGE_REFUSED_DUE_TO_TIMEOUT = 1;
    private static final int BADGE_REFUSED_DUE_TO_TOO_MANY_RETRIES = 2;

    private final String devEndpoint;
    private String endpoint;
    private final String endpointVersion;
    private String restEndpoint;
    private final String devDomain;
    private final String devBlahguaDomain;
    private String domain;
    private DBCollection badgeCollection;
    private DBCollection transactionCollection;
    private DBCollection appCollection;
    private MailManager emailMgr;
    private DefaultHttpClient client;
    //    private ClientConnectionManager connectionManager;
    private PoolingClientConnectionManager connectionPoolMgr;
    private Integer maxHttpConnections;
    private Integer maxHttpConnectionsPerRoute;
    private Integer httpConnectionTimeoutInMs;
    private Integer devBlahguarestPort;

    public BadgeManager(
            String devEndpoint,
            String endpoint,
            String devDomain,
            String domain,
            String devBlahguaDomain,
            String endpointVersion
    ) {
        this.devEndpoint = devEndpoint;
        this.endpoint = endpoint;
        this.devDomain = devDomain;
        this.domain = domain;
        this.devBlahguaDomain = devBlahguaDomain;
        this.endpointVersion = endpointVersion;
        singleton = this;
    }

    public static final BadgeManager getInstance() {
        return singleton;
    }

    public void start() {
        try {
            this.endpoint = SystemManager.getInstance().isDevMode() ? devEndpoint : endpoint;
            this.domain = SystemManager.getInstance().isDevMode() ? devDomain : domain;
            this.restEndpoint = this.endpoint + "/" + this.endpointVersion;
            final MongoStoreManager storeManager = MongoStoreManager.getInstance();
            if (storeManager == null) {
                logger.severe("BadgeManager: Start preconditions failed");
                throw new WebServiceException("Start preconditions failed");
            }
            this.badgeCollection = storeManager.getBadgesCollection();
            this.transactionCollection = storeManager.getTransactionCollection();
            this.appCollection = storeManager.getAppCollection();
            this.emailMgr = MailManager.getInstance();
            if (badgeCollection == null || transactionCollection == null || appCollection == null || emailMgr == null) {
                logger.severe("BadgeManager: Start preconditions failed");
                throw new WebServiceException("Start preconditions failed");
            }
            startHttpClient();
//            maybeRegisterBlahguaApp();
            logger.info("*** BadgeManager Started (rest endpoint @" + restEndpoint + ") ***");
        } catch (Exception e) {
            throw new WebServiceException("Failed to start badge manager", e);
        }
    }

//    /**
//     * Simple kludge to register blahgua.com
//     */
//    private void maybeRegisterBlahguaApp() {
//        // The app domain is its unique id.
//        final DBObject blahgua = new BasicDBObject(ApplicationDAO.ID_FIELDNAME, "blahgua.com");
//        if (appCollection.getCount(blahgua) != 0) {
//            return;
//        }
//        blahgua.put(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME, "v2/badges/add");
//        blahgua.put(ApplicationDAO.PASSWORD_FIELDNAME, "sheep"); // TODO kludge. Replace with digest and salt.
//        blahgua.put(ApplicationDAO.SPONSOR_ENDPOINT_FIELDNAME, "beta.blahgua.com");
//        blahgua.put(ApplicationDAO.STATUS_FIELDNAME, ApplicationDAO.STATUS_ACTIVE); // status := active
//        blahgua.put(ApplicationDAO.CREATED_FIELDNAME, new Date());
//        final WriteResult insert = appCollection.insert(blahgua);
//        final String error = insert.getError();
//        if (error != null) {
//            throw new WebServiceException("Failed to register blahgua app due to a DB error: " + error);
//        }
//        logger.info("Registered blahgua.com as an app");
//    }

    public void shutdown() {
        if (connectionPoolMgr != null) {
            connectionPoolMgr.shutdown();
        }
        logger.info("*** BadgeManager Shutdown ***");
    }

    private String getEndpoint() {
        return endpoint;
    }

    private String getRestEndpoint() {
        return restEndpoint;
    }

    private String getDomain() {
        return domain;
    }

    private String getDevBlahguaDomain() {
        return devBlahguaDomain;
    }

    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * <p>Sends map as a POST to the specified url and returns the http status code.</p>
     *
     * @param url
     * @param map
     * @return
     */
    private int postBadgeCreationNotification(String url, Map<String, Object> map) throws SystemErrorException {
        HttpPost post = null;
        try {
            post = new HttpPost(url);
            post.setHeader("Content-Type", MediaType.APPLICATION_JSON);

            // Set the entity
            final String jsonString = new ObjectMapper().writeValueAsString(map);
            final StringEntity stringEntity = new StringEntity(jsonString, "UTF-8");
            post.setEntity(stringEntity);

            // Execute remote method and get response
            final HttpResponse response = getHttpClient().execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            // TODO authority should process the error code and return error info or log it here
            return statusCode;
        } catch (Exception e) {
            post.abort();
            throw new SystemErrorException("failed to post badge creation notification to sponsoring app; endpoint '" + url + "'; data='" + map + "'", e);
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    /**
     * <p>First phase of badge creation transaction.</p>
     * <p>Initiates a badge creation transaction with an end user browser/app via
     * the badge-sponsor app.</p>
     *
     * @param appName     The username of the badge-sponsor app.
     * @param appPassword The bad-sponsor app's password.
     * @return An entity containing either an error code or the HTML5 form
     *         that initiates the transaction with the end user.
     */
    public Response initBadgingTransaction(String appName, String appPassword) {

        if (!checkApplication(appName, appPassword)) {  // be harsh, be cruel
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final Map<String, Object> entity = new HashMap<String, Object>();
        final byte[] rand = new byte[20];
        try {
            SystemManager.getInstance().setSecureRandomBytes(rand);
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "unsupported encoding", e);
            entity.put("error", e.getMessage());
            return Response.serverError().entity(entity).build();
        }
        final String txToken = appName + Base64.encodeBase64URLSafeString(rand);

        final DBObject tr = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txToken);
        tr.put(TransactionDAO.STATE_FIELDNAME, TransactionDAO.STATE_PENDING_CREDENTIALS);    // state := pending getting email address
        tr.put(TransactionDAO.SPONSOR_APP_ID_FIELDNAME, appName);  // app id := id of sponsor app
        tr.put(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME, new Date());
        tr.put(TransactionDAO.RETRY_COUNT_FIELDNAME, 0);
        final WriteResult result = transactionCollection.insert(tr);
        if (result.getError() != null) {
            logger.severe("Failed to initiate tx. DB error: " + result.getError());
            return Response.serverError().build();
        } else {
            final String form = createInfoRequestForm(txToken, false);
            entity.put("form", form);
            entity.put("tx", txToken);
            return Response.ok(entity).build();
        }
    }


    /**
     * <p>Second phase of badge creation transaction.</p>
     * <p>Obtains credentials (in our case, the user's email address)
     * directly from the user's browser/app.</p>
     *
     * @param txToken
     * @param emailAddress
     * @return
     */
    public Response processUserCredentials(String txToken, String emailAddress) {

        // First, validate the tx token to make sure we're dealing with a bona fide client
        if (txToken == null) {
            return makeGenericResponse("crednotok", null);
        }
        final DBObject queryTx = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txToken);
        final DBObject tx = transactionCollection.findOne(queryTx);
        if (tx == null) {
            return makeGenericResponse("crednotokreg", null);
        }
        final Object state = tx.get(TransactionDAO.STATE_FIELDNAME);
        if (state == null || !state.equals(TransactionDAO.STATE_PENDING_CREDENTIALS)) {
            return makeGenericResponse("credstateconflict", null);
        }
        final Object created = tx.get(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME);
        if (created != null) { // check 15 minute timeout
            final Date c = (Date) created;
            if ((c.getTime() + FIFTEEN_MINUTES_IN_MS) < System.currentTimeMillis()) {
                return makeGenericResponse("credtimeout", VERIFICATION_TIMEOUT_MESSAGE);
            }
        } else {
            return makeGenericResponse("crednocreate", null);
        }

        if (!isEmailAddressValid(emailAddress)) {
            // TODO retransmit badges to blahgua
            return Response.ok(createInfoRequestForm(txToken, true)).build();
        }

        final List<DBObject> badges = getActiveBadgesForEmailAddress(emailAddress);
        if (badges.size() > 0) {  // retransmit existing badge(s)
            // TODO it might be that, say, the Tech Industry badge hasn't expired, but the email badge has. In that
            // TODO case we should transmit the unexpired badge and proceed to create the expired one. https://eweware.atlassian.net/browse/BA-17

            final String txId = (String) tx.get(TransactionDAO.ID_FIELDNAME);
            final String appId = (String) tx.get(TransactionDAO.SPONSOR_APP_ID_FIELDNAME);
//            final String email = (String) tx.get(TransactionDAO.USER_EMAIL_ADDRESS_FIELDNAME);


//            final String emailDomain = getEmailDomain(email);
            // Get/check app
            final DBObject app = appCollection.findOne(new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appId));
            if (app == null) {
                logger.warning("Ignored attempt to complete badge creation for nonexistent app id. txId '" + txId + "', appId '" + appId + "'");
                return makeGenericResponse("noappreg", APP_NOT_REGISTERED_ERROR_MESSAGE);
            }
            final String endpoint = SystemManager.getInstance().isDevMode() ? ("http://" + getDevBlahguaDomain()) : (String) app.get(ApplicationDAO.SPONSOR_ENDPOINT_FIELDNAME);
            final String relativePath = (String) app.get(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME);
            final Response response = transmitBadges(txId, appId, endpoint, relativePath, badges);
            return (response == null) ? makeGenericResponse(null, BADGE_ALREADY_GRANTED_AND_ACTIVE) : response;
        } else {  // sense a validation response message and verification email
            return makeValidationCodeResponse(txToken, emailAddress, queryTx);
        }


    }

    private Response makeValidationCodeResponse(String txToken, String emailAddress, DBObject queryTx) {
        // Create SHORT(!) validation code. It expires in 10 minutes and only two retries are allowed.
        final byte[] rand = new byte[8];
        try {
            SystemManager.getInstance().setSecureRandomBytes(rand);
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Encryption error", e);
            return makeGenericResponse("credencode", null);
        }
        final String verificationCode = Base64.encodeBase64URLSafeString(rand);

        // Update transaction state
        final DBObject update = new BasicDBObject(TransactionDAO.STATE_FIELDNAME, TransactionDAO.STATE_PENDING_VERIFICATION_CODE);
        final DBObject setter = new BasicDBObject("$set", update);
        update.put(TransactionDAO.VERIFICATION_CODE_FIELDNAME, verificationCode);
        update.put(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME, new Date());
        update.put(TransactionDAO.USER_EMAIL_ADDRESS_FIELDNAME, emailAddress);
        final WriteResult wr = transactionCollection.update(queryTx, setter);
        if (wr.getError() != null) {
            logger.severe("Failed to update transaction token '" + txToken + "' code 'txupdb'. DB error: " + wr.getError());
            return makeGenericResponse("txupdb", null);
        }

        // TODO Send verification email with the verificationCode
        try {
            emailMgr.send(emailAddress, "Your Badging Request", makeEmailBody(verificationCode));
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email", e);
            return makeGenericResponse("mai", null);
        }

        // Return HTML form requesting authorization code from user
        return Response.ok(createVerificationCodeRequestForm(txToken, false)).build();
    }

    /**
     * <p>A user has entered the verification code. Grant badge if conditions allow it.</p>
     *
     * @param txToken
     * @param verificationCode
     * @return
     */
    public Response verify(String txToken, String verificationCode) {

        // First, ensure we have a valid tx
        if (txToken == null) {
            return makeGenericResponse("vernotok", null);
        }
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txToken);
        final DBObject tx = transactionCollection.findOne(txQuery);
        if (tx == null) {
            return makeGenericResponse("vernotokreg", null);
        }

        final Object vcode = tx.get(TransactionDAO.VERIFICATION_CODE_FIELDNAME);
        if (verificationCode.equals(vcode.toString())) {
            return createAndTransmitBadge(tx);
        }

        final Date created = (Date) tx.get(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME);
        final Integer retries = (Integer) tx.get(TransactionDAO.RETRY_COUNT_FIELDNAME);
        if (retries != null && retries > 2) {
            transmitBadgeRefusal(tx, TransactionDAO.REFUSAL_TOO_MANY_RETRIES);
            return makeGenericResponse("vertoomanyattempts", "<p>Sorry, too many attempts to enter verification code. Try again later.</p>");
        } else if ((created.getTime() + TEN_MINUTES_IN_MS) < System.currentTimeMillis()) {
            transmitBadgeRefusal(tx, TransactionDAO.REFUSAL_USER_TIMEOUT);
            return makeGenericResponse("vertimeout", VERIFICATION_TIMEOUT_MESSAGE);
        }

        transactionCollection.update(txQuery, new BasicDBObject("$inc", new BasicDBObject(TransactionDAO.RETRY_COUNT_FIELDNAME, 1)));

        return Response.ok(createVerificationCodeRequestForm(txToken, true)).build();
    }

    // Expects non-null errorCode!
    private Response makeGenericResponse(String errorCode, String msg) {
        final StringBuilder b = new StringBuilder((msg == null) ? DEFAULT_SYSTEM_ERROR_MESSAGE : msg);
        b.append("<div>Code ");
        b.append(errorCode);
        b.append("</div>");
        b.append("<input type='button' onclick='ba_cancel_submit(\"");
        b.append(errorCode);
        b.append("\")' value='OK'/>");
        return Response.ok(b.toString()).build();
    }

    /**
     * <p>Tells sponsor that badge has been refused.</p>
     *
     * @param tx
     * @param newTxState
     */
    private void transmitBadgeRefusal(DBObject tx, String newTxState) {

        final String txId = (String) tx.get(TransactionDAO.ID_FIELDNAME);
        final String appId = (String) tx.get(TransactionDAO.SPONSOR_APP_ID_FIELDNAME);

        // Get/check app
        final DBObject app = appCollection.findOne(new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appId));
        if (app == null) {
            logger.warning("Ignored attempt to refuse badge creation for nonexistent app id. txId '" + txId + "', appId '" + appId + "'");
            return;
        }
        final String endpoint = SystemManager.getInstance().isDevMode() ? ("http://" + getDevBlahguaDomain()) : (String) app.get(ApplicationDAO.SPONSOR_ENDPOINT_FIELDNAME);
        final String relativePath = (String) app.get(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME);

        // Update transaction
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txId);
        final BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(TransactionDAO.STATE_FIELDNAME, newTxState));
        update.put("$inc", new BasicDBObject(TransactionDAO.RETRY_COUNT_FIELDNAME, 1));
        final WriteResult result = MongoStoreManager.getInstance().getTransactionCollection().update(txQuery, update);
        if (result.getError() != null) {
            logger.severe("Error updating tx status (tx id '" + txId + "') in DB; accepting tx anyway. DB error: " + result.getError());
            // fall through
        }

        // Notify sponsor app
        final Map<String, Object> entity = new HashMap<String, Object>();
        entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
        entity.put(BadgingNotificationEntity.AUTHORITY_FIELDNAME, getDomain());
        entity.put(BadgingNotificationEntity.STATE_FIELDNAME, newTxState);
        final String url = endpoint + "/" + relativePath;
        try {
            final int status = postBadgeCreationNotification(url, entity);
            if (status != HttpStatus.SC_ACCEPTED) { // Requestor dropped on the floor
                logger.warning("Sponsor app did not accept badge refusal. Returned http status=" + status);
                // TODO roll back?
            }
        } catch (SystemErrorException e) {
            logger.log(Level.SEVERE, "Failed to post badge id refusal to sponsor url '" + url + "'.", e);
            // TODO roll back?
        }
    }

    /**
     * Actually create and transmit the badge(s).
     *
     * @param tx
     * @return
     */
    private Response createAndTransmitBadge(DBObject tx) {

        final String txId = (String) tx.get(TransactionDAO.ID_FIELDNAME);
        final String email = (String) tx.get(TransactionDAO.USER_EMAIL_ADDRESS_FIELDNAME);
        final String appId = (String) tx.get(TransactionDAO.SPONSOR_APP_ID_FIELDNAME);
        final String emailDomain = getEmailDomain(email);

        // Get/check app
        final DBObject app = appCollection.findOne(new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appId));
        if (app == null) {
            logger.warning("Ignored attempt to complete badge creation for nonexistent app id. txId '" + txId + "', appId '" + appId + "'");
            return makeGenericResponse("noappreg", APP_NOT_REGISTERED_ERROR_MESSAGE);
        }
        final String relativePath = (String) app.get(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME);
        final String endpoint = SystemManager.getInstance().isDevMode() ? ("http://" + getDevBlahguaDomain()) : (String) app.get(ApplicationDAO.SPONSOR_ENDPOINT_FIELDNAME);

        // Make badges
        final List<DBObject> badges = new ArrayList<DBObject>(3);
        final Date expires = new Date(System.currentTimeMillis() + ONE_YEAR_IN_MILLIS);

        // Make email type badge
        final Object result = createBadge(emailDomain, email, BadgeDAO.BADGE_TYPE_EMAIL, expires, appId, txId, relativePath);
        if (result instanceof Response) {
            final Response response = (Response) result;
            logger.warning("Tried to create badge for email '" + email + "' but got a bad response status=" + response.getStatus() + " entity '" + response.getEntity() + "'");
            return (Response) result;
        } else if (!(result instanceof DBObject)) {
            // TODO deal with this case
        }
        badges.add((DBObject) result);
//        final String badgeId = badge.get(BadgeDAO.ID_FIELDNAME).toString();

        // Create abstracted badges, if available

        final DBCollection graphCollection = MongoStoreManager.getInstance().getGraphCollection();
        final BasicDBObject graphQuery = new BasicDBObject(GraphDAOConstants.DOMAIN, emailDomain);
        final DBCursor cursor = graphCollection.find(graphQuery);
        for (DBObject obj : cursor) {
            final String abstraction = (String) obj.get(GraphDAOConstants.ABSTRACTION);
            final Object abstractBadge = createBadge(abstraction, email, BadgeDAO.BADGE_TYPE_ABSTRACTION, expires, appId, txId, relativePath);
            if (abstractBadge instanceof Response) {
                final Response response = (Response) result;
                logger.warning("Tried to create abstract badge named '" + abstraction + "' for email '" + email + "' but got a bad response status=" + response.getStatus() + " entity '" + response.getEntity() + "'");
                return (Response) result;
            } else if (!(result instanceof DBObject)) {
                // TODO deal with this case
            }
            badges.add((DBObject) abstractBadge);
        }


        // Update transaction
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txId);
        final WriteResult wr = MongoStoreManager.getInstance().getTransactionCollection().update(txQuery, new BasicDBObject("$set", new BasicDBObject(TransactionDAO.STATE_FIELDNAME, TransactionDAO.STATE_AWARDED_BADGE)));
        if (wr.getError() != null) {
            logger.severe("Error updating tx id '" + txId + "' status for sponsor app '" + appId + "'. Ignored. DB error: " + wr.getError());
            // fall through anyway
        }

        // Transmit badge(s) to sponsor app
        final Response response = transmitBadges(txId, appId, endpoint, relativePath, badges);
        return (response == null) ? makeGenericResponse("granted", BADGE_SUCCESSFULLY_GRANTED_AND_ACCEPTED_BY_SPONSOR_MESSAGE) : response;
    }

    /**
     * <p> Transmits specified badges to requesting/sponsoring app.</p>
     * <p>Returns a response whenever there is some error. Returns null if operation succeeds.</p>
     */
    private Response transmitBadges(String txId, String appId, String endpoint, String relativePath, List<DBObject> badges) {

        final List<Map<String, Object>> entities = new ArrayList<Map<String, Object>>(badges.size());
        for (DBObject newBadge : badges) {

            final Map<String, Object> entity = new HashMap<String, Object>();
            entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
            entity.put(BadgingNotificationEntity.BADGE_ID_FIELDNAME, newBadge.get(BadgeDAO.ID_FIELDNAME).toString());
            entity.put(BadgingNotificationEntity.BADGE_TYPE_FIELDNAME, newBadge.get(BadgeDAO.BADGE_TYPE_FIELDNAME));
            entity.put(BadgingNotificationEntity.AUTHORITY_FIELDNAME, getDomain());
            entity.put(BadgingNotificationEntity.BADGE_NAME_FIELDNAME, newBadge.get(BadgeDAO.BADGE_NAME_FIELDNAME)); // display name
            entity.put(BadgingNotificationEntity.STATE_FIELDNAME, BadgingNotificationEntity.STATE_GRANTED); // status = granted
            entity.put(BadgingNotificationEntity.EXPIRATION_DATETIME_FIELDNAME, DateUtils.formatDateTime((Date) newBadge.get(BadgeDAO.EXPIRATION_DATETIME_FIELDNAME))); // badge expiration date

            entities.add(entity);
        }
        final String url = endpoint + "/" + relativePath;
        int status = 0;
        try {
            final Map<String, Object> map = new HashMap<String, Object>(1);
            map.put("badges", entities);
            map.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
            map.put(BadgingNotificationEntity.AUTHORITY_FIELDNAME, getDomain());
            map.put(BadgingNotificationEntity.STATE_FIELDNAME, BadgingNotificationEntity.STATE_GRANTED);
            logger.info("SENDING BADGES:\n" + map);
            status = postBadgeCreationNotification(url, map);
        } catch (SystemErrorException e) {
            logger.log(Level.SEVERE, "Failed to notify (POST) granted badge id(s) '" + getBadgeIdsAsList(badges) + "' to app '" + appId + "' tx id '" + txId + "' at app url '" + url + "'.", e);
            return makeGenericResponse("notifyerror", BADGE_GRANTED_BUT_SPONSOR_APP_FAILED_ACK);
        }
        if (status != HttpStatus.SC_ACCEPTED) { // Requestor dropped on the floor
            logger.severe("Sponsor app '" + appId + "' did not accept badge id(s) '" + getBadgeIdsAsList(badges) + "' for tx id '" + txId + "'. Returned http status=" + status);
            final String code = "notifynotaccepted-" + status;
            return makeGenericResponse(code, BADGE_GRANTED_BUT_NOT_ACCEPTED_BY_SPONSOR_APP);
        }
        return null;
    }

    private List<String> getBadgeIdsAsList(List<DBObject> badges) {
        final List<String> ids = new ArrayList<String>(badges.size());
        for (DBObject badge : badges) {
            ids.add(badge.get(BadgeDAO.ID_FIELDNAME).toString());
        }
        return ids;
    }


    /**
     * Creates the badge by inserting it to the DB. If there's an error,
     * it returns an appropriate Response object, else it returns the badge DBObject.
     */
    private Object createBadge(String badgeName, String badgeOwnerEmailAddress, String badgeType, Date expires, String appId, String txId, String relativePath) {
        final DBObject badge = new BasicDBObject(BadgeDAO.BADGE_NAME_FIELDNAME, badgeName);
        badge.put(BadgeDAO.OWNER_EMAIL_ADDRESS, badgeOwnerEmailAddress);
        badge.put(BadgeDAO.EXPIRATION_DATETIME_FIELDNAME, expires);
        badge.put(BadgeDAO.BADGE_TYPE_FIELDNAME, badgeType);
        badge.put(BadgeDAO.CREATED_DATETIME_FIELDNAME, new Date());
        badge.put(BadgeDAO.REQUESTING_APP_ID, appId);
        final WriteResult insert = badgeCollection.insert(badge);
        if (insert.getError() != null) {
            logger.severe("DB error inserting granted badge for user '" + badgeName + "' tx id '" + txId + "' appId '" + appId + "'. DB error: " + insert.getError());
            final Map<String, Object> entity = new HashMap<String, Object>();
            entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
            entity.put(BadgingNotificationEntity.STATE_FIELDNAME, BadgingNotificationEntity.STATE_SERVER_ERROR);
            final String url = endpoint + "/" + relativePath;
            try {
                final int status = postBadgeCreationNotification(url, entity);
                if (status != HttpStatus.SC_ACCEPTED) {
                    logger.warning("Sponsor app '" + appId + "' at '" + url + "' tx id '" + txId + "' did not accept notification of server db error. No recovery needed. Returned http status=" + status);
                    return makeGenericResponse("badstat-" + status, null);
                }
            } catch (SystemErrorException e) {
                logger.warning("Error while posting server db error msg to sponsor app '" + appId + "' at '" + url + "' for transaction id='" + txId);
                final String code = "syscode-" + ((e.getErrorCode() == null) ? "" : e.getErrorCode().toString());
                return makeGenericResponse(code, null);
            }
        }
        return badge;
    }

    /**
     * <p>Retrieves all active badges owned by user with specified email address.</p>
     *
     * @param emailAddress
     * @return
     */
    private List<DBObject> getActiveBadgesForEmailAddress(String emailAddress) {
        final List<DBObject> badges = new ArrayList<DBObject>(5);
        final BasicDBObject query = new BasicDBObject(BadgeDAO.OWNER_EMAIL_ADDRESS, emailAddress);
        final DBCursor badgeCursor = badgeCollection.find(query);
        final Date now = new Date();
        for (DBObject badge : badgeCursor) {
            final Date expires = (Date) badge.get(BadgeDAO.EXPIRATION_DATETIME_FIELDNAME);
            if (expires == null || expires.before(now)) {
                badges.add(badge);
            }
        }
        return badges;
    }

    private static String getEmailDomain(String email) {
        return email.substring(email.indexOf("@") + 1);
    }

    private String makeEmailBody(String verificationCode) {
        final StringBuilder b = new StringBuilder();
        b.append("<p>To confirm that you own this email address, please enter the following verification code in your browser or application.</p>");
        b.append("<div style='font-weight:bold'>");
        b.append(verificationCode);
        b.append("</div>");
        b.append("<p>This code will expire in 15 minutes.</p>");
        return b.toString();
    }

    private boolean isEmailAddressValid(String emailAddress) {
        return (emailAddress != null && emailAddress.indexOf("@") != -1); // TODO
    }

    /**
     * <p>This form is sent to the browser (or app) of the user to be badged
     * via the badge-sponsor app.</p>
     *
     * @param txToken      The token identifying this transaction.
     * @param invalidEmail
     * @return An HTML5 string containing a form to be filled out by the user.
     *         Subsequent interaction with the user is directly handled by
     *         the badging authority server.
     */
    private String createInfoRequestForm(String txToken, boolean invalidEmail) {
        final StringBuilder b = new StringBuilder();
        b.append("<script src='");
        b.append(getEndpoint());
        b.append("/js/ba_api.js'></script>");
        b.append("<form id='ba_form' action='");
        b.append(getRestEndpoint());
        b.append("/badges/credentials' method='post'>");
        if (invalidEmail) {
            b.append("<div style='color:red'>You entered an invalid email address. Please re-enter it.</div>");
        }
        // Note: onchange is a workaround to extract the value from the input field. Gave up trying to understand how this is "supposed" to work.
        b.append("  Email Address: <input name='e' type='text' onchange='ba_email_address = this.value' size='30'/>");
        b.append("  <div>");
        b.append("    <input type='hidden' id='ba_end' name='end' value='" + getRestEndpoint() + "'/>");
        b.append("    <input type='hidden' id='ba_tk' name='tk' value='" + txToken + "'/>");
        b.append("    <input type='submit' onclick='ba_submit1(); return false' value='Submit'/>");
        b.append("    <input type='button' onclick='ba_cancel_submit(\"credentials\")' value='Cancel'/>");
        b.append("  </div>");
        b.append("</form>");
        return b.toString();
    }

    private String createVerificationCodeRequestForm(String txToken, boolean retry) {
        final StringBuilder b = new StringBuilder();
        b.append("<form id='ba_form' method='post' action='");
        b.append(getRestEndpoint());
        b.append("/badges/verify'>");
        if (retry) {
            b.append("<div>Sorry, the verification code that you sent was incorrect.</div>");
        }
        // Note: onchange is a workaround to extract the value from the input field. Gave up trying to understand how this is "supposed" to work.
        b.append("<div>Enter the verification code in the email sent to you: <input name='code' onchange='ba_verification_code = this.value' type='text' size='30' /></div>");
        b.append("  <div>");
        b.append("    <input type='hidden' id='ba_end' name='end' value='" + getRestEndpoint() + "'/>");
        b.append("    <input type='hidden' id='ba_tk' name='tk' value='" + txToken + "'/>");
        b.append("    <input type='submit' onclick='ba_submit2(); return false' value='Submit'/>");
        b.append("    <input type='button' onclick='ba_cancel_submit(\"verification\")' value='Cancel'/>");
        b.append("  </div>");
        b.append("</form>");
        return b.toString();
    }

    private boolean checkApplication(String appName, String appPassword) {
        final DBObject query = new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appName);
        final DBObject app = appCollection.findOne(query);
        if (app != null) {
            final String password = (String) app.get(ApplicationDAO.PASSWORD_FIELDNAME);
            if (password != null && password.equals(appPassword)) {
                return true;
            }
        }
        return false;
    }

    private void startHttpClient() {
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        if (SystemManager.getInstance().isDevMode()) { // Debug blahguarest port 8080
            schemeRegistry.register(new Scheme("http", getDevBlahguarestPort(), PlainSocketFactory.getSocketFactory()));
        }
        connectionPoolMgr = new PoolingClientConnectionManager(schemeRegistry);
        connectionPoolMgr.setMaxTotal(getMaxHttpConnections()); // maximum total connections
        connectionPoolMgr.setDefaultMaxPerRoute(getMaxHttpConnectionsPerRoute()); // maximumconnections per route

        // Create a client that can be shared by multiple threads
        client = new DefaultHttpClient(connectionPoolMgr);

        // Set timeouts (if not set, thread may block forever)
        final HttpParams httpParams = client.getParams();
        httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, getHttpConnectionTimeoutInMs());
        httpParams.setLongParameter(ConnManagerPNames.TIMEOUT, getHttpConnectionTimeoutInMs());
        httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, getHttpConnectionTimeoutInMs());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (connectionPoolMgr != null) {
                    connectionPoolMgr.shutdown();
                }
            }
        }));
    }

//    private void startHttpClient() {
//        client = new DefaultHttpClient();
//        connectionManager = client.getConnectionManager();
//        // double insurance:
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (connectionManager != null) {
//                    connectionManager.shutdown();
//                }
//            }
//        }));
//    }

    public Integer getMaxHttpConnections() {
        return maxHttpConnections;
    }

    public void setMaxHttpConnections(Integer maxHttpConnections) {
        this.maxHttpConnections = maxHttpConnections;
    }

    public Integer getMaxHttpConnectionsPerRoute() {
        return maxHttpConnectionsPerRoute;
    }

    public void setMaxHttpConnectionsPerRoute(Integer maxHttpConnectionsPerRoute) {
        this.maxHttpConnectionsPerRoute = maxHttpConnectionsPerRoute;
    }

    public Integer getHttpConnectionTimeoutInMs() {
        return httpConnectionTimeoutInMs;
    }

    public void setHttpConnectionTimeoutInMs(Integer httpConnectionTimeoutInMs) {
        this.httpConnectionTimeoutInMs = httpConnectionTimeoutInMs;
    }

    public Integer getDevBlahguarestPort() {
        return devBlahguarestPort;
    }

    public void setDevBlahguarestPort(Integer port) {
        this.devBlahguarestPort = port;
    }
}
