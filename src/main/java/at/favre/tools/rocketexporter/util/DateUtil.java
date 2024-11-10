package at.favre.tools.rocketexporter.util;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

import java.text.ParseException;
import java.util.Date;

public class DateUtil {
    public static String toPersianDate(Date date, String dateFormat) {
        ULocale locale = new ULocale("@calendar=persian");
        Calendar calendar = Calendar.getInstance(locale);
        calendar.setTime(date);
        DateFormat df = new com.ibm.icu.text.SimpleDateFormat(dateFormat, locale);
        return df.format(calendar);
    }

    public static Date fromPersianDate(String date, String dateFormat) throws RuntimeException {
        ULocale locale = new ULocale("@calendar=persian");
        DateFormat df = new com.ibm.icu.text.SimpleDateFormat(dateFormat, locale);
        try {
            return df.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException("Incorrect format " + dateFormat);
        }
    }

}