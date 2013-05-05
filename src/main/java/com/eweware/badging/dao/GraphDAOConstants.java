package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 5/4/13 Time: 4:58 PM
 */
public class GraphDAOConstants {

    /**
     * The id is composed of two bar-delimited strings. The first
     * string is the domain, and the second string is the name of
     * the abstract badge. E.g., 'apple.com|Tech Industry'.
     */
    public static final String GRAPH_ID = "_id";

    /**
     * The name of the domain (e.g., apple.com).
     */
    public static final String DOMAIN = "D";

    /**
     * The name of one of the domain's abstractions.
     * E.g., from apple.com, the abstraction might
     * be "Tech Industry"
     */
    public static final String ABSTRACTION = "A";
}
