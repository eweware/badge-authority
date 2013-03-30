package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 4:20 PM
 */
public class TransactionDAO {

    public static final String ID_FIELDNAME = "_id";
    public static final String STATE_FIELDNAME = "S";
    public static final String REQUESTING_APP_ID_FIELDNAME = "A";
    public static final String TRANSACTION_STARTED_DATETIME_FIELDNAME = "C";
    public static final String VERIFICATION_CODE_FIELDNAME = "V";
    public static final String RETRY_COUNT_FIELDNAME = "R";
    public static final String USER_EMAIL_ADDRESS_FIELDNAME = "E";
    public static final String REFUSAL_REASON_CODE_FIELDNAME = "N";

    /**
     * Values for STATE_FIELDNAME
     */
    public static final String STATE_PENDING_CREDENTIALS = "p";
    public static final String STATE_PENDING_VERIFICATION_CODE = "v";
    public static final String STATE_AWARDED_BADGE = "a";
    public static final String STATE_REFUSED_BADGE = "r";

    /**
     * Values for REFUSAL_REASON_CODE_FIELDNAME
     */
    public static final String REFUSAL_TOO_MANY_RETRIES = "r";
    public static final String REFUSAL_USER_TIMEOUT = "t";
}
