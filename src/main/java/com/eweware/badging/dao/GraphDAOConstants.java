package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 5/4/13 Time: 4:58 PM
 */
public class GraphDAOConstants {

    /**
     * The id is composed of two bar-delimited strings. The first
     * string is the domain, and the second string is the name of
     * the inferred badge. E.g., 'apple.com|Tech Industry'.
     */
    public static final String GRAPH_ID = "_id";

    /**
     * The version of this representation of the graph item.
     * If the version changes, BadgeManager.getBadgeTypes()
     * must be able to handle it.
     * @see main.java.com.eweware.badging.mgr.BadgeManager#getBadgeTypes()
     */
    public static final String VERSION = "V";

    /**
     * The name of the domain (e.g., apple.com).
     */
    public static final String DOMAIN = "D";

    /**
     * The name of one of the domain's inferred badges.
     * E.g., from apple.com, the inferred badge name might
     * be "Tech Industry"
     */
    public static final String INFERRED_BADGE_NAME = "A";
}
