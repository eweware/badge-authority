package main.java.com.eweware.badging.dao;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/24/13 Time: 4:20 PM
 */
public class TransactionDAO {

    /**
     * Unique id for the transaction
     */
    public static final String ID_FIELDNAME = "_id";

    /**
     * The transaction's current state (see below
     * for possible values)
     */
    public static final String STATE_FIELDNAME = "S";

    /**
     * The id of the requesting/sponsoring app.
     */
    public static final String SPONSOR_APP_ID_FIELDNAME = "A";

    /**
     * Time the transaction started. A transaction can timeout
     * due to user inaction within an application-dependent
     * time duration.
     */
    public static final String TRANSACTION_STARTED_DATETIME_FIELDNAME = "C";

    /**
     * The verification code sent to the user requesting the badge.
     */
    public static final String VERIFICATION_CODE_FIELDNAME = "V";

    /**
     * The number of retries for an operation during this transaction.
     */
    public static final String RETRY_COUNT_FIELDNAME = "R";

    /**
     * The
     */
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
