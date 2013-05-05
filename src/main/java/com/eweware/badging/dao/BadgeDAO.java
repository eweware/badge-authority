package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 4:33 PM
 */
public class BadgeDAO {

    public static final String ID_FIELDNAME = "_id";
    public static final String REQUESTING_APP_ID = "A";
    public static final String CREATED_DATETIME_FIELDNAME = "C";
    public static final String EXPIRATION_DATETIME_FIELDNAME = "X";
    public static final String BADGE_NAME_FIELDNAME = "E";

    /**
     * <p>Badge type, a String value. Supported types are "e" (based
     * on email domain) and "a" (abstracted badge based on the
     * email domain).</p>
     */
    public static final String BADGE_TYPE_FIELDNAME = "Y";

    /**
     * Badge type values
     */
    public static final String BADGE_TYPE_EMAIL = "e";
    public static final String BADGE_TYPE_ABSTRACTION = "a";
}
