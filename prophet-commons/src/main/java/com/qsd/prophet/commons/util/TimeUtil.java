package com.qsd.prophet.commons.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * @author zhengyu
 */
public class TimeUtil {

    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static String getTimeFormatString(Date date, String format) {
        SimpleDateFormat f = new SimpleDateFormat(format, Locale.ENGLISH);
        return f.format(date);
    }

    public static String getCurrentTime(String format) {
        String returnStr = null;
        SimpleDateFormat f = new SimpleDateFormat(format, Locale.ENGLISH);
        Date date = new Date();
        returnStr = f.format(date);
        return returnStr;
    }

    public static int getCurrentHour() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(System.currentTimeMillis()));
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMinute() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(System.currentTimeMillis()));
        return calendar.get(Calendar.MINUTE);
    }

    /**
     * 从礼拜天开始=1，礼拜六=7
     *
     * @return
     */
    public static int getCurrentWeekDay() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(System.currentTimeMillis()));
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public static int getStaticPointInPeriod(int period) {
        return ((getCurrentWeekDay() - 1) * 24 * 60 + (getCurrentHour()) * 60 + getCurrentMinute()) % period;
    }

    /**
     * 获取当前时间多少分钟之前的数据
     *
     * @param timeSpan 时间跨度
     * @return
     */
    public static String getTimeSpanBefore(int timeSpan) {
        return getTimeSpanBefore(timeSpan, YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 获取当前时间多少分钟之前的数据
     *
     * @param timeSpan 时间跨度
     * @return
     */
    public static String getTimeSpanBefore(int timeSpan, String format) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(System.currentTimeMillis() / (60000L) * (60000L)));
        calendar.add(Calendar.MINUTE, 0 - timeSpan);

        Date date = calendar.getTime();
        SimpleDateFormat f = new SimpleDateFormat(format, Locale.ENGLISH);
        return f.format(date);
    }

    /**
     * 获取当前时间多少分钟之前的数据，返回long类型字符串
     *
     * @param timeSpan 时间跨度
     * @return
     */
    public static long getLongTimeSpanBefore(int timeSpan) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(System.currentTimeMillis() / (60000L) * (60000L)));
        calendar.add(Calendar.MINUTE, 0 - timeSpan);

        Date date = calendar.getTime();
        return date.getTime();
    }

    public static boolean isTimeFormat(String timeStr, String timeFormat) {

        if (timeStr == null || timeStr.isEmpty()) {
            return false;
        }
        boolean result = true;
        try {
            SimpleDateFormat dFormat = new SimpleDateFormat(timeFormat, Locale.ENGLISH);
            dFormat.setLenient(false);
            dFormat.parse(timeStr);
        } catch (Throwable e) {
            result = false;
        }

        return result;
    }

    public static Long getLongTimeStamp(String timeStr, String timeFormat) {
        SimpleDateFormat f = new SimpleDateFormat(timeFormat, Locale.ENGLISH);
        Date date = null;
        try {
            date = f.parse(timeStr);
        } catch (Throwable e) {
            return null;
        }

        return date.getTime();
    }

    /**
     * 把毫秒数转换成最近的一分钟，向前一分钟取值
     *
     * @param timeStampMili
     * @return
     */
    public static long getTimeStampMinute(long timeStampMili) {
        return timeStampMili / 60000 * 60000;
    }

    /**
     * 将时间戳转换为分钟的字符串形式
     *
     * @param timeStampMili
     * @return
     */
    public static String getStringMinute(long timeStampMili) {
        SimpleDateFormat formatter = new SimpleDateFormat(YYYY_MM_DD_HH_MM);
        String timeMinuteString;
        try {
            timeMinuteString = formatter.format(new Date(timeStampMili));
        } catch (Exception e) {
            timeMinuteString = null;
        }
        return timeMinuteString;
    }
}
