package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 5:25 PM
 */
public class ApplicationDAOConstants {

    public static final String ID_FIELDNAME = "_id";

    // TODO replace with DIGEST & SALT
    public static final String PASSWORD_FIELDNAME = "W";

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
