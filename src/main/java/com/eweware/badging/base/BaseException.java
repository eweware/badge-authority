package main.java.com.eweware.badging.base;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/8/13 Time: 1:26 PM
 */
public class BaseException extends Exception {

    private Object entity;
    private Integer errorCode;

    public BaseException() {
        super();
    }

    public BaseException(String s) {
        super(s);
    }

    public BaseException(Throwable throwable) {
        super(throwable);
    }

    public BaseException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BaseException(String msg, Object entity, Integer errorCode) {
        this(msg);
        this.errorCode = errorCode;
        this.entity = entity;
    }

    public BaseException(String msg, Throwable throwable, Integer errorCode) {
        super(msg, throwable);
        this.errorCode = errorCode;
    }

    public BaseException(String msg, Throwable throwable, Object entity, Integer errorCode) {
        super(msg, throwable);
        this.errorCode = errorCode;
        this.entity = entity;
    }

    public BaseException(String msg, Integer errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }

    public BaseException(String msg, Object entity) {
        super(msg);
        this.entity = entity;
    }

    public BaseException(Throwable e, Integer errorCode) {
        super(e);
        this.errorCode = errorCode;
    }

    public Object getEntity() {
        return entity;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

}
