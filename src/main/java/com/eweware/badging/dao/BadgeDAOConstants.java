package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 4:33 PM
 */
public class BadgeDAOConstants {

    /**
     * A unique badge id for this authority. This id
     * can be transmitted to requestors.
     */
    public static final String ID_FIELDNAME = "_id";

    /**
     * The id of the requesting/sponsoring app.
     */
    public static final String REQUESTING_APP_ID = "A";

    /**
     * The email address of the owner of the badge.
     * This is used to retrieve type of badges
     * owned by this entity.
     */
    public static final String OWNER_EMAIL_ADDRESS = "O";

    /**
     * The time this badge was created.
     */
    public static final String CREATED_DATETIME_FIELDNAME = "C";

    /**
     * The time this badge expires. If null, the badge
     * has no expiration time.
     */
    public static final String EXPIRATION_DATETIME_FIELDNAME = "X";

    /**
     * The name of the badge.
     */
    public static final String BADGE_NAME_FIELDNAME = "E";

    /**
     * <p>Badge type, a String value. Supported types are "e" (based
     * on email domain) and "a" (inferred badge based on the
     * email domain).</p>
     */
    public static final String BADGE_TYPE_ID_FIELDNAME = "Y";

    /**
     * Badge type values
     */
    public static final String BADGE_TYPE_EMAIL = "e";
    public static final String BADGE_TYPE_INFERRED = "a";
}
