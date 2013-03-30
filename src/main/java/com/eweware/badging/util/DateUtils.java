package main.java.com.eweware.badging.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/26/13 Time: 10:34 AM
 */
public class DateUtils {

    private static final Logger logger = Logger.getLogger("DateUtils");

    // Commonly used regular expressions
    public static final String ISO_DATE_FORMAT_REGEXP = "^(\\d{4}(?:(?:(?:\\-)?(?:00[1-9]|0[1-9][0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6]))?|(?:(?:\\-)?(?:1[0-2]|0[1-9]))?|(?:(?:\\-)?(?:1[0-2]|0[1-9])(?:\\-)?(?:0[1-9]|[12][0-9]|3[01]))?|(?:(?:\\-)?W(?:0[1-9]|[1-4][0-9]5[0-3]))?|(?:(?:\\-)?W(?:0[1-9]|[1-4][0-9]5[0-3])(?:\\-)?[1-7])?)?)$";

    /**
     * Converts a Date instance to an ISO datetime string representation.
     *
     * @param date The date
     * @return String   The ISO string representation of the datetime
     */
    public static String formatDateTime(Date date) {
        return DateFormatUtils.formatUTC(date, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
    }

    /**
     * Converts a Date instance to an ISO date string representation (no time component)
     *
     * @param date The date
     * @return String   The ISO string representation of the date
     */
    public static String formatDate(Date date) {
        return DateFormatUtils.format(date, DateFormatUtils.ISO_DATE_FORMAT.getPattern());
    }


    /**
     * Converts a date in milliseconds to an ISO datetime string representation.
     *
     * @param utcInMillis The time in millis
     * @return String   The ISO string representation of the datetime
     */
    public static String formatDateTime(long utcInMillis) {
        return DateFormatUtils.formatUTC(utcInMillis, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
    }

    /**
     * Converts a calendar to a date string representation suitable for, e.g., user trackers
     * @param cal
     * @return
     */
    public static String formatYearMonthDate(Calendar cal) {
        final String pattern = "yyMM"; // TODO make configurable?
        return DateFormatUtils.format(cal, pattern, TimeZone.getTimeZone("UTC"));
    }

    /**
     * Converts a calendar to a date string representation suitable for, e.g., blah and comment trackers
     * @param cal
     * @return
     */
    public static String formatYearMonthDateDate(Calendar cal) {
        final String pattern = "yyMMdd"; // TODO make configurable?
        return DateFormatUtils.format(cal, pattern, TimeZone.getTimeZone("UTC"));
    }

    /**
     * Checks whether the datetime string is in ISO format.
     *
     * @param isoDateTime The ISO datetime string
     * @return boolean  Returns true if the format is correct.
     */
    public static boolean checkISODateTime(String isoDateTime) {
        try {
            org.apache.commons.lang3.time.DateUtils.parseDate(isoDateTime, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
            return true;
        } catch (ParseException e) {
            logger.log(Level.WARNING, "failed ISO datetime check", e);
            return false;
        }
    }

    /**
     * Creates a Date out of an ISO datetime string.
     *
     * @param isoDateTime The ISO string representation of a date.
     * @return Date Returns the Date or null if the string can't be parsed as an ISO datetime
     */
    public static Date fromISODateTimeToUTC(String isoDateTime) throws ParseException {
        if (isoDateTime.lastIndexOf(".") != -1) {
            isoDateTime = isoDateTime.substring(0, isoDateTime.lastIndexOf("."));
        }
        return org.apache.commons.lang3.time.DateUtils.parseDate(
                isoDateTime,
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern(),
                DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
    }

    /**
     * Creates a Date out of a date (no time component) ISO string
     * @param isoDate  The ISO date
     * @return Date The date parsed out of the ISO date string
     */
    public static Date fromISODateToUTC(String isoDate) throws ParseException {
        return org.apache.commons.lang3.time.DateUtils.parseDate(
                isoDate,
                DateFormatUtils.ISO_DATE_FORMAT.getPattern(),
                DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.getPattern());
    }

    /**
     * Converts the string to a calendar
     * @param dateAsString A date string in the format "yyMMdd"
     * @return Calendar Returns the calendar represented by the string or the current calendar if the string is null
     */
    public final static Calendar convertToCalendar(String dateAsString) throws Exception {
        Calendar startDate = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (dateAsString != null) {
            try {
                startDate.setTime(org.apache.commons.lang3.time.DateUtils.parseDateStrictly(dateAsString, "yyMMdd"));
            } catch (ParseException e) {
                throw new Exception("Invalid start date=" + dateAsString + "'. Should be yyMMdd (e.g., 120827 for August 27, 2012)", e);
            }
        }
        return startDate;
    }
}
