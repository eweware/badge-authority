package main.java.com.eweware.badging.mgr;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.sun.jersey.api.client.ClientResponse;
import main.java.com.eweware.badging.base.SystemErrorException;
import main.java.com.eweware.badging.dao.ApplicationDAO;
import main.java.com.eweware.badging.dao.BadgeDAO;
import main.java.com.eweware.badging.dao.TransactionDAO;
import main.java.com.eweware.badging.payload.BadgingNotificationEntity;
import main.java.com.eweware.badging.util.DateUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;

import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/21/13 Time: 10:41 AM
 */
public final class BadgeManager {

    private static final Logger logger = Logger.getLogger("BadgeManager");

    private static BadgeManager singleton;

    private static final long TEN_MINUTES_IN_MS = (1000l * 60 * 10);
    private static final long FIFTEEN_MINUTES_IN_MS = (1000l * 60 * 15);
    private static final long NINETY_DAYS_IN_MILLIS = (1000l * 60 * 60 * 24 * 90);

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
    private ClientConnectionManager connectionManager;

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
        maybeRegisterBlahguaApp();
        logger.info("*** BadgeManager Started (rest endpoint @" + restEndpoint + ") ***");
    }

    /** Simple kludge to register blahgua.com */
    private void maybeRegisterBlahguaApp() {
        // The app domain is its unique id.
        final DBObject blahgua = new BasicDBObject(ApplicationDAO.ID_FIELDNAME, "blahgua.com");
        if (appCollection.getCount(blahgua) != 0) {
            return;
        }
        blahgua.put(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME, "v2/badges/add");
        blahgua.put(ApplicationDAO.PASSWORD_FIELDNAME, "sheep"); // TODO kludge. Replace with digest and salt.
        blahgua.put(ApplicationDAO.REQUESTING_ENDPOINT_FIELDNAME, "beta.blahgua.com");
        blahgua.put(ApplicationDAO.STATUS_FIELDNAME, ApplicationDAO.STATUS_ACTIVE); // status := active
        blahgua.put(ApplicationDAO.CREATED_FIELDNAME, new Date());
        final WriteResult insert = appCollection.insert(blahgua);
        final String error = insert.getError();
        if (error != null) {
            throw new WebServiceException("Failed to register blahgua app due to a DB error: " + error);
        }
        logger.info("Registered blahgua.com as an app");
    }

    public void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
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
     * <p>Sends map as a POST to the specified endpoint and returns the http status code.</p>
     * @param endpoint
     * @param map
     * @return
     */
    private int postBadgeCreationNotification(String endpoint, Map<String, Object> map) throws SystemErrorException {
        HttpPost post = null;
        try {
            post = new HttpPost(endpoint);
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
            throw new SystemErrorException("failed to post", e);
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    /**
     * <p>First phase of badge creation transaction.</p>
     * <p>Initiates a badge creation transaction with an end user browser/app via
     * the badge-requesting app.</p>
     * @param appName   The username of the badge-requesting app.
     * @param appPassword   The bad-requesting app's password.
     * @return  An entity containing either an error code or the HTML5 form
     * that initiates the transaction with the end user.
     */
    public Response initBadgingTransaction(String appName, String appPassword) {

        if (!checkApplication(appName, appPassword)) {
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
        tr.put(TransactionDAO.REQUESTING_APP_ID_FIELDNAME, appName);  // app id := id of requesting app
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
     * @param txToken
     * @param emailAddress
     * @return
     */
    public Response processUserCredentials(String txToken, String emailAddress) {

        // First, validate the tx token to make sure we're dealing with a bona fide client
        if (txToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final DBObject queryTx = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txToken);
        final DBObject tx = transactionCollection.findOne(queryTx);
        if (tx == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final Object state = tx.get(TransactionDAO.STATE_FIELDNAME);
        if (state == null || !state.equals(TransactionDAO.STATE_PENDING_CREDENTIALS)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final Object created = tx.get(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME);
        if (created != null) { // check 15 minute timeout
            final Date c = (Date) created;
            if ((c.getTime() + FIFTEEN_MINUTES_IN_MS) < System.currentTimeMillis()) {
                return Response.status(ClientResponse.Status.GATEWAY_TIMEOUT).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (!isEmailAddressValid(emailAddress)) {
            return Response.ok(createInfoRequestForm(txToken, true)).build();
        }

        // Create SHORT(!) validation code. It expires in 10 minutes and only two retries are allowed.
        final byte[] rand = new byte[8];
        try {
            SystemManager.getInstance().setSecureRandomBytes(rand);
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Encryption error", e);
            return Response.serverError().build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        // TODO Send verification email with the verificationCode
        try {
            emailMgr.send(emailAddress, "Your Badging Request", makeEmailBody(verificationCode));
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        // Return HTML form requesting authorization code from user
        return Response.ok(createVerificationCodeRequestForm(txToken, false)).build();
    }

    public Response verify(String txToken, String verificationCode) {

        // First, ensure we have a valid tx
        if (txToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txToken);
        final DBObject tx = transactionCollection.findOne(txQuery);
        if (tx == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final Object vcode = tx.get(TransactionDAO.VERIFICATION_CODE_FIELDNAME);
        if (verificationCode.equals(vcode.toString())) {
            return Response.ok(createAndTransmitBadge(tx)).build();
        }

        final Date created = (Date) tx.get(TransactionDAO.TRANSACTION_STARTED_DATETIME_FIELDNAME);
        final Integer retries = (Integer) tx.get(TransactionDAO.RETRY_COUNT_FIELDNAME);
        if (retries != null && retries > 2) {
            transmitBadgeRefusal(tx, TransactionDAO.REFUSAL_TOO_MANY_RETRIES);
            return Response.ok("<p>Sorry, too many attempts to enter verification code. Try again later.").build();
        } else if ((created.getTime() + TEN_MINUTES_IN_MS) < System.currentTimeMillis()) {
            transmitBadgeRefusal(tx, TransactionDAO.REFUSAL_USER_TIMEOUT);
            return Response.ok("<p>Sorry, your verification code has expired.</p><p>Once you receive your email, you have ten minutes to enter the code.</p>").build();
        }

        transactionCollection.update(txQuery, new BasicDBObject("$inc", new BasicDBObject(TransactionDAO.RETRY_COUNT_FIELDNAME, 1)));

        return Response.ok(createVerificationCodeRequestForm(txToken, true)).build();
    }

    /**
     * <p>Tells requestor that badge has been refused.</p>
     * @param tx
     * @param newTxState
     */
    private void transmitBadgeRefusal(DBObject tx, String newTxState) {

        final String txId = (String) tx.get(TransactionDAO.ID_FIELDNAME);
        final String appId = (String) tx.get(TransactionDAO.REQUESTING_APP_ID_FIELDNAME);

        // Get/check app
        final DBObject app = appCollection.findOne(new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appId));
        if (app == null) {
            logger.warning("Ignored attempt to refuse badge creation for nonexistent app id. txId '" + txId + "', appId '" + appId + "'");
            return;
        }
        final String endpoint = SystemManager.getInstance().isDevMode() ? getDevBlahguaDomain() : (String) app.get(ApplicationDAO.REQUESTING_ENDPOINT_FIELDNAME);
        final String relativePath = (String) app.get(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME);

        // Update transaction
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txId);
        final BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(TransactionDAO.STATE_FIELDNAME, newTxState));
        update.put("$inc", new BasicDBObject(TransactionDAO.RETRY_COUNT_FIELDNAME, 1));
        final WriteResult result = MongoStoreManager.getInstance().getTransactionCollection().update(txQuery, update);
        if (result.getError() != null) {
            logger.severe("Error updating tx status (tx id '" + txId + "') in DB: " + result.getError());
            // fall through
        }

        // Notify requesting app
        final Map<String, Object> entity = new HashMap<String, Object>();
        entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
        entity.put(BadgingNotificationEntity.AUTHORITY_FIELDNAME, getDomain());
        entity.put(BadgingNotificationEntity.STATE_FIELDNAME, newTxState);
        final String url = "http://" + endpoint + "/" + relativePath;
        try {
            final int status = postBadgeCreationNotification(url, entity);
            if (status != HttpStatus.SC_ACCEPTED) { // Requestor dropped on the floor
                logger.warning("Requesting app did not accept badge refusal. Returned http status=" + status);
                // TODO roll back?
            }
        } catch (SystemErrorException e) {
            logger.log(Level.SEVERE, "Failed to post badge id refusal to requesting url '" + url + "'.", e);
            // TODO roll back?
        }
    }

    private String createAndTransmitBadge(DBObject tx) {

        final String txId = (String) tx.get(TransactionDAO.ID_FIELDNAME);
        final String email = (String) tx.get(TransactionDAO.USER_EMAIL_ADDRESS_FIELDNAME);
        final String appId = (String) tx.get(TransactionDAO.REQUESTING_APP_ID_FIELDNAME);

        // Get/check app
        final DBObject app = appCollection.findOne(new BasicDBObject(ApplicationDAO.ID_FIELDNAME, appId));
        if (app == null) {
            logger.warning("Ignored attempt to complete badge creation for nonexistent app id. txId '" + txId + "', appId '" + appId + "'");
            return "<p>Your badge request couldn't be handled because your sponsor is no longer registered in this badge authority</p>";
        }
        final String endpoint = SystemManager.getInstance().isDevMode() ? getDevBlahguaDomain() : (String) app.get(ApplicationDAO.REQUESTING_ENDPOINT_FIELDNAME);
        final String relativePath = (String) app.get(ApplicationDAO.BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME);

        // Create badge
        final DBObject badge = new BasicDBObject(BadgeDAO.USER_EMAIL_ADDRESS_FIELDNAME, email);
        final Date expires = new Date(System.currentTimeMillis() + NINETY_DAYS_IN_MILLIS);
        badge.put(BadgeDAO.EXPIRATION_DATETIME_FIELDNAME, expires);
        badge.put(BadgeDAO.CREATED_DATETIME_FIELDNAME, new Date());
        badge.put(BadgeDAO.REQUESTING_APP_ID, appId);
        final WriteResult insert = badgeCollection.insert(badge);
        final String error = insert.getError();
        if (error != null) {
            logger.severe("Error inserting badge for user '" + email + "': " + error);
            final Map<String, Object> entity = new HashMap<String, Object>();
            entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
            entity.put(BadgingNotificationEntity.STATE_FIELDNAME, BadgingNotificationEntity.STATE_SERVER_ERROR);
            final String url = "http://" + endpoint + "/" + relativePath;
            try {
                final int status = postBadgeCreationNotification(url, entity);
                if (status != HttpStatus.SC_ACCEPTED) {
                    logger.warning("Requesting app '" + appId + "' at '" + url + "' did not accept notification of server db error. No recovery needed. Returned http status=" + status);
                    return "<p>Sorry, your badge request could not be granted due to a technical problem. Please try again later.</p>";
                }
            } catch (SystemErrorException e) {
                logger.warning("Error while posting server db error msg to requesting app '" + appId + "' at '" + url + "' for transaction id='" + txId);
                return "<p>Sorry, your badge request could not be granted due to a technical problem. Please try again later.</p>";
            }
        }
        final String badgeId = badge.get(BadgeDAO.ID_FIELDNAME).toString();

        // Update transaction
        final DBObject txQuery = new BasicDBObject(TransactionDAO.ID_FIELDNAME, txId);
        final WriteResult result = MongoStoreManager.getInstance().getTransactionCollection().update(txQuery, new BasicDBObject("$set", new BasicDBObject(TransactionDAO.STATE_FIELDNAME, TransactionDAO.STATE_AWARDED_BADGE)));
        if (result.getError() != null) {
            logger.severe("Error updating tx status (tx id '" + txId + "') in DB: " + result.getError());
            // fall through anyway
        }

        // Transmit badge to requesting app
        final String emailDomain = getEmailDomain(email);
        final Map<String, Object> entity = new HashMap<String, Object>();
        entity.put(BadgingNotificationEntity.TRANSACTION_ID_FIELDNAME, txId);
        entity.put(BadgingNotificationEntity.BADGE_ID_FIELDNAME, badgeId);
        entity.put(BadgingNotificationEntity.AUTHORITY_FIELDNAME, getDomain());
        entity.put(BadgingNotificationEntity.DISPLAY_NAME_FIELDNAME, emailDomain); // display name
        entity.put(BadgingNotificationEntity.STATE_FIELDNAME, BadgingNotificationEntity.STATE_GRANTED); // status = granted
        entity.put(BadgingNotificationEntity.EXPIRATION_DATETIME_FIELDNAME, DateUtils.formatDateTime(expires)); // badge expiration date
        final String url = "http://" + endpoint + "/" + relativePath;
        try {
            final int status = postBadgeCreationNotification(url, entity);
            if (status != HttpStatus.SC_ACCEPTED) { // Requestor dropped on the floor
                logger.warning("Requesting app '" + appId + "' did not accept badge id '" + badgeId + "'. Returned http status=" + status);
                return "<p>Your badge request has been granted, but your sponsor refused to accept the badge.</p>";
            }
        } catch (SystemErrorException e) {
            logger.log(Level.SEVERE, "Failed to post badge id '" + badgeId + "' for app '" + appId + "' at requesting url '" + url + "'.", e);
            return "<p>Your badge request has been granted. However, your sponsor has not been notified due to a network problem.</p>";
        }
        return "<p>Congratulations! Your badge request has been granted.</p>";
    }

    private static String getEmailDomain(String email) {
        return email.substring(email.indexOf("@")+1);
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
     * via the badge-requesting app.</p>
     *
     * @param txToken The token identifying this transaction.
     * @param invalidEmail
     * @return  An HTML5 string containing a form to be filled out by the user.
     * Subsequent interaction with the user is directly handled by
     * the badging authority server.
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
        b.append("    <input type='button' onclick='ba_cancel_submit()' value='Cancel'/>");
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
        b.append("    <input type='hidden' id='ba_tk' name='end' value='" + getRestEndpoint() + "'/>");
        b.append("    <input type='hidden' id='ba_tk' name='tk' value='" + txToken + "'/>");
        b.append("    <input type='submit' onclick='ba_submit2(); return false' value='Submit'/>");
        b.append("    <input type='button' onclick='ba_cancel_submit()' value='Cancel'/>");
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
        client = new DefaultHttpClient();
        connectionManager = client.getConnectionManager();
        // double insurance:
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (connectionManager != null) {
                    connectionManager.shutdown();
                }
            }
        }));
    }
}
