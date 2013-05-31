package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 5:25 PM
 */
public class ApplicationDAOConstants {

    public static final String ID_FIELDNAME = "_id";

    /**
     * <p>The display name of this application. This is used in the UI
     * interactions.</p>
     */
    public static final String APP_DISPLAY_NAME = "D";

    // TODO replace with DIGEST & SALT
    public static final String PASSWORD_FIELDNAME = "W";

    /**
     * <p>The REST endpoint suffix (i.e., without the hostname/port) to which to transmit
     * badges to this application.</p>
     */
    public static final String BADGE_CREATION_REST_CALLBACK_RELATIVE_PATH_FIELDNAME = "P"; /* e.g., "badges/add" */

    public static final String SPONSOR_ENDPOINT_FIELDNAME = "E";

    public static final String STATUS_FIELDNAME = "S";

    public static final String CREATED_FIELDNAME = "C";

    /**
     * Status values
     */
    public static final String STATUS_ACTIVE = "a";
    public static final String STATUS_MEMBERSHIP_EXPIRED = "x";
    public static final String STATUS_MEMBERSHIP_SUSPENDED = "s";
}
